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

package com.facebook.plugin.utils;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.apache.commons.lang.StringUtils;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PsiHelper {

    public static Map<Module, PsiFile> findAllFiles(
            final Project project,
            final String fileName) {
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        String projectFilePath = project.getBasePath();

        PsiFile[] psiFiles = FilenameIndex.getFilesByName(
                project,
                fileName,
                GlobalSearchScope.projectScope(project));

        Map<Module, PsiFile> moduleFiles = new HashMap<>();
        for (PsiFile psiFile : psiFiles) {
            try {
                VirtualFile virtualFile = psiFile.getVirtualFile();
                Module module = fileIndex.getModuleForFile(virtualFile);
                String modulePath = module.getModuleFile().getParent().getPath();

                if (!modulePath.equals(projectFilePath)) {
                    moduleFiles.put(module, psiFile);
                }
            } catch (Exception e) {
                // Swallow
            }
        }

        return moduleFiles;
    }

    public static List<PsiFile> findAllFiles(final Module module, final String fileName) {
        ProjectFileIndex fileIndex =
                ProjectRootManager.getInstance(module.getProject()).getFileIndex();

        PsiFile[] psiFiles = FilenameIndex.getFilesByName(
                module.getProject(),
                fileName,
                module.getModuleContentScope());

        List<PsiFile> moduleFiles = new ArrayList<>();
        for (PsiFile psiFile : psiFiles) {
            try {
                VirtualFile virtualFile = psiFile.getVirtualFile();
                Module fileModule = fileIndex.getModuleForFile(virtualFile);

                if (fileModule == module) {
                    moduleFiles.add(psiFile);
                }
            } catch (Exception e) {
                // Swallow
            }
        }

        return moduleFiles;
    }

    /**
     * This class should not be present in the final release.
     * I'm just using it here for my own sanity because digging down the Psi tree inside
     * the debugger is really unpleasant
     */
    public static class Printer {
        private static PrintStream out = null;
        private static final String fileName = "/Users/plj/printedElements";

        public static void printElement(final PsiElement element) {
            try {
                if (out == null) {
                    out = new PrintStream(new FileOutputStream(fileName));
                }
                out.println(element.getContainingFile().getName());
                out.println("===============================================================");
                printElement(out, "", element);
                out.println("===============================================================\n\n");
            } catch (Exception e) {
                Messages.showErrorDialog(e.toString(), "Error in Printer");
            }
        }

        private static void printElement(
                final PrintStream out,
                final String indent,
                final PsiElement element)
                throws Exception {
            String contents = element.getText();
            int i = contents.indexOf('\n');
            if (i >= 0) {
                contents = contents.substring(0, i) + "\\n...";
            }

            out.printf(StringUtils.rightPad(indent + element.getClass().getSimpleName(), 40));
            out.print(StringUtils.rightPad(element.toString(), 40));
            out.print("| ");
            out.print(indent.replace("+", " "));
            out.print(contents);
            out.println();

            for (PsiElement child : element.getChildren()) {
                printElement(out, indent + "+ ", child);
            }
        }
    }
}
