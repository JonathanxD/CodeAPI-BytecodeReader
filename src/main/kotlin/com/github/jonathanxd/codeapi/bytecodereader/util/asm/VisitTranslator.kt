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
package com.github.jonathanxd.codeapi.bytecodereader.util.asm

import com.github.jonathanxd.codeapi.*
import com.github.jonathanxd.codeapi.base.*
import com.github.jonathanxd.codeapi.bytecodereader.env.EmulatedFrame
import com.github.jonathanxd.codeapi.bytecodereader.env.StackManager
import com.github.jonathanxd.codeapi.bytecodereader.extra.MagicPart
import com.github.jonathanxd.codeapi.bytecodereader.extra.UnknownPart
import com.github.jonathanxd.codeapi.bytecodereader.util.*
import com.github.jonathanxd.codeapi.common.CONSTRUCTOR
import com.github.jonathanxd.codeapi.common.CodeNothing
import com.github.jonathanxd.codeapi.factory.*
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.operator.Operators
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.util.*
import com.github.jonathanxd.iutils.annotation.Named
import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.iutils.description.DescriptionUtil
import com.github.jonathanxd.iutils.iterator.IteratorUtil
import com.github.jonathanxd.iutils.map.ListHashMap
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.util.ArrayList
import java.util.Arrays
import java.util.logging.Level
import java.util.logging.Logger

object VisitTranslator {

    private val logger = Logger.getLogger("CodeAPI_Translator")

    val TRY_DATA = typedKeyOf<MutableList<TryData>>("TRY_DATA")

    val CATCH_BLOCKS = typedKeyOf<ListHashMap<@Named("Start") Label, CatchData>>("CATCH_BLOCKS")

    fun visitInsn(opcode: Int, frame: EmulatedFrame): CodeInstruction? {
        when (opcode) {
            Opcodes.DUP, // Handling DUP* is very hard for CodeAPI
            Opcodes.DUP_X1, Opcodes.DUP_X2, Opcodes.DUP2, Opcodes.DUP2_X1, Opcodes.DUP2_X2 -> return null
            Opcodes.NOP,
            Opcodes.POP, // Ignore POP
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

        var value: CodeInstruction? = null

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
            value = Conversions.handleReturn(opcode, if (opcode == Opcodes.RETURN) null else frame.operandStack.pop())
        }

        if (opcode == Opcodes.ARRAYLENGTH) {
            val access = frame.operandStack.popAs(VariableAccess::class.java)
            value = arrayLength(access.type, access)
        }

        if (opcode == Opcodes.ATHROW) {
            value = throwException(frame.operandStack.pop())
        }

        if (opcode == Opcodes.IASTORE || opcode == Opcodes.LASTORE || opcode == Opcodes.FASTORE || opcode == Opcodes.DASTORE || opcode == Opcodes.AASTORE || opcode == Opcodes.BASTORE || opcode == Opcodes.CASTORE || opcode == Opcodes.SASTORE) {

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

                value = setArrayValue(array.type, array, position, valueToInsert.type, valueToInsert)
            }

        }

        if (opcode == Opcodes.IALOAD || opcode == Opcodes.LALOAD || opcode == Opcodes.FALOAD || opcode == Opcodes.DALOAD || opcode == Opcodes.BALOAD || opcode == Opcodes.CALOAD || opcode == Opcodes.SALOAD) {
            // Load array values
            val pop = frame.operandStack.pop(2)

            val array = pop[0] as VariableAccess
            val position = pop[1]

            // TODO: check if array.type.arrayComponent is the right type
            value = accessArrayValue(array.type, array, position, array.type.arrayComponent)
        }
        if (value == null)
            logger.log(Level.WARNING, "Cannot parse insn opcode: '$opcode'")

        return value ?: this.createInstruction("visitInsn[opcode=${org.objectweb.asm.util.Printer.OPCODES[opcode].toLowerCase()}]")
    }

    fun visitIntInsn(opcode: Int, operand: Int): CodeInstruction {
        if (opcode == Opcodes.BIPUSH) {
            return Literals.BYTE(operand.toByte())
        } else if (opcode == Opcodes.SIPUSH) {
            return Literals.SHORT(operand.toShort())
        } else if (opcode == Opcodes.ANEWARRAY) {
            val arrayType = ArrayUtil.getArrayType(operand)

            val args = mutableListOf<CodeInstruction>()

            // TODO: Analyze multi sized arrays

            return ArrayConstructor(arrayType, ArgsBackedSize(args), args)
        } else {

            return this.createInstruction("visitIntInsn[opcode=$opcode, operand=$operand]")
        }
    }

    fun visitVarInsn(opcode: Int, slot: Int, frame: EmulatedFrame): CodeInstruction? {

        if (opcode == Opcodes.ILOAD || opcode == Opcodes.LLOAD || opcode == Opcodes.FLOAD || opcode == Opcodes.DLOAD || opcode == Opcodes.ALOAD) {
            val varInfo = frame.getInfo(slot) ?: throw IllegalArgumentException("No variable found at slot `$slot` in '$frame'")

            return accessVariable(varInfo.type, varInfo.name)
        } else if (opcode == Opcodes.ISTORE || opcode == Opcodes.LSTORE || opcode == Opcodes.FSTORE || opcode == Opcodes.DSTORE || opcode == Opcodes.ASTORE) {

            val pop = frame.operandStack.pop()

            val get: CodeInstruction? = frame.localVariableTable.getOrNull(slot)

            val info = frame.getInfo(slot)

            val type = info?.type ?: pop.type
            val name = info?.name ?: "var$slot"

            return if (pop !is CatchVariable) {
                if (get == null) {
                    val variable = variable(type, name, pop)
                    frame.store(accessVariable(variable), slot)
                    variable
                } else {
                    setVariableValue(type, name, pop)
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

    fun visitTypeInsn(opcode: Int, type: String, typeResolver: TypeResolver, frame: EmulatedFrame): CodeInstruction {
        val codeType = typeResolver.resolveUnknown(type)

        when (opcode) {
            Opcodes.NEW -> return New(codeType)
            Opcodes.CHECKCAST -> {
                val codePart = frame.operandStack.pop()

                return cast(codePart.typeOrNull, codeType, codePart)
            }
            Opcodes.INSTANCEOF -> {
                val codePart = frame.operandStack.pop()
                return isInstanceOf(codePart, codeType)
            }
            Opcodes.ANEWARRAY -> {
                val args = mutableListOf<CodeInstruction>()
                return ArrayConstructor(codeType, ArgsBackedSize(args), args)
            }
            else -> {
                this.logger.warning("Cannot handle opcode: '$opcode'!")
                return this.createInstruction("visitTypeInsn[opcode=$opcode, type=$type]")
            }
        }
    }

    fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String, typeResolver: TypeResolver, frame: EmulatedFrame): CodeInstruction {
        val codeOwner = typeResolver.resolveUnknown(owner)
        val codeType = typeResolver.resolveUnknown(desc)

        when (opcode) {
            Opcodes.GETSTATIC -> {
                return accessStaticField(codeOwner, codeType, name)
            }
            Opcodes.PUTSTATIC -> {
                val value = frame.operandStack.pop()

                return setStaticFieldValue(codeOwner, codeType, name, value)
            }
            Opcodes.GETFIELD -> {
                val instance = frame.operandStack.pop()

                return accessField(codeOwner, instance, codeType, name)
            }
            Opcodes.PUTFIELD -> {
                val pop = frame.operandStack.pop(2)
                val instance = pop[0]
                val value = pop[1]

                return setFieldValue(codeOwner, instance, codeType, name, value)
            }
        }

        return this.createInstruction("visitFieldInsn[opcode=$opcode, owner=$owner, name=$name, desc=$desc]")
    }

    fun visitMethodInsn(opcode: Int,
                        owner: String,
                        name: String,
                        desc: String,
                        itf: Boolean,
                        declaringType: CodeType,
                        typeResolver: TypeResolver,
                        frame: EmulatedFrame): CodeInstruction {
        try {
            // Resolve the method owner type
            val ownerType = typeResolver.resolve(owner, itf)

            // Parse the method description
            val description = DescriptionUtil.parseDescription(ownerType.javaSpecName + ":" + name + desc)

            // Get number of arguments
            val arguments = description.parameterTypes.size

            // Pop all arguments from operand stack
            val argumentsList = frame.operandStack.pop(arguments)

            // Gets the invocation type from asm opcode
            val invokeType = Conversions.fromAsm(opcode)

            // Invocation target part, for static invocations uses Access (more coming later)
            var target: CodeInstruction = if (invokeType != InvokeType.INVOKE_STATIC) {
                // Pops the invocation target from operand stack
                frame.operandStack.pop()
            } else Access.STATIC

            // Create TypeSpecification from the method description
            val spec = DescriptionHelper.toTypeSpec(description, typeResolver)

            // Method invocation part
            val methodInvocation: MethodInvocation

            if (target is New) {
                // Create invocation of a constructor of a class
                methodInvocation = invokeSpecial(target.localization, target, CONSTRUCTOR, spec, argumentsList)
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
                    methodInvocation = target.invoke(invokeType, ownerType, name, spec, argumentsList)
                }
            }

            return methodInvocation

        } catch (e: Exception) {
            this.logger.log(Level.WARNING, "Method -> $owner:$name$desc", e)

            return this.createInstruction("visitMethodInsn[opcode=$opcode, owner=$owner, name=$name, desc=$desc, itf=$itf]")
        }

    }

    fun visitDynamicMethodInsn(name: String,
                               desc: String,
                               bsm: Handle,
                               bsmArgs: Array<Any>,
                               typeResolver: TypeResolver,
                               frame: EmulatedFrame): CodeInstruction {
        try {
            // Parse bootstrap method description
            val description = DescriptionUtil.parseDescription("L?;:" + name + desc)

            // Get number of arguments
            val arguments = description.parameterTypes.size

            // Pop arguments from stack
            val argumentList = frame.operandStack.pop(arguments)

            // Specify InvokeType as Dynamic
            val invokeType = Conversions.fromAsm_H(bsm.tag)

            // Gets spec of handle
            val spec = Conversions.specFromHandle(bsm, typeResolver)

            // Create a method invocation of the bootstrap method
            val methodInvocation = MethodInvocation(
                    invokeType = invokeType,
                    arguments = argumentList,
                    target = Access.THIS,
                    spec = spec.methodTypeSpec
            )

            // Create dynamic invocation of the bootstrap method
            return Conversions.fromHandle(bsm, bsmArgs, methodInvocation, typeResolver)

        } catch (e: Exception) {
            this.logger.log(Level.WARNING, "DynamicMethod -> dynamic:$name$desc", e)

            return this.createInstruction("visitInvokeDynamicInsn[name=$name, desc=$desc, bsm=$bsm, bsmArgs=${Arrays.toString(bsmArgs)}]")
        }

    }

    fun visitLdcInsn(cst: Any): CodeInstruction {
        if (Conversions.isLiteral(cst)) {
            val literal = Conversions.toLiteral(cst)

            return literal
        } else {
            //return this.createInstruction { methodVisitor -> methodVisitor.visitLdcInsn(cst) }
            return this.createInstruction("visitLdcInsn[cst=$cst]")
        }
    }

    fun visitIincInsn(slot: Int, increment: Int, frame: EmulatedFrame): CodeInstruction {
        val variable = frame.load(slot)

        if (variable !is VariableAccess) {
            this.logger.warning("Cannot handle variable increment. Variable: '$variable', slot: '$slot', increment: '$increment'!")
            return this.createInstruction("visitIincInsn[slot=$slot, increment=$increment]")
        } else {

            val literal: CodeInstruction = if (increment != 1 && increment != -1) Literals.INT(increment) else CodeNothing

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
     * inside CodeSource
     */
    fun handleExceptionTable(insns: Array<AbstractInsnNode>,
                             index: Int,
                             tryCatchBlocks: List<TryCatchBlockNode>,
                             labelNode: LabelNode,
                             bodyStack: StackManager<CodeSource>,
                             typeResolver: TypeResolver,
                             frame: EmulatedFrame,
                             data: TypedData): List<CodeInstruction> {

        val label = labelNode.label
        val instruction = mutableListOf<CodeInstruction>()

        val tryDatas = TRY_DATA.getOrSet(data, mutableListOf())

        tryCatchBlocks.forEach {
            val start = it.start.label
            val end = it.end.label
            val handler = it.handler.label

            val type = if (it.type != null) typeResolver.resolveUnknown(it.type) else Types.THROWABLE

            val tryData = tryDatas.firstOrNull { it.start == start && it.end == end }
                    ?: TryData(start, end, null, mutableListOf()).also {
                TRY_DATA.add(data, it)
            }

            val catchData = tryData.catchDataList.firstOrNull { it.handler == handler }?.also {

                if (type !in it.builder.exceptionTypes)
                    it.builder.exceptionTypes += type
            } ?: CatchData(handler, CatchStatement.Builder.builder()
                    .exceptionTypes(type)
                    .body(MutableCodeSource.create()),
                    null).also {
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
                /*val tryBlock = CodeAPI.tryBlock(MutableCodeSource(), catchBlocks.flatMap { it.value })
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

            instruction += TryStatement(body = CodeSource.fromIterable(frame.operandStack.filter(sub)),
                    catchStatements = catchStm,
                    finallyStatement = CodeSource.empty())

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
                        .body(CodeSource.fromIterable(frame.operandStack.filter(sub2)))
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

    fun visitJumpInsn(opcode: Int,
                      label: LabelNode,
                      bodyStack: StackManager<CodeSource>,
                      typeResolver: TypeResolver,
                      frame: EmulatedFrame,
                      data: TypedData): CodeInstruction? {
        /*val take: List<CodePart>
        val operator: Operator

        // Convert the `if` to a CodeAPI if expression, the jump insn means 'branch if', then the operation should be "inverted"
        // but some problems may occur for "OR" if operations, so the `if` translation will occur later.
        // When the 'next' `if` is an "OR" `if`, the 'current' `if operator` should not be "inverted", to do that correctly
        // the CodeAPI should translate the expression after reading all `if expressions`.
        // A example: `if(a == 9 || b == 7)`, the first `if expression` will be not 'inverted' in bytecode, it will
        // branch directly to `if body`, but the second `if expression` will be 'inverted' to branch to out of the if body.
        // Is not easy to determine if is `and` or `or` in the first expression (Read next expression when find a JumpInsn
        // is a good approach, but I don't wan't to skip `if arguments` because to do that I need to handle de frame and locals)
        // **It is not easy**

        // To translate while we should loop all instructions and find a GOTO to current label.
        when (opcode) {
            Opcodes.IFEQ -> {
                take = frame.operandStack.pop(1) + Literals.TRUE
                operator = Operators.EQUAL_TO
            }
            Opcodes.IFNE -> {
                take = frame.operandStack.pop(1) + Literals.FALSE
                operator = Operators.EQUAL_TO
            }
            Opcodes.IFLT -> {
                take = frame.operandStack.pop(2)
                operator = Operators.LESS_THAN
            }

            else -> {
                take = emptyList()
                operator = Operators.LESS_THAN
            }
        }*/

        //val args = frame.operandStack.pop(takeN)
        return createInstruction("visitJumpInsn[opcode=${opcode.opcodeName}, label=${label.label}]")
    }

    // Extra
    fun initMethod(method: MethodDeclarationBase, parameters: List<CodeParameter>, frame: EmulatedFrame) {
        var pos = 0

        if (!method.modifiers.contains(CodeModifier.STATIC)) {
            frame.storeAccess(Access.THIS, pos)
            ++pos
        }

        val collect = parameters
                .map { codeParameter -> accessVariable(codeParameter.type, codeParameter.name) }

        frame.storeValues(collect, pos)
    }

    fun fixParametersNames(parameters: List<CodeParameter>, parametersNodes: List<ParameterNode>, frame: EmulatedFrame): List<CodeParameter> =
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

    inline fun readVariableTable(nodes: List<LocalVariableNode>, typeResolver: TypeResolver, storeFunc: (Int, ReflectType, String) -> Unit) {
        nodes.forEach {
            val signatureType = it.signature?.let { GenericUtil.parse(typeResolver, it)?.types?.get(0) }
            val type = signatureType ?: typeResolver.resolveUnknown(it.desc)
            val name = it.name
            val index = it.index
            storeFunc(index, type, name)
        }
    }

    private fun createInstruction(str: String): CodeInstruction {
        return UnknownPart(str)
    }

    private class ArgsBackedSize(val arguments: List<CodeInstruction>) : List<CodeInstruction> {

        override val size: Int
            get() = 1

        private val element get() = Literals.INT(arguments.size)

        override fun contains(element: CodeInstruction): Boolean =
                this.element == element

        override fun containsAll(elements: Collection<CodeInstruction>): Boolean =
                elements.size == 1 && elements.single() == this.element

        override fun get(index: Int): CodeInstruction =
                if (index == 0) element else throw NoSuchElementException()

        override fun indexOf(element: CodeInstruction): Int =
                if (element == this.element) 0 else throw NoSuchElementException()

        override fun isEmpty(): Boolean = false

        override fun iterator(): Iterator<CodeInstruction> =
                IteratorUtil.single(this.element)


        override fun lastIndexOf(element: CodeInstruction): Int =
                if (element == this.element) 0 else throw NoSuchElementException()

        override fun listIterator(): ListIterator<CodeInstruction> = object : ListIterator<CodeInstruction> {
            var index = -1

            override fun hasPrevious(): Boolean = index != -1

            override fun next(): CodeInstruction = this@ArgsBackedSize[++index]

            override fun nextIndex(): Int = index + 1

            override fun previous(): CodeInstruction = this@ArgsBackedSize[index--]

            override fun previousIndex(): Int = index

            override fun hasNext(): Boolean = index != 0

        }


        override fun listIterator(index: Int): ListIterator<CodeInstruction> {
            return if (index != 0) throw IndexOutOfBoundsException() else listIterator()
        }

        override fun subList(fromIndex: Int, toIndex: Int): List<CodeInstruction> = this

    }

    object CatchVariable : CodeInstruction, CodePart // ', CodePart' to fix processing loop

    data class TryData(val start: Label,
                       val end: Label,
                       var endOfTryCatch: Label? = null,
                       val catchDataList: MutableList<CatchData>)

    data class CatchData(val handler: Label,
                         val builder: CatchStatement.Builder,
                         var end: Label?,
                         var variable: VariableDeclaration? = null)

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