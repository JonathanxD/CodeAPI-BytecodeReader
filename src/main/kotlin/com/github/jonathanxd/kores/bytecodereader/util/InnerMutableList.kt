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
package com.github.jonathanxd.kores.bytecodereader.util

import com.github.jonathanxd.iutils.iterator.IteratorUtil

class InnerMutableList<E>(val outer: MutableList<E>, val inner: MutableList<E>) : MutableList<E> {

    override val size: Int
        get() = this.outer.size + inner.size

    override fun contains(element: E): Boolean =
        element in this.outer || element in this.inner

    override fun containsAll(elements: Collection<E>): Boolean =
        this.outer.containsAll(elements) || this.inner.containsAll(elements)

    override fun get(index: Int): E =
        if (index < this.outer.size) this.outer[index] else this.inner[index - this.outer.size]

    override fun indexOf(element: E): Int =
        this.outer.indexOf(element).let { if (it == -1) this.inner.indexOf(element) + this.outer.size else it }

    override fun isEmpty(): Boolean =
        this.outer.isEmpty() && this.inner.isEmpty()

    override fun iterator(): MutableIterator<E> =
        IteratorUtil.mergeIterator(this.outer.iterator(), this.inner.iterator())

    override fun lastIndexOf(element: E): Int =
        this.outer.lastIndexOf(element).let { if (it == -1) this.inner.lastIndexOf(element) + this.outer.size else it }

    override fun add(element: E): Boolean =
        this.inner.add(element)

    override fun add(index: Int, element: E) =
        this.inner.add(this.outer.size + index, element)

    override fun addAll(index: Int, elements: Collection<E>): Boolean =
        this.inner.addAll(this.outer.size + index, elements)

    override fun addAll(elements: Collection<E>): Boolean =
        this.inner.addAll(elements)

    override fun clear() {
        this.outer.clear()
        this.inner.clear()
    }

    override fun listIterator(): MutableListIterator<E> =
        IteratorUtil.mergeListIterator(this.outer.listIterator(), this.inner.listIterator())

    override fun listIterator(index: Int): MutableListIterator<E> =
        when {
            index < this.outer.size -> IteratorUtil.mergeListIterator(
                this.outer.listIterator(index),
                this.inner.listIterator()
            )
            index == this.outer.size -> this.inner.listIterator()
            else -> this.inner.listIterator(index - this.outer.size)
        }

    override fun remove(element: E): Boolean =
        this.inner.remove(element) || this.outer.remove(element)

    override fun removeAll(elements: Collection<E>): Boolean =
        this.inner.removeAll(elements) || this.outer.removeAll(elements)

    override fun removeAt(index: Int): E =
        when {
            index < this.outer.size -> this.outer.removeAt(index)
            else -> this.inner.removeAt(index - this.outer.size)
        }

    override fun retainAll(elements: Collection<E>): Boolean =
        this.inner.retainAll(elements) || this.outer.removeAll(elements)

    override fun set(index: Int, element: E): E =
        when {
            index < this.outer.size -> this.outer.set(index, element)
            else -> this.inner.set(index + this.outer.size, element)
        }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> =
        when {
            fromIndex < this.outer.size && toIndex < this.outer.size ->
                this.outer.subList(fromIndex, toIndex)
            fromIndex < this.outer.size && toIndex > this.outer.size ->
                InnerMutableList(
                    this.outer.subList(fromIndex, this.outer.size),
                    this.inner.subList(0, toIndex - this.outer.size)
                )
            fromIndex > this.outer.size && toIndex > this.outer.size ->
                this.inner.subList(fromIndex - this.outer.size, toIndex - this.outer.size)
            else -> this
        }

}