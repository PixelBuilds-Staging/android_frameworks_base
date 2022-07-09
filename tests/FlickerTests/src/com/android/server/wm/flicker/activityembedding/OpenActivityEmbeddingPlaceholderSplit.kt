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

package com.android.server.wm.flicker.activityembedding

import android.platform.test.annotations.Presubmit
import android.view.Surface
import android.view.WindowManagerPolicyConstants
import androidx.test.filters.RequiresDevice
import com.android.server.wm.flicker.FlickerBuilderProvider
import com.android.server.wm.flicker.FlickerParametersRunnerFactory
import com.android.server.wm.flicker.FlickerTestParameter
import com.android.server.wm.flicker.FlickerTestParameterFactory
import com.android.server.wm.flicker.dsl.FlickerBuilder
import com.android.server.wm.flicker.helpers.ActivityEmbeddingAppHelper
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.junit.runners.Parameterized

/**
 * Test opening an activity that will launch another activity as ActivityEmbedding placeholder in
 * split.
 *
 * To run this test: `atest FlickerTests:OpenActivityEmbeddingPlaceholderSplit`
 */
@RequiresDevice
@RunWith(Parameterized::class)
@Parameterized.UseParametersRunnerFactory(FlickerParametersRunnerFactory::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class OpenActivityEmbeddingPlaceholderSplit(private val testSpec: FlickerTestParameter) :
    ActivityEmbeddingTestBase() {

    @FlickerBuilderProvider
    fun buildFlicker(): FlickerBuilder {
        return FlickerBuilder(instrumentation).apply {
            setup {
                eachRun {
                    testApp.launchViaIntent(wmHelper)
                }
            }
            transitions {
                testApp.launchPlaceholderSplit(wmHelper)
            }
            teardown {
                test {
                    device.pressHome()
                    testApp.exit(wmHelper)
                }
            }
        }
    }

    @Presubmit
    @Test
    fun mainActivityBecomesInvisible() {
        testSpec.assertLayers {
            isVisible(ActivityEmbeddingAppHelper.MAIN_ACTIVITY_COMPONENT)
                    .then()
                    .isInvisible(ActivityEmbeddingAppHelper.MAIN_ACTIVITY_COMPONENT)
        }
    }

    @Presubmit
    @Test
    fun placeholderSplitBecomesVisible() {
        testSpec.assertLayers {
            isInvisible(ActivityEmbeddingAppHelper.PLACEHOLDER_PRIMARY_COMPONENT)
                    .then()
                    .isVisible(ActivityEmbeddingAppHelper.PLACEHOLDER_PRIMARY_COMPONENT)
        }
        testSpec.assertLayers {
            isInvisible(ActivityEmbeddingAppHelper.PLACEHOLDER_SECONDARY_COMPONENT)
                    .then()
                    .isVisible(ActivityEmbeddingAppHelper.PLACEHOLDER_SECONDARY_COMPONENT)
        }
    }

    companion object {
        /**
         * Creates the test configurations.
         *
         * See [FlickerTestParameterFactory.getConfigNonRotationTests] for configuring
         * repetitions, screen orientation and navigation modes.
         */
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun getParams(): Collection<FlickerTestParameter> {
            return FlickerTestParameterFactory.getInstance()
                    .getConfigNonRotationTests(
                            repetitions = 1,
                            supportedRotations = listOf(Surface.ROTATION_0, Surface.ROTATION_90),
                            supportedNavigationModes = listOf(
                                    WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY,
                                    WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY
                            )
                    )
        }
    }
}
