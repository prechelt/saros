package saros.communication.extensions;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import saros.misc.xstream.XStreamExtensionProvider;

/**
 * Packet containing data for exchanging version details. This packet extension is <b>NOT</b>
 * affected by the current Saros Extension protocol version because it must be always possible to
 * communicate with older and newer Saros Version to determine compatibility.
 *
 * <p>To offer the most generic solution possible this packet does not contain any detailed data but
 * allows exchanging variable data in it is string representation.
 */
@XStreamAlias(/* VersionExchangeExtension */ "VEREX")
public class VersionExchangeExtension {

  public static final Provider PROVIDER = new Provider();

  @XStreamAlias("data")
  private final Map<String, String> data;

  public VersionExchangeExtension(Map<String, String> data) {
    if (data == null) this.data = Collections.emptyMap();
    else this.data = new HashMap<>(data);
  }
  /**
   * Returns the data map.
   *
   * @return data map
   */
  public Map<String, String> getData() {
    return data;
  }

  public static class Provider extends XStreamExtensionProvider<VersionExchangeExtension> {

    private Provider() {
      super(SarosPacketExtension.EXTENSION_NAMESPACE, "verex", VersionExchangeExtension.class);
    }
  }
}
