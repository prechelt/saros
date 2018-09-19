package de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import de.fu_berlin.inf.ag_se.browser.extensions.IJQueryBrowser;
import de.fu_berlin.inf.ag_se.browser.html.ISelector;
import de.fu_berlin.inf.ag_se.browser.html.ISelector.NameSelector;
import de.fu_berlin.inf.dpp.stf.server.HTMLSTFRemoteObject;
import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteHTMLInputField;

public final class RemoteHTMLInputField extends HTMLSTFRemoteObject implements
    IRemoteHTMLInputField {

    private static final RemoteHTMLInputField INSTANCE = new RemoteHTMLInputField();
    private static final Logger log = Logger
        .getLogger(RemoteHTMLInputField.class);

    private IJQueryBrowser browser;
    private ISelector selector;

    public static RemoteHTMLInputField getInstance() {
        return INSTANCE;
    }

    @Override
    public void enter(String text) throws RemoteException {
        String name = getSelectorName();
        browser.val(selector, text);
        browser.run(String.format("view.setFieldValue('%s','%s')", name, text));
    }

    @Override
    public String getValue() throws RemoteException {
        String name = getSelectorName();
        Object value = browser.syncRun(String.format(
            "return view.getFieldValue('%s')", name));
        return value != null ? value.toString() : null;
    }

    @Override
    public void clear() throws RemoteException {
        enter("");
    }

    void setBrowser(IJQueryBrowser browser) {
        this.browser = browser;
    }

    void setSelector(ISelector selector) {
        this.selector = selector;
    }

    private String getSelectorName() {
        return ((NameSelector) selector).getName();
    }
}
