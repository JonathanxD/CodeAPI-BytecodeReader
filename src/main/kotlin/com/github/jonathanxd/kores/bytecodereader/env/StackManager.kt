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

import com.github.jonathanxd.kores.bytecodereader.extra.InternalPart
import com.github.jonathanxd.kores.bytecodereader.util.InnerMutableList
import com.github.jonathanxd.kores.bytecodereader.util.IntNode
import com.github.jonathanxd.kores.bytecodereader.util.filterWithIndex
import com.github.jonathanxd.kores.bytecodereader.util.remove
import java.util.*

class StackManager<E>(val parent: StackManager<E>?) {

    val ofThis = mutableListOf<E>()
    val stack: MutableList<E> =
        if (parent != null) InnerMutableList(parent.stack.toMutableList(), ofThis)
        else ofThis

    val excludedIndexes = mutableListOf<Int>()


    val size: Int
        get() = stack.size

    fun dup() {
        this.push(this.peek())
    }

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

    fun replaceFirst(predicate: (E) -> Boolean, transformer: (E) -> E) {
        this.stack.indices.reversed().forEach { index ->
            val v = this.stack[index]
            if (predicate(v)) {
                this.stack[index] = transformer(v)
                return@replaceFirst
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> popAs(): T = this.popAs(T::class.java)

    fun popAll(): List<E> {
        val popped = ArrayList(this.ofThis)

        this.ofThis.clear()

        return popped
    }

    fun popAllFilterExcluded(): List<E> {
        val popped = ArrayList(filter(this.ofThis))

        this.ofThis.clear()

        return popped
    }

    @Suppress("UNCHECKED_CAST")
    fun pop(): E {
        this.checkEmpty()

        for (i in this.stack.indices.reversed()) {
            this.stack[i] as? InternalPart
                    ?: return this.stack.removeAt(i)
        }

        throw IllegalStateException("Cannot pop value from stack.")
    }

    @Suppress("UNCHECKED_CAST")
    fun pop(filter: (E) -> Boolean): E {
        this.checkEmpty()

        for (i in this.stack.indices.reversed()) {
            val part = this.stack[i]

            if (filter(part)) {
                this.stack.removeAt(i)
                return part
            }

        }

        throw IllegalStateException("Cannot pop value from stack.")
    }


    fun peekFind(predicate: (E) -> Boolean): IntNode<E> =
        peekFindOrNull(predicate)
                ?: throw IllegalStateException("Cannot peek value from stack/Cannot find value in stack.")

    fun peekFindOrNull(predicate: (E) -> Boolean): IntNode<E>? {
        if (this.isEmpty)
            return null

        this.checkEmpty()

        for (i in this.stack.indices.reversed()) {
            val instruction = this.stack[i]

            if (predicate(instruction))
                return IntNode(i, instruction)
        }

        return null
    }

    fun peekFindReversed(predicate: (E) -> Boolean): IntNode<E> =
        peekFindReversedOrNull(predicate)
                ?: throw IllegalStateException("Cannot peek value from stack/Cannot find value in stack.")

    fun peekFindReversedOrNull(predicate: (E) -> Boolean): IntNode<E>? {
        if (this.isEmpty)
            return null

        this.checkEmpty()

        for (i in this.stack.indices) {
            val instruction = this.stack[i]

            if (predicate(instruction))
                return IntNode(i, instruction)
        }

        return null
    }

    fun peekAll(): List<E> = this.stack.toList()

    fun peek(): E {
        this.checkEmpty()

        for (i in this.stack.indices.reversed()) {
            val instruction = this.stack[i]

            if (instruction !is InternalPart)
                return instruction
        }

        throw IllegalStateException("Cannot peek value from stack.")
    }

    fun filter(list: List<E>): List<E> =
        list.filterIndexed { index, i -> index !in this.excludedIndexes && i !is InternalPart }

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

    val isNotEmpty: Boolean
        get() = !this.isEmpty

    val currentIsEmpty get() = this.ofThis.none(StackManager.NOT_IP())
    val currentIsNotEmpty get() = !this.currentIsEmpty

    private fun checkEmpty() {
        if (this.stack.none(StackManager.NOT_IP()))
            throw NoSuchElementException("Empty stack.")
    }

    private fun checkEmpty(n: Int) {
        this.checkEmpty()

        if (this.stack.filter(StackManager.NOT_IP()).size - n < 0)
            throw NoSuchElementException("Cannot get '" + n + "' elements from stack. Stack size: " + this.stack.size)
    }

    fun copy(): StackManager<E> = StackManager(this.parent).also {
        it.ofThis.addAll(this.ofThis)
    }

    companion object {

        fun <E> NOT_IP() = { part: E -> part !is InternalPart }
    }
}
