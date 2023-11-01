/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.lint.aidl

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.google.android.lint.findCallExpression
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UDeclarationsExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.skipParenthesizedExprDown

import java.util.EnumSet

class EnforcePermissionHelperDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement?>> =
            listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler = AidlStubHandler(context)

    private inner class AidlStubHandler(val context: JavaContext) : UElementHandler() {
        override fun visitMethod(node: UMethod) {
            if (context.evaluator.isAbstract(node)) return
            if (!node.hasAnnotation(ANNOTATION_ENFORCE_PERMISSION)) return

            if (!isContainedInSubclassOfStub(context, node)) {
                context.report(
                    ISSUE_MISUSING_ENFORCE_PERMISSION,
                    node,
                    context.getLocation(node),
                    "The class of ${node.name} does not inherit from an AIDL generated Stub class"
                )
                return
            }

            val targetExpression = getHelperMethodCallSourceString(node)
            val message =
                "Method must start with $targetExpression or super.${node.name}(), if applicable"

            val firstExpression = (node.uastBody as? UBlockExpression)
                    ?.expressions?.firstOrNull()

            if (firstExpression == null) {
                context.report(
                    ISSUE_ENFORCE_PERMISSION_HELPER,
                    context.getLocation(node),
                    message,
                )
                return
            }

            val firstExpressionSource = firstExpression.skipParenthesizedExprDown()
              .asSourceString()
              .filterNot(Char::isWhitespace)

            if (firstExpressionSource != targetExpression &&
                  firstExpressionSource != "super.$targetExpression") {
                // calling super.<methodName>() is also legal
                val directSuper = context.evaluator.getSuperMethod(node)
                val firstCall = findCallExpression(firstExpression)?.resolve()
                if (directSuper != null && firstCall == directSuper) return

                val locationTarget = getLocationTarget(firstExpression)
                val expressionLocation = context.getLocation(locationTarget)

                context.report(
                    ISSUE_ENFORCE_PERMISSION_HELPER,
                    context.getLocation(node),
                    message,
                    getHelperMethodFix(node, expressionLocation),
                )
            }
        }
    }

    companion object {
        private const val HELPER_SUFFIX = "_enforcePermission"

        private const val EXPLANATION = """
            The @EnforcePermission annotation can only be used on methods whose class extends from
            the Stub class generated by the AIDL compiler. When @EnforcePermission is applied, the
            AIDL compiler generates a Stub method to do the permission check called yourMethodName$HELPER_SUFFIX.

            yourMethodName$HELPER_SUFFIX must be executed before any other operation. To do that, you can
            either call it directly, or call it indirectly via super.yourMethodName().
            """

        val ISSUE_ENFORCE_PERMISSION_HELPER: Issue = Issue.create(
                id = "MissingEnforcePermissionHelper",
                briefDescription = """Missing permission-enforcing method call in AIDL method
                    |annotated with @EnforcePermission""".trimMargin(),
                explanation = EXPLANATION,
                category = Category.SECURITY,
                priority = 6,
                severity = Severity.ERROR,
                implementation = Implementation(
                        EnforcePermissionHelperDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
        )

        val ISSUE_MISUSING_ENFORCE_PERMISSION: Issue = Issue.create(
                id = "MisusingEnforcePermissionAnnotation",
                briefDescription = "@EnforcePermission cannot be used here",
                explanation = EXPLANATION,
                category = Category.SECURITY,
                priority = 6,
                severity = Severity.ERROR,
                implementation = Implementation(
                        EnforcePermissionHelperDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
        )

        /**
         * handles an edge case with UDeclarationsExpression, where sourcePsi is null,
         * resulting in an incorrect Location if used directly
         */
        private fun getLocationTarget(firstExpression: UExpression): PsiElement? {
            if (firstExpression.sourcePsi != null) return firstExpression.sourcePsi
            if (firstExpression is UDeclarationsExpression) {
                return firstExpression.declarations.firstOrNull()?.sourcePsi
            }
            return null
        }
    }
}
