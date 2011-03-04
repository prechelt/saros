package de.fu_berlin.inf.dpp.stf.client.testProject.testsuits.initialising;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fu_berlin.inf.dpp.stf.client.testProject.testsuits.STFTest;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.STFBotShell;

public class TestHandleContacts extends STFTest {

    /**
     * Preconditions:
     * <ol>
     * <li>init Alice</li>
     * <li>init Bob</li>
     * </ol>
     * 
     * @throws RemoteException
     */
    @BeforeClass
    public static void runBeforeClass() throws RemoteException {
        initTesters(TypeOfTester.ALICE, TypeOfTester.BOB);
        setUpWorkbench();
        setUpSaros();
    }

    @After
    public void runAfterEveryTest() throws RemoteException {
        resetBuddies();
    }

    // FIXME these testAddContact assumes that testRemoveContact succeeds
    // FIXME all the other tests in the suite would fail if testAddContact fails

    /**
     * Steps:
     * <ol>
     * <li>bob delete buddy alice.</li>
     * </ol>
     * 
     * Result:
     * <ol>
     * <li>bob and alice don't contact each other.</li>
     * </ol>
     * 
     * @throws RemoteException
     */
    @Test
    public void testBobRemoveBuddyAlice() throws RemoteException {
        assertTrue(alice.sarosBot().views().buddiesView()
            .hasBuddy(bob.getJID()));
        assertTrue(bob.sarosBot().views().buddiesView()
            .hasBuddy(alice.getJID()));
        deleteBuddies(bob, alice);
        assertFalse(bob.sarosBot().views().buddiesView()
            .hasBuddy(alice.getJID()));
        assertFalse(alice.sarosBot().views().buddiesView()
            .hasBuddy(bob.getJID()));
    }

    /**
     * Steps:
     * <ol>
     * <li>alice delete buddy bob.</li>
     * </ol>
     * 
     * Result:
     * <ol>
     * <li>bob and alice don't contact each other.</li>
     * </ol>
     * 
     * @throws RemoteException
     */
    @Test
    public void testAliceRemoveBuddyBob() throws RemoteException {
        assertTrue(alice.sarosBot().views().buddiesView()
            .hasBuddy(bob.getJID()));
        assertTrue(bob.sarosBot().views().buddiesView()
            .hasBuddy(alice.getJID()));
        deleteBuddies(alice, bob);
        assertFalse(bob.sarosBot().views().buddiesView()
            .hasBuddy(alice.getJID()));
        assertFalse(alice.sarosBot().views().buddiesView()
            .hasBuddy(bob.getJID()));
    }

    /**
     * Steps:
     * <ol>
     * <li>alice delete buddy bob first and then add bob.</li>
     * </ol>
     * 
     * Result:
     * <ol>
     * <li>bob and alice contact each other.</li>
     * </ol>
     * 
     * @throws RemoteException
     */
    @Test
    public void testAliceAddBuddyBob() throws RemoteException {
        deleteBuddies(alice, bob);
        addBuddies(alice, bob);
        assertTrue(bob.sarosBot().views().buddiesView()
            .hasBuddy(alice.getJID()));
        assertTrue(alice.sarosBot().views().buddiesView()
            .hasBuddy(bob.getJID()));
    }

    /**
     * Steps:
     * <ol>
     * <li>bob delete buddy alice first and then add alice again.</li>
     * </ol>
     * 
     * Result:
     * <ol>
     * <li>bob and alice contact each other.</li>
     * </ol>
     * 
     * @throws RemoteException
     */
    @Test
    public void testBobAddBuddyAlice() throws RemoteException {
        deleteBuddies(bob, alice);
        addBuddies(bob, alice);
        assertTrue(bob.sarosBot().views().buddiesView()
            .hasBuddy(alice.getJID()));
        assertTrue(alice.sarosBot().views().buddiesView()
            .hasBuddy(bob.getJID()));
    }

    /**
     * Steps:
     * <ol>
     * <li>alice click toolbar button "Add a new contact".</li>
     * <li>alice enter invalid contact name in the popup window "New contact"</li>
     * </ol>
     * 
     * Result:
     * <ol>
     * <li>alice should get error message "Contact look up failed".</li>
     * </ol>
     * 
     * @throws RemoteException
     */
    @Test
    public void testAddNoValidContact() throws RemoteException {
        alice.bot().view(VIEW_SAROS_BUDDIES).toolbarButton(TB_ADD_A_NEW_BUDDY)
            .click();
        Map<String, String> labelsAndTexts = new HashMap<String, String>();
        labelsAndTexts.put("XMPP/Jabber ID", "bob@bla");

        alice.bot().waitUntilShellIsOpen(SHELL_ADD_BUDDY);
        STFBotShell shell = alice.bot().shell(SHELL_ADD_BUDDY);
        shell.activate();
        shell.bot().comboBoxWithLabel(LABEL_XMPP_JABBER_ID).setText("bob@bla");
        shell.bot().button(FINISH).click();
        alice.bot().waitUntilShellIsOpen(SHELL_SERVER_NOT_FOUND);
        assertTrue(alice.bot().shell(SHELL_SERVER_NOT_FOUND).isActive());
        alice.bot().shell(SHELL_SERVER_NOT_FOUND).confirm(NO);

    }

}
