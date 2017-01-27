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

import com.github.jonathanxd.bytecodereader.extra.UnknownPart
import com.github.jonathanxd.bytecodereader.util.filterWithIndex
import com.github.jonathanxd.bytecodereader.util.remove
import com.github.jonathanxd.codeapi.CodePart
import java.util.*
import java.util.function.Predicate

class StackManager {

    private val stack = ArrayList<CodePart>()

    fun push(part: CodePart) {
        this.stack.add(part)
    }

    fun push(parts: List<CodePart>) {
        this.stack.addAll(parts)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> popAs(type: Class<T>): T {
        val pop = this.pop()

        if (!type.isInstance(pop))
            throw ClassCastException("Value '$pop' popped from stack cannot be cast to '$type'!")

        return pop as T
    }

    fun popAll(): List<CodePart> {
        val popped = ArrayList(this.stack)

        this.stack.clear()

        return popped
    }

    fun pop(): CodePart {
        this.checkEmpty()

        this.checkEmpty()

        for (i in this.stack.indices.reversed()) {
            val codePart = this.stack[i] as? UnknownPart ?: return this.stack.removeAt(i)

        }

        throw IllegalStateException("Cannot peek value from stack.")
    }

    fun peekFind(predicate: Predicate<CodePart>): CodePart {
        this.checkEmpty()

        for (i in this.stack.indices.reversed()) {
            val codePart = this.stack[i]

            if (codePart !is UnknownPart && predicate.test(codePart))
                return codePart
        }

        throw IllegalStateException("Cannot peek value from stack/Cannot find value in stack.")
    }

    fun peek(): CodePart {
        this.checkEmpty()

        for (i in this.stack.indices.reversed()) {
            val codePart = this.stack[i]

            if (codePart !is UnknownPart)
                return codePart
        }

        throw IllegalStateException("Cannot peek value from stack.")
    }

    fun pop(n: Int): List<CodePart> {
        if (n == 0)
            return ArrayList()

        this.checkEmpty(n)

        val intNodes = this.stack.filterWithIndex(StackManager.NOT_IP)

        val size = intNodes.size
        val start = size - n

        val sub = intNodes.subList(start, intNodes.size)

        val result = sub.map { it.value }

        remove(this.stack, sub.map { it.key }.toIntArray())

        return result
    }

    private fun peek(n: Int): List<CodePart> {
        if (n == 0)
            return ArrayList()

        this.checkEmpty(n)

        val collect = this.stack.filter(StackManager.NOT_IP)

        val size = collect.size
        val start = size - n

        return ArrayList(collect.subList(start, size))
    }

    val isEmpty: Boolean
        get() = this.stack.none(StackManager.NOT_IP)

    private fun checkEmpty() {
        if (this.stack.none(StackManager.NOT_IP))
            throw NoSuchElementException("Empty stack.")
    }

    private fun checkEmpty(n: Int) {
        this.checkEmpty()

        if (this.stack.filter(StackManager.NOT_IP).size - n < 0)
            throw NoSuchElementException("Cannot get '" + n + "' elements from stack. Stack size: " + this.stack.size)
    }

    companion object {
        private val NOT_IP = { part: CodePart -> part !is UnknownPart }
    }
}
