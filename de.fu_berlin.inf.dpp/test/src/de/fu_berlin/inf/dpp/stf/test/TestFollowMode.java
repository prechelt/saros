package de.fu_berlin.inf.dpp.stf.test;

import static org.junit.Assert.assertTrue;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.stf.sarosswtbot.BotConfiguration;
import de.fu_berlin.inf.dpp.stf.sarosswtbot.Musician;
import de.fu_berlin.inf.dpp.stf.sarosswtbot.SarosConstant;

public class TestFollowMode {
    // bots
    protected static Musician alice;
    protected static Musician bob;

    @BeforeClass
    public static void configureInvitee() throws RemoteException,
        NotBoundException {

        alice = new Musician(new JID(BotConfiguration.JID_ALICE),
            BotConfiguration.PASSWORD_ALICE, BotConfiguration.HOST_ALICE,
            BotConfiguration.PORT_ALICE);
        alice.initBot();
        alice.newProjectWithClass(BotConfiguration.PROJECTNAME,
            BotConfiguration.PACKAGENAME, BotConfiguration.CLASSNAME);

        bob = new Musician(new JID(BotConfiguration.JID_BOB),
            BotConfiguration.PASSWORD_BOB, BotConfiguration.HOST_BOB,
            BotConfiguration.PORT_BOB);
        bob.initBot();

        alice.buildSession(bob, BotConfiguration.PROJECTNAME,
            SarosConstant.SHARE_PROJECT, SarosConstant.CREATE_NEW_PROJECT);

    }

    @AfterClass
    public static void cleanupBob() throws RemoteException {
        bob.xmppDisconnect();
        bob.deleteResource(BotConfiguration.PROJECTNAME);
    }

    @AfterClass
    public static void cleanupAice() throws RemoteException {
        alice.xmppDisconnect();
        alice.deleteResource(BotConfiguration.PROJECTNAME);
    }

    @After
    public void StopFollowMode() throws RemoteException {
        if (bob.isInFollowMode(alice))
            bob.clickCMStopfollowingThisUserInSPSView(alice);
        if (alice.isInFollowMode(bob))
            alice.clickCMStopfollowingThisUserInSPSView(bob);
    }

    @Test
    public void testBobFollowAlice() throws RemoteException {
        alice.setTextInJavaEditor(BotConfiguration.CONTENTPATH,
            BotConfiguration.PROJECTNAME, BotConfiguration.PACKAGENAME,
            BotConfiguration.CLASSNAME);

        bob.followUser(alice);
        bob.waitUntilJavaEditorActive(BotConfiguration.CLASSNAME);
        assertTrue(bob.isInFollowMode(alice));
        assertTrue(bob.isJavaEditorActive(BotConfiguration.CLASSNAME));

        String textFromInviter = alice.getTextOfJavaEditor(
            BotConfiguration.PROJECTNAME, BotConfiguration.PACKAGENAME,
            BotConfiguration.CLASSNAME);
        bob.waitUntilFileEqualWithFile(BotConfiguration.PROJECTNAME,
            BotConfiguration.PACKAGENAME, BotConfiguration.CLASSNAME,
            textFromInviter);
        String textFormInvitee = bob.getTextOfJavaEditor(
            BotConfiguration.PROJECTNAME, BotConfiguration.PACKAGENAME,
            BotConfiguration.CLASSNAME);

        assertTrue(textFromInviter.equals(textFormInvitee));

        alice.newJavaClassInProject(BotConfiguration.PROJECTNAME,
            BotConfiguration.PACKAGENAME, BotConfiguration.CLASSNAME2);
        bob.waitUntilJavaEditorActive(BotConfiguration.CLASSNAME2);
        assertTrue(bob.isJavaEditorActive(BotConfiguration.CLASSNAME2));
    }

    @Test
    public void testAliceFollowBob() throws RemoteException {
        alice.followUser(bob);
        assertTrue(alice.isInFollowMode(bob));
        bob.activateJavaEditor(BotConfiguration.CLASSNAME);
        alice.waitUntilJavaEditorActive(BotConfiguration.CLASSNAME);
        assertTrue(alice.isJavaEditorActive(BotConfiguration.CLASSNAME));
    }
}
