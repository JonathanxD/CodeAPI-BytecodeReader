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
@file:JvmName("CollectionUtil")
package com.github.jonathanxd.bytecodereader.util

import com.github.jonathanxd.iutils.`object`.IntNode

inline fun <T> List<T>.filterWithIndex(predicate: (T) -> Boolean): MutableList<IntNode<T>> {
    return this.filterIndexedAndMap({ _, t -> predicate(t) }, { i, t -> IntNode(i, t) })
}

inline fun <T> List<T>.filterWithIndex(predicate: (Int, T) -> Boolean): MutableList<IntNode<T>> {
    return this.filterIndexedAndMap(predicate, { i, t -> IntNode(i, t) })
}

fun <T> remove(list: MutableList<T>, indices: IntArray) {
    val iterator = list.iterator()
    var i = 0

    while (iterator.hasNext()) {
        iterator.next()
        for (index in indices) {
            if (index == i) {
                iterator.remove()
                break
            }
        }

        ++i
    }
}

inline fun <T, R> List<T>.filterIndexedAndMap(filter: (Int, T) -> Boolean, mapper: (Int, T) -> R): MutableList<R> {
    val list = mutableListOf<R>()

    for (i in this.indices) {
        val get = this[i]
        if (filter(i, get))
            list.add(mapper(i, get))
    }

    return list
}
