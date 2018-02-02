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
package com.github.jonathanxd.kores.bytecodereader.util.asm

import com.github.jonathanxd.kores.base.KoresModifier
import org.objectweb.asm.Opcodes

object ModifierUtil {

    var CLASS = 0
    var FIELD = 1
    var METHOD = 2
    var PARAMETER = 3

    fun fromAccess(elementType: Int, access: Int): Set<KoresModifier> {
        val modifiers = java.util.HashSet<KoresModifier>()

        if (elementType == CLASS || elementType == FIELD || elementType == METHOD) {

            if (access eq Opcodes.ACC_PUBLIC) {
                modifiers.add(KoresModifier.PUBLIC)
            } else if (access eq Opcodes.ACC_PRIVATE) {
                modifiers.add(KoresModifier.PRIVATE)
            } else if (access eq Opcodes.ACC_PROTECTED) {
                modifiers.add(KoresModifier.PROTECTED)
            } else {
                modifiers.add(KoresModifier.PACKAGE_PRIVATE)
            }

        }

        if (elementType == FIELD) {
            if (access eq Opcodes.ACC_VOLATILE) {
                modifiers.add(KoresModifier.VOLATILE)
            }

            if (access eq Opcodes.ACC_TRANSIENT) {
                modifiers.add(KoresModifier.TRANSIENT)
            }

        }

        if (elementType == METHOD) {
            if (access eq Opcodes.ACC_SYNCHRONIZED) {
                modifiers.add(KoresModifier.SYNCHRONIZED)
            }

            if (access eq Opcodes.ACC_BRIDGE) {
                modifiers.add(KoresModifier.BRIDGE)
            }

            if (access eq Opcodes.ACC_VARARGS) {
                modifiers.add(KoresModifier.VARARGS)
            }

            if (access eq Opcodes.ACC_NATIVE) {
                modifiers.add(KoresModifier.NATIVE)
            }

            if (access eq Opcodes.ACC_STRICT) {
                modifiers.add(KoresModifier.STRICTFP)
            }

        }

        if (elementType == PARAMETER) {
            if (access eq Opcodes.ACC_MANDATED) {
                modifiers.add(KoresModifier.MANDATED)
            }
        }

        if (elementType == CLASS || elementType == METHOD) {
            if (access eq Opcodes.ACC_ABSTRACT) {
                modifiers.add(KoresModifier.ABSTRACT)
            }
        }

        if (elementType == FIELD || elementType == METHOD) {
            if (access eq Opcodes.ACC_STATIC) {
                modifiers.add(KoresModifier.STATIC)
            }
        }

        if (elementType == CLASS || elementType == FIELD || elementType == METHOD || elementType == PARAMETER) {
            if (access eq Opcodes.ACC_FINAL) {
                modifiers.add(KoresModifier.FINAL)
            }

            if (access eq Opcodes.ACC_SYNTHETIC) {
                modifiers.add(KoresModifier.SYNTHETIC)
            }
        }

        return modifiers
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline infix fun Int.eq(other: Int) = other and this != 0

}