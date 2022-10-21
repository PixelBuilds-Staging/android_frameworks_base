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

package com.android.settingslib.spa.widget.preference

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessAlarm
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.MusicOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.ui.SettingsSlider
import com.android.settingslib.spa.widget.util.EntryHighlight

/**
 * The widget model for [SliderPreference] widget.
 */
interface SliderPreferenceModel {
    /**
     * The title of this [SliderPreference].
     */
    val title: String

    /**
     * The initial position of the [SliderPreference].
     */
    val initValue: Int

    /**
     * The value range for this [SliderPreference].
     */
    val valueRange: IntRange
        get() = 0..100

    /**
     * The lambda to be invoked during the value change by dragging or a click. This callback is
     * used to get the real time value of the [SliderPreference].
     */
    val onValueChange: ((value: Int) -> Unit)?
        get() = null

    /**
     * The lambda to be invoked when value change has ended. This callback is used to get when the
     * user has completed selecting a new value by ending a drag or a click.
     */
    val onValueChangeFinished: (() -> Unit)?
        get() = null

    /**
     * The icon image for [SliderPreference]. If not specified, the slider hides the icon by default.
     */
    val icon: ImageVector?
        get() = null

    /**
     * Indicates whether to show step marks. If show step marks, when user finish sliding,
     * the slider will automatically jump to the nearest step mark. Otherwise, the slider hides
     * the step marks by default.
     *
     * The step is fixed to 1.
     */
    val showSteps: Boolean
        get() = false
}

/**
 * Settings slider widget.
 *
 * Data is provided through [SliderPreferenceModel].
 */
@Composable
fun SliderPreference(model: SliderPreferenceModel) {
    EntryHighlight {
        SliderPreference(
            title = model.title,
            initValue = model.initValue,
            valueRange = model.valueRange,
            onValueChange = model.onValueChange,
            onValueChangeFinished = model.onValueChangeFinished,
            icon = model.icon,
            showSteps = model.showSteps,
        )
    }
}

@Composable
internal fun SliderPreference(
    title: String,
    initValue: Int,
    modifier: Modifier = Modifier,
    valueRange: IntRange = 0..100,
    onValueChange: ((value: Int) -> Unit)? = null,
    onValueChangeFinished: (() -> Unit)? = null,
    icon: ImageVector? = null,
    showSteps: Boolean = false,
) {
    BaseLayout(
        title = title,
        subTitle = {
            SettingsSlider(
                initValue,
                modifier,
                valueRange,
                onValueChange,
                onValueChangeFinished,
                showSteps
            )
        },
        icon = if (icon != null) ({
            Icon(imageVector = icon, contentDescription = null)
        }) else null,
    )
}

@Preview
@Composable
private fun SliderPreferencePreview() {
    SettingsTheme {
        val initValue = 30
        var sliderPosition by rememberSaveable { mutableStateOf(initValue) }
        SliderPreference(
            title = "Alarm Volume",
            initValue = 30,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {
                println("onValueChangeFinished: the value is $sliderPosition")
            },
            icon = Icons.Outlined.AccessAlarm,
        )
    }
}

@Preview
@Composable
private fun SliderPreferenceIconChangePreview() {
    SettingsTheme {
        var icon by remember { mutableStateOf(Icons.Outlined.MusicNote) }
        SliderPreference(
            title = "Media Volume",
            initValue = 40,
            onValueChange = { it: Int ->
                icon = if (it > 0) Icons.Outlined.MusicNote else Icons.Outlined.MusicOff
            },
            icon = icon,
        )
    }
}

@Preview
@Composable
private fun SliderPreferenceStepsPreview() {
    SettingsTheme {
        SliderPreference(
            title = "Display Text",
            initValue = 2,
            valueRange = 1..5,
            showSteps = true,
        )
    }
}
