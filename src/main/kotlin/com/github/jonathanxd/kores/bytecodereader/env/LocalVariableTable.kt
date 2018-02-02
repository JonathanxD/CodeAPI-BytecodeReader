/*
 *      Kores-BytecodeReader - Translates JVM Bytecode to Kores Structure <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2018 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/) <jonathan.scripter@programmer.net>
 *      Copyright (c) contributors
 *
 *
 *      Permission is hereby granted, free of charge, to any person obtaining a copy
 *      of this software and associated documentation files (the "Software"), to deal
 *      in the Software without restriction, including without limitation the rights
 *      to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *      copies of the Software, and to permit persons to whom the Software is
 *      furnished to do so, subject to the following conditions:
 *
 *      The above copyright notice and this permission notice shall be included in
 *      all copies or substantial portions of the Software.
 *
 *      THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *      IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *      FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *      AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *      LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *      OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *      THE SOFTWARE.
 */
package com.github.jonathanxd.kores.bytecodereader.env

import com.github.jonathanxd.iutils.map.ListHashMap
import com.github.jonathanxd.kores.Instruction
import org.objectweb.asm.tree.LabelNode
import java.lang.reflect.Type
import java.util.*

class LocalVariableTable {

    private val table = HashMap<Int, Instruction>()
    private val variableTable = ListHashMap<Int, VariableInfo>()
    private val currentVariables = HashMap<Int, VariableInfo>()

    fun store(part: Instruction, index: Int) {
        this.table[index] = part
    }

    fun getVariableNames() = variableTable.values.flatMap { it }.map { it.name }

    fun getOrNull(index: Int): Instruction? = this.table[index]

    operator fun get(index: Int): Instruction {
        if (!this.table.containsKey(index))
            throw NoSuchElementException("The slot '$index' is empty.")

        return this.table[index]!!
    }

    fun containsSlot(slot: Int): Boolean {
        return this.table.containsKey(slot)
    }

    fun visitLabel(label: LabelNode) {
        this.variableTable.forEach { slot, variables ->
            for (variable in variables) {
                if (variable.start?.label == label.label) {
                    this.currentVariables[slot] = variable
                    break
                }
                if (variable.end?.label == label.label && this.currentVariables[slot] == variable) {
                    this.currentVariables.remove(slot)
                }
            }
        }
    }

    fun storeVariableInfo(
        slot: Int,
        variableType: Type,
        variableName: String,
        start: LabelNode?,
        end: LabelNode?
    ) {
        val variable = VariableInfo(variableType, variableName, start, end)
        this.variableTable.putToList(slot, variable)

        if (start == null) {
            this.currentVariables[slot] = variable
        }
    }

    fun getInfo(slot: Int): VariableInfo? = this.variableTable[slot]?.lastOrNull()

    data class VariableInfo internal constructor(
        val type: Type,
        val name: String,
        val start: LabelNode?,
        val end: LabelNode?
    )
}
