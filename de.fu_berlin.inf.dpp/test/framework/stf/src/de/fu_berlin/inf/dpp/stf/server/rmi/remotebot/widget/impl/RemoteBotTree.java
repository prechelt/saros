package de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

import de.fu_berlin.inf.dpp.stf.server.StfRemoteObject;
import de.fu_berlin.inf.dpp.stf.server.bot.widget.ContextMenuHelper;
import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.impl.RemoteWorkbenchBot;
import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotMenu;
import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotTree;
import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotTreeItem;

public final class RemoteBotTree extends StfRemoteObject implements
    IRemoteBotTree {

    private static final Logger log = Logger.getLogger(RemoteBotTree.class);

    private static final RemoteBotTree INSTANCE = new RemoteBotTree();

    private SWTBotTree widget;

    public static RemoteBotTree getInstance() {
        return INSTANCE;
    }

    public IRemoteBotTree setWidget(SWTBotTree tree) {
        widget = tree;
        return this;
    }

    public IRemoteBotTree uncheckAllItems() {
        for (SWTBotTreeItem item : widget.getAllItems()) {
            while (item.isChecked())
                item.uncheck();
        }
        return this;
    }

    public IRemoteBotMenu contextMenu(String... texts) throws RemoteException {
        return RemoteBotMenu.getInstance().setWidget(
            ContextMenuHelper.getContextMenu(widget, texts));
    }

    public IRemoteBotTreeItem collapseNode(String nodeText)
        throws RemoteException {
        return RemoteBotTreeItem.getInstance().setWidget(
            widget.collapseNode(nodeText));
    }

    public IRemoteBotTreeItem expandNode(String nodeText, boolean recursive)
        throws RemoteException {
        return RemoteBotTreeItem.getInstance().setWidget(
            widget.expandNode(nodeText, recursive));
    }

    public IRemoteBotTreeItem expandNode(String... nodes)
        throws RemoteException {
        return RemoteBotTreeItem.getInstance().setWidget(
            widget.expandNode(nodes));
    }

    public IRemoteBotTree select(int... indices) throws RemoteException {
        return setWidget(widget.select(indices));
    }

    public IRemoteBotTree select(String... items) throws RemoteException {
        return setWidget(widget.select(items));
    }

    public IRemoteBotTree unselect() throws RemoteException {
        return setWidget(widget.unselect());
    }

    public IRemoteBotTreeItem selectTreeItem(String... pathToTreeItem)
        throws RemoteException {
        RemoteBotTreeItem.getInstance().setWidget(
            widget.expandNode(pathToTreeItem).select());
        RemoteBotTreeItem.getInstance().setSWTBotTree(widget);
        return RemoteBotTreeItem.getInstance();
    }

    public IRemoteBotTreeItem selectTreeItemWithRegex(String... regexNodes)
        throws RemoteException {
        assert widget != null : "the passed tree is null.";

        SWTBotTreeItem currentItem = null;
        SWTBotTreeItem[] allChildrenOfCurrentItem;

        for (String regex : regexNodes) {

            if (currentItem == null)
                allChildrenOfCurrentItem = widget.getAllItems();
            else
                allChildrenOfCurrentItem = currentItem.getItems();

            boolean itemWithRegexFound = false;

            for (SWTBotTreeItem child : allChildrenOfCurrentItem) {

                if (child.getText().matches(regex)) {
                    currentItem = child;
                    if (!child.isExpanded())
                        child.expand();
                    itemWithRegexFound = true;
                    break;
                }
            }

            if (!itemWithRegexFound) {
                throw new WidgetNotFoundException(
                    "tree item matching the regex '" + regex
                        + "' cannot be found. Nodes: "
                        + Arrays.asList(regexNodes));
            }
        }

        if (currentItem != null) {
            SWTBotTreeItem item = currentItem.select();
            RemoteBotTreeItem.getInstance().setWidget(item);
            RemoteBotTreeItem.getInstance().setSWTBotTree(widget);
            return RemoteBotTreeItem.getInstance();
        }

        throw new WidgetNotFoundException("unknown error: " + widget.getText()
            + ", " + Arrays.asList(regexNodes));

    }

    public IRemoteBotTreeItem selectTreeItemAndWait(String... pathToTreeItem)
        throws RemoteException {
        SWTBotTreeItem selectedTreeItem = null;
        for (String node : pathToTreeItem) {
            try {
                if (selectedTreeItem == null) {
                    waitUntilItemExists(node);
                    selectedTreeItem = widget.expandNode(node);
                } else {

                    RemoteBotTreeItem treeItem = RemoteBotTreeItem
                        .getInstance();
                    treeItem.setWidget(selectedTreeItem);
                    treeItem.setSWTBotTree(widget);
                    treeItem.waitUntilSubItemExists(node);
                    selectedTreeItem = selectedTreeItem.expandNode(node);
                }
            } catch (WidgetNotFoundException e) {
                log.error("tree item \"" + node + "\" not found", e);
            }
        }
        if (selectedTreeItem != null) {
            SWTBotTreeItem item = selectedTreeItem.select();
            RemoteBotTreeItem.getInstance().setWidget(item);
            RemoteBotTreeItem.getInstance().setSWTBotTree(widget);
            return RemoteBotTreeItem.getInstance();
        }

        throw new WidgetNotFoundException("unknown error: " + widget.getText()
            + ", " + Arrays.asList(pathToTreeItem));
    }

    /**********************************************
     * 
     * states
     * 
     **********************************************/
    public boolean hasItems() throws RemoteException {
        return widget.hasItems();
    }

    public int rowCount() throws RemoteException {
        return widget.rowCount();
    }

    public int selectionCount() throws RemoteException {
        return widget.selectionCount();
    }

    public int columnCount() throws RemoteException {
        return widget.columnCount();
    }

    public List<String> columns() throws RemoteException {
        return widget.columns();
    }

    public List<String> getTextOfItems() throws RemoteException {
        List<String> allItemTexts = new ArrayList<String>();
        for (SWTBotTreeItem item : widget.getAllItems()) {
            allItemTexts.add(item.getText());
        }
        return allItemTexts;
    }

    public boolean existsSubItem(String treeItemText) throws RemoteException {
        return getTextOfItems().contains(treeItemText);
    }

    public boolean existsSubItemWithRegex(String regex) throws RemoteException {
        for (String subItem : getTextOfItems()) {
            if (subItem.matches(regex))
                return true;
        }
        return false;
    }

    /**********************************************
     * 
     * waits until
     * 
     **********************************************/
    public void waitUntilItemExists(final String itemText)
        throws RemoteException {

        RemoteWorkbenchBot.getInstance().waitUntil(new DefaultCondition() {
            public boolean test() throws Exception {
                return existsSubItem(itemText);
            }

            public String getFailureMessage() {
                return "tree '" + widget.getText()
                    + "' does not contain the treeItem: " + itemText;
            }
        });
    }

    public void waitUntilItemNotExists(final String itemText)
        throws RemoteException {
        RemoteWorkbenchBot.getInstance().waitUntil(new DefaultCondition() {
            public boolean test() throws Exception {
                return !existsSubItem(itemText);
            }

            public String getFailureMessage() {
                return "tree '" + widget.getText()
                    + "' still contains the treeItem: " + itemText;
            }
        });
    }

}
