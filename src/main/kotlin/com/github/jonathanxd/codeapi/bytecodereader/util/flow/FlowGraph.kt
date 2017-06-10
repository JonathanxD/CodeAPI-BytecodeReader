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
package com.github.jonathanxd.codeapi.bytecodereader.util.flow

import com.github.jonathanxd.codeapi.CodeInstruction
import com.github.jonathanxd.codeapi.operator.Operator
import org.objectweb.asm.Label

/**
 * Denotes an `if` node.
 *
 * This is like `CodeAPI IfExpr` but hold a [Target] instead of a body.
 *
 * The [position] property means the position in the bytecode. If target position is less than
 * FlowNode position, then this flow node is an `while` statement.
 *
 * As the Reader does a forwarding analysis, an [next] node is holden.
 */
data class FlowNode(val position: Int,
                    val operation: Operator.Conditional,
                    val expr1: CodeInstruction,
                    val expr2: CodeInstruction,
                    val target: Target,
                    var body: Int = -1,
                    var elseIndex: Int = -1,
                    var previous: FlowNode? = null, // Flexible
                    val next: FlowNode? = null)

data class Target(val position: Int,
                  val label: Label)

