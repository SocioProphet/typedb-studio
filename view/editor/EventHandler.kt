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

import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.awtEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.ClipboardManager
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.view.common.KeyMapper
import com.vaticle.typedb.studio.view.common.KeyMapper.Command
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.COPY
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.CUT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.DELETE_CHAR_NEXT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.DELETE_CHAR_PREV
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.DELETE_LINE_END
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.DELETE_LINE_START
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.DELETE_WORD_NEXT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.DELETE_WORD_PREV
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.EMOJI_WINDOW
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.ENTER
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.ENTER_SHIFT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.MOD_ENTER
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.MOVE_CHAR_LEFT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.MOVE_CHAR_RIGHT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.MOVE_END
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.MOVE_HOME
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.MOVE_LINE_DOWN
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.MOVE_LINE_END
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.MOVE_LINE_LEFT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.MOVE_LINE_RIGHT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.MOVE_LINE_START
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.MOVE_LINE_UP
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.MOVE_PAGE_DOWN
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.MOVE_PAGE_UP
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.MOVE_PARAGRAPH_NEXT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.MOVE_PARAGRAPH_PREV
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.MOVE_WORD_LEFT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.MOVE_WORD_RIGHT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.PASTE
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.REDO
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_ALL
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_CHAR_LEFT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_CHAR_RIGHT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_END
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_HOME
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_LINE_DOWN
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_LINE_END
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_LINE_LEFT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_LINE_RIGHT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_LINE_START
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_LINE_UP
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_NONE
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_PAGE_DOWN
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_PAGE_UP
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_PARAGRAPH_NEXT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_PARAGRAPH_PREV
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_WORD_LEFT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.SELECT_WORD_RIGHT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.TAB
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.TAB_SHIFT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.TEXT_SIZE_DECREASE
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.TEXT_SIZE_INCREASE
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.TEXT_SIZE_RESET
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.TOGGLE_COMMENT
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.UNDO
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.ContextMenu
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.editor.TextProcessor.Companion.normaliseWhiteSpace

internal class EventHandler constructor(
    private val target: InputTarget,
    private val toolbar: TextToolbar.State,
    private val clipboard: ClipboardManager,
    initProcessor: TextProcessor
) {

    internal var processor: TextProcessor by mutableStateOf(initProcessor)

    internal fun handleEditorEvent(event: KeyEvent): Boolean {
        return if (event.type == KeyEventType.KeyUp) false
        else when {
            event.isTypedEvent -> processor.insertText(event.awtEvent.keyChar.toString())
            else -> KeyMapper.CURRENT.map(event)?.let { executeEditorCommand(it) } ?: false
        }
    }

    internal fun handleToolbarEvent(event: KeyEvent): Boolean {
        return if (event.type == KeyEventType.KeyUp) false
        else KeyMapper.CURRENT.map(event)?.let { executeWindowCommand(it) } ?: false
    }

    private fun executeEditorCommand(command: Command): Boolean {
        when (command) {
            MOVE_CHAR_LEFT -> target.moveCursorPrevByChar() // because we only display left to right
            MOVE_CHAR_RIGHT -> target.moveCursorNextByChar() // because we only display left to right
            MOVE_WORD_LEFT -> target.moveCursorPrevByWord() // because we only display left to right
            MOVE_WORD_RIGHT -> target.moveCursorNexBytWord() // because we only display left to right
            MOVE_PARAGRAPH_PREV -> target.moveCursorPrevByParagraph()
            MOVE_PARAGRAPH_NEXT -> target.moveCursorNextByParagraph()
            MOVE_LINE_LEFT -> target.moveCursorToStartOfLine() // because we only display left to right
            MOVE_LINE_RIGHT -> target.moveCursorToEndOfLine() // because we only display left to right
            MOVE_LINE_START -> target.moveCursorToStartOfLine()
            MOVE_LINE_END -> target.moveCursorToEndOfLine()
            MOVE_LINE_UP -> target.moveCursorUpByLine()
            MOVE_LINE_DOWN -> target.moveCursorDownByLine()
            MOVE_PAGE_UP -> target.moveCursorUpByPage()
            MOVE_PAGE_DOWN -> target.moveCursorDownByPage()
            MOVE_HOME -> target.moveCursorToStart()
            MOVE_END -> target.moveCursorToEnd()
            SELECT_CHAR_LEFT -> target.moveCursorPrevByChar(true) // because we only display left to right
            SELECT_CHAR_RIGHT -> target.moveCursorNextByChar(true) // because we only display left to right
            SELECT_WORD_LEFT -> target.moveCursorPrevByWord(true) // because we only display left to right
            SELECT_WORD_RIGHT -> target.moveCursorNexBytWord(true) // because we only display left to right
            SELECT_PARAGRAPH_PREV -> target.moveCursorPrevByParagraph(true)
            SELECT_PARAGRAPH_NEXT -> target.moveCursorNextByParagraph(true)
            SELECT_LINE_LEFT -> target.moveCursorToStartOfLine(true) // because we only display left to right
            SELECT_LINE_RIGHT -> target.moveCursorToEndOfLine(true) // because we only display left to right
            SELECT_LINE_START -> target.moveCursorToStartOfLine(true)
            SELECT_LINE_END -> target.moveCursorToEndOfLine(true)
            SELECT_LINE_UP -> target.moveCursorUpByLine(true)
            SELECT_LINE_DOWN -> target.moveCursorDownByLine(true)
            SELECT_PAGE_UP -> target.moveCursorUpByPage(true)
            SELECT_PAGE_DOWN -> target.moveCursorDownByPage(true)
            SELECT_HOME -> target.moveCursorToStart(true)
            SELECT_END -> target.moveCursorToEnd(true)
            SELECT_ALL -> target.selectAll()
            SELECT_NONE -> target.selectNone()
            DELETE_CHAR_PREV -> deleteSelectionOr { target.moveCursorPrevByChar(true); processor.deleteSelection() }
            DELETE_CHAR_NEXT -> deleteSelectionOr { target.moveCursorNextByChar(true); processor.deleteSelection() }
            DELETE_WORD_PREV -> deleteSelectionOr { target.moveCursorPrevByWord(true); processor.deleteSelection() }
            DELETE_WORD_NEXT -> deleteSelectionOr { target.moveCursorNexBytWord(true); processor.deleteSelection() }
            DELETE_LINE_START -> deleteSelectionOr { target.moveCursorToStartOfLine(true); processor.deleteSelection() }
            DELETE_LINE_END -> deleteSelectionOr { target.moveCursorToEndOfLine(true); processor.deleteSelection() }
            TOGGLE_COMMENT -> processor.toggleComment()
            TAB -> processor.indentTab()
            TAB_SHIFT -> processor.outdentTab()
            ENTER, ENTER_SHIFT -> processor.insertNewLine()
            MOD_ENTER -> mayRunSelectionOrFile()
            CUT -> cut()
            COPY -> copy()
            PASTE -> paste()
            UNDO -> processor.undo()
            REDO -> processor.redo()
            TEXT_SIZE_INCREASE -> GlobalState.editor.increaseScale()
            TEXT_SIZE_DECREASE -> GlobalState.editor.decreaseScale()
            TEXT_SIZE_RESET -> GlobalState.editor.resetScale()
            EMOJI_WINDOW -> {
                // TODO: https://github.com/JetBrains/compose-jb/issues/1754
                // androidx.compose.foundation.text.showCharacterPalette()
            }
            else -> return executeWindowCommand(command)
        }
        return true
    }

    private fun executeWindowCommand(command: Command): Boolean {
        when (command) {
            Command.FIND -> toolbar.showFinder()
            Command.REPLACE -> toolbar.mayShowReplacer()
            Command.ESCAPE -> target.selection?.let { target.selectNone() } ?: hideToolbar()
            else -> return false
        }
        return true
    }

    private fun deleteSelectionOr(elseFn: () -> Unit) {
        if (target.selection != null) processor.deleteSelection()
        else elseFn()
    }

    private fun cut() {
        if (target.selection == null) return
        copy()
        processor.deleteSelection()
    }

    internal fun copy() {
        if (target.selection == null) return
        clipboard.setText(target.selectedText())
    }

    private fun paste() {
        clipboard.getText()?.let { if (it.text.isNotEmpty()) processor.insertText(normaliseWhiteSpace(it.text)) }
    }

    private fun mayRunSelectionOrFile() {
        if (target.selection != null) mayRunSelection() else mayRunFile()
    }

    private fun mayRunFile() {
        if (!GlobalState.client.isReadyToRunQuery) return
        processor.file?.let { if (it.isRunnable) GlobalState.resource.openAndMayRun(it.asRunnable()) }
    }

    private fun mayRunSelection() {
        if (!GlobalState.client.isReadyToRunQuery) return
        processor.file?.let {
            if (it.isRunnable) GlobalState.resource.openAndMayRun(it.asRunnable(), target.selectedText().text)
        }
    }

    private fun hideToolbar(): Boolean {
        return if (toolbar.showToolbar) {
            toolbar.hide()
            true
        } else false
    }

    internal fun contextMenuFn(): List<List<ContextMenu.Item>> {
        val groupedMenuItems = listOf(
            textEditingMenuItems(),
            findAndReplaceMenuItems()
        ).toMutableList()
        if (processor.file != null) groupedMenuItems.add(runQueryMenuItems())
        groupedMenuItems.add(textResizerMenuItems())
        return groupedMenuItems
    }

    private fun textEditingMenuItems(): List<ContextMenu.Item> {
        val menuItems = listOf(copySelectionMenuItem()).toMutableList()
        if (processor.isWritable) menuItems.addAll(listOf(cutSelectionMenuItem(), pasteTextMenuItem()))
        return menuItems
    }

    private fun findAndReplaceMenuItems(): List<ContextMenu.Item> {
        val menuItems = listOf(findTextMenuItem()).toMutableList()
        if (processor.isWritable) menuItems.add(replaceMenuItem())
        return menuItems
    }

    private fun runQueryMenuItems(): List<ContextMenu.Item> {
        return listOf(runFileMenuItem(), runSelectionMenuItem())
    }

    private fun textResizerMenuItems(): List<ContextMenu.Item> {
        return listOf(increaseTextSizeMenuItem(), decreaseTextSizeMenuItem(), resetTextSizeMenuItem())
    }

    private fun cutSelectionMenuItem() = ContextMenu.Item(
        label = Label.CUT,
        icon = Icon.Code.CUT,
        info = "${KeyMapper.CURRENT.modKey} + X",
        enabled = processor.isWritable && target.selection != null
    ) { cut() }

    private fun copySelectionMenuItem() = ContextMenu.Item(
        label = Label.COPY,
        icon = Icon.Code.COPY,
        info = "${KeyMapper.CURRENT.modKey} + C",
        enabled = target.selection != null
    ) { copy() }

    private fun pasteTextMenuItem() = ContextMenu.Item(
        label = Label.PASTE,
        icon = Icon.Code.PASTE,
        info = "${KeyMapper.CURRENT.modKey} + V",
        enabled = processor.isWritable && !clipboard.getText().isNullOrBlank()
    ) { paste() }

    private fun findTextMenuItem() = ContextMenu.Item(
        label = Label.FIND,
        icon = Icon.Code.MAGNIFYING_GLASS,
        info = "${KeyMapper.CURRENT.modKey} + F"
    ) { toolbar.showFinder() }

    private fun replaceMenuItem() = ContextMenu.Item(
        label = Label.REPLACE,
        icon = Icon.Code.RIGHT_LEFT,
        info = "${KeyMapper.CURRENT.modKey} + R",
        enabled = processor.isWritable
    ) { toolbar.mayShowReplacer() }

    private fun runFileMenuItem() = ContextMenu.Item(
        label = Label.RUN_FILE,
        icon = Icon.Code.PLAY,
        iconColor = { Theme.colors.secondary },
        info = "${KeyMapper.CURRENT.modKey} + ${Label.ENTER}",
        enabled = processor.file?.isRunnable == true && GlobalState.client.isReadyToRunQuery
    ) { mayRunFile() }

    private fun runSelectionMenuItem() = ContextMenu.Item(
        label = Label.RUN_SELECTION,
        icon = Icon.Code.PLAY,
        iconColor = { Theme.colors.secondary },
        info = "${KeyMapper.CURRENT.modKey} + ${Label.ENTER}",
        enabled = processor.file?.isRunnable == true && target.selection != null && GlobalState.client.isReadyToRunQuery
    ) { mayRunSelection() }

    private fun increaseTextSizeMenuItem() = ContextMenu.Item(
        label = Label.INCREASE_TEXT_SIZE,
        icon = Icon.Code.ARROWS_MAXIMIZE,
        info = "${KeyMapper.CURRENT.modKey} + =",
        enabled = !GlobalState.editor.isMaxScale
    ) { GlobalState.editor.increaseScale() }

    private fun decreaseTextSizeMenuItem() = ContextMenu.Item(
        label = Label.DECREASE_TEXT_SIZE,
        icon = Icon.Code.ARROWS_MINIMIZE,
        info = "${KeyMapper.CURRENT.modKey} + -",
        enabled = !GlobalState.editor.isMinScale
    ) { GlobalState.editor.decreaseScale() }

    private fun resetTextSizeMenuItem() = ContextMenu.Item(
        label = Label.RESET_TEXT_SIZE,
        icon = Icon.Code.EXPAND,
        info = "${KeyMapper.CURRENT.modKey} + 0",
        enabled = !GlobalState.editor.isDefaultScale
    ) { GlobalState.editor.resetScale() }
}