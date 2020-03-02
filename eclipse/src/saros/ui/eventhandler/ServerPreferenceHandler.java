package saros.ui.eventhandler;

import org.eclipse.jface.preference.IPreferenceStore;
import saros.preferences.PreferenceConstants;
import saros.versioning.VersionManager;

public class ServerPreferenceHandler {

  public ServerPreferenceHandler(VersionManager versionManager, IPreferenceStore preferenceStore) {
    if (Boolean.getBoolean("saros.server.SUPPORTED")) {
      versionManager.setLocalInfo(PreferenceConstants.SERVER_SUPPORT, Boolean.TRUE.toString());
    }
  }
}
