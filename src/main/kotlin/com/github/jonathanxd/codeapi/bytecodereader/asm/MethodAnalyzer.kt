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
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.MutableCodeSource
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.base.comment.Comments
import com.github.jonathanxd.codeapi.bytecodereader.env.EmulatedFrame
import com.github.jonathanxd.codeapi.bytecodereader.env.Environment
import com.github.jonathanxd.codeapi.bytecodereader.env.StackManager
import com.github.jonathanxd.codeapi.bytecodereader.util.Conversions
import com.github.jonathanxd.codeapi.bytecodereader.util.asm.Ignore
import com.github.jonathanxd.codeapi.bytecodereader.util.asm.ModifierUtil
import com.github.jonathanxd.codeapi.bytecodereader.util.asm.VisitTranslator
import com.github.jonathanxd.codeapi.factory.parameter
import com.github.jonathanxd.codeapi.generic.GenericSignature
import com.github.jonathanxd.codeapi.type.TypeRef
import com.github.jonathanxd.codeapi.util.require
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.util.EnumSet
import java.util.logging.Logger

object MethodAnalyzer {
    private val logger = Logger.getLogger("CodeAPI_MethodAnalyzer")

    @Suppress("UNCHECKED_CAST")
    fun analyze(methodNode: MethodNode, environment: Environment): MethodDeclarationBase {
        val methodName = methodNode.name
        val desc = methodNode.desc
        val asmParameters = methodNode.parameters as? List<ParameterNode> ?: emptyList()

        val modifiers = EnumSet.copyOf(ModifierUtil.fromAccess(ModifierUtil.METHOD, methodNode.access))

        val declaration = TYPE_DECLARATION_REF.require(environment.data)

        val typeSpec = Conversions.typeSpecFromDesc(environment, declaration, methodName, desc)

        val parameters = typeSpec.parameterTypes.mapIndexed { i, codeType ->

            val name = if (asmParameters.size > i) asmParameters[i].name else "param$i"

            parameter(type = codeType, name = name)
        }

        val exceptions = methodNode.exceptions.map { environment.resolveUnknown(it as String) }

        val method: MethodDeclarationBase =
                when (methodName) {
                    "<init>" -> ConstructorDeclaration(
                            modifiers = modifiers,
                            parameters = parameters,
                            body = MutableCodeSource.create(),
                            annotations = emptyList(),
                            genericSignature = GenericSignature.Companion.empty(),
                            comments = Comments.Absent,
                            innerTypes = emptyList(),
                            throwsClause = exceptions)
                    "<clinit>" -> StaticBlock(Comments.Absent, emptyList(), MutableCodeSource.create())
                    else -> MethodDeclaration(
                            name = methodName,
                            modifiers = modifiers,
                            parameters = parameters,
                            returnType = typeSpec.returnType,
                            body = MutableCodeSource.create(),
                            annotations = emptyList(),
                            genericSignature = GenericSignature.Companion.empty(),
                            comments = Comments.Absent,
                            innerTypes = emptyList(),
                            throwsClause = exceptions)
                }

        val instructions = methodNode.instructions

        val analyze = Analyze(instructions, method, methodNode, declaration, environment)

        return analyze.analyze()

    }

    class Analyze(val instructions: InsnList,
                  val method: MethodDeclarationBase,
                  val methodNode: MethodNode,
                  val declaringType: TypeRef,
                  val environment: Environment) {

        val frame = EmulatedFrame()
        val bodyStack = StackManager<CodeSource>()

        @Suppress("UNCHECKED_CAST")
        fun analyze(): MethodDeclarationBase {

            val array = instructions.toArray()

            val localVariables = methodNode.localVariables as? List<LocalVariableNode> ?: emptyList()
            val localParameters = methodNode.parameters as? List<ParameterNode> ?: emptyList()
            val exceptionTable = methodNode.tryCatchBlocks as? List<TryCatchBlockNode> ?: emptyList()
            //val labels = mutableListOf<LabelNode>()

            VisitTranslator.readVariableTable(localVariables, this.environment, this.frame::storeInfo)

            val parameters = VisitTranslator.fixParametersNames(method.parameters, localParameters, this.frame)
            var ignore = Ignore(intArrayOf())

            VisitTranslator.initMethod(this.method, parameters, this.frame)

            array.forEachIndexed { i, it ->

                if (it is LabelNode) {
                    ignore = this.handleExceptionTable(array, i, exceptionTable, it)
                }

                if (!ignore.indexes.contains(i)) {
                    when (it) { // TODO: Handle JumpNode
                        is InsnNode -> this.visitInsn(it.opcode)
                        is VarInsnNode -> this.visitVarInsn(it.opcode, it.`var`)
                        is IntInsnNode -> this.visitIntInsn(it.opcode, it.operand)
                        is TypeInsnNode -> this.visitTypeInsn(it.opcode, it.desc)
                        is FieldInsnNode -> this.visitFieldInsn(it.opcode, it.owner, it.name, it.desc)
                        is MethodInsnNode -> this.visitMethodInsn(it.opcode, it.owner, it.name, it.desc, it.itf)
                        is InvokeDynamicInsnNode -> this.visitInvokeDynamicInsn(it.name, it.desc, it.bsm, it.bsmArgs)
                        is LdcInsnNode -> this.visitLdcInsn(it.cst)
                        is IincInsnNode -> this.visitIincInsn(it.`var`, it.incr)
                        is JumpInsnNode -> this.visitJumpInsn(it.opcode, it.label)
                        is LabelNode -> {
                            //labels += it
                            it.accept(OperandAddVisitor(this.frame.operandStack))
                        }
                        else -> {
                            it.accept(OperandAddVisitor(this.frame.operandStack))
                            logger.warning("Insn '$it' isn't supported yet.")
                        }
                    }
                }

            }

            val codeParts = this.frame.operandStack.popAll()

            val source = this.method.body.toMutable()

            source.addAll(codeParts)

            return method.builder().parameters(parameters).body(source).build()
        }

        fun visitJumpInsn(opcode: Int, label: LabelNode) {
            VisitTranslator.visitJumpInsn(opcode, label, bodyStack, environment, frame, environment.data)?.pushToOperand()
        }

        fun handleExceptionTable(insns: Array<AbstractInsnNode>, index: Int, tryCatchBlocks: List<TryCatchBlockNode>, label: LabelNode): Ignore {
            return VisitTranslator.handleExceptionTable(insns, index, tryCatchBlocks, label, bodyStack, environment, frame, environment.data)
        }

        fun visitInsn(opcode: Int) {
            if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN && this.method is StaticBlock)
                return

            VisitTranslator.visitInsn(opcode, this.frame)?.pushToOperand()
        }

        fun visitVarInsn(opcode: Int, slot: Int) {
            VisitTranslator.visitVarInsn(opcode, slot, this.frame)?.pushToOperand()
        }

        fun visitIntInsn(opcode: Int, operand: Int) {
            VisitTranslator.visitIntInsn(opcode, operand).pushToOperand()
        }

        fun visitTypeInsn(opcode: Int, type: String) {
            VisitTranslator.visitTypeInsn(opcode, type, this.environment, this.frame).pushToOperand()
        }

        fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String) {
            VisitTranslator.visitFieldInsn(opcode, owner, name, desc, this.environment, this.frame).pushToOperand()
        }

        fun visitLdcInsn(cst: Any) {
            VisitTranslator.visitLdcInsn(cst).pushToOperand()
        }

        fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
            VisitTranslator.visitMethodInsn(opcode, owner, name, desc, itf, declaringType, this.environment, this.frame).pushToOperand()
        }

        fun visitInvokeDynamicInsn(name: String, desc: String, bsm: Handle, bsmArgs: Array<Any>) {
            VisitTranslator.visitDynamicMethodInsn(name, desc, bsm, bsmArgs, this.environment, this.frame).pushToOperand()

        }

        fun visitIincInsn(slot: Int, increment: Int) {
            VisitTranslator.visitIincInsn(slot, increment, this.frame).pushToOperand()
        }


        private fun CodeInstruction.pushToOperand() {
            if (bodyStack.isEmpty) {
                frame.operandStack.push(this)
            } else {
                val peek = bodyStack.peek()

                if (peek !is MutableCodeSource) {
                    ((peek as BodyHolder).body as MutableCodeSource).add(this)
                } else {
                    peek.add(this)
                }
            }
        }
    }

}
