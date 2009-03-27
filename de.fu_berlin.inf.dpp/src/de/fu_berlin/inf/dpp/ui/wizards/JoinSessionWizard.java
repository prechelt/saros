/*
 * DPP - Serious Distributed Pair Programming
 * (c) Freie Universitaet Berlin - Fachbereich Mathematik und Informatik - 2006
 * (c) Riad Djemili - 2006
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 1, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package de.fu_berlin.inf.dpp.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;

import de.fu_berlin.inf.dpp.PreferenceConstants;
import de.fu_berlin.inf.dpp.Saros;
import de.fu_berlin.inf.dpp.invitation.IIncomingInvitationProcess;
import de.fu_berlin.inf.dpp.invitation.IInvitationProcess.IInvitationUI;
import de.fu_berlin.inf.dpp.invitation.IInvitationProcess.State;
import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.util.Util;

/**
 * A wizard that guides the user through an incoming invitiation process.
 * 
 * Todo:
 * 
 * o Automatically switch to follow mode
 * 
 * o Suggest if the project is a CVS project that the user checks it out and
 * offers an option to transfer the settings
 * 
 * @author rdjemili
 */
public class JoinSessionWizard extends Wizard {

    private static final Logger log = Logger.getLogger(JoinSessionWizard.class
        .getName());

    ShowDescriptionPage descriptionPage;

    EnterProjectNamePage namePage;

    WizardDialogAccessable wizardDialog;

    IIncomingInvitationProcess process;

    public JoinSessionWizard(IIncomingInvitationProcess process) {
        this.process = process;

        setWindowTitle("Session Invitation");
        setHelpAvailable(false);
        setNeedsProgressMonitor(true);
    }

    public IInvitationUI getInvitationUI() {
        return new IInvitationUI() {

            public void cancel(final JID jid, final String errorMsg,
                final boolean replicated) {
                Util.runSafeSWTAsync(log, new Runnable() {
                    public void run() {

                        if (errorMsg != null) {
                            MessageDialog.openError(getShell(),
                                "Invitation aborted",
                                "Could not complete invitation with "
                                    + jid.getBase()
                                    + " because an error occurred (" + errorMsg
                                    + ")");
                        } else {
                            // errorMsg == null means canceled either by us or
                            // peer
                            if (replicated) {
                                MessageDialog.openInformation(getShell(),
                                    "Invitation cancelled",
                                    "Invitation was cancelled by peer.");
                            }
                        }

                        if (replicated) {
                            /*
                             * TODO The entanglement between UI and process is
                             * too complicated to sort out, how to close this
                             * dialog when it is currently executing the
                             * finishing synchronization
                             */
                            wizardDialog.close();
                        }
                    }
                });
            }

            public void runGUIAsynch(Runnable runnable) {
                // TODO this cannot be ignored an InvitationUI like the
                // JoinSessionWizard need to implement this
            }

            public void updateInvitationProgress(JID jid) {
                // ignored, not needed atm
            }
        };
    }

    @Override
    public void createPageControls(Composite pageContainer) {
        this.descriptionPage.createControl(pageContainer);
        // create namePage lazily
    }

    @Override
    public void addPages() {
        this.descriptionPage = new ShowDescriptionPage(this);
        this.namePage = new EnterProjectNamePage(this);

        addPage(this.descriptionPage);
        addPage(this.namePage);
    }

    @Override
    public boolean performFinish() {

        if (this.process.getState() == State.CANCELED) {
            return true;
        }

        final IProject source = this.namePage.getSourceProject();
        final String target = this.namePage.getTargetProjectName();

        try {
            getContainer().run(true, true, new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException {

                    JoinSessionWizard.this.process.accept(source, target,
                        monitor);
                }
            });
        } catch (InvocationTargetException e) {
            log.warn("Exception while requesting remote file list", e);
        } catch (InterruptedException e) {
            log.debug("Request of remote file list canceled/interrupted", e);
        }

        return true;
    }

    @Override
    public boolean performCancel() {
        try {
            this.process.cancel(null, false);
        } catch (RuntimeException e) {
            log.error("Failed to cancel process: ", e);
        }

        return true;
    }

    public void setWizardDlg(WizardDialogAccessable wd) {
        this.wizardDialog = wd;
    }

    public boolean isAutoAcceptInvitation() {
        return Saros.getDefault().getPreferenceStore().getBoolean(
            PreferenceConstants.AUTO_ACCEPT_INVITATION);
    }

    public void pressWizardButton(final int buttonID) {
        Util.runSafeAsync(log, new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.warn("Internal error: ", e);
                    return;
                }
                Util.runSafeSWTAsync(log, new Runnable() {
                    public void run() {
                        wizardDialog.buttonPressed(buttonID);
                    }
                });
            }
        });
    }
}
