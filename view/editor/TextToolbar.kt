/*
 * Copyright (C) 2021 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.vaticle.typedb.studio.view.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.TextInput
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme

object TextToolbar {

    private val MAX_WIDTH = 800.dp
    private val MIN_WIDTH = 260.dp
    private val INPUT_MIN_HEIGHT = 28.dp
    private val INPUT_MAX_HEIGHT = 120.dp
    private val BUTTON_AREA_WIDTH = 160.dp
    private val BUTTON_HEIGHT = 24.dp
    private val BUTTON_SPACING = 4.dp

    private fun toolBarHeight(state: TextFinder): Dp {
        var height = Separator.WEIGHT + finderInputHeight(state)
        if (state.showReplacer) height += replacerInputHeight(state)
        return height
    }

    private fun finderInputHeight(state: TextFinder): Dp {
        return state.findTextHeight.coerceIn(INPUT_MIN_HEIGHT, INPUT_MAX_HEIGHT)
    }

    private fun replacerInputHeight(state: TextFinder): Dp {
        return state.replaceTextHeight.coerceIn(INPUT_MIN_HEIGHT, INPUT_MAX_HEIGHT)
    }

    @Composable
    internal fun Area(state: TextFinder) {
        Box { // We render a character to find out the default height of a line for the given font
            Text(
                text = "0", style = Theme.typography.body1,
                onTextLayout = { state.lineHeight = Theme.toDP(it.size.height, state.density) }
            )
            // TODO: figure out how to set min width to MIN_WIDTH
            Row(modifier = Modifier.widthIn(max = MAX_WIDTH).height(toolBarHeight(state))) {
                Column(Modifier.weight(1f)) {
                    FinderTextInput(state)
                    if (state.showReplacer) {
                        Separator.Horizontal()
                        ReplacerTextInput(state)
                    }
                }
                Separator.Vertical()
                Buttons(state)
            }
        }
        Separator.Horizontal()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun FinderTextInput(state: TextFinder) {
        TextInput(
            value = state.findText,
            placeholder = Label.FIND,
            onValueChange = { state.findText = it },
            modifier = Modifier.height(finderInputHeight(state)),
            leadingIcon = Icon.Code.MAGNIFYING_GLASS,
            singleLine = false,
            shape = null,
            border = null,
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ReplacerTextInput(state: TextFinder) {
        TextInput(
            value = state.replaceText,
            placeholder = Label.REPLACE,
            onValueChange = { state.replaceText = it },
            modifier = Modifier.height(replacerInputHeight(state)),
            leadingIcon = Icon.Code.RIGHT_LEFT,
            singleLine = false,
            shape = null,
            border = null,
        )
    }

    @Composable
    private fun Buttons(state: TextFinder) {
        Row(modifier = Modifier.offset(x = -INPUT_MIN_HEIGHT - Separator.WEIGHT)) {
            FinderToggles(state)
            Separator.Vertical()
            Column(Modifier.width(BUTTON_AREA_WIDTH)) {
                FinderButtons(state)
                ReplacerButtons(state)
            }
        }
    }

    @Composable
    private fun FinderToggles(state: TextFinder) {
        Form.IconButton(
            Icon.Code.FONT_CASE,
            onClick = { state.toggleCaseSensitive() },
            modifier = Modifier.size(INPUT_MIN_HEIGHT),
            iconColor = if (state.isCaseSensitive) Theme.colors.secondary else Theme.colors.icon,
            bgColor = Theme.colors.surface,
            rounded = false
        )
    }

    @Composable
    private fun FinderButtons(state: TextFinder) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(INPUT_MIN_HEIGHT)
        ) {
            Spacer(Modifier.width(BUTTON_SPACING))
            FindNextButton(state)
            FindPreviousButton(state)
            Spacer(Modifier.width(BUTTON_SPACING))
            FinderStatus(state, Modifier.weight(1f))
        }
    }

    @Composable
    private fun FinderStatus(state: TextFinder, modifier: Modifier) {
        Form.Text(
            value = state.status,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }

    @Composable
    private fun FindPreviousButton(state: TextFinder) {
        Form.IconButton(
            icon = Icon.Code.CHEVRON_UP,
            onClick = { state.findPrevious() },
            modifier = Modifier.size(INPUT_MIN_HEIGHT),
            bgColor = Color.Transparent,
            rounded = false
        )
    }

    @Composable
    private fun FindNextButton(state: TextFinder) {
        Form.IconButton(
            icon = Icon.Code.CHEVRON_DOWN,
            onClick = { state.findNext() },
            modifier = Modifier.size(INPUT_MIN_HEIGHT),
            bgColor = Color.Transparent,
            rounded = false
        )
    }

    @Composable
    private fun ReplacerButtons(state: TextFinder) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.height(INPUT_MIN_HEIGHT)
        ) {
            Spacer(Modifier.width(BUTTON_SPACING))
            ReplaceNextButton(state)
            Spacer(Modifier.width(BUTTON_SPACING))
            ReplaceAllButton(state)
        }
    }

    @Composable
    private fun ReplaceNextButton(state: TextFinder) {
        Form.TextButton(
            text = Label.REPLACE,
            onClick = { state.replaceNext() },
            modifier = Modifier.height(BUTTON_HEIGHT)
        )
    }

    @Composable
    private fun ReplaceAllButton(state: TextFinder) {
        Form.TextButton(
            text = Label.REPLACE_ALL,
            onClick = { state.replaceAll() },
            modifier = Modifier.height(BUTTON_HEIGHT)
        )
    }
}