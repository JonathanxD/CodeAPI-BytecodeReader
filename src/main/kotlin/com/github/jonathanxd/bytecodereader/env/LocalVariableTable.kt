/*
 *      CodeAPI-BytecodeReader - Framework to generate Java code and Bytecode code. <https://github.com/JonathanxD/CodeAPI-BytecodeReader>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2017 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/ & https://github.com/TheRealBuggy/) <jonathan.scripter@programmer.net>
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
package com.github.jonathanxd.bytecodereader.env

import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.type.CodeType
import java.util.*

class LocalVariableTable {

    private val table = HashMap<Int, CodePart>()
    private val variableTable = HashMap<Int, VariableInfo>()

    fun store(part: CodePart, index: Int) {
        this.table.put(index, part)
    }

    fun getOrNull(index: Int): CodePart? {
        return this.table[index]
    }

    operator fun get(index: Int): CodePart {
        if (!this.table.containsKey(index))
            throw NoSuchElementException("The slot '$index' is empty.")

        return this.table[index]!!
    }

    fun containsSlot(slot: Int): Boolean {
        return this.table.containsKey(slot)
    }


    fun storeVariableInfo(slot: Int, variableType: CodeType, variableName: String) {
        this.variableTable.put(slot, VariableInfo(variableType, variableName))
    }

    fun getInfo(slot: Int): VariableInfo? {
        return this.variableTable[slot]
    }

    inner class VariableInfo internal constructor(val type: CodeType, val name: String) {

        override fun equals(obj: Any?): Boolean {
            if (obj == null || obj !is VariableInfo)
                return super.equals(obj)

            return this.name == obj.name && this.type.`is`(obj.type)
        }

        override fun hashCode(): Int {
            return Objects.hash(this.name, this.type)
        }
    }

}
