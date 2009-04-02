package de.fu_berlin.inf.dpp.ui.actions;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.Action;
import org.picocontainer.Disposable;
import org.picocontainer.annotations.Inject;

import de.fu_berlin.inf.dpp.PreferenceConstants;
import de.fu_berlin.inf.dpp.Saros;
import de.fu_berlin.inf.dpp.User;
import de.fu_berlin.inf.dpp.editor.AbstractSharedEditorListener;
import de.fu_berlin.inf.dpp.editor.EditorManager;
import de.fu_berlin.inf.dpp.editor.ISharedEditorListener;
import de.fu_berlin.inf.dpp.project.AbstractSessionListener;
import de.fu_berlin.inf.dpp.project.AbstractSharedProjectListener;
import de.fu_berlin.inf.dpp.project.ISessionListener;
import de.fu_berlin.inf.dpp.project.ISessionManager;
import de.fu_berlin.inf.dpp.project.ISharedProject;
import de.fu_berlin.inf.dpp.project.ISharedProjectListener;
import de.fu_berlin.inf.dpp.ui.SarosUI;
import de.fu_berlin.inf.dpp.util.Util;

/**
 * Action to enter into FollowMode
 * 
 * TODO Rename to GlobalFollowModeAction
 */
public class FollowModeAction extends Action implements Disposable {

    public static final String ACTION_ID = FollowModeAction.class.getName();

    private static final Logger log = Logger.getLogger(FollowModeAction.class
        .getName());

    ISharedProjectListener roleChangeListener = new AbstractSharedProjectListener() {
        @Override
        public void roleChanged(User user, boolean replicated) {
            updateEnablement();
        }
    };

    ISessionListener sessionListener = new AbstractSessionListener() {
        @Override
        public void sessionStarted(ISharedProject sharedProject) {

            sharedProject.addListener(roleChangeListener);
            updateEnablement();

            /*
             * Automatically start follow mode at the beginning of a session if
             * Auto-Follow-Mode is enabled.
             */
            if (isEnabled()
                && Saros.getDefault().getPreferenceStore().getBoolean(
                    PreferenceConstants.AUTO_FOLLOW_MODE)) {
                run();
            }
        }

        @Override
        public void sessionEnded(ISharedProject sharedProject) {
            sharedProject.removeListener(roleChangeListener);
            updateEnablement();
        }
    };

    ISharedEditorListener editorListener = new AbstractSharedEditorListener() {
        @Override
        public void followModeChanged(boolean enabled) {
            setChecked(enabled);
            updateEnablement();

            // should not be disabled but checked
            assert (!isEnabled() && isChecked()) == false;

        }
    };

    @Inject
    ISessionManager sessionManager;

    public FollowModeAction() {
        super(null, AS_CHECK_BOX);

        setImageDescriptor(SarosUI.getImageDescriptor("/icons/monitor_add.png"));
        setToolTipText("Enable/disable follow mode");
        setId(ACTION_ID);

        Saros.getDefault().reinject(this);

        sessionManager.addSessionListener(sessionListener);
        EditorManager.getDefault().addSharedEditorListener(editorListener);

        updateEnablement();
    }

    /**
     * @review runSafe OK
     */
    @Override
    public void run() {
        Util.runSafeSWTSync(log, new Runnable() {
            public void run() {

                User toFollow = getNewToFollow();

                log.info("setFollowing to " + toFollow);
                EditorManager.getDefault().setFollowing(toFollow);
            }

            private User getNewToFollow() {
                ISharedProject project = sessionManager.getSharedProject();

                assert project != null;

                User following = EditorManager.getDefault().getFollowedUser();

                if (following != null) {
                    return null;
                } else {
                    for (User user : project.getParticipants()) {
                        if (user.equals(Saros.getDefault().getLocalUser()))
                            continue;
                        if (user.isDriver()) {
                            return user;
                        }
                    }
                    return null;
                }
            }
        });
    }

    protected boolean canFollow() {
        ISharedProject project = sessionManager.getSharedProject();

        if (project == null)
            return false;

        User following = EditorManager.getDefault().getFollowedUser();

        if (following != null) {
            // While following the button must be enabled to de-follow
            return true;
        }
        int drivers = 0;
        for (User user : project.getParticipants()) {
            if (user.equals(Saros.getDefault().getLocalUser()))
                continue;
            if (user.isDriver())
                drivers++;
        }

        return drivers == 1;
    }

    protected void updateEnablement() {
        setEnabled(canFollow());
    }

    public void dispose() {
        sessionManager.removeSessionListener(sessionListener);
        EditorManager.getDefault().removeSharedEditorListener(editorListener);
    }
}
