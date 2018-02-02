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

import com.github.jonathanxd.kores.Instructions
import com.github.jonathanxd.kores.base.MethodDeclarationBase
import com.github.jonathanxd.kores.bytecodereader.env.EmulatedFrame
import com.github.jonathanxd.kores.bytecodereader.env.Environment
import com.github.jonathanxd.kores.bytecodereader.env.StackManager
import com.github.jonathanxd.kores.bytecodereader.util.asm.VisitTranslator
import com.github.jonathanxd.kores.type.TypeRef
import org.objectweb.asm.tree.*

class FlowedMethodAnalyzer(
    val instructions: InsnList,
    val method: MethodDeclarationBase,
    val methodNode: MethodNode,
    val declaringType: TypeRef,
    val environment: Environment,
    val nodes: List<Bl>
) {
    val frame = EmulatedFrame()
    val bodyStack = StackManager<Instructions>(null)

    @Suppress("UNCHECKED_CAST")
    fun analyze(): MethodDeclarationBase {

        val array = instructions.toArray()

        val localVariables =
            methodNode.localVariables as? List<LocalVariableNode> ?: emptyList()
        val localParameters = methodNode.parameters as? List<ParameterNode> ?: emptyList()
        val exceptionTable =
            methodNode.tryCatchBlocks as? List<TryCatchBlockNode> ?: emptyList()
        //val labels = mutableListOf<LabelNode>()

        VisitTranslator.readVariableTable(
            localVariables,
            this.environment,
            this.frame::storeInfo
        )

        val parameters =
            VisitTranslator.fixParametersNames(method.parameters, localParameters, this.frame)

        VisitTranslator.initMethod(this.method, parameters, this.frame)

        val codeParts = VisitTranslator.visitInsns(
            nodes,
            array,
            exceptionTable,
            declaringType,
            frame,
            environment
        )
        val source = this.method.body.toMutable()

        source.addAll(codeParts)

        return method.builder().parameters(parameters).body(source).build()
    }


}