/*
 * Copyright (c) 2017-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.plugin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class FbToolWindowForm implements ToolWindowFactory {

    // global
    private static ToolWindow sToolWindow;
    private JPanel containerPanel;
    // header
    private JPanel headerPanel;
    private BreadCrumbForm header;
    // title
    private JPanel titlePanel;
    private TitlePanelForm defaultTitle;
    // content
    private JScrollPane scrollPane;
    private ScrollablePanel contentPanel;
    private ActionTree defaultContentView;
    // footer
    private JPanel footerPanel;
    private BreadCrumbForm footer;

    public static void showToolWindow() {
        if (sToolWindow != null) {
            if (sToolWindow.isVisible()) {
                sToolWindow.hide(null);
            } else {
                sToolWindow.show(null);
            }
        }
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        final Content content = contentFactory.createContent(containerPanel, "", false);
        toolWindow.getContentManager().addContent(content);

        initTitle();
        initHeader();
        initActionsTree(project);
        initFooter();
    }

    private void initHeader() {
        headerPanel.removeAll();
        header = new BreadCrumbForm(this);
        headerPanel.add(header.getComponent(), BorderLayout.CENTER);
        headerPanel.setVisible(false);
    }

    private void initTitle() {
        titlePanel.removeAll();
        defaultTitle = new TitlePanelForm();
        titlePanel.add(defaultTitle.getComponent(), BorderLayout.CENTER);
    }

    private void initActionsTree(@NotNull Project project) {
        contentPanel.removeAll();
        defaultContentView = new ActionTree(project, this);
        contentPanel.add(defaultContentView, BorderLayout.CENTER);
    }

    private void initFooter() {
        footerPanel.removeAll();
        footer = new BreadCrumbForm(this);
        footerPanel.add(footer.getComponent(), BorderLayout.CENTER);
        footerPanel.setVisible(false);
    }

    /**
     * Shows the Viewable. If it is not yet added it also adds it to the View
     * @param viewable the Viewable to show.
     */
    void addView(String titleKey, SectionTitlePanelForm sectionTitle, Viewable viewable) {
        // header
        header.setText(ResourceBundle.getBundle("values/strings").getString(titleKey));
        headerPanel.setVisible(true);

        // title
        titlePanel.removeAll();
        titlePanel.add(sectionTitle.getComponent(), BorderLayout.CENTER);

        // content
        contentPanel.removeAll();
        contentPanel.add(viewable.getComponent(), BorderLayout.CENTER);
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMinimum());

        // footer
        footer.setText(ResourceBundle.getBundle("values/strings").getString(titleKey));
        footerPanel.setVisible(true);
    }

    void removeView() {
        // header
        headerPanel.setVisible(false);

        // title
        titlePanel.removeAll();
        titlePanel.add(defaultTitle.getComponent(), BorderLayout.CENTER);

        // content
        contentPanel.removeAll();
        contentPanel.add(defaultContentView, BorderLayout.CENTER);
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMinimum());

        // footer
        footerPanel.setVisible(false);
    }

    @Override
    public void init(ToolWindow toolWindow) {
        sToolWindow = toolWindow;
    }
}
