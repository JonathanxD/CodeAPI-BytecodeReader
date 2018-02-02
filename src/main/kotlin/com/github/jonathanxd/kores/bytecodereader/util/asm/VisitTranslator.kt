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

import com.github.jonathanxd.controlflowhelper.BasicBlock
import com.github.jonathanxd.controlflowhelper.JumpEdgeType
import com.github.jonathanxd.iutils.annotation.Named
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.description.DescriptionUtil
import com.github.jonathanxd.iutils.iterator.IteratorUtil
import com.github.jonathanxd.iutils.kt.add
import com.github.jonathanxd.iutils.kt.typedKeyOf
import com.github.jonathanxd.iutils.map.ListHashMap
import com.github.jonathanxd.iutils.recursion.Element
import com.github.jonathanxd.iutils.recursion.ElementUtil
import com.github.jonathanxd.iutils.recursion.Elements
import com.github.jonathanxd.kores.*
import com.github.jonathanxd.kores.base.*
import com.github.jonathanxd.kores.builder.Builder
import com.github.jonathanxd.kores.bytecodereader.asm.Bl
import com.github.jonathanxd.kores.bytecodereader.asm.MethodAnalyzer
import com.github.jonathanxd.kores.bytecodereader.asm.OperandAddVisitor
import com.github.jonathanxd.kores.bytecodereader.env.EmulatedFrame
import com.github.jonathanxd.kores.bytecodereader.env.Environment
import com.github.jonathanxd.kores.bytecodereader.extra.InternalPart
import com.github.jonathanxd.kores.bytecodereader.extra.MagicPart
import com.github.jonathanxd.kores.bytecodereader.extra.UnknownPart
import com.github.jonathanxd.kores.bytecodereader.util.*
import com.github.jonathanxd.kores.common.CONSTRUCTOR
import com.github.jonathanxd.kores.common.DynamicMethodSpec
import com.github.jonathanxd.kores.common.KoresNothing
import com.github.jonathanxd.kores.common.VariableRef
import com.github.jonathanxd.kores.factory.*
import com.github.jonathanxd.kores.literal.Literals
import com.github.jonathanxd.kores.operator.Operators
import com.github.jonathanxd.kores.type.*
import com.github.jonathanxd.kores.util.TypeResolver
import com.github.jonathanxd.kores.util.resolveUnknown
import com.github.jonathanxd.kores.util.toTypeSpec
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.util.ArrayList
import java.util.Arrays
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.NoSuchElementException

object VisitTranslator {

    private val logger = Logger.getLogger("Kores_Translator")

    val LAST_DUP_VARS = typedKeyOf<MutableList<VariableRef>>("LAST_DUP_VARS")
    val TRY_DATA = typedKeyOf<MutableList<TryData>>("TRY_DATA")

    val CATCH_BLOCKS = typedKeyOf<ListHashMap<@Named("Start") Label, CatchData>>("CATCH_BLOCKS")

    fun visitInsns(
        blocks: List<Bl>,
        array: Array<AbstractInsnNode>,
        exceptionTable: List<TryCatchBlockNode>,
        declaringType: TypeRef,
        frame: EmulatedFrame,
        environment: Environment
    ): List<Instruction> {
        fun Instruction?.push() = this?.let { frame.operandStack.push(it) }

        fun visitNode(index: Int, insn: AbstractInsnNode) {
            when (insn) { // TODO: Handle JumpNode
                is InsnNode -> this.visitInsn(insn.opcode, frame, environment.data).push()
                is VarInsnNode -> this.visitVarInsn(insn.opcode, insn.`var`, frame).push()
                is IntInsnNode -> this.visitIntInsn(insn.opcode, insn.operand).push()
                is TypeInsnNode -> this.visitTypeInsn(
                    insn.opcode,
                    insn.desc,
                    environment,
                    frame
                ).push()
                is FieldInsnNode -> this.visitFieldInsn(
                    insn.opcode,
                    insn.owner,
                    insn.name,
                    insn.desc,
                    environment,
                    frame
                ).push()
                is MethodInsnNode -> this.visitMethodInsn(
                    insn.opcode,
                    insn.owner,
                    insn.name,
                    insn.desc,
                    insn.itf,
                    declaringType,
                    environment,
                    frame
                ).push()
                is InvokeDynamicInsnNode -> this.visitDynamicMethodInsn(
                    insn.name,
                    insn.desc,
                    insn.bsm,
                    insn.bsmArgs,
                    environment,
                    frame
                ).push()
                is LdcInsnNode -> this.visitLdcInsn(insn.cst).push()
                is IincInsnNode -> this.visitIincInsn(insn.`var`, insn.incr, frame).push()
                is JumpInsnNode -> this.visitJumpInsn(
                    insn.opcode,
                    insn.label,
                    frame
                ).push()
                is LabelNode -> {
                    //labels += insn
                    this.visitLabel(array, insn, index, frame, environment.data).push()
                    insn.accept(OperandAddVisitor(frame.operandStack))
                    this.handleExceptionTable(
                        array,
                        index,
                        exceptionTable,
                        insn,
                        environment,
                        frame,
                        environment.data
                    ).forEach { it.push() }
                }
                else -> {
                    insn.accept(OperandAddVisitor(frame.operandStack))
                    MethodAnalyzer.logger.warning("Insn '$insn' isn't supported yet.")
                }
            }
        }

        val ifs = mutableListOf<IfExprData>()
        val stmData = mutableListOf<IfStatementData>()
        val constructed = mutableListOf<IfStatement>()

        blocks.forEachIndexed { index, it ->
            val block = it.block

            while (stmData.isNotEmpty()) {
                val last = stmData.last()
                if (last.end == block) {
                    val all = frame.operandStack.popAllFilterExcluded()
                    (last.stm.body as MutableInstructions).addAll(all)
                    frame.exitStack()
                    if (last.else_ != null) {
                        frame.enterStack()
                        break
                    } else {
                        stmData.removeAt(stmData.lastIndex)
                    }
                } else if (last.else_ == block) {
                    val all = frame.operandStack.popAllFilterExcluded()
                    (last.stm.elseStatement as MutableInstructions).addAll(all)
                    stmData.removeAt(stmData.lastIndex)
                    frame.exitStack()
                } else {
                    break
                }
            }

            it.nodes.forEachIndexed { i, abstractInsnNode ->
                val index = block.entryPoint + i
                visitNode(index, abstractInsnNode)
            }

            fun endIfDirect() {
                val (start, end) = ifs.last().findIfStartAndEnd(ifs)
                val exprs = simplify(ifs.first().createInsTo(mutableListOf(), ifs, start, end))
                val stm =
                    IfStatement(exprs, MutableInstructions.create(), MutableInstructions.create())
                val data = IfStatementData(ifs.toList(), stm, start, end, ifs.last().else_)
                stmData += data
                frame.operandStack.parent!!.push(stm)
                constructed += stm
                ifs.clear()
            }

            fun endIf(): Boolean {
                if (frame.operandStack.currentIsNotEmpty && ifs.isNotEmpty() /*|| ifs.lastOrNull()?.else_ != null*/) {
                    endIfDirect()
                    return true
                }
                return false
            }

            if (it is Bl.IfExprBlock) {
                val jump = (it.nodes.lastOrNull() as JumpInsnNode)

                when {
                    jump.opcode.isValidIfExprJmp() -> {
                        val expr = toIfExpr(it, frame)
                        val target = it.success.value?.let {
                            it.nodes.firstOrNull() as? LabelNode
                        }
                        val errorTarget = it.fail.value?.let {
                            it.nodes.firstOrNull() as? LabelNode
                        }

                        val enter = ifs.isEmpty()

                        if (enter) {
                            frame.enterStack()
                        }

                        val ended = endIf()

                        val if_ = IfExprData(
                            it.nodes.firstOrNull() as? LabelNode ?: frame.lastLabel,
                            expr,
                            it,
                            target,
                            errorTarget,
                            null
                        )

                        if (index + 1 < blocks.size
                                && (blocks[index + 1] !is Bl.IfExprBlock
                                        || (blocks[index + 1] as? Bl.IfExprBlock)?.isIfExpr() == false)
                        ) {
                            ifs += if_
                            if (ended)
                                frame.enterStack()
                            endIfDirect()
                        } else {
                            ifs += if_
                        }
                    }
                    jump.opcode.isGoto() -> {
                        frame.operandStack.popAs<JumpInsn>()
                        val success = it.block.successors.first { it.type is JumpEdgeType }.dest
                        if (stmData.isNotEmpty()) {
                            check(stmData.last().else_ == null)
                            stmData.last().else_ = success
                        } else {
                            ifs[ifs.lastIndex] = ifs[ifs.lastIndex].copy(else_ = success)
                        }
                    }
                    else -> TODO("")
                }
            }
        }
        return frame.operandStack.popAllFilterExcluded().filter { it !is InternalPart }
    }

    private fun Bl.IfExprBlock.isIfExpr() =
        (this.block.insns.last() as JumpInsnNode).opcode.isValidIfExprJmp()

    private fun IfExprData.findIfStart(): BasicBlock {
        if (this.bl.block.successors.isEmpty()) {
            return this.bl.block
        } else {
            val elements = Elements<BasicBlock>()
            elements.insert(Element(this.bl.block))
            var elem = elements.nextElement()

            while (elem != null) {
                if (elem.value.successors.isNotEmpty())
                    elements.insertFromPair(ElementUtil.fromIterable(elem.value.successors.map { it.dest }))
                else
                    return elem.value
                elem = elements.nextElement()
            }
        }

        TODO("")
    }

    private fun IfExprData.findIfStartAndEnd(exprData: List<IfExprData>): Pair<BasicBlock, BasicBlock> {

        if (this.bl.block.successors.isEmpty()) {
            return this.bl.block to this.bl.block
        } else {
            val allBlocks = mutableListOf<BasicBlock>()

            this.bl.block.successors.forEach {
                allBlocks += it.dest
            }

            val filtered = allBlocks.filter { block -> exprData.none { it.bl.block == block } }
            val min = filtered.minBy { it.entryPoint }
            val max = filtered.maxBy { it.entryPoint }
            return min!! to max!!
        }
    }

    private fun List<IfExprData>.resolveFor(bl: Bl): IfExprData? =
        this.firstOrNull { it.bl == bl }

    private fun IfExprData.createInsTo(
        expressions: MutableList<Instruction>,
        exprData: List<IfExprData>,
        ifStart: BasicBlock,
        ifEnd: BasicBlock
    ): MutableList<Instruction> {
        // Target of if branch success
        val success = this.bl.success.value

        // Target of if branch fail
        val fail = this.bl.fail.value

        val jumpToBody = this.bl.success.value?.block == ifStart
        val successSc = this.successIsJumpToAnotherIf(exprData)

        val expr = if (!jumpToBody) expr.copy(operation = this.expr.operation.inverse()) else expr
        expressions += expr

        fun Bl.IfExprBlock.or(data: IfExprData) {
            val n = mutableListOf<Instruction>()
            n += expressions.removeAt(expressions.lastIndex)
            n += Operators.OR
            data.createInsTo(n, exprData, ifStart, ifEnd)
            expressions += IfGroup(n)
        }

        fun Bl.IfExprBlock.and(data: IfExprData) {
            expressions += Operators.AND
            data.createInsTo(expressions, exprData, ifStart, ifEnd)
        }

        if (success is Bl.IfExprBlock) {
            val sc = exprData.resolveFor(success)
            if (sc != null) {
                if (this.bl.target == sc.label && !successSc) {
                    success.and(sc)
                } else if (successSc) {
                    success.or(sc)
                } else {
                    success.and(sc)
                }
            }
        }

        if (fail is Bl.IfExprBlock) {
            val sc = exprData.resolveFor(fail)
            if (sc != null) {
                if (jumpToBody) {
                    fail.or(sc)
                } else {
                    fail.and(sc)
                }
            }
        }

        return expressions
    }

    enum class JumpType {
        TO_BODY,
        TO_OUTSIDE,
        TO_OTHER_IF
    }

    private fun IfExprData.successIsJumpToAnotherIf(exprData: List<IfExprData>): Boolean =
        exprData.any { it.label == this.bl.target }


    private fun toIfExpr(virtual: Bl.IfExprBlock, frame: EmulatedFrame): IfExpr {
        val jump = frame.operandStack.peek()
        if (jump is JumpIfExprInsn) {
            frame.operandStack.pop()
            return jump.expr
        }

        if (jump is VisitTranslator.JumpInsn) {
            frame.operandStack.pop()
            if (jump.op.isValidIfExprJmp()) {
                val operator = jump.op.conditionalOperator
                val args = jump.op.getJmpArgs(frame)
                return IfExpr(args[0], operator, args[1])
            }
        }

        throw IllegalStateException("Invalid insn: '$jump' for IfExpr: '$virtual' in frame: $frame.")
    }

    fun visitInsn(opcode: Int, frame: EmulatedFrame, data: TypedData): Instruction? {
        when (opcode) {
            Opcodes.DUP -> {
                val pop = frame.operandStack.pop()
                val type = pop.type
                val varName = frame.varList.getUniqueName("dupVar$")

                val vari = variable(type, varName, pop)

                frame.varList.addVariable(varName)

                // Store duplicated
                /*LAST_DUP_VARS.add(data, VariableRef(type, varName))
                LAST_DUP_VARS.add(data, VariableRef(type, varName))*/

                frame.operandStack.push(accessVariable(vari))
                frame.operandStack.push(accessVariable(vari))

                return vari
            }
        // Handling DUP* is very hard for Kores
            Opcodes.DUP_X1, Opcodes.DUP_X2, Opcodes.DUP2, Opcodes.DUP2_X1, Opcodes.DUP2_X2 -> return null
            Opcodes.NOP,
            Opcodes.POP -> {
                // Accesses the last variable
                /*val req = LAST_DUP_VARS.require(data)
                val varRef = req.removeAt(req.size - 1)*/

                frame.operandStack.pop()
                //return accessVariable(varRef) // TODO: Remove: Ignore POP
            }
            Opcodes.POP2, // Ignore POP
            Opcodes.SWAP, // Need review
            Opcodes.LCMP, // TODO: If Expression translation
            Opcodes.FCMPL, Opcodes.FCMPG, Opcodes.DCMPL, Opcodes.DCMPG, // Need review (issue: #35)
            Opcodes.MONITORENTER, // No equivalent
            Opcodes.MONITOREXIT -> {
                // No equivalent
                if (opcode != Opcodes.NOP)
                    return this.createInstruction("visitInsn[opcode=${org.objectweb.asm.util.Printer.OPCODES[opcode].toLowerCase()}]")
            }
        }

        var value: Instruction? = null

        if (opcode == Opcodes.ACONST_NULL)
            value = Literals.NULL

        if (opcode == Opcodes.ICONST_M1)
            value = Literals.INT(-1)

        if (opcode == Opcodes.ICONST_0)
            value = Literals.INT(0)

        if (opcode == Opcodes.ICONST_1)
            value = Literals.INT(1)

        if (opcode == Opcodes.ICONST_2)
            value = Literals.INT(2)

        if (opcode == Opcodes.ICONST_3)
            value = Literals.INT(3)

        if (opcode == Opcodes.ICONST_4)
            value = Literals.INT(4)

        if (opcode == Opcodes.ICONST_5)
            value = Literals.INT(5)

        if (opcode == Opcodes.LCONST_0)
            value = Literals.LONG(0L)

        if (opcode == Opcodes.LCONST_1)
            value = Literals.LONG(1L)

        if (opcode == Opcodes.FCONST_0)
            value = Literals.FLOAT(0f)

        if (opcode == Opcodes.FCONST_1)
            value = Literals.FLOAT(1f)

        if (opcode == Opcodes.FCONST_2)
            value = Literals.FLOAT(2f)

        if (opcode == Opcodes.DCONST_0)
            value = Literals.DOUBLE(0.0)

        if (opcode == Opcodes.DCONST_1)
            value = Literals.DOUBLE(1.0)

        if (opcode >= Opcodes.I2L && opcode <= Opcodes.I2S) {
            value = Conversions.handleConversion(opcode, frame.operandStack.pop())
        }

        if (opcode >= Opcodes.INEG && opcode <= Opcodes.DNEG) {
            value = Conversions.handleNegation(opcode, frame.operandStack.pop())
        }

        if (opcode >= Opcodes.IADD && opcode <= Opcodes.LXOR) {
            val pop = frame.operandStack.pop(2)

            value = Conversions.handleMathAndBitwise(opcode, pop[0], pop[1])
        }

        if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
            value = Conversions.handleReturn(
                opcode,
                if (opcode == Opcodes.RETURN) null else frame.operandStack.pop()
            )
        }

        if (opcode == Opcodes.ARRAYLENGTH) {
            val access = frame.operandStack.popAs(VariableAccess::class.java)
            value = arrayLength(access.type, access)
        }

        if (opcode == Opcodes.ATHROW) {
            value = throwException(frame.operandStack.pop())
        }

        if (opcode == Opcodes.IASTORE
                || opcode == Opcodes.LASTORE
                || opcode == Opcodes.FASTORE
                || opcode == Opcodes.DASTORE
                || opcode == Opcodes.AASTORE
                || opcode == Opcodes.BASTORE
                || opcode == Opcodes.CASTORE
                || opcode == Opcodes.SASTORE
        ) {

            val pop = frame.operandStack.pop(3)

            var array = pop[0] // Removed
            val position = pop[1]
            val valueToInsert = pop[2]

            if (array is ArrayConstructor) {

                val arguments = ArrayList(array.arguments)

                arguments.add(valueToInsert)

                array = array.builder().arguments(arguments).build()

                value = array // Will be Added
            } else {
                array as VariableAccess

                value = setArrayValue(
                    array.type,
                    array,
                    position,
                    valueToInsert.type,
                    valueToInsert
                )
            }

        }

        if (opcode == Opcodes.IALOAD
                || opcode == Opcodes.LALOAD
                || opcode == Opcodes.FALOAD
                || opcode == Opcodes.DALOAD
                || opcode == Opcodes.BALOAD
                || opcode == Opcodes.CALOAD
                || opcode == Opcodes.SALOAD
        ) {
            // Load array values
            val pop = frame.operandStack.pop(2)

            val array = pop[0] as VariableAccess
            val position = pop[1]

            // TODO: check if array.type.arrayComponent is the right type
            value = accessArrayValue(array.type, array, position, array.type.arrayComponent)
        }
        if (value == null)
            logger.log(Level.WARNING, "Cannot parse insn opcode: '$opcode'")

        return value
                ?: this.createInstruction("visitInsn[opcode=${org.objectweb.asm.util.Printer.OPCODES[opcode].toLowerCase()}]")
    }

    fun visitIntInsn(opcode: Int, operand: Int): Instruction {
        if (opcode == Opcodes.BIPUSH) {
            return Literals.BYTE(operand.toByte())
        } else if (opcode == Opcodes.SIPUSH) {
            return Literals.SHORT(operand.toShort())
        } else if (opcode == Opcodes.ANEWARRAY) {
            val arrayType = ArrayUtil.getArrayType(operand)

            val args = mutableListOf<Instruction>()

            // TODO: Analyze multi sized arrays

            return ArrayConstructor(arrayType, ArgsBackedSize(args), args)
        } else {

            return this.createInstruction("visitIntInsn[opcode=$opcode, operand=$operand]")
        }
    }

    fun visitVarInsn(opcode: Int, slot: Int, frame: EmulatedFrame): Instruction? {

        if (opcode == Opcodes.ILOAD || opcode == Opcodes.LLOAD || opcode == Opcodes.FLOAD || opcode == Opcodes.DLOAD || opcode == Opcodes.ALOAD) {
            val varInfo = frame.getInfo(slot)
                    ?: throw IllegalArgumentException("No variable found at slot `$slot` in '$frame'")

            return accessVariable(varInfo.type, varInfo.name)
        } else if (opcode == Opcodes.ISTORE || opcode == Opcodes.LSTORE || opcode == Opcodes.FSTORE || opcode == Opcodes.DSTORE || opcode == Opcodes.ASTORE) {

            val pop = frame.operandStack.pop()

            val get: Instruction? = frame.localVariableTable.getOrNull(slot)

            val info = frame.getInfo(slot)

            val type = info?.type ?: pop.type
            val name = info?.name ?: "var$slot"

            val value = if (type.`is`(Types.BOOLEAN) && pop is Literals.IntLiteral)
                if (pop.name == "1") Literals.TRUE else Literals.FALSE
            else pop

            return if (value !is CatchVariable) {
                if (get == null) {
                    val variable = variable(type, name, value)
                    frame.store(accessVariable(variable), slot)
                    variable
                } else {
                    setVariableValue(type, name, value)
                }
            } else {
                null
            }
        } else {
            if (opcode == Opcodes.RET)
                this.logger.warning("Cannot handle RET opcode")

            return this.createInstruction("visitVarInsn[opcode=$opcode, slot=$slot]")
        }

    }

    fun visitTypeInsn(
        opcode: Int,
        type: String,
        typeResolver: TypeResolver,
        frame: EmulatedFrame
    ): Instruction {
        val koresType = typeResolver.resolveUnknown(type)

        when (opcode) {
            Opcodes.NEW -> return New(koresType)
            Opcodes.CHECKCAST -> {
                val codePart = frame.operandStack.pop()

                return cast(codePart.typeOrNull, koresType, codePart)
            }
            Opcodes.INSTANCEOF -> {
                val codePart = frame.operandStack.pop()
                return isInstanceOf(codePart, koresType)
            }
            Opcodes.ANEWARRAY -> {
                val args = mutableListOf<Instruction>()
                return ArrayConstructor(koresType, ArgsBackedSize(args), args)
            }
            else -> {
                this.logger.warning("Cannot handle opcode: '$opcode'!")
                return this.createInstruction("visitTypeInsn[opcode=$opcode, type=$type]")
            }
        }
    }

    fun visitFieldInsn(
        opcode: Int,
        owner: String,
        name: String,
        desc: String,
        typeResolver: TypeResolver,
        frame: EmulatedFrame
    ): Instruction {
        val codeOwner = typeResolver.resolveUnknown(owner)
        val koresType = typeResolver.resolveUnknown(desc)

        when (opcode) {
            Opcodes.GETSTATIC -> {
                return accessStaticField(codeOwner, koresType, name)
            }
            Opcodes.PUTSTATIC -> {
                val value = frame.operandStack.pop()

                return setStaticFieldValue(codeOwner, koresType, name, value)
            }
            Opcodes.GETFIELD -> {
                val instance = frame.operandStack.pop()

                return accessField(codeOwner, instance, koresType, name)
            }
            Opcodes.PUTFIELD -> {
                val pop = frame.operandStack.pop(2)
                val instance = pop[0]
                val value = pop[1]

                return setFieldValue(codeOwner, instance, koresType, name, value)
            }
        }

        return this.createInstruction("visitFieldInsn[opcode=$opcode, owner=$owner, name=$name, desc=$desc]")
    }

    fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        desc: String,
        itf: Boolean,
        declaringType: KoresType,
        typeResolver: TypeResolver,
        frame: EmulatedFrame
    ): Instruction {
        try {
            // Resolve the method owner type
            val ownerType = typeResolver.resolve(owner, itf)

            // Parse the method description
            val description =
                DescriptionUtil.parseDescription("${ownerType.javaSpecName}:$name$desc")

            // Get number of arguments
            val arguments = description.parameterTypes.size

            // Pop all arguments from operand stack
            val argumentsList = frame.operandStack.pop(arguments)

            // Gets the invocation type from asm opcode
            val invokeType = Conversions.fromAsm(opcode)

            // Invocation target part, for static invocations uses Access (more coming later)
            val target: Instruction = if (invokeType != InvokeType.INVOKE_STATIC) {
                // Pops the invocation target from operand stack
                frame.operandStack.pop()
            } else Access.STATIC

            // Create TypeSpecification from the method description
            val spec = toTypeSpec(description, typeResolver)

            // Method invocation part
            val methodInvocation: MethodInvocation

            if (target is New) {
                // Create invocation of a constructor of a class
                methodInvocation =
                        invokeSpecial(target.localization, target, CONSTRUCTOR, spec, argumentsList)
            } else {
                // If target is not a constructor (NEW)
                // If invoke type is special, create a super/this constructor invocation
                if (invokeType == InvokeType.INVOKE_SPECIAL) {
                    // If method type is same as method declaring type
                    if (ownerType.`is`(declaringType)) {
                        // Create this constructor invocation
                        methodInvocation = invokeThisConstructor(spec, argumentsList)
                    } else {
                        // If is not same, invoke super class constructor
                        methodInvocation = invokeSuperConstructor(ownerType, spec, argumentsList)
                    }
                } else {
                    // If is not invoke special, invoke normally.
                    methodInvocation =
                            target.invoke(invokeType, ownerType, name, spec, argumentsList)
                }
            }

            return methodInvocation

        } catch (e: Exception) {
            this.logger.log(Level.WARNING, "Method -> $owner:$name$desc", e)

            return this.createInstruction("visitMethodInsn[opcode=$opcode, owner=$owner, name=$name, desc=$desc, itf=$itf]")
        }

    }

    fun visitDynamicMethodInsn(
        name: String,
        desc: String,
        bsm: Handle,
        bsmArgs: Array<Any>,
        typeResolver: TypeResolver,
        frame: EmulatedFrame
    ): Instruction {
        try {
            // Parse bootstrap method description
            val description = DescriptionUtil.parseDescription("L?;:" + name + desc)
            val typeSpec = toTypeSpec(description, typeResolver)

            // Get number of arguments
            val arguments = description.parameterTypes.size

            // Pop arguments from stack
            val argumentList = frame.operandStack.pop(arguments)

            // Create a method invocation of the bootstrap method
            val methodInvocation = DynamicMethodSpec(
                name = name,
                typeSpec = typeSpec,
                arguments = argumentList
            )

            // Create dynamic invocation of the bootstrap method
            return Conversions.fromHandle(bsm, bsmArgs, methodInvocation, typeResolver)

        } catch (e: Exception) {
            this.logger.log(Level.WARNING, "DynamicMethod -> dynamic:$name$desc", e)

            return this.createInstruction(
                "visitInvokeDynamicInsn[name=$name, desc=$desc, bsm=$bsm, bsmArgs=${Arrays.toString(
                    bsmArgs
                )}]"
            )
        }

    }

    fun visitLdcInsn(cst: Any): Instruction {
        if (Conversions.isLiteral(cst)) {
            val literal = Conversions.toLiteral(cst)

            return literal
        } else {
            //return this.createInstruction { methodVisitor -> methodVisitor.visitLdcInsn(cst) }
            return this.createInstruction("visitLdcInsn[cst=$cst]")
        }
    }

    fun visitIincInsn(slot: Int, increment: Int, frame: EmulatedFrame): Instruction {
        val variable = frame.load(slot)

        if (variable !is VariableAccess) {
            this.logger.warning("Cannot handle variable increment. Variable: '$variable', slot: '$slot', increment: '$increment'!")
            return this.createInstruction("visitIincInsn[slot=$slot, increment=$increment]")
        } else {

            val literal: Instruction =
                if (increment != 1 && increment != -1) Literals.INT(increment) else KoresNothing

            val operation = if (increment > 0) {
                Operators.ADD
            } else {
                Operators.SUBTRACT
            }

            return operate(variable, operation, literal)
        }

    }

    /**
     * Handling process is very simple. First we collect all information about try statement,
     * and then when end is reached we analyze stack, pop all part of try and catch bodies and put them
     * inside Instructions
     */
    fun handleExceptionTable(
        insns: Array<AbstractInsnNode>,
        index: Int,
        tryCatchBlocks: List<TryCatchBlockNode>,
        labelNode: LabelNode,
        typeResolver: TypeResolver,
        frame: EmulatedFrame,
        data: TypedData
    ): List<Instruction> {

        val label = labelNode.label
        val instruction = mutableListOf<Instruction>()

        val tryDatas = TRY_DATA.getOrSet(data, mutableListOf())

        tryCatchBlocks.forEach {
            val start = it.start.label
            val end = it.end.label
            val handler = it.handler.label

            val type =
                if (it.type != null) typeResolver.resolveUnknown(it.type) else Types.THROWABLE

            val tryData = tryDatas.firstOrNull { it.start == start && it.end == end }
                    ?: TryData(start, end, null, mutableListOf()).also {
                        TRY_DATA.add(data, it)
                    }

            val catchData = tryData.catchDataList.firstOrNull { it.handler == handler }?.also {

                if (type !in it.builder.exceptionTypes)
                    it.builder.exceptionTypes += type
            } ?: CatchData(
                handler, CatchStatement.Builder.builder()
                    .exceptionTypes(type)
                    .body(MutableInstructions.create()),
                null
            ).also {
                tryData.catchDataList += it
            }

            if (end == label) {
                val next = insns.nextMatch(index + 1, { it.isFlowStopInsn() })

                next as JumpInsnNode

                val endOfTryCatch = next.label.label

                if (tryData.endOfTryCatch == null)
                    tryData.endOfTryCatch = endOfTryCatch

                //catchData.end = endOfTryCatch

            } else if (it.handler.label == label) {
                /*val tryBlock = Kores.tryBlock(MutableInstructions(), catchBlocks.flatMap { it.value })
                bodyStack*/
                val next0 = insns.nextNodeMatch(index + 1, { it is VarInsnNode })!!
                val index = next0.first
                val next = next0.second

                next as VarInsnNode

                val info = frame.getInfo(next.`var`)!!

                catchData.variable = variable(info.type, info.name)

                frame.operandStack.push(CatchVariable)

            } else if (tryCatchBlocks.filter { b -> it != b }.any { it.handler.label == label }
                    || (tryData.endOfTryCatch != null && tryData.endOfTryCatch == label)) {
                val next = insns.previousMatch(index - 1, { it.isFlowStopInsn() })

                catchData.end = label
            }


        }

        val datas = tryDatas.filter { it.endOfTryCatch == label }

        if (datas.isNotEmpty()) {
            val block = datas.single()

            val start = frame.operandStack.peekFind {
                it is MagicPart && it.obj == block.start
            }

            val end = frame.operandStack.peekFind {
                it is MagicPart && it.obj == block.end
            }

            val sub = frame.operandStack.sub(start.index, end.index)

            val catchStm = mutableListOf<CatchStatement>()

            instruction += TryStatement(
                body = Instructions.fromIterable(frame.operandStack.filter(sub)),
                catchStatements = catchStm,
                finallyStatement = Instructions.empty()
            )

            sub.clear()

            val endOfTryCatchBlock = frame.operandStack.size - 1

            block.catchDataList.forEach {
                val startIndex = frame.operandStack.peekFind { find ->
                    find is MagicPart && find.obj == it.handler
                }

                val endIndex = if (it.end == label)
                    endOfTryCatchBlock
                else frame.operandStack.peekFind { find ->
                    find is MagicPart && find.obj == it.end
                }.index

                val sub2 = frame.operandStack.sub(startIndex.index, endIndex)

                catchStm += it.builder
                    .variable(it.variable!!)
                    .body(Instructions.fromIterable(frame.operandStack.filter(sub2)))
                    .build()

                sub2.clear()

                frame.operandStack.removeIf { f ->
                    f is MagicPart
                            && (f.obj == it.handler
                            || f.obj == it.end)
                }
            }

            frame.operandStack.filterList {
                it is MagicPart
                        && (it.obj == block.start
                        || it.obj == block.end)
            }

        }


        return instruction

    }

    fun visitJumpInsn(
        opcode: Int,
        label: LabelNode,
        frame: EmulatedFrame
    ): Instruction? {
        if (opcode.isValidIfExprJmp()) {
            val grab = opcode.getJmpArgs(frame)
            return JumpIfExprInsn(IfExpr(grab[0], opcode.conditionalOperator, grab[1]), label)
        }

        return JumpInsn(
            opcode,
            label
        )
    }

    data class JumpInsn(val op: Int, val label: LabelNode) : Instruction {
        override fun builder(): Builder<KoresPart, *> {
            return super.builder()
        }
    }

    data class JumpIfExprInsn(val expr: IfExpr, val label: LabelNode) : Instruction {
        override fun builder(): Builder<KoresPart, *> {
            return super.builder()
        }
    }

    fun getNextIfInsn(insns: Array<AbstractInsnNode>, start: Int): Int {
        val isUseless = { node: AbstractInsnNode ->
            node is LineNumberNode
                    || node is LabelNode
                    || node is FrameNode
        }

        var x = start

        while (x < insns.size && isUseless(insns[x])) {
            ++x
        }

        val args = mutableListOf<AbstractInsnNode>()

        while (x < insns.size
                && (insns[x] !is JumpInsnNode || isUseless(insns[x]))
        ) {
            val inx = insns[x]

            if (!isUseless(inx))
                args += inx

            ++x
        }

        if (x < insns.size) {
            val insnNode = insns[x]

            if (insnNode is JumpInsnNode) {
                if (insnNode.opcode.isValidIfExprJmp()) {
                    if (args.size == insnNode.opcode.argsSize) {
                        return x
                    }
                }
            }
        }

        return -1
    }


    fun visitLabel(
        insns: Array<AbstractInsnNode>,
        label: LabelNode,
        index: Int,
        frame: EmulatedFrame,
        data: TypedData
    ): Instruction? {
        frame.visitLabel(label)
        // TODO: Labels
        return null
    }

    // Extra
    fun initMethod(
        method: MethodDeclarationBase,
        parameters: List<KoresParameter>,
        frame: EmulatedFrame
    ) {
        var pos = 0

        if (!method.modifiers.contains(KoresModifier.STATIC)) {
            frame.storeAccess(Access.THIS, pos)
            ++pos
        }

        val collect = parameters
            .map { koresParameter -> accessVariable(koresParameter.type, koresParameter.name) }

        frame.storeValues(collect, pos)
    }

    fun fixParametersNames(
        parameters: List<KoresParameter>,
        parametersNodes: List<ParameterNode>,
        frame: EmulatedFrame
    ): List<KoresParameter> =
        parameters.mapIndexed { i, parameter ->
            //@Shadow
            @Suppress("NAME_SHADOWING")
            val i = i + 1
            val nodeName = if (parametersNodes.size > i) parametersNodes[i].name else null

            val info = frame.getInfo(i)
            val name: String = info?.name ?: nodeName ?: parameter.name
            val type: ReflectType = info?.type ?: parameter.type

            return@mapIndexed parameter.builder().name(name).type(type).build()
        }

    inline fun readParameters(nodes: List<ParameterNode>, nameFunc: (Int, String) -> Unit) {
        nodes.forEachIndexed { i, parameterNode ->
            nameFunc(i, parameterNode.name)
        }
    }

    inline fun readVariableTable(
        nodes: List<LocalVariableNode>,
        typeResolver: TypeResolver,
        storeFunc: (Int, ReflectType, String, LabelNode?, LabelNode?) -> Unit
    ) {
        nodes.forEach {
            val signatureType =
                it.signature?.let { GenericUtil.parse(typeResolver, it)?.types?.get(0) }
            val type = signatureType ?: typeResolver.resolveUnknown(it.desc)
            val name = it.name
            val index = it.index
            storeFunc(index, type, name, it.start, it.end)
        }
    }

    private fun createInstruction(str: String): Instruction {
        return UnknownPart(str)
    }

    private class ArgsBackedSize(val arguments: List<Instruction>) : List<Instruction> {

        override val size: Int
            get() = 1

        private val element get() = Literals.INT(arguments.size)

        override fun contains(element: Instruction): Boolean =
            this.element == element

        override fun containsAll(elements: Collection<Instruction>): Boolean =
            elements.size == 1 && elements.single() == this.element

        override fun get(index: Int): Instruction =
            if (index == 0) element else throw NoSuchElementException()

        override fun indexOf(element: Instruction): Int =
            if (element == this.element) 0 else throw NoSuchElementException()

        override fun isEmpty(): Boolean = false

        override fun iterator(): Iterator<Instruction> =
            IteratorUtil.single(this.element)


        override fun lastIndexOf(element: Instruction): Int =
            if (element == this.element) 0 else throw NoSuchElementException()

        override fun listIterator(): ListIterator<Instruction> =
            object : ListIterator<Instruction> {
                var index = -1

                override fun hasPrevious(): Boolean = index != -1

                override fun next(): Instruction = this@ArgsBackedSize[++index]

                override fun nextIndex(): Int = index + 1

                override fun previous(): Instruction = this@ArgsBackedSize[index--]

                override fun previousIndex(): Int = index

                override fun hasNext(): Boolean = index != 0

            }


        override fun listIterator(index: Int): ListIterator<Instruction> {
            return if (index != 0) throw IndexOutOfBoundsException() else listIterator()
        }

        override fun subList(fromIndex: Int, toIndex: Int): List<Instruction> = this

    }

    object CatchVariable : Instruction, KoresPart // ', CodePart' to fix processing loop

    data class TryData(
        val start: Label,
        val end: Label,
        var endOfTryCatch: Label? = null,
        val catchDataList: MutableList<CatchData>
    )

    data class CatchData(
        val handler: Label,
        val builder: CatchStatement.Builder,
        var end: Label?,
        var variable: VariableDeclaration? = null
    )

    fun AbstractInsnNode.isFlowStopInsn() =
        (this is JumpInsnNode
                && this.opcode == Opcodes.GOTO)
                || this.opcode == Opcodes.ATHROW
                || (this.opcode == Opcodes.ARETURN
                || this.opcode == Opcodes.IRETURN
                || this.opcode == Opcodes.DRETURN
                || this.opcode == Opcodes.FRETURN
                || this.opcode == Opcodes.LRETURN
                || this.opcode == Opcodes.RETURN)
}

/*
/**
         * Finds where the if statement body starts
         */
        fun findBody(): Int {
            //var flows = mutableListOf<String>() // Temporary string

            var lindex = -1

            val iterator = ArrayIterator(insns)

            for(i in 0..index)
                iterator.nextValid()

            while(iterator.hasNext()) {
                val insn = iterator.nextValid()

                if (insn is JumpInsnNode)
                    throw IllegalStateException("Unexpected jump insn")

                if (!iterator.hasNext())
                    break

                val next = iterator.nextValid()

                if(next is JumpInsnNode) {
                    if(!next.opcode.isValidIfExprJmp())
                        continue

                    val args = next.opcode.argsSize

                    if(args != 1)
                        throw IllegalStateException("Invalid opcode, only one arg provided for '$args' jump insn. Position: ${iterator.index}.")
                    lindex = iterator.index
                } else {
                    if (!iterator.hasNext())
                        break

                    val next2 = iterator.nextValid()

                    if(next2 is JumpInsnNode) {

                        val args = next2.opcode.argsSize

                        if(args != 2)
                            throw IllegalStateException("Invalid opcode, only two args provided for $args jump insn. Position: ${iterator.index}.")
                    }

                    lindex = iterator.index
                }
            }

            return lindex
        }

 */