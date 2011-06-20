package de.fu_berlin.inf.dpp.stf.test.editing;

import static de.fu_berlin.inf.dpp.stf.client.tester.SarosTester.ALICE;
import static de.fu_berlin.inf.dpp.stf.client.tester.SarosTester.BOB;
import static org.junit.Assert.assertEquals;

import java.rmi.RemoteException;

import org.junit.BeforeClass;
import org.junit.Test;

import de.fu_berlin.inf.dpp.stf.annotation.TestLink;
import de.fu_berlin.inf.dpp.stf.client.StfTestCase;
import de.fu_berlin.inf.dpp.stf.client.util.Util;
import de.fu_berlin.inf.dpp.stf.test.Constants;

@TestLink(id = "Saros-87:Observer trying to type")
public class EditWithReadAccessOnlyTest extends StfTestCase {

    @BeforeClass
    public static void beforeClass() throws Exception {
        initTesters(ALICE, BOB);
        setUpWorkbench();
        setUpSaros();
    }

    @Test
    public void testEditingWithReadOnlyAccess() throws RemoteException {
        Util.setUpSessionWithAJavaProjectAndAClass("foo", "bar", "HelloWorld",
            ALICE, BOB);

        BOB.superBot().views().packageExplorerView()
            .waitUntilResourceIsShared(Constants.PROJECT1);

        ALICE.superBot().views().packageExplorerView()
            .selectClass("foo", "bar", "HelloWorld").open();

        BOB.superBot().views().packageExplorerView()
            .selectClass("foo", "bar", "HelloWorld").open();

        ALICE.remoteBot().waitUntilEditorOpen("HelloWorld.java");
        BOB.remoteBot().waitUntilEditorOpen("HelloWorld.java");

        String aliceEditorText = ALICE.remoteBot().editor("HelloWorld.java")
            .getText();

        ALICE.superBot().views().sarosView().selectParticipant(BOB.getJID())
            .restrictToReadOnlyAccess();

        ALICE.superBot().views().sarosView().selectParticipant(BOB.getJID())
            .waitUntilHasReadOnlyAccess();

        BOB.remoteBot().editor("HelloWorld.java").typeText("1234567890");
        String bobEditorText = BOB.remoteBot().editor("HelloWorld.java")
            .getText();

        assertEquals(aliceEditorText, bobEditorText);

    }
}
