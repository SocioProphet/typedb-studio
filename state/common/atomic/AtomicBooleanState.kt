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

package com.vaticle.typedb.studio.state.common.atomic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.atomic.AtomicBoolean

class AtomicBooleanState(initValue: Boolean) {

    var state by mutableStateOf(initValue); private set
    val atomic = AtomicBoolean(initValue)

    fun set(value: Boolean) {
        atomic.set(value)
        state = value
    }

    fun compareAndSet(expected: Boolean, new: Boolean): Boolean {
        return if (atomic.compareAndSet(expected, new)) {
            state = new
            true
        } else false
    }
}