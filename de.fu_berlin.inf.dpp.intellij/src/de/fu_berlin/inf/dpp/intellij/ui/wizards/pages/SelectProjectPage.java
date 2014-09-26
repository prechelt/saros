/*
 *
 *  DPP - Serious Distributed Pair Programming
 *  (c) Freie Universität Berlin - Fachbereich Mathematik und Informatik - 2010
 *  (c) NFQ (www.nfq.com) - 2014
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 1, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * /
 */

package de.fu_berlin.inf.dpp.intellij.ui.wizards.pages;

import de.fu_berlin.inf.dpp.intellij.ui.Messages;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Selects local project.
 * FIXME: Add tabs for multiple projects.
 */
public class SelectProjectPage extends AbstractWizardPage {

    private DocumentListener projectNameListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent documentEvent) {
            checkForNewSameNameAsOld();
        }

        @Override
        public void removeUpdate(DocumentEvent documentEvent) {
            checkForNewSameNameAsOld();
        }

        @Override
        public void changedUpdate(DocumentEvent documentEvent) {
            checkForNewSameNameAsOld();
        }
    };

    private void checkForNewSameNameAsOld() {
        String newName = getLocalProjectName();
        if (!newName.equals(projectName)) {
            wizard.disableNextButton();
        } else {
            wizard.enableNextButtion();
        }
    }

    private enum ProjectOptions {
        NEW_PROJECT, EXISTING_PROJECT
    }

    private JFileChooser fileChooser;

    private JRadioButton rdbCreateNewProject;
    private JRadioButton rdbUseExistingProject;

    private JTextField fldNewProjectName;
    private JTextField fldExistingProjectName;

    private JButton browseButton;

    private String projectName;

    private JLabel lblNewProject;
    private JLabel lblExistingProject;

    public SelectProjectPage(String id, String projectName,
        String newProjectName, String projectBase,
        PageActionListener pageListener) {
        super(id, pageListener);
        this.projectName = projectName;
        create(newProjectName, projectBase);
    }

    private void create(String newProjectName, String projectBase) {

        JTabbedPane tabbedPane = new JTabbedPane();
        add(tabbedPane);

        JPanel pnlProject = new JPanel();
        tabbedPane.addTab(projectName, pnlProject);

        pnlProject.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(8, 12, 8, 12);

        rdbCreateNewProject = new JRadioButton(
            Messages.EnterProjectNamePage_create_new_project);

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.LINE_START;
        pnlProject.add(rdbCreateNewProject, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.LINE_END;
        lblNewProject = new JLabel(Messages.EnterProjectNamePage_project_name);
        pnlProject.add(lblNewProject, c);

        fldNewProjectName = new JTextField();
        fldNewProjectName.setText(newProjectName);
        fldNewProjectName.getDocument()
            .addDocumentListener(projectNameListener);

        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.LINE_START;
        pnlProject.add(fldNewProjectName, c);

        rdbUseExistingProject = new JRadioButton(
            Messages.EnterProjectNamePage_use_existing_project);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.LINE_START;
        pnlProject.add(rdbUseExistingProject, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.LINE_END;
        lblExistingProject = new JLabel(
            Messages.EnterProjectNamePage_project_name);
        pnlProject.add(lblExistingProject, c);

        fldExistingProjectName = new JTextField();
        fldExistingProjectName.getDocument()
            .addDocumentListener(projectNameListener);

        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.LINE_START;
        pnlProject.add(fldExistingProjectName, c);

        browseButton = new JButton("Browse");
        browseButton.setSize(20, 10);

        c.gridx = 2;
        c.gridy = 3;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        pnlProject.add(browseButton, c);

        //set table size
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 1;
        pnlProject.add(Box.createHorizontalStrut(100), c);

        c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = 1;
        pnlProject.add(Box.createHorizontalStrut(300), c);

        c.gridx = 2;
        c.gridy = 4;
        c.gridwidth = 1;
        pnlProject.add(Box.createHorizontalStrut(20), c);

        fileChooser = new JFileChooser(new File(projectBase));

        //radio
        rdbCreateNewProject
            .setActionCommand(ProjectOptions.NEW_PROJECT.toString());
        rdbCreateNewProject.setSelected(true);
        rdbUseExistingProject
            .setActionCommand(ProjectOptions.EXISTING_PROJECT.toString());
        rdbUseExistingProject.setSelected(false);

        ActionListener rdbListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equalsIgnoreCase(
                    ProjectOptions.EXISTING_PROJECT.toString())) {
                    doExistingProject();
                } else {
                    doNewProject();
                }

            }
        };
        rdbCreateNewProject.addActionListener(rdbListener);
        rdbUseExistingProject.addActionListener(rdbListener);

        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        ActionListener browseListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = fileChooser
                    .showOpenDialog(SelectProjectPage.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    fldExistingProjectName.setText(file.getAbsolutePath());

                }

            }
        };
        browseButton.addActionListener(browseListener);

        doNewProject();
    }

    private void doExistingProject() {
        rdbCreateNewProject.setSelected(false);
        rdbUseExistingProject.setSelected(true);

        fldNewProjectName.setEnabled(false);
        lblNewProject.setEnabled(false);

        fldExistingProjectName.setEnabled(true);
        lblExistingProject.setEnabled(true);
        browseButton.setEnabled(true);
    }

    private void doNewProject() {
        rdbCreateNewProject.setSelected(true);
        rdbUseExistingProject.setSelected(false);

        fldNewProjectName.setEnabled(true);
        lblNewProject.setEnabled(true);

        lblExistingProject.setEnabled(false);
        fldExistingProjectName.setEnabled(false);
        browseButton.setEnabled(false);
    }

    @Override
    public boolean isBackButtonEnabled() {
        return false;
    }

    @Override
    public boolean isNextButtonEnabled() {
        return true;
    }

    public boolean isNewProjectSelected() {
        return rdbCreateNewProject.isSelected();
    }

    public String getExistingProjectPath() {
        return fldExistingProjectName.isEnabled() ?
            fldExistingProjectName.getText() :
            "";
    }

    public String getLocalProjectName() {
        return fldExistingProjectName.isEnabled() ?
            new File(getExistingProjectPath()).getName() :
            fldNewProjectName.getText();
    }
}
