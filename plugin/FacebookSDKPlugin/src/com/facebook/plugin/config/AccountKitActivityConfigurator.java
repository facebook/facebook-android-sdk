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
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.android.dom.manifest.Activity;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AccountKitActivityConfigurator {
    private static final String codePropertiesFile = "values/code.properties";

    private final Properties codeProperties;
    private final String requestCode;

    private PsiElementFactory elementFactory;

    public AccountKitActivityConfigurator(final String requestCode)
            throws IOException {
        this.requestCode = requestCode;
        codeProperties = new Properties();

        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream(codePropertiesFile);
        if (inputStream != null) {
            codeProperties.load(inputStream);
        }
    }

    public boolean isConfigured(final PsiClass psiClass) {
        // TODO: add later
        return false;
    }

    public void doConfigure(final PsiClass psiClass) {
        new WriteCommandAction(psiClass.getProject()) {
            @Override
            protected void run(@NotNull Result result) {
                configureClass(psiClass);

                CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(psiClass);
            }
        }.execute();

        OpenFileAction.openFile(
                psiClass.getContainingFile().getVirtualFile().getPath(),
                psiClass.getProject());
    }

    private void configureClass(final PsiClass psiClass) {
        elementFactory = PsiElementFactory.SERVICE.getInstance(psiClass.getProject());

        PsiFile psiFile = psiClass.getContainingFile();
        if (!(psiFile instanceof PsiJavaFile)) {
            return;
        }

        final PsiImportList importList = ((PsiJavaFile) psiFile).getImportList();
        for (String packageName : codeProperties.getProperty("importsList").split(",")) {
            packageName = packageName.trim();
            if (!importList.getText().contains(packageName)) {
                PsiImportStatement importStatement =
                        elementFactory.createImportStatementOnDemand(packageName);
                for (PsiElement element : importStatement.getChildren()) {
                    // Sometimes, IntelliJ tries to be "helpful" and imports className.* for us
                    // We actually don't want this to happen
                    if (element.getText().equals("*")) {
                        element.getPrevSibling().delete();
                        element.delete();
                    }
                }
                importList.add(importStatement);
            }
        }

        PsiField frameworkRequestCode = findOrCreateField(
                psiClass,
                "AK_FRAMEWORK_REQUEST_CODE",
                "codeBlockFrameworkRequestField");
        frameworkRequestCode.setInitializer(
                elementFactory.createExpressionFromText(requestCode, psiClass));

        PsiMethod onCreateMethod = findOrCreateMethod(
                psiClass,
                "onCreate",
                "codeBlockOnCreateMethod");
        addStatement(onCreateMethod, "codeBlockOnCreate");

        PsiMethod onActivityResult = findOrCreateMethod(
                psiClass,
                "onActivityResult",
                "codeBlockOnActivityResultMethod");
        addStatement(onActivityResult, "codeBlockOnActivityResult");

        findOrCreateMethod(psiClass, "onSuccessfulLogin", "codeBlockOnSuccessfulLogin");
        findOrCreateMethod(psiClass, "onUnsuccessfulLogin", "codeBlockOnUnsuccessfulLogin");
        findOrCreateMethod(psiClass, "doLogin", "codeBlockDoLogin");
    }

    private PsiMethod findOrCreateMethod(
            final PsiClass psiClass,
            final String methodName,
            final String methodBodyPropKey) {
        PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
        if (methods.length > 0) {
            return methods[0];
        }

        PsiMethod method = elementFactory.createMethodFromText(
                codeProperties.getProperty(methodBodyPropKey),
                psiClass);

        PsiMethod[] allMethods = psiClass.getMethods();
        if (allMethods.length > 0) {
            psiClass.addAfter(method, allMethods[allMethods.length - 1]);
        } else {
            psiClass.addAfter(method, psiClass.getLastChild());
        }

        CodeStyleManager.getInstance(psiClass.getProject()).reformat(method);

        methods = psiClass.findMethodsByName(methodName, false);
        if (methods.length > 0) {
            return methods[0];
        } else {
            return method;
        }
    }

    private PsiField findOrCreateField(
            final PsiClass psiClass,
            final String fieldName,
            final String fieldBodyPropKey) {
        PsiField field = psiClass.findFieldByName(fieldName, false);
        if (field != null) {
            return field;
        }

        field = elementFactory.createFieldFromText(
                codeProperties.getProperty(fieldBodyPropKey),
                psiClass);

        PsiField[] allFields = psiClass.getAllFields();
        if (allFields.length > 0) {
            psiClass.addAfter(field, allFields[allFields.length - 1]);
        } else {
            psiClass.addAfter(field, psiClass.getFirstChild());
        }

        CodeStyleManager.getInstance(psiClass.getProject()).reformat(field);
        return field;
    }

    private void addStatement(final PsiMethod method, final String statementPropKey) {
        PsiStatement statement = elementFactory.createStatementFromText(
                codeProperties.getProperty(statementPropKey),
                method);
        method.getBody().add(statement);
    }

    public static final class AvailableActivity {
        private final Module module;
        private final PsiClass psiClass;

        AvailableActivity(final Module module, final PsiClass psiClass) {
            this.module = module;
            this.psiClass = psiClass;
        }

        public Module getModule() {
            return module;
        }

        public PsiClass getPsiClass() {
            return psiClass;
        }

        @Override
        public String toString() {
            return psiClass.getQualifiedName() + " (" + module.getName() + ")";
        }
    }

    public static AvailableActivity[] getActivityClasses(final Project project) {
        Module[] allModules = ModuleManager.getInstance(project).getModules();
        List<AvailableActivity> res = new ArrayList<>();

        for (Module module : allModules) {
            List<Activity> activities;
            try {
                activities = AndroidFacet.getInstance(module)
                        .getManifest()
                        .getApplication()
                        .getActivities();
            } catch (Exception e) {
                // Module doesn't have a valid Android facet. Skip it
                continue;
            }

            for (Activity activity : activities) {
                if ((activity != null) &&
                        (activity.getActivityClass() != null) &&
                        (activity.getActivityClass().getValue() != null)) {
                    res.add(new AvailableActivity(module, activity.getActivityClass().getValue()));
                }
            }
        }

        return res.toArray(new AvailableActivity[res.size()]);
    }
}
