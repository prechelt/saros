package saros.ui.eventhandler;

import org.eclipse.jface.preference.IPreferenceStore;
import saros.communication.info.InfoManager;
import saros.preferences.PreferenceConstants;

public class ServerPreferenceHandler {

  public ServerPreferenceHandler(InfoManager infoManager, IPreferenceStore preferenceStore) {
    if (Boolean.getBoolean("saros.server.SUPPORTED")) {
      infoManager.setLocalInfo(PreferenceConstants.SERVER_SUPPORT, Boolean.TRUE.toString());
    }
  }
}
