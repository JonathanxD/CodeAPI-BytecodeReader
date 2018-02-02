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
package com.github.jonathanxd.kores.bytecodereader.asm

import com.github.jonathanxd.kores.Instruction
import com.github.jonathanxd.kores.base.FieldDeclaration
import com.github.jonathanxd.kores.bytecodereader.env.Environment
import com.github.jonathanxd.kores.bytecodereader.util.Conversions
import com.github.jonathanxd.kores.bytecodereader.util.GenericUtil
import com.github.jonathanxd.kores.bytecodereader.util.asm.ModifierUtil
import com.github.jonathanxd.kores.common.KoresNothing
import org.objectweb.asm.tree.FieldNode
import java.util.*

object FieldAnalyzer {
    @Suppress("UNCHECKED_CAST")
    fun analyze(fieldNode: FieldNode, environment: Environment): FieldDeclaration {
        val koresModifiers = ModifierUtil.fromAccess(ModifierUtil.FIELD, fieldNode.access)

        var type = environment.resolveUnknown(fieldNode.desc)

        val valuePart: Instruction =
            fieldNode.value?.let { Conversions.toLiteral(it) } ?: KoresNothing

        val genericSignature = GenericUtil.parse(environment, fieldNode.signature)

        if (genericSignature.types.size == 1)
            type = genericSignature.types[0]

        return FieldDeclaration.Builder.builder()
            .modifiers(EnumSet.copyOf(koresModifiers))
            .type(type)
            .name(fieldNode.name)
            .value(valuePart)
            .build()

    }
}
