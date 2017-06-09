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
import com.github.jonathanxd.codeapi.bytecodereader.env.StackManager
import com.github.jonathanxd.codeapi.bytecodereader.extra.MagicPart
import com.github.jonathanxd.codeapi.bytecodereader.extra.UnknownPart
import org.objectweb.asm.*

internal class OperandAddVisitor(val stackManager: StackManager<CodeInstruction>,
                                 api: Int = Opcodes.ASM5,
                                 methodVisitor: MethodVisitor? = null) : MethodVisitor(api, methodVisitor) {

    override fun visitMultiANewArrayInsn(desc: String?, dims: Int) {
        super.visitMultiANewArrayInsn(desc, dims)
        this.push("visitMultiANewArrayInsn[desc=$desc, dims=$dims]")
    }

    override fun visitFrame(type: Int, nLocal: Int, local: Array<out Any>?, nStack: Int, stack: Array<out Any>?) {
        super.visitFrame(type, nLocal, local, nStack, stack)
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        super.visitVarInsn(opcode, `var`)
        this.push("visitVarInsn[opcode=$opcode, var=$`var`]")
    }

    override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
        super.visitTryCatchBlock(start, end, handler, type)
        this.push("visitTryCatchBlock[start=$start, end=$end, handler=$handler, type=$type]")
    }

    override fun visitLookupSwitchInsn(dflt: Label?, keys: IntArray?, labels: Array<out Label>?) {
        super.visitLookupSwitchInsn(dflt, keys, labels)
        this.push("visitLookupSwitchInsn[dflt=$dflt, keys=${java.util.Arrays.toString(keys)}, labels=${java.util.Arrays.toString(labels)}]")
    }

    override fun visitJumpInsn(opcode: Int, label: Label?) {
        super.visitJumpInsn(opcode, label)
        this.push("visitJumpInsn[opcode=$opcode, label=$label]")
    }

    override fun visitLdcInsn(cst: Any?) {
        super.visitLdcInsn(cst)
        this.push("ldc[cst=$cst]")
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        super.visitIntInsn(opcode, operand)
        this.push("visitIntInsn[opcode=$opcode, operand=$operand]")
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
        super.visitTypeInsn(opcode, type)
        this.push("visitTypeInsn[opcode=$opcode, type=$type]")
    }

    override fun visitAnnotationDefault(): AnnotationVisitor {
        return super.visitAnnotationDefault()
    }

    override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor {
        return super.visitAnnotation(desc, visible)
    }

    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, desc: String?, visible: Boolean): AnnotationVisitor {
        return super.visitTypeAnnotation(typeRef, typePath, desc, visible)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        super.visitMaxs(maxStack, maxLocals)
    }

    override fun visitInvokeDynamicInsn(name: String?, desc: String?, bsm: Handle?, vararg bsmArgs: Any?) {
        super.visitInvokeDynamicInsn(name, desc, bsm, *bsmArgs)
        this.push("visitInvokeDynamicInsn[name=$name, desc=$desc, bsm=$bsm, bsmArgs=${java.util.Arrays.toString(bsmArgs)}]")
    }

    override fun visitLabel(label: Label?) {
        super.visitLabel(label)
        //this.push("visitLabel[label=$label]")
        label?.let { this.stackManager.push(MagicPart(it)) }
    }

    override fun visitTryCatchAnnotation(typeRef: Int, typePath: TypePath?, desc: String?, visible: Boolean): AnnotationVisitor {
        return super.visitTryCatchAnnotation(typeRef, typePath, desc, visible)
    }

    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, desc: String?) {
        super.visitMethodInsn(opcode, owner, name, desc)
        this.push("visitMethodInsn[opcode=$opcode, owner=$owner, name=$name, desc=$desc]")
    }

    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, desc: String?, itf: Boolean) {
        super.visitMethodInsn(opcode, owner, name, desc, itf)
        this.push("visitMethodInsn[opcode=$opcode, owner=$owner, name=$name, desc=$desc, itf=$itf]")
    }

    override fun visitInsn(opcode: Int) {
        super.visitInsn(opcode)
        if (opcode != Opcodes.NOP)
            this.push("visitInsn[opcode=$opcode]")
    }

    override fun visitInsnAnnotation(typeRef: Int, typePath: TypePath?, desc: String?, visible: Boolean): AnnotationVisitor {
        return super.visitInsnAnnotation(typeRef, typePath, desc, visible)
    }

    override fun visitParameterAnnotation(parameter: Int, desc: String?, visible: Boolean): AnnotationVisitor {
        return super.visitParameterAnnotation(parameter, desc, visible)
    }

    override fun visitIincInsn(`var`: Int, increment: Int) {
        super.visitIincInsn(`var`, increment)
        this.push("visitIincInsn[var=$`var`, increment=$increment]")
    }

    override fun visitLineNumber(line: Int, start: Label?) {
        super.visitLineNumber(line, start)
        //this.push("visitLineNumber[line=$`line`, start=$start]")
    }

    override fun visitLocalVariableAnnotation(typeRef: Int, typePath: TypePath?, start: Array<out Label>?, end: Array<out Label>?, index: IntArray?, desc: String?, visible: Boolean): AnnotationVisitor {
        return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible)
    }

    override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label?, vararg labels: Label?) {
        super.visitTableSwitchInsn(min, max, dflt, *labels)
        this.push("visitTableSwitchInsn[min=$min, max=$max, dlft=$dflt, labels=${java.util.Arrays.toString(labels)}]")
    }

    override fun visitEnd() {
        super.visitEnd()
    }

    override fun visitLocalVariable(name: String?, desc: String?, signature: String?, start: Label?, end: Label?, index: Int) {
        super.visitLocalVariable(name, desc, signature, start, end, index)
        this.push("visitLocalVariable[name=$name, desc=$desc, start=$start, end=$end, index=$index]")
    }

    override fun visitParameter(name: String?, access: Int) {
        super.visitParameter(name, access)
        this.push("visitParameter[name=$name, access=$access]")
    }

    override fun visitAttribute(attr: Attribute?) {
        super.visitAttribute(attr)
        this.push("visitAttribute[attr=$attr]")
    }

    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, desc: String?) {
        super.visitFieldInsn(opcode, owner, name, desc)
        this.push("visitFieldInsn[opcode=$opcode, owner=$owner, name=$name, desc=$desc]")
    }

    override fun visitCode() {
        super.visitCode()
    }

    private fun push(str: String) {
        this.stackManager.push(UnknownPart(str))
    }

}