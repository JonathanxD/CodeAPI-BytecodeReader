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
package com.github.jonathanxd.bytecodereader.asm

import com.github.jonathanxd.bytecodereader.env.Environment
import com.github.jonathanxd.bytecodereader.util.GenericUtil
import com.github.jonathanxd.bytecodereader.util.asm.ModifierUtil
import com.github.jonathanxd.bytecodereader.util.forEachAs
import com.github.jonathanxd.codeapi.MutableCodeSource
import com.github.jonathanxd.codeapi.base.TypeDeclaration
import com.github.jonathanxd.codeapi.builder.ClassDeclarationBuilder
import com.github.jonathanxd.codeapi.builder.InterfaceDeclarationBuilder
import com.github.jonathanxd.codeapi.read.bytecode.asm.MethodAnalyzer
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.type.GenericType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.util.*
import com.github.jonathanxd.codeapi.bytecodecommon.GenericUtil as CommonGenericUtil

object ClassAnalyzer {

    @Suppress("UNCHECKED_CAST")
    fun analyze(classNode: ClassNode): TypeDeclaration {
        val environment = Environment()

        val modifiers = ModifierUtil.fromAccess(ModifierUtil.CLASS, classNode.access)

        val isInterface = classNode.access and Opcodes.ACC_INTERFACE != 0

        val type = environment.typeResolver.resolve(classNode.name, isInterface)

        var superClass = environment.resolveUnknown(classNode.superName)
        var interfaces = (classNode.interfaces as? List<String>)?.map { environment.resolveUnknown(it) } ?: emptyList()

        val signature = GenericUtil.parseFull(environment.typeResolver, classNode.signature)

        val genericSignature = signature.signature

        if (signature.superType != null)
            superClass = signature.superType

        if (signature.interfaces.isNotEmpty())
            interfaces = signature.interfaces.toList()

        val body = MutableCodeSource()
        val declaration: TypeDeclaration

        if (isInterface) {
            declaration = InterfaceDeclarationBuilder.builder()
                    .withModifiers(EnumSet.copyOf(modifiers))
                    .withQualifiedName(type.canonicalName)
                    .withImplementations(interfaces)
                    .withGenericSignature(genericSignature)
                    .withBody(body)
                    .build()
        } else {
            declaration = ClassDeclarationBuilder.builder()
                    .withModifiers(EnumSet.copyOf(modifiers))
                    .withQualifiedName(type.canonicalName)
                    .withSuperClass(superClass)
                    .withImplementations(interfaces)
                    .withGenericSignature(genericSignature)
                    .withBody(body)
                    .build()
        }

        checkSignature(classNode.signature, declaration, superClass, interfaces)

        environment.data.registerData(TYPE_DECLARATION, declaration)

        classNode.methods?.forEachAs { it: MethodNode ->
            body.add(MethodAnalyzer.analyze(it, environment))
        }

        classNode.fields?.forEachAs { it: FieldNode ->
            body.add(FieldAnalyzer.analyze(it, environment))
        }

        return declaration
    }

    private fun checkSignature(signature: String?, declaration: TypeDeclaration, superClass: CodeType, interfaces: List<CodeType>) {
        val superClassIsGeneric = superClass is GenericType
        val anyInterfaceIsGeneric = interfaces.any { it is GenericType }

        val sign = CommonGenericUtil.genericTypesToAsmString(declaration, superClass, interfaces, superClassIsGeneric, anyInterfaceIsGeneric)

        if (signature != sign) {
            throw IllegalStateException("Signature parsed incorrectly: expected: $signature. current: $sign")
        }
    }

}
