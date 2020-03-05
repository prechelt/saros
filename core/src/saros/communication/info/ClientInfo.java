package saros.communication.info;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

class ClientInfo {
  static final String VERSION_KEY = "version";

  private static final Logger log = Logger.getLogger(ClientInfo.class);

  private final Map<String, String> infos;
  private final Version version;

  private ClientInfo(Map<String, String> infos, Version version) {
    this.infos = infos;
    this.version = version;
  }

  static ClientInfo parseFeatures(String contact, Map<String, String> infos) {
    if (infos == null) {
      log.warn("contact: " + contact + ", no info data");
      return new ClientInfo(Collections.emptyMap(), Version.INVALID);
    }

    Version version;
    String versionString = infos.get(VERSION_KEY);
    if (versionString == null) {
      version = Version.INVALID;
      log.warn("contact: " + contact + ", remote version string not found in info data");
    } else {
      version = Version.parseVersion(versionString);
      if (version == Version.INVALID)
        log.warn("contact: " + contact + ", remote version string is invalid: " + versionString);
    }

    return new ClientInfo(new HashMap<>(infos), version);
  }

  Version getVersion() {
    return version;
  }

  String getInfo(String key) {
    return infos.get(key);
  }

  @Override
  public String toString() {
    return String.format("ClientInfo [version=%s, infos=%s]", version, infos);
  }
}
