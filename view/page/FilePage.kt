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

package com.vaticle.typedb.studio.view.page

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaticle.typedb.studio.state.project.File
import com.vaticle.typedb.studio.state.resource.Resource
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.editor.TextEditor

class FilePage private constructor(
    private var file: File,
    private val editor: TextEditor.State
) : Page(file) {

    override val icon: Form.IconArg = when {
        file.isTypeQL -> Form.IconArg(Icon.Code.RECTANGLE_CODE) { Theme.colors.secondary }
        else -> Form.IconArg(Icon.Code.FILE_LINES)
    }

    companion object {
        @Composable
        fun create(file: File): FilePage {
            return FilePage(file, TextEditor.createState(file))
        }
    }

    override fun updateResourceInner(resource: Resource) {
        // TODO: guarantee that new file has identical content as previous, or update content.
        file = resource as File
        editor.updateFile(file)
    }

    @Composable
    override fun Content() {
        TextEditor.Layout(state = editor, modifier = Modifier.fillMaxSize())
    }
}
