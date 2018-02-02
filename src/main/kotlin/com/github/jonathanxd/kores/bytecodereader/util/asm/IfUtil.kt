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

import com.github.jonathanxd.kores.Instruction
import com.github.jonathanxd.kores.base.IfGroup
import com.github.jonathanxd.kores.operator.Operator
import com.github.jonathanxd.kores.operator.Operators

fun Operator.Conditional.inverse(): Operator.Conditional {
    return when (this) {
        Operators.EQUAL_TO -> Operators.NOT_EQUAL_TO
        Operators.NOT_EQUAL_TO -> Operators.EQUAL_TO
        Operators.LESS_THAN -> Operators.GREATER_THAN_OR_EQUAL_TO
        Operators.LESS_THAN_OR_EQUAL_TO -> Operators.GREATER_THAN
        Operators.GREATER_THAN -> Operators.GREATER_THAN_OR_EQUAL_TO
        Operators.GREATER_THAN_OR_EQUAL_TO -> Operators.LESS_THAN
        else -> throw IllegalArgumentException("Cannot get inverse operator of $this")
    }
}

tailrec fun simplify(expressions: List<Instruction>): List<Instruction> {
    return if (expressions.singleOrNull() is IfGroup) {
        simplify((expressions.single() as IfGroup).expressions)
    } else {
        expressions
    }
}