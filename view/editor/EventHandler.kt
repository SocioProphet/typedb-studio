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
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.ENTER_SHIFT_MOD
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
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.UNDO
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.ContextMenu
import com.vaticle.typedb.studio.view.common.component.Icon

internal class EventHandler(
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
            TAB -> processor.indentTab()
            TAB_SHIFT -> processor.outdentTab()
            ENTER, ENTER_SHIFT, ENTER_SHIFT_MOD -> processor.insertNewLine()
            CUT -> cut()
            COPY -> copy()
            PASTE -> paste()
            UNDO -> processor.undo()
            REDO -> processor.redo()
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

    private fun copy() {
        if (target.selection == null) return
        clipboard.setText(target.selectedText())
    }

    private fun paste() {
        clipboard.getText()?.let { if (it.text.isNotEmpty()) processor.insertText(it.text) }
    }

    private fun hideToolbar(): Boolean {
        return if (toolbar.showToolbar) {
            toolbar.hide()
            true
        } else false
    }

    internal fun contextMenuFn(): List<List<ContextMenu.Item>> {
        val selection = target.selection
        val modKey = KeyMapper.CURRENT.modKey
        val hasClipboard = !clipboard.getText().isNullOrBlank()
        return listOf(
            listOf(
                ContextMenu.Item(
                    Label.CUT, Icon.Code.CUT, "$modKey + X", processor.isWritable && selection != null
                ) { cut() },
                ContextMenu.Item(Label.COPY, Icon.Code.COPY, "$modKey + C", selection != null) { copy() },
                ContextMenu.Item(
                    Label.PASTE, Icon.Code.PASTE, "$modKey + V", processor.isWritable && hasClipboard
                ) { paste() }
            ),
            listOf(
                ContextMenu.Item(Label.FIND, Icon.Code.MAGNIFYING_GLASS, "$modKey + F") { toolbar.showFinder() },
                ContextMenu.Item(
                    Label.REPLACE, Icon.Code.RIGHT_LEFT, "$modKey + R", processor.isWritable
                ) { toolbar.mayShowReplacer() }
            )
        )
    }
}