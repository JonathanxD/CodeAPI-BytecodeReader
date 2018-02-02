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
package com.github.jonathanxd.kores.bytecodereader.util

import com.github.jonathanxd.controlflowhelper.BasicBlock
import com.github.jonathanxd.kores.Instruction
import com.github.jonathanxd.kores.base.IfExpr
import com.github.jonathanxd.kores.base.IfStatement
import com.github.jonathanxd.kores.bytecodereader.asm.Bl
import org.objectweb.asm.tree.LabelNode

data class IfExprData(
    val label: LabelNode, // Where expression resides, but does not mean that jumping to this label will jump to the if expression
    val expr: IfExpr,
    val bl: Bl.IfExprBlock,
    val successTarget: LabelNode?,
    val errorTarget: LabelNode?,
    val else_: BasicBlock?
) : Instruction

data class IfStatementData(
    val exprs: List<IfExprData>,
    val stm: IfStatement,
    val start: BasicBlock,
    val end: BasicBlock,
    var else_: BasicBlock?
)