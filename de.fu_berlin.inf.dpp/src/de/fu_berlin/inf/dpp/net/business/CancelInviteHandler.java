/**
 * 
 */
package de.fu_berlin.inf.dpp.net.business;

import org.apache.log4j.Logger;
import org.picocontainer.annotations.Inject;

import de.fu_berlin.inf.dpp.annotations.Component;
import de.fu_berlin.inf.dpp.invitation.InvitationProcess;
import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.net.internal.XMPPChatReceiver;
import de.fu_berlin.inf.dpp.net.internal.extensions.CancelInviteExtension;
import de.fu_berlin.inf.dpp.observables.InvitationProcessObservable;
import de.fu_berlin.inf.dpp.observables.SessionIDObservable;
import de.fu_berlin.inf.dpp.util.Util;

@Component(module = "net")
public class CancelInviteHandler extends CancelInviteExtension {

    private static Logger log = Logger.getLogger(CancelInviteHandler.class
        .getName());

    @Inject
    protected InvitationProcessObservable invitationProcesses;

    public CancelInviteHandler(SessionIDObservable sessionID,
        XMPPChatReceiver receiver) {
        super(sessionID);
        receiver.addPacketListener(this, getFilter());
    }

    @Override
    public void invitationCanceledReceived(JID sender, String errorMsg) {
        InvitationProcess process = invitationProcesses
            .getInvitationProcess(sender);
        if (process != null) {
            log.debug("Inv" + Util.prefix(sender)
                + ": Received invitation cancel message");
            process.remoteCancel(errorMsg);
        } else {
            log.warn("Inv[unkown user]: Received invitation cancel message");
        }
    }
}