package saros.net.xmpp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.bitlet.weupnp.GatewayDevice;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import saros.net.stun.IStunService;
import saros.net.upnp.IUPnPService;
import saros.net.util.NetworkingUtils;
import saros.util.ThreadUtils;

/**
 * Access class for accessing the Smack Socks5 proxy. It supports UPNP and STUN to retrieve possible
 * Socks5 candidates and allows access to the local Smack Socks5 proxy behind gateways that supports
 * UPNP.
 */
class Socks5ProxySupport {

  private static final int STUN_DISCOVERY_TIMEOUT = 10000;

  // DO NOT CHANGE THE CONTENT OF THIS STRING, NEVER NEVER NEVER !!!
  private static final String PORT_MAPPING_DESCRIPTION = "Saros Socks5 TCP";

  private static final Logger log = Logger.getLogger(Socks5ProxySupport.class);

  private static final Object socks5AddressReplacementLock = new Object();

  private final IUPnPService upnpService;
  private final IStunService stunService;

  private final Semaphore addressRetrieval = new Semaphore(2);

  /** The current gateway device to use for port mapping or <code>null</code>. */
  private volatile GatewayDevice device;

  /** The current used Socks5 proxy or <code>null</code>. */
  private Socks5Proxy socks5Proxy;

  /**
   * There is so much magic involved in Smack. Performing a disconnect shuts the procy down so we
   * need to remember the port.
   */
  private int socks5ProxyPort;

  public Socks5ProxySupport(final IUPnPService upnpService, final IStunService stunService) {
    this.upnpService = upnpService;
    this.stunService = stunService;
  }

  /**
   * Enables the Socks5 proxy. If the port number is negative the logic will try to find an unused
   * port starting with the positive value of the port number up to 65535.
   *
   * @param port the port to use
   * @param proxyAddresses collection containing addresses that should be published as public
   *     addresses for connection purpose
   * @param gatewayDeviceId the ID an UPNP device for port mapping or <code>null</code>
   * @param useExternalGatewayDeviceAddress if <code>true</code> the public address of the device
   *     will be published
   * @param stunServerAddress address of a stun server to retrieve and publish public addresses or
   *     <code>null</code>
   *     @param stunServerPort port of the stun server, not used if <code>stunServerAddress<code> is <code>null</code>
   * @return <code>true</code> if the proxy was successfully started, <code>false</code> if it could
   *     not be started or is already running
   */
  public synchronized boolean enableProxy(
      final int port,
      final Collection<String> proxyAddresses,
      final String gatewayDeviceId,
      final boolean useExternalGatewayDeviceAddress,
      final String stunServerAddress,
      final int stunServerPort) {

    if (socks5Proxy != null) return false;

    SmackConfiguration.setLocalSocks5ProxyEnabled(true);
    SmackConfiguration.setLocalSocks5ProxyPort(port);

    final Socks5Proxy newSocks5Proxy = Socks5Proxy.getSocks5Proxy();
    socks5Proxy = newSocks5Proxy;

    if (socks5Proxy == null) {
      log.warn("failed to start Socks5 proxy on port: " + port);
      SmackConfiguration.setLocalSocks5ProxyEnabled(false);
      return false;
    }

    socks5ProxyPort = socks5Proxy.getPort();

    // Unlikely but the connection was already lost, but signal that the proxy as running.
    if (socks5ProxyPort <= 0) return true;

    // Remove any addresses that Smack already discovery because we use our own logic.
    socks5Proxy.replaceLocalAddresses(Collections.emptyList());

    log.info(
        "started Socks5 proxy on port: "
            + socks5Proxy.getPort()
            + " [listening on all interfaces]");

    final List<String> proxyAddressesToPublish = new ArrayList<>();

    if (proxyAddresses != null && proxyAddresses.isEmpty())
      log.warn("Socks5 preconfigured addresses list is empty, using autodetect mode");

    if (proxyAddresses == null || proxyAddresses.isEmpty())
      NetworkingUtils.getAllNonLoopbackLocalIPAddresses(true)
          .stream()
          .map(InetAddress::getHostAddress)
          .forEach(proxyAddressesToPublish::add);
    else proxyAddressesToPublish.addAll(proxyAddresses);

    addSocks5ProxyAddresses(socks5Proxy, proxyAddressesToPublish, true);

    if (stunService != null && stunServerAddress != null && !stunServerAddress.isEmpty()) {
      try {
        addressRetrieval.acquire(1);
      } catch (InterruptedException e) {
        log.warn("interupted while waiting to trigger a STUN discovery");
        Thread.currentThread().interrupt();
        return true;
      }

      ThreadUtils.runSafeAsync(
          "Saros-stun-discovery",
          log,
          () -> {
            try {
              discoverAndPublishStunAddresses(newSocks5Proxy, stunServerAddress, stunServerPort);
            } finally {
              addressRetrieval.release(1);
            }
          });
    }

    if (upnpService != null && gatewayDeviceId != null && !gatewayDeviceId.isEmpty()) {
      try {
        addressRetrieval.acquire(1);
      } catch (InterruptedException e) {
        log.warn("interupted while waiting to trigger a UPNP port mapping");
        Thread.currentThread().interrupt();
        return true;
      }

      ThreadUtils.runSafeAsync(
          "upnp-portmapping",
          log,
          () -> {
            try {
              mapPortAndPublishPublicAddress(
                  newSocks5Proxy, gatewayDeviceId, useExternalGatewayDeviceAddress);
            } finally {
              addressRetrieval.release(1);
            }
          });
    }

    return true;
  }

  /**
   * Stops the current Socks5 proxy if enabled and disables further usage of the Socks5 proxy. This
   * method can be safely called to just prevent the global usage of the Socks5 proxy.
   */
  public synchronized void disableProxy() {

    if (socks5Proxy == null) {
      SmackConfiguration.setLocalSocks5ProxyEnabled(false);
      return;
    }

    Socks5Proxy.getSocks5Proxy().stop();

    SmackConfiguration.setLocalSocks5ProxyEnabled(false);

    socks5Proxy = null;

    if (socks5ProxyPort < 0) return;

    log.info("stopped Socks5 proxy on port: " + socks5ProxyPort);

    boolean wasInterrupted = false;

    // we must wait here otherwise the device is not yet known

    while (true) {
      try {
        addressRetrieval.acquire(2);
        break;
      } catch (InterruptedException e) {
        wasInterrupted = true;
      }
    }

    try {

      if (device == null) return;

      if (!upnpService.isMapped(device, socks5ProxyPort, IUPnPService.TCP)) return;

      if (!upnpService.deletePortMapping(device, socks5ProxyPort, IUPnPService.TCP)) {
        log.warn(
            "failed to delete port mapping on device: "
                + device.getFriendlyName()
                + " ["
                + socks5ProxyPort
                + "|"
                + IUPnPService.TCP
                + "]");
      }

      log.info(
          "removed port mapping on device: "
              + device.getFriendlyName()
              + " ["
              + socks5ProxyPort
              + "|"
              + IUPnPService.TCP
              + "]");

    } finally {
      addressRetrieval.release(2);
      device = null;

      if (wasInterrupted) Thread.currentThread().interrupt();
    }
  }

  /* do not change the proxy parameter in all methods below with the instance variable as this code
  is not executed inside a synchronized block ! */

  private void mapPortAndPublishPublicAddress(
      final Socks5Proxy proxy,
      final String gatewayDeviceId,
      final boolean useExternalGatewayDeviceAddress) {

    assert (upnpService != null);

    final int socks5ProxyPort = proxy.getPort();

    if (socks5ProxyPort <= 0) return;

    final List<GatewayDevice> devices = upnpService.getGateways(false);

    if (devices == null) {
      log.warn("aborting UPNP port mapping due to network failure");
      return;
    }

    for (GatewayDevice currentDevice : devices) {
      if (gatewayDeviceId.equals(currentDevice.getUSN())) {
        device = currentDevice;
        break;
      }
    }

    if (device == null) {
      log.warn(
          "could not find gateway device with id: + "
              + gatewayDeviceId
              + " in the current network environment");
      return;
    }

    upnpService.deletePortMapping(device, socks5ProxyPort, IUPnPService.TCP);

    if (!upnpService.createPortMapping(
        device, socks5ProxyPort, IUPnPService.TCP, PORT_MAPPING_DESCRIPTION)) {

      log.warn(
          "failed to create port mapping on device: "
              + device.getFriendlyName()
              + " ["
              + socks5ProxyPort
              + "|"
              + IUPnPService.TCP
              + "]");

      device = null;
      return;
    }

    log.info(
        "added port mapping on device: "
            + device.getFriendlyName()
            + " ["
            + socks5ProxyPort
            + "|"
            + IUPnPService.TCP
            + "]");

    if (!useExternalGatewayDeviceAddress) return;

    final InetAddress externalAddress = upnpService.getExternalAddress(device);

    if (externalAddress != null)
      addSocks5ProxyAddresses(
          proxy, Collections.singletonList(externalAddress.getHostAddress()), true);
  }

  private void discoverAndPublishStunAddresses(
      final Socks5Proxy proxy, final String stunServerAddress, final int stunServerPort) {

    assert (stunService != null);

    final Collection<InetSocketAddress> addresses =
        stunService.discover(stunServerAddress, stunServerPort, STUN_DISCOVERY_TIMEOUT);

    if (addresses.isEmpty()) {
      log.warn(
          "could not discover any public address using STUN server "
              + stunServerAddress
              + " (port="
              + stunServerPort
              + ")");

      return;
    }

    // stun returns always literal IP addresses
    final List<String> discoveredAddresses =
        addresses.stream().map(InetSocketAddress::getHostString).collect(Collectors.toList());
    log.debug(
        "STUN discovery result for STUN server "
            + stunServerAddress
            + " (port="
            + stunServerPort
            + ") : "
            + discoveredAddresses);

    addSocks5ProxyAddresses(proxy, discoveredAddresses, true);
  }

  private static void addSocks5ProxyAddresses(
      final Socks5Proxy proxy, final Collection<String> addresses, boolean inFront) {

    synchronized (socks5AddressReplacementLock) {
      final List<String> newAddresses = new ArrayList<>();

      if (inFront) newAddresses.addAll(addresses);

      newAddresses.addAll(proxy.getLocalAddresses());

      if (!inFront) newAddresses.addAll(addresses);

      final List<String> distinctAddresses =
          newAddresses.stream().sequential().distinct().collect(Collectors.toList());

      log.info("Socks5 proxy - public published IP addresses : " + distinctAddresses);

      proxy.replaceLocalAddresses(
          newAddresses.stream().sequential().distinct().collect(Collectors.toList()));
    }
  }
}
