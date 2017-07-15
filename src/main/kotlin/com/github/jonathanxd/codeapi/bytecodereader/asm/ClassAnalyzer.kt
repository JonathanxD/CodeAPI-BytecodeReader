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

import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.base.comment.Comments
import com.github.jonathanxd.codeapi.bytecodereader.env.Environment
import com.github.jonathanxd.codeapi.bytecodereader.util.GenericUtil
import com.github.jonathanxd.codeapi.bytecodereader.util.asm.ModifierUtil
import com.github.jonathanxd.codeapi.bytecodereader.util.forEachAs
import com.github.jonathanxd.codeapi.type.GenericType
import com.github.jonathanxd.codeapi.type.TypeRef
import com.github.jonathanxd.codeapi.util.`is`
import com.github.jonathanxd.codeapi.util.canonicalName
import com.github.jonathanxd.codeapi.util.codeType
import com.github.jonathanxd.codeapi.util.genericTypesToDescriptor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Type
import java.util.EnumSet

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

        val innerTypes = mutableListOf<TypeDeclaration>()
        val fields = mutableListOf<FieldDeclaration>()
        val constructors = mutableListOf<ConstructorDeclaration>()
        val methods = mutableListOf<MethodDeclaration>()
        var staticBlock = StaticBlock(Comments.Absent, emptyList(), CodeSource.empty())

        val ref = TypeRef(type.canonicalName, isInterface)

        TYPE_DECLARATION_REF.set(environment.data, ref)

        classNode.methods?.forEachAs { it: MethodNode ->
            val analyze = MethodAnalyzer.analyze(classNode.name, it, environment)

            when (analyze) {
                is ConstructorDeclaration -> constructors += analyze
                is MethodDeclaration -> methods += analyze
                else -> staticBlock = analyze as StaticBlock
            }
        }

        classNode.fields?.forEachAs { it: FieldNode ->
            fields += FieldAnalyzer.analyze(it, environment)
        }

        val builder: TypeDeclaration.Builder<TypeDeclaration, *> = if (isInterface) {
            InterfaceDeclaration.Builder.builder()
                    .implementations(interfaces)
        } else {
            ClassDeclaration.Builder.builder()
                    .superClass(superClass)
                    .implementations(interfaces)
        }

        val declaration = builder
                .modifiers(EnumSet.copyOf(modifiers))
                .qualifiedName(type.canonicalName)
                .genericSignature(genericSignature)
                .staticBlock(staticBlock)
                .fields(fields)
                .constructors(constructors)
                .methods(methods)
                .innerTypes(innerTypes)
                .build()


        checkSignature(classNode.signature, declaration, superClass, interfaces)

        return declaration
    }

    private fun checkSignature(signature: String?, declaration: TypeDeclaration, superClass: Type, interfaces: List<Type>) {
        val superClassIsGeneric = superClass.codeType is GenericType
        val anyInterfaceIsGeneric = interfaces.any { it.codeType is GenericType }

        val sign = genericTypesToDescriptor(declaration, superClass, interfaces, superClassIsGeneric, anyInterfaceIsGeneric)

        if (signature != sign) {
            throw IllegalStateException("Signature parsed incorrectly: expected: $signature. current: $sign")
        }
    }

}
