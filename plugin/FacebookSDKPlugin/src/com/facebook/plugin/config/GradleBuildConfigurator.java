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

package com.facebook.plugin.config;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.WritingAccessProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrStatementOwner;

import java.io.File;

public class GradleBuildConfigurator implements Configurator {

    private static final String BLOCK_REPOSITORIES = "repositories";
    private static final String BLOCK_DEPENDENCIES = "dependencies";
    private static final String GRADLE_SCRIPT_NAME = GradleConstants.DEFAULT_SCRIPT_NAME;
    private static final String FORMAT_REPOSITORY = "{repository}()";
    private static final String FORMAT_DEPENDENCY = "compile '{library}:{version}'";

    private final String repository;
    private final String library;
    private final String version;

    GradleBuildConfigurator(final String repository,
                                   final String library,
                                   final String version) {
        this.repository = repository;
        this.library = library;
        this.version = version;
    }

    @Override
    public boolean isConfigured(final Module module) {
        // First, check if the module has the dependency present
        GroovyFile moduleGradleFile = getBuildGradleFile(module.getProject(), module);
        if ((moduleGradleFile != null) &&
                moduleGradleFile.getText().contains(library)) {
            return true;
        }

        // If not, it might still be configured at a project level, so check that too
        GroovyFile projectGradleFile = getBuildGradleFile(module.getProject(), null);
        return ((projectGradleFile != null) &&
                projectGradleFile.getText().contains(library));
    }

    @Override
    public void doConfigure(final Module module) {
        final GroovyFile gradleFile = getBuildGradleFile(module.getProject(), module);
        if ((gradleFile != null) &&
                WritingAccessProvider.isPotentiallyWritable(gradleFile.getVirtualFile(), null)) {

            new WriteCommandAction(gradleFile.getProject()) {
                @Override
                protected void run(@NotNull Result result) {

                    final GrClosableBlock repositoriesBlock =
                            findOrCreateBlock(gradleFile, BLOCK_REPOSITORIES);
                    if (!repositoriesBlock.getText().contains(repository)) {
                        addChildExpression(
                            repositoriesBlock,
                            FORMAT_REPOSITORY.replace("{repository}", repository));
                    }

                    final GrClosableBlock dependenciesBlock =
                            findOrCreateBlock(gradleFile, BLOCK_DEPENDENCIES);
                    if (!repositoriesBlock.getText().contains(library)) {
                        addChildExpression(
                            dependenciesBlock,
                            FORMAT_DEPENDENCY.replace("{library}", library)
                                        .replace("{version}", version));
                    }

                    CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(gradleFile);
                }
            }.execute();

            OpenFileAction.openFile(gradleFile.getVirtualFile().getPath(), module.getProject());
        } else {
            Messages.showErrorDialog(
                module.getProject(),
                "Cannot find or modify build.gradle file for module " + module.getName(),
                "Facebook SDK Plugin");
        }
    }

    private static GroovyFile getBuildGradleFile(final Project project, final Module module) {
        final String basePath = (module == null)
                ? project.getBasePath()
                : new File(module.getModuleFilePath()).getParent();

        final VirtualFile file = VfsUtil.findFileByIoFile(
                new File(basePath + File.separator + GRADLE_SCRIPT_NAME),
                true);
        if (file == null) {
            return null;
        }

        final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile instanceof GroovyFile) {
            return (GroovyFile) psiFile;
        } else {
            return null;
        }
    }

    private static GrClosableBlock getBlockByName(final PsiElement parent, final String name) {
        final GrMethodCallExpression[] allExpressions =
                PsiTreeUtil.getChildrenOfType(parent, GrMethodCallExpression.class);
        if (allExpressions == null) {
            return null;
        }

        for (GrMethodCallExpression expression : allExpressions) {
            final GrExpression invokedExpression = expression.getInvokedExpression();

            if ((expression.getClosureArguments().length > 0) &&
                    (invokedExpression.getText().equalsIgnoreCase(name))) {
                return expression.getClosureArguments()[0];
            }
        }

        return null;
    }

    private static GrClosableBlock findOrCreateBlock(
            final GrStatementOwner parent,
            final String name) {
        final GrClosableBlock block = getBlockByName(parent, name);
        if (block != null) {
            return block;
        }

        addChildExpression(parent, name + "{\n}\n");

        return getBlockByName(parent, name);
    }

    private static GrExpression addChildExpression(
            final GrStatementOwner parent,
            final String exprText) {
        final GroovyPsiElementFactory factory =
                GroovyPsiElementFactory.getInstance(parent.getProject());
        final GrExpression child = factory.createExpressionFromText(exprText);

        CodeStyleManager.getInstance(parent.getProject()).reformat(child);

        final GrStatement[] statements = parent.getStatements();
        if (statements.length > 0) {
            parent.addAfter(child, statements[statements.length - 1]);
        } else {
            parent.addAfter(child, parent.getFirstChild());
        }

        return child;
    }
}
