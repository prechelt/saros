package de.fu_berlin.inf.dpp.stf.test.stf.basicwidget;

import static de.fu_berlin.inf.dpp.stf.client.tester.SarosTester.ALICE;
import static de.fu_berlin.inf.dpp.stf.client.tester.SarosTester.BOB;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.CM_RENAME;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.MENU_CLOSE;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.MENU_FILE;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.MENU_NEW;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.MENU_OTHER;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.MENU_PACKAGE;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.MENU_PREFERENCES;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.MENU_SAROS;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.MENU_SHOW_VIEW;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.MENU_WINDOW;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.NODE_ANNOTATIONS;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.NODE_BUDDIES;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.NODE_CONSOLE;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.NODE_EDITORS;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.NODE_GENERAL;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.NODE_SAROS;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.NODE_TEXT_EDITORS;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.SHELL_NEW_JAVA_PACKAGE;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.SHELL_PREFERNCES;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.SHELL_SET_NEW_NICKNAME;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.SHELL_SHOW_VIEW;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.SRC;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.SUFFIX_JAVA;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.TB_COLLAPSE_ALL;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.VIEW_PACKAGE_EXPLORER;
import static de.fu_berlin.inf.dpp.stf.shared.Constants.VIEW_SAROS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fu_berlin.inf.dpp.stf.client.StfTestCase;
import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotShell;
import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotView;
import de.fu_berlin.inf.dpp.stf.test.Constants;
import de.fu_berlin.inf.dpp.ui.preferencePages.GeneralPreferencePage;

public class BasicWidgetTreeTest extends StfTestCase {

    @BeforeClass
    public static void selectTesters() throws Exception {
        select(ALICE, BOB);
    }

    @Before
    public void beforeEveryTest() throws RemoteException {
        closeAllShells();
    }

    @After
    public void afterEveryTest() throws RemoteException {
        clearWorkspaces();
    }

    @Test
    public void existsTreeItemInShell() throws RemoteException {
        ALICE.remoteBot().menu(MENU_WINDOW).menu(MENU_SHOW_VIEW)
            .menu(MENU_OTHER).click();
        ALICE.remoteBot().waitUntilShellIsOpen(SHELL_SHOW_VIEW);
        IRemoteBotShell shell = ALICE.remoteBot().shell(SHELL_SHOW_VIEW);
        shell.activate();
        assertTrue(shell.bot().tree().selectTreeItem(NODE_GENERAL)
            .existsSubItem(NODE_CONSOLE));

    }

    @Test
    public void existsTreeItemInShell2() throws RemoteException {
        ALICE.remoteBot().menu(MENU_SAROS).menu(MENU_PREFERENCES).click();
        ALICE.remoteBot().waitUntilShellIsOpen(SHELL_PREFERNCES);
        IRemoteBotShell shell = ALICE.remoteBot().shell(SHELL_PREFERNCES);
        shell.activate();
        assertTrue(shell.bot().tree()
            .selectTreeItem(NODE_GENERAL, NODE_EDITORS, NODE_TEXT_EDITORS)
            .existsSubItem(NODE_ANNOTATIONS));
    }

    @Test
    public void existsTreeItemWithRegexsInShell() throws RemoteException {
        ALICE.remoteBot().menu(MENU_SAROS).menu(MENU_PREFERENCES).click();

        ALICE.remoteBot().waitUntilShellIsOpen(SHELL_PREFERNCES);
        IRemoteBotShell shell = ALICE.remoteBot().shell(SHELL_PREFERNCES);
        shell.activate();
        assertTrue(shell.bot().tree()
            .selectTreeItem(NODE_GENERAL, NODE_EDITORS, NODE_TEXT_EDITORS)
            .existsSubItemWithRegex(NODE_ANNOTATIONS));
    }

    @Test
    public void existsTreeItemInView() throws RemoteException {
        ALICE
            .superBot()
            .views()
            .packageExplorerView()
            .tree()
            .newC()
            .javaProjectWithClasses(Constants.PROJECT1, Constants.PKG1,
                Constants.CLS1);
        IRemoteBotView view = ALICE.remoteBot().view(VIEW_PACKAGE_EXPLORER);
        view.show();
        assertTrue(view.bot().tree().existsSubItem(Constants.PROJECT1));

        assertTrue(ALICE.superBot().views().packageExplorerView().tree()
            .existsWithRegex(Constants.PROJECT1));
        assertTrue(view.bot().tree()
            .selectTreeItem(Constants.PROJECT1, SRC, Constants.PKG1)
            .existsSubItem(Constants.CLS1 + SUFFIX_JAVA));
        ALICE.superBot().views().packageExplorerView()
            .selectPkg(Constants.PROJECT1, Constants.PKG1)
            .existsWithRegex(Constants.CLS1_SUFFIX);
    }

    @Test
    public void existsTreeItemWithRegexsInView() throws RemoteException {
        ALICE
            .superBot()
            .views()
            .packageExplorerView()
            .tree()
            .newC()
            .javaProjectWithClasses(Constants.PROJECT1, Constants.PKG1,
                Constants.CLS1);
        IRemoteBotView view = ALICE.remoteBot().view(VIEW_PACKAGE_EXPLORER);
        view.show();

        assertTrue(view.bot().tree()
            .existsSubItemWithRegex(Pattern.quote(Constants.PROJECT1) + ".*"));
        assertTrue(ALICE.superBot().views().packageExplorerView().tree()
            .existsWithRegex(Constants.PROJECT1));
        assertTrue(view.bot().tree().selectTreeItem(Constants.PROJECT1, SRC)
            .existsSubItemWithRegex(Pattern.quote(Constants.PKG1) + ".*"));

        assertTrue(ALICE.superBot().views().packageExplorerView()
            .selectSrc(Constants.PROJECT1).existsWithRegex(Constants.PKG1));
        assertTrue(view.bot().tree()
            .selectTreeItem(Constants.PROJECT1, SRC, Constants.PKG1)
            .existsSubItemWithRegex(Pattern.quote(Constants.CLS1) + ".*"));

        assertTrue(ALICE.superBot().views().packageExplorerView()
            .selectPkg(Constants.PROJECT1, Constants.PKG1)
            .existsWithRegex(Constants.CLS1_SUFFIX));
        ALICE.superBot().views().packageExplorerView().tree().newC()
            .cls(Constants.PROJECT1, Constants.PKG1, Constants.CLS2);

        assertTrue(view.bot().tree()
            .selectTreeItem(Constants.PROJECT1, SRC, Constants.PKG1)
            .existsSubItemWithRegex(Pattern.quote(Constants.CLS2) + ".*"));
        assertTrue(ALICE.superBot().views().packageExplorerView()
            .selectPkg(Constants.PROJECT1, Constants.PKG1)
            .existsWithRegex(Constants.CLS2_SUFFIX));
    }

    @Test
    public void existsTreeItemWithRegexsInView2() throws RemoteException {
        ALICE.superBot().views().sarosView()
            .connectWith(ALICE.getJID(), ALICE.getPassword());
        assertTrue(ALICE.remoteBot().view(VIEW_SAROS).bot().tree()
            .selectTreeItem(NODE_BUDDIES)
            .existsSubItemWithRegex(Pattern.quote(BOB.getName()) + ".*"));
    }

    @Test
    public void selectTreeItemInView() throws RemoteException {
        ALICE
            .superBot()
            .views()
            .packageExplorerView()
            .tree()
            .newC()
            .javaProjectWithClasses(Constants.PROJECT1, Constants.PKG1,
                Constants.CLS1);
        ALICE.remoteBot().view(VIEW_PACKAGE_EXPLORER).show();
        assertTrue(ALICE.remoteBot().isEditorOpen(Constants.CLS1_SUFFIX));
        ALICE.remoteBot().view(VIEW_PACKAGE_EXPLORER)
            .toolbarButtonWithRegex(TB_COLLAPSE_ALL).click();
        ALICE
            .remoteBot()
            .view(VIEW_PACKAGE_EXPLORER)
            .bot()
            .tree()
            .selectTreeItem(Constants.PROJECT1, SRC, Constants.PKG1,
                Constants.CLS1_SUFFIX);
        ALICE.remoteBot().menu(MENU_FILE).menu(MENU_CLOSE).click();

        assertFalse(ALICE.remoteBot().isEditorOpen(Constants.CLS1_SUFFIX));
    }

    @Test
    public void selectTreeItemInShell() throws RemoteException {
        ALICE.remoteBot().menu(MENU_SAROS).menu(MENU_PREFERENCES).click();

        ALICE.remoteBot().waitUntilShellIsOpen(SHELL_PREFERNCES);
        IRemoteBotShell shell = ALICE.remoteBot().shell(SHELL_PREFERNCES);
        shell.activate();
        shell.bot().tree().selectTreeItem(NODE_SAROS);
        assertTrue(shell
            .bot()
            .buttonInGroup(GeneralPreferencePage.CHANGE_BTN_TEXT,
                GeneralPreferencePage.ACCOUNT_GROUP_TITLE).isVisible());
    }

    @Test
    public void selectTreeItemWithRegexs() throws RemoteException {
        ALICE.superBot().views().packageExplorerView().tree().newC()
            .javaProject(Constants.SVN_PROJECT_COPY);

        ALICE
            .superBot()
            .views()
            .packageExplorerView()
            .selectProject(Constants.SVN_PROJECT_COPY)
            .team()
            .shareProjectUsingSpecifiedFolderName(Constants.SVN_REPOSITORY_URL,
                Constants.SVN_PROJECT_PATH);

        IRemoteBotView view = ALICE.remoteBot().view(VIEW_PACKAGE_EXPLORER);
        ALICE.remoteBot().sleep(1000);

        view.show();
        view.bot()
            .tree()
            .selectTreeItemWithRegex(
                Pattern.quote(Constants.SVN_PROJECT_COPY) + ".*",
                Pattern.quote(SRC) + ".*",
                Pattern.quote(Constants.SVN_PKG) + ".*",
                Pattern.quote(Constants.SVN_CLS1) + ".*");
    }

    @Test
    public void existsContextOfTreeItemInView() throws RemoteException {
        ALICE.superBot().views().sarosView()
            .connectWith(ALICE.getJID(), ALICE.getPassword());
        assertTrue(ALICE.remoteBot().view(VIEW_SAROS).bot().tree()
            .selectTreeItemWithRegex(NODE_BUDDIES, BOB.getName() + ".*")
            .existsContextMenu(CM_RENAME));
    }

    @Test
    public void existsSubmenuOfContextOfTreeItemInView() throws RemoteException {
        ALICE.superBot().views().packageExplorerView().tree().newC()
            .javaProject(Constants.PROJECT1);
        String[] contextNames = { MENU_NEW, MENU_PACKAGE };
        assertTrue(ALICE.remoteBot().view(VIEW_PACKAGE_EXPLORER).bot().tree()
            .selectTreeItem(Constants.PROJECT1).existsContextMenu(contextNames));

    }

    @Test
    public void isContextOfTreeItemInViewEnabled() throws RemoteException {
        ALICE.superBot().views().sarosView()
            .connectWith(ALICE.getJID(), ALICE.getPassword());
        ALICE.superBot().views().sarosView().waitUntilIsConnected();
        assertTrue(ALICE.remoteBot().view(VIEW_SAROS).bot().tree()
            .selectTreeItemWithRegex(NODE_BUDDIES, BOB.getName() + ".*")
            .isContextMenuEnabled(CM_RENAME));
    }

    @Test
    public void clickContextsOfTreeItemInView() throws RemoteException {
        ALICE.superBot().views().sarosView()
            .connectWith(ALICE.getJID(), ALICE.getPassword());
        ALICE.remoteBot().view(VIEW_SAROS).bot().tree()
            .selectTreeItemWithRegex(NODE_BUDDIES, BOB.getName() + ".*")
            .contextMenus(CM_RENAME).click();
        assertTrue(ALICE.remoteBot().isShellOpen(SHELL_SET_NEW_NICKNAME));
    }

    @Test
    public void clickSubMenuOfContextsOfTreeItemInView() throws RemoteException {
        ALICE.superBot().views().packageExplorerView().tree().newC()
            .javaProject(Constants.PROJECT1);

        ALICE.remoteBot().view(VIEW_PACKAGE_EXPLORER).bot().tree()
            .selectTreeItem(Constants.PROJECT1)
            .contextMenus(MENU_NEW, MENU_PACKAGE).click();

        ALICE.remoteBot().waitUntilShellIsOpen(SHELL_NEW_JAVA_PACKAGE);
        assertTrue(ALICE.remoteBot().shell(SHELL_NEW_JAVA_PACKAGE).activate());
    }
}
