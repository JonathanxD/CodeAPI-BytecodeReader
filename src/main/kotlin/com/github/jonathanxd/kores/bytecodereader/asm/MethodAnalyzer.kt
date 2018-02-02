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

import com.github.jonathanxd.controlflowhelper.BasicBlock
import com.github.jonathanxd.controlflowhelper.Block
import com.github.jonathanxd.controlflowhelper.ControlFlowHelper
import com.github.jonathanxd.controlflowhelper.EdgeTypes
import com.github.jonathanxd.iutils.kt.require
import com.github.jonathanxd.kores.Instruction
import com.github.jonathanxd.kores.Instructions
import com.github.jonathanxd.kores.MutableInstructions
import com.github.jonathanxd.kores.base.ConstructorDeclaration
import com.github.jonathanxd.kores.base.MethodDeclaration
import com.github.jonathanxd.kores.base.MethodDeclarationBase
import com.github.jonathanxd.kores.base.StaticBlock
import com.github.jonathanxd.kores.base.comment.Comments
import com.github.jonathanxd.kores.bytecodereader.env.EmulatedFrame
import com.github.jonathanxd.kores.bytecodereader.env.Environment
import com.github.jonathanxd.kores.bytecodereader.env.StackManager
import com.github.jonathanxd.kores.bytecodereader.extra.InternalPart
import com.github.jonathanxd.kores.bytecodereader.util.Conversions
import com.github.jonathanxd.kores.bytecodereader.util.asm.ModifierUtil
import com.github.jonathanxd.kores.bytecodereader.util.asm.VisitTranslator
import com.github.jonathanxd.kores.factory.parameter
import com.github.jonathanxd.kores.generic.GenericSignature
import com.github.jonathanxd.kores.type.TypeRef
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.util.*
import java.util.logging.Logger

object MethodAnalyzer {
    val logger = Logger.getLogger("Kores_MethodAnalyzer")

    @Suppress("UNCHECKED_CAST")
    fun analyze(
        owner: String,
        methodNode: MethodNode,
        environment: Environment
    ): MethodDeclarationBase {

        val methodName = methodNode.name
        val desc = methodNode.desc
        val asmParameters = methodNode.parameters as? List<ParameterNode> ?: emptyList()

        val modifiers =
            EnumSet.copyOf(ModifierUtil.fromAccess(ModifierUtil.METHOD, methodNode.access))

        val declaration = TYPE_DECLARATION_REF.require(environment.data)

        val typeSpec = Conversions.typeSpecFromDesc(environment, declaration, methodName, desc)

        val parameters = typeSpec.parameterTypes.mapIndexed { i, koresType ->

            val name = if (asmParameters.size > i) asmParameters[i].name else "param$i"

            parameter(type = koresType, name = name)
        }

        val exceptions = methodNode.exceptions.map { environment.resolveUnknown(it as String) }

        val method: MethodDeclarationBase =
            when (methodName) {
                "<init>" -> ConstructorDeclaration(
                    modifiers = modifiers,
                    parameters = parameters,
                    body = MutableInstructions.create(),
                    annotations = emptyList(),
                    genericSignature = GenericSignature.empty(),
                    comments = Comments.Absent,
                    innerTypes = emptyList(),
                    throwsClause = exceptions
                )
                "<clinit>" -> StaticBlock(
                    Comments.Absent,
                    emptyList(),
                    MutableInstructions.create()
                )
                else -> MethodDeclaration(
                    name = methodName,
                    modifiers = modifiers,
                    parameters = parameters,
                    returnType = typeSpec.returnType,
                    body = MutableInstructions.create(),
                    annotations = emptyList(),
                    genericSignature = GenericSignature.empty(),
                    comments = Comments.Absent,
                    innerTypes = emptyList(),
                    throwsClause = exceptions
                )
            }

        val instructions = methodNode.instructions

        val nodes = exprs(ControlFlowHelper.analyze(methodNode), methodNode)

        val analyze =
            FlowedMethodAnalyzer(instructions, method, methodNode, declaration, environment, nodes)

        return analyze.analyze()

    }

    private fun exprs(blocks: List<BasicBlock>, node: MethodNode): List<Bl> {
        val list = mutableListOf<Bl>()
        val insns = node.instructions.toArray().toList()

        blocks.forEach {
            if (it is Block) {
                val blockInsns = it.insns

                list += if (blockInsns.lastOrNull() is JumpInsnNode) {
                    val node = blockInsns.lastOrNull() as JumpInsnNode
                    val true_ = it.successors.firstOrNull { it.type == EdgeTypes.trueType }
                    val false_ = it.successors.firstOrNull { it.type == EdgeTypes.falseType }

                    Bl.UnIfExprBlock(
                        blockInsns,
                        it,
                        node.label,
                        true_?.dest,
                        false_?.dest
                    )
                } else {
                    Bl.NormalBlock(blockInsns, it)
                }
            }
        }

        val full = list.getFull()
        return full
    }

    fun List<Bl>.getFull(): List<Bl> {
        val newList = mutableListOf<Bl>()

        return this.mapTo(newList) {
            when (it) {
                is Bl.UnIfExprBlock -> {
                    Bl.IfExprBlock(it.nodes,
                        it.block,
                        it.target,
                        lazy(LazyThreadSafetyMode.NONE) {
                            newList.firstOrNull { i -> i.block == it.success }
                        },
                        lazy(LazyThreadSafetyMode.NONE) {
                            newList.firstOrNull { i -> i.block == it.fail }
                        })
                }
                else -> it
            }
        }.also {
                it.forEach {
                    if (it is Bl.IfExprBlock) {
                        it.success.value
                        it.fail.value
                    }
                }
            }
    }


    fun List<Bl>.getSuccessBl(expr: Bl.UnIfExprBlock): Bl? =
        this.firstOrNull { it.block == expr.success }

    fun List<Bl>.getFailBl(expr: Bl.UnIfExprBlock): Bl? =
        this.firstOrNull { it.block == expr.fail }


    class Analyze(
        val instructions: InsnList,
        val method: MethodDeclarationBase,
        val methodNode: MethodNode,
        val declaringType: TypeRef,
        val environment: Environment
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

            array.forEachIndexed { i, it ->

                when (it) { // TODO: Handle JumpNode
                    is InsnNode -> this.visitInsn(it.opcode)
                    is VarInsnNode -> this.visitVarInsn(it.opcode, it.`var`)
                    is IntInsnNode -> this.visitIntInsn(it.opcode, it.operand)
                    is TypeInsnNode -> this.visitTypeInsn(it.opcode, it.desc)
                    is FieldInsnNode -> this.visitFieldInsn(it.opcode, it.owner, it.name, it.desc)
                    is MethodInsnNode -> this.visitMethodInsn(
                        it.opcode,
                        it.owner,
                        it.name,
                        it.desc,
                        it.itf
                    )
                    is InvokeDynamicInsnNode -> this.visitInvokeDynamicInsn(
                        it.name,
                        it.desc,
                        it.bsm,
                        it.bsmArgs
                    )
                    is LdcInsnNode -> this.visitLdcInsn(it.cst)
                    is IincInsnNode -> this.visitIincInsn(it.`var`, it.incr)
                    is JumpInsnNode -> this.visitJumpInsn(array, it.opcode, i, it.label)
                    is LabelNode -> {
                        //labels += it
                        this.visitLabel(array, i, it)
                        it.accept(OperandAddVisitor(this.frame.operandStack))
                        this.handleExceptionTable(array, i, exceptionTable, it)
                    }
                    else -> {
                        it.accept(OperandAddVisitor(this.frame.operandStack))
                        logger.warning("Insn '$it' isn't supported yet.")
                    }
                }

            }

            val codeParts =
                this.frame.operandStack.popAllFilterExcluded().filter { it !is InternalPart }

            val source = this.method.body.toMutable()

            source.addAll(codeParts)

            return method.builder().parameters(parameters).body(source).build()
        }

        fun visitJumpInsn(
            insns: Array<AbstractInsnNode>,
            opcode: Int,
            index: Int,
            label: LabelNode
        ) {
            VisitTranslator.visitJumpInsn(
                opcode,
                label,
                frame
            )?.pushToOperand()
        }

        fun visitLabel(insns: Array<AbstractInsnNode>, index: Int, label: LabelNode) {
            VisitTranslator.visitLabel(
                insns,
                label,
                index,
                frame,
                environment.data
            )?.pushToOperand()
        }

        fun handleExceptionTable(
            insns: Array<AbstractInsnNode>,
            index: Int,
            tryCatchBlocks: List<TryCatchBlockNode>,
            label: LabelNode
        ) {
            VisitTranslator.handleExceptionTable(
                insns,
                index,
                tryCatchBlocks,
                label,
                environment,
                frame,
                environment.data
            )
                .forEach { it.pushToOperand() }
        }

        fun visitInsn(opcode: Int) {
            if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN && this.method is StaticBlock)
                return

            VisitTranslator.visitInsn(opcode, this.frame, this.environment.data)?.pushToOperand()
        }

        fun visitVarInsn(opcode: Int, slot: Int) {
            VisitTranslator.visitVarInsn(opcode, slot, this.frame)?.pushToOperand()
        }

        fun visitIntInsn(opcode: Int, operand: Int) {
            VisitTranslator.visitIntInsn(opcode, operand).pushToOperand()
        }

        fun visitTypeInsn(opcode: Int, type: String) {
            VisitTranslator.visitTypeInsn(opcode, type, this.environment, this.frame)
                .pushToOperand()
        }

        fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String) {
            VisitTranslator.visitFieldInsn(opcode, owner, name, desc, this.environment, this.frame)
                .pushToOperand()
        }

        fun visitLdcInsn(cst: Any) {
            VisitTranslator.visitLdcInsn(cst).pushToOperand()
        }

        fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
            VisitTranslator.visitMethodInsn(
                opcode,
                owner,
                name,
                desc,
                itf,
                declaringType,
                this.environment,
                this.frame
            ).pushToOperand()
        }

        fun visitInvokeDynamicInsn(name: String, desc: String, bsm: Handle, bsmArgs: Array<Any>) {
            VisitTranslator.visitDynamicMethodInsn(
                name,
                desc,
                bsm,
                bsmArgs,
                this.environment,
                this.frame
            ).pushToOperand()

        }

        fun visitIincInsn(slot: Int, increment: Int) {
            VisitTranslator.visitIincInsn(slot, increment, this.frame).pushToOperand()
        }


        private fun Instruction.pushToOperand() {
            frame.operandStack.push(this)
            /*if (bodyStack.isEmpty) {
                frame.operandStack.push(this)
            } else {
                val peek = bodyStack.peek()

                if (peek !is MutableInstructions) {
                    ((peek as BodyHolder).body as MutableInstructions).add(this)
                } else {
                    peek.add(this)
                }
                frame.operandStack.pushExcluded(this)
            }*/
        }
    }

}