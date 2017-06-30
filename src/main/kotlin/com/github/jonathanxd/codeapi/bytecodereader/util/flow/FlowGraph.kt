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
import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.base.IfExpr
import com.github.jonathanxd.codeapi.base.IfGroup
import com.github.jonathanxd.codeapi.base.IfStatement
import com.github.jonathanxd.codeapi.bytecodereader.util.asm.inverse
import com.github.jonathanxd.codeapi.operator.Operator
import com.github.jonathanxd.codeapi.operator.Operators
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
 *
 * Obs: Not all right side nodes points to left when should because Reader do a lot of magic to achieve that on it own
 */
data class FlowNode(val position: Int,
                    val operation: Operator.Conditional,
                    val expr1: CodeInstruction,
                    val expr2: CodeInstruction,
                    val target: Target,
                    var body: Int = -1,
                    var elseRange: IntRange = IntRange.EMPTY,
                    // TODO: Can be replaced by dominator, which will point to first FlowNode in the graph
                    var definedStm: IfStatement? = null,
                    var previous: FlowNode? = null, // Flexible;
                    var right: FlowNode? = null,
                    var left: FlowNode? = null,
                    var isInverse: Boolean = false) {

    override fun toString(): String =
        "FlowNode(position=$position, operation=$operation, expr1=$expr1, expr2=$expr2, target=$target, body=$body, else=$elseRange)"

}

data class Target(val position: Int,
                  val label: Label)

fun FlowNode.buildIfExprs(inverse: Boolean): List<CodeInstruction> {
    val insns = mutableListOf<CodeInstruction>()

    this.buildIfExprsTo(insns, inverse)

    return insns
}

fun FlowNode.buildIfExprsTo(insns: MutableList<CodeInstruction>, inverse: Boolean) {

    val operation = if(inverse == !this.isInverse) this.operation.inverse() else this.operation
    insns += IfExpr(this.expr1, operation, this.expr2)

    /*fun FlowNode.build(op: Operator.Conditional) {
        val previous = this.previous
        if (previous != null) {
            if (previous.target.position <= this.target.position) {
                groupLeft(insns, previous, this)
                insns += op
                this.buildIfExprsTo(insns, inverse)
            } else {
                insns += op
                this.buildIfExprsTo(groupRight(insns), inverse)
            }
        } else {
            insns += op
            this.buildIfExprsTo(insns, inverse)
        }
    }*/

    val right = this.right
    if(right != null) {
        val previous = this.previous
        if (previous != null) {
            if(previous.target.position > this.target.position) {
                insns += Operators.AND
                val r = insns.removeAt(insns.size - 1)
                val x = groupRight(insns)
                x.add(r)
                right.buildIfExprsTo(x, inverse)
            } else {
                insns += Operators.AND
                right.buildIfExprsTo(insns, inverse)
            }
        } else {
            insns += Operators.AND
            right.buildIfExprsTo(insns, inverse)
        }
    }

    val left = this.left
    // TODO: Better Or Logic
    if(left != null) {
        val previous = this.previous
        if (previous != null) {
            if (previous.target.position != this.target.position) {
                val r = insns.removeAt(insns.size - 1)
                val x = groupRight(insns)
                x.add(r)
                x += Operators.OR
                left.buildIfExprsTo(x, inverse)

            } else {
                groupLeft(insns, previous, this)
                insns += Operators.OR
                left.buildIfExprsTo(insns, inverse)
            }
        } else {
            insns += Operators.OR
            left.buildIfExprsTo(insns, inverse)
        }
    }

}

fun groupLeft(insns: MutableList<CodeInstruction>, previous: FlowNode, current: FlowNode) {
    val group = IfGroup(insns.toList())
    insns.clear()
    insns += group
}

fun groupRight(insns: MutableList<CodeInstruction>): MutableList<CodeInstruction> {
    val new = mutableListOf<CodeInstruction>()
    val group = IfGroup(new)
    insns += group

    return new
}