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
package com.github.jonathanxd.codeapi.bytecodereader.env

import com.github.jonathanxd.codeapi.bytecodereader.extra.InternalPart
import com.github.jonathanxd.codeapi.bytecodereader.extra.MagicPart
import com.github.jonathanxd.codeapi.bytecodereader.util.IntNode
import com.github.jonathanxd.codeapi.bytecodereader.util.filterWithIndex
import com.github.jonathanxd.codeapi.bytecodereader.util.remove
import org.objectweb.asm.Label
import java.util.ArrayList
import java.util.NoSuchElementException

class StackManager<E> {

    private val stack = mutableListOf<E>()
    val excludedIndexes = mutableListOf<Int>()

    val size: Int
        get() = stack.size

    fun push(part: E) {
        this.stack.add(part)
    }

    fun pushExcluded(part: E) {
        this.stack.add(part)
        excludedIndexes += stack.lastIndex
    }

    fun push(parts: List<E>) {
        this.stack.addAll(parts)
    }

    fun filterList(predicate: (E) -> Boolean) = stack.filter(predicate)

    fun removeIf(predicate: (E) -> Boolean) = stack.removeIf(predicate)

    @Suppress("UNCHECKED_CAST")
    fun <T> popAs(type: Class<T>): T {
        val pop = this.pop()

        if (!type.isInstance(pop))
            throw ClassCastException("Value '$pop' popped from stack cannot be cast to '$type'!")

        return pop as T
    }

    fun popAll(): List<E> {
        val popped = ArrayList(this.stack)

        this.stack.clear()

        return popped
    }

    fun popAllFilterExcluded(): List<E> {
        val popped = ArrayList(filter(this.stack))

        this.stack.clear()

        return popped
    }

    @Suppress("UNCHECKED_CAST")
    fun pop(): E {
        this.checkEmpty()

        for (i in this.stack.indices.reversed()) {
            val part = this.stack[i] as? InternalPart ?: return this.stack.removeAt(i)

            if (part !is InternalPart) // TODO: review
                return part as E
        }

        throw IllegalStateException("Cannot peek value from stack.")
    }


    fun peekFind(predicate: (E) -> Boolean): IntNode<E> =
            peekFindOrNull(predicate)
                    ?: throw IllegalStateException("Cannot peek value from stack/Cannot find value in stack.")

    fun peekFindOrNull(predicate: (E) -> Boolean): IntNode<E>? {
        this.checkEmpty()

        for (i in this.stack.indices.reversed()) {
            val codeInstruction = this.stack[i]

            if (predicate(codeInstruction))
                return IntNode(i, codeInstruction)
        }

        return null
    }

    fun peek(): E {
        this.checkEmpty()

        for (i in this.stack.indices.reversed()) {
            val codeInstruction = this.stack[i]

            if (codeInstruction !is InternalPart)
                return codeInstruction
        }

        throw IllegalStateException("Cannot peek value from stack.")
    }

    fun filter(list: MutableList<E>): List<E> =
            list.filterIndexed { index, i -> index !in this.excludedIndexes }

    fun pop(n: Int): List<E> {
        if (n == 0)
            return ArrayList()

        this.checkEmpty(n)

        val intNodes = this.stack.filterWithIndex(StackManager.NOT_IP())

        val size = intNodes.size
        val start = size - n

        val sub = intNodes.subList(start, intNodes.size)

        val result = sub.map { it.second }

        remove(this.stack, sub.map { it.first }.toIntArray())

        return result
    }

    fun sub(start: Int, end: Int): MutableList<E> {
        return this.stack.subList(start, end)
    }

    private fun peek(n: Int): List<E> {
        if (n == 0)
            return ArrayList()

        this.checkEmpty(n)

        val collect = this.stack.filter(StackManager.NOT_IP())

        val size = collect.size
        val start = size - n

        return ArrayList(collect.subList(start, size))
    }

    val isEmpty: Boolean
        get() = this.stack.none(StackManager.NOT_IP())

    private fun checkEmpty() {
        if (this.stack.none(StackManager.NOT_IP()))
            throw NoSuchElementException("Empty stack.")
    }

    private fun checkEmpty(n: Int) {
        this.checkEmpty()

        if (this.stack.filter(StackManager.NOT_IP()).size - n < 0)
            throw NoSuchElementException("Cannot get '" + n + "' elements from stack. Stack size: " + this.stack.size)
    }

    companion object {

        fun <E> NOT_IP() = { part: E -> part !is InternalPart }
    }
}
