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
package com.github.jonathanxd.codeapi.bytecodereader.util

import com.github.jonathanxd.codeapi.CodeInstruction
import com.github.jonathanxd.codeapi.bytecodereader.env.StackManager
import org.objectweb.asm.Label
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.util.Printer

/**
 * Unsafe treat **this** [Iterable] as [Iterable] of [U]
 */
@Suppress("UNCHECKED_CAST")
inline fun <U> Iterable<*>.unsafeForEach(func: (U) -> Unit) {
    (this as Iterable<U>).forEach(func)
}

/**
 * Cast the elements of **this** [Iterable] to [U].
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified U> Iterable<*>.mapAs() = this.map { it as U }

/**
 * Foreach elements of this [Iterable] as [U]
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified U> Iterable<*>.forEachAs(func: (U) -> Unit) = this.mapAs<U>().forEach(func)

inline val Int.opcodeName: String get() = Printer.OPCODES[this].toLowerCase()


/**
 * Finds [label] and returns the index of label (or -1 if label cannot be found).
 */
fun findLabel(stackManager: StackManager<CodeInstruction>, insns: Array<AbstractInsnNode>, label: Label): Int =
        /*stackManager.peekFindOrNull { it is MagicPart && it.obj == label }?.index*/
        insns.withIndex().find { it.value is LabelNode && (it.value as LabelNode).label == label }?.index
                ?: -1

inline fun loopIgnoringUseless(insns: Array<AbstractInsnNode>,
                               start: Int,
                               useLess: (AbstractInsnNode) -> Boolean,
                               func: (Int, AbstractInsnNode) -> Boolean) {

    for (i in start..insns.size - 1) {
        if (!useLess(insns[i]))
            if (!func(i, insns[i]))
                break
    }
}