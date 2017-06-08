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
package com.github.jonathanxd.codeapi.bytecodereader.asm

import com.github.jonathanxd.codeapi.CodeInstruction
import com.github.jonathanxd.codeapi.base.FieldDeclaration
import com.github.jonathanxd.codeapi.bytecodereader.env.Environment
import com.github.jonathanxd.codeapi.bytecodereader.util.Conversions
import com.github.jonathanxd.codeapi.bytecodereader.util.GenericUtil
import com.github.jonathanxd.codeapi.bytecodereader.util.asm.ModifierUtil
import com.github.jonathanxd.codeapi.common.CodeNothing
import org.objectweb.asm.tree.FieldNode
import java.util.EnumSet

object FieldAnalyzer {
    @Suppress("UNCHECKED_CAST")
    fun analyze(fieldNode: FieldNode, environment: Environment): FieldDeclaration {
        val codeModifiers = ModifierUtil.fromAccess(ModifierUtil.FIELD, fieldNode.access)

        var type = environment.resolveUnknown(fieldNode.desc)

        val valuePart: CodeInstruction = fieldNode.value?.let { Conversions.toLiteral(it) } ?: CodeNothing

        val genericSignature = GenericUtil.parse(environment, fieldNode.signature)

        if (genericSignature != null && genericSignature.types.size == 1)
            type = genericSignature.types[0]

        return FieldDeclaration.Builder.builder()
                .modifiers(EnumSet.copyOf(codeModifiers))
                .type(type)
                .name(fieldNode.name)
                .value(valuePart)
                .build()

    }
}
