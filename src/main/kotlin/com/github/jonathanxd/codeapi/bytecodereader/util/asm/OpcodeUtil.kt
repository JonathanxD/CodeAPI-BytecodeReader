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

import com.github.jonathanxd.codeapi.CodeInstruction
import com.github.jonathanxd.codeapi.Types
import com.github.jonathanxd.codeapi.bytecodereader.env.EmulatedFrame
import com.github.jonathanxd.codeapi.bytecodereader.util.opcodeName
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.operator.Operator
import com.github.jonathanxd.codeapi.operator.Operators
import com.github.jonathanxd.codeapi.util.`is`
import com.github.jonathanxd.codeapi.util.typeOrNull
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*

/**
 * Gets amount of args of an Opcode
 */
val Int.argsSize: Int
    get() = when (this) {
        IFEQ, IFNE,
        IFLT, IFLE,
        IFGT, IFGE,
        IFNULL, IFNONNULL -> 1
        IF_ICMPEQ, IF_ICMPNE,
        IF_ICMPLT, IF_ICMPLE,
        IF_ICMPGT, IF_ICMPGE,
        IF_ACMPEQ, IF_ACMPNE -> 2
        else -> -1
    }

/**
 * Gets arguments of an jump instruction (`ifeq`, `ifne`, etc...)
 */
fun Int.getJmpArgs(frame: EmulatedFrame): List<CodeInstruction> {
    val size = this.argsSize

    require(size > -1)

    return when (this) {
        IFEQ, IFNE,
        IFLT, IFLE,
        IFGT, IFGE -> {
            val pop = frame.operandStack.pop(size)
            pop + zero(pop.single())
        }
        IFNULL, IFNONNULL -> frame.operandStack.pop(size) + Literals.NULL
        IF_ICMPEQ, IF_ICMPNE,
        IF_ICMPLT, IF_ICMPLE,
        IF_ICMPGT, IF_ICMPGE,
        IF_ACMPEQ, IF_ACMPNE -> frame.operandStack.pop(2)
        else -> throw IllegalArgumentException("Input opcode '${this.opcodeName}' is not an valid if jump opcode.")
    }
}

/**
 * Creates an `ZERO` based on type of [instruction], this hack fixes the boolean translation,
 * if [instruction] is a boolean, then a [Literals.FALSE] is returned instead of `0`, otherwise a [Literals.TRUE]
 * is returned. Don't worry about readability, post-processor will convert `a != false` to `a == true`.
 */
fun zero(instruction: CodeInstruction) =
        if (instruction.typeOrNull?.`is`(Types.BOOLEAN) ?: false) Literals.FALSE
        else Literals.INT(0)

/**
 * Stores conditional operator of an [Opcode][Opcodes]. Note that for
 * `null` and `non null` this property follows semantic of [getJmpArgs],
 * this means that this property returns [Operators.EQUAL_TO] for `null`
 * and [Operators.NOT_EQUAL_TO] for `non null`.
 */
val Int.conditionalOperator: Operator.Conditional
    get() = when (this) {
        IFEQ, IF_ICMPEQ, IF_ACMPEQ, IFNULL -> Operators.EQUAL_TO
        IFNE, IF_ICMPNE, IF_ACMPNE, IFNONNULL -> Operators.NOT_EQUAL_TO
        IFLT, IF_ICMPLT -> Operators.LESS_THAN
        IFLE, IF_ICMPLE -> Operators.LESS_THAN_OR_EQUAL_TO
        IFGT, IF_ICMPGT -> Operators.GREATER_THAN
        IFGE, IF_ICMPGE -> Operators.GREATER_THAN_OR_EQUAL_TO
        else -> throw IllegalArgumentException("Input opcode '${this.opcodeName}' is not an valid conditional operator.")
    }

/**
 * Returns true if this is an valid jmp (a jmp that can be translated to CodeAPI IfExpr)
 */
fun Int.isValidIfExprJmp() = when (this) {
    IFEQ, IF_ICMPEQ, IF_ACMPEQ, IFNULL,
    IFNE, IF_ICMPNE, IF_ACMPNE, IFNONNULL,
    IFLT, IF_ICMPLT, IFLE, IF_ICMPLE,
    IFGT, IF_ICMPGT, IFGE, IF_ICMPGE -> true
    else -> false
}