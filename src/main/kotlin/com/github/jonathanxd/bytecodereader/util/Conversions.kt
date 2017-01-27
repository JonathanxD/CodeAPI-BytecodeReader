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
package com.github.jonathanxd.bytecodereader.util

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.Types
import com.github.jonathanxd.codeapi.base.TypeDeclaration
import com.github.jonathanxd.codeapi.base.impl.OperateImpl
import com.github.jonathanxd.codeapi.common.*
import com.github.jonathanxd.codeapi.helper.OperateHelper
import com.github.jonathanxd.codeapi.literal.Literal
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.operator.Operator
import com.github.jonathanxd.codeapi.operator.Operators
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.util.CodePartUtil
import com.github.jonathanxd.codeapi.util.DescriptionHelper
import com.github.jonathanxd.codeapi.util.TypeResolver
import com.github.jonathanxd.iutils.description.DescriptionUtil
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

object Conversions : Opcodes {

    fun isLiteral(o: Any): Boolean {
        return o is Byte
                || o is Short
                || o is Int
                || o is Double
                || o is Float
                || o is Long
                || o is String
    }

    fun toLiteral(o: Any): Literal {
        if (o is Byte) {
            return Literals.BYTE(o)
        } else if (o is Short) {
            return Literals.SHORT(o)
        } else if (o is Int) {
            return Literals.INT(o)
        } else if (o is Double) {
            return Literals.DOUBLE(o)
        } else if (o is Float) {
            return Literals.FLOAT(o)
        } else if (o is Long) {
            return Literals.LONG(o)
        } else if (o is String) {
            return Literals.STRING(o)
        } else {
            throw IllegalArgumentException("Cannot convert '$o' to Literal.")
        }
    }

    fun handleReturn(opcode: Int, input: CodePart?): CodePart {

        if (opcode == Opcodes.RETURN)
        // No computation is needed for void return.
            return CodeAPI.returnValue(Types.VOID, null)

        // Note that Java bytecode cover a limited set of types, is better to CodeAPI infer the types
        // Example, types like Boolean, Character, Short, etc... is covered by IRETURN
        // and CodeAPI works better with the original types (CodeAPI don't like if you
        // return a boolean value using INT type like: CodeAPI.returnValue(Types.INT, Literals.FALSE)).
        var type: CodeType? = if (input != null) CodePartUtil.getTypeOrNull(input) else null

        if (type == null) {
            when (opcode) {
                Opcodes.IRETURN -> {
                    type = Types.INT
                }

                Opcodes.LRETURN -> {
                    type = Types.LONG
                }

                Opcodes.FRETURN -> {
                    type = Types.FLOAT
                }

                Opcodes.DRETURN -> {
                    type = Types.DOUBLE
                }

                Opcodes.ARETURN -> {
                    type = Types.OBJECT
                }

                else -> {
                    throw IllegalArgumentException("Cannot handle return opcode: '$opcode'")
                }
            }
        }

        return CodeAPI.returnValue(type, input)
    }

    fun handleMathAndBitwise(opcode: Int, input1: CodePart, input2: CodePart): CodePart {
        val operation: Operator.Math

        if (opcode >= Opcodes.IADD && opcode <= Opcodes.DADD) {
            operation = Operators.ADD
        } else if (opcode >= Opcodes.ISUB && opcode <= Opcodes.DSUB) {
            operation = Operators.SUBTRACT
        } else if (opcode >= Opcodes.IMUL && opcode <= Opcodes.DMUL) {
            operation = Operators.MULTIPLY
        } else if (opcode >= Opcodes.IDIV && opcode <= Opcodes.DDIV) {
            operation = Operators.DIVISION
        } else if (opcode >= Opcodes.IREM && opcode <= Opcodes.DREM) {
            operation = Operators.REMAINDER
        } else if (opcode >= Opcodes.ISHL && opcode <= Opcodes.LSHL) {
            operation = Operators.SIGNED_LEFT_SHIFT
        } else if (opcode >= Opcodes.ISHR && opcode <= Opcodes.LSHR) {
            operation = Operators.SIGNED_RIGHT_SHIFT
        } else if (opcode >= Opcodes.IUSHR && opcode <= Opcodes.LUSHR) {
            operation = Operators.UNSIGNED_RIGHT_SHIFT
        } else if (opcode >= Opcodes.IAND && opcode <= Opcodes.LAND) {
            operation = Operators.BITWISE_AND
        } else if (opcode >= Opcodes.IOR && opcode <= Opcodes.LOR) {
            operation = Operators.BITWISE_INCLUSIVE_OR
        } else if (opcode >= Opcodes.IXOR && opcode <= Opcodes.LXOR) {
            operation = Operators.BITWISE_EXCLUSIVE_OR
        } else {
            throw IllegalArgumentException("Cannot handle math or bitwise opcode: '$opcode'")
        }

        return OperateImpl(input1, operation, input2)
    }

    fun handleNegation(opcode: Int, input: CodePart): CodePart {
        return OperateHelper.builder(input)
                .neg()
                .build()
    }

    fun handleConversion(opcode: Int, input: CodePart): CodePart {
        val from: CodeType
        val to: CodeType

        when (opcode) {
            Opcodes.I2L -> {
                from = Types.INT
                to = Types.LONG
            }// TODO START
            Opcodes.I2F -> {
                from = Types.INT
                to = Types.FLOAT
            }
            Opcodes.I2D -> {
                from = Types.INT
                to = Types.DOUBLE
            }
            Opcodes.L2I -> {
                from = Types.LONG
                to = Types.INT
            }
            Opcodes.L2F -> {
                from = Types.LONG
                to = Types.FLOAT
            }
            Opcodes.L2D -> {
                from = Types.LONG
                to = Types.DOUBLE
            }
            Opcodes.F2I -> {
                from = Types.FLOAT
                to = Types.INT
            }
            Opcodes.F2L -> {
                from = Types.FLOAT
                to = Types.LONG
            }
            Opcodes.F2D -> {
                from = Types.FLOAT
                to = Types.DOUBLE
            }
            Opcodes.D2I -> {
                from = Types.DOUBLE
                to = Types.INT
            }
            Opcodes.D2L -> {
                from = Types.DOUBLE
                to = Types.LONG
            }
            Opcodes.D2F -> {
                from = Types.DOUBLE
                to = Types.FLOAT
            }
            Opcodes.I2B -> {
                from = Types.INT
                to = Types.BOOLEAN
            }
            Opcodes.I2C -> {
                from = Types.INT
                to = Types.CHAR
            }
            Opcodes.I2S -> {
                from = Types.INT
                to = Types.SHORT
            }
            else -> {
                throw IllegalArgumentException("Cannot handle conversion opcode: '$opcode'!")
            }
        }

        return CodeAPI.cast(from, to, input)
    }

    /**
     * Convert asm invocation opcode to [InvokeType].
     *
     * @param opcode Opcode to convert
     * @return asm flag corresponding to `invokeType`.
     */
    fun fromAsm(opcode: Int): InvokeType {
        when (opcode) {
            Opcodes.INVOKEINTERFACE -> return InvokeType.INVOKE_INTERFACE
            Opcodes.INVOKESPECIAL -> return InvokeType.INVOKE_SPECIAL
            Opcodes.INVOKEVIRTUAL -> return InvokeType.INVOKE_VIRTUAL
            Opcodes.INVOKESTATIC -> return InvokeType.INVOKE_STATIC
            Opcodes.INVOKEDYNAMIC -> return InvokeType.INVOKE_DYNAMIC
            else -> throw RuntimeException("Cannot determine InvokeType of opcde '$opcode'")
        }
    }

    /**
     * Convert asm [dynamic] invocation opcode to [InvokeType].
     *
     * @param opcode Opcode to convert
     * @return asm flag corresponding to `invokeType` (dynamic).
     */
    fun fromAsm_H(opcode: Int): InvokeType {
        when (opcode) {
            Opcodes.H_INVOKEINTERFACE -> return InvokeType.INVOKE_INTERFACE
            Opcodes.H_INVOKESPECIAL -> return InvokeType.INVOKE_SPECIAL
            Opcodes.H_INVOKEVIRTUAL -> return InvokeType.INVOKE_VIRTUAL
            Opcodes.H_INVOKESTATIC -> return InvokeType.INVOKE_STATIC
            else -> throw RuntimeException("Cannot determine InvokeType of opcode '$opcode'")
        }
    }


    @Suppress("NAME_SHADOWING")
    fun typeSpecFromDesc(resolver: TypeResolver, typeDeclaration: TypeDeclaration, methodName: String, desc: String): TypeSpec {
        val desc = "${typeDeclaration.javaSpecName}:$methodName$desc"

        val parameterTypes = DescriptionUtil.getParameterTypes(desc)
        val returnType = DescriptionUtil.getType(desc)

        return TypeSpec(resolver.resolveUnknown(returnType),
                parameterTypes
                        .map { resolver.resolveUnknown(it) })
    }

    fun specFromHandle(handle: Handle, typeResolver: TypeResolver): MethodInvokeSpec {
        val invokeType = fromAsm_H(handle.tag)

        val owner = typeResolver.resolveUnknown(handle.owner)
        val desc = owner.javaSpecName + ":" + handle.name + handle.desc

        val description = DescriptionUtil.parseDescription(desc)

        return MethodInvokeSpec(
                invokeType = invokeType,
                methodTypeSpec = MethodTypeSpec(
                        localization = owner,
                        methodName = handle.name,
                        typeSpec = TypeSpec(
                                returnType = typeResolver.resolveUnknown(description.returnType),
                                parameterTypes = description.parameterTypes.map { typeResolver.resolveUnknown(it) }
                        )
                )
        )
    }

    fun fromHandle(handle: Handle, args: Array<Any>, typeResolver: TypeResolver): InvokeDynamic {
        val invokeType = fromAsm_H(handle.tag)
        val fullMethodSpec = specFromHandle(handle, typeResolver)

        return InvokeDynamic.Bootstrap(
                methodTypeSpec = fullMethodSpec.methodTypeSpec,
                invokeType = invokeType,
                arguments = bsmArgsFromAsm(args, typeResolver)
        )
    }

    fun bsmArgsFromAsm(asmArgs: Array<Any>?, typeResolver: TypeResolver): Array<Any> {
        if (asmArgs == null || asmArgs.isEmpty())
            return emptyArray()

        val codeAPIArgsList = mutableListOf<Any>()

        for (asmArg in asmArgs) {
            if (asmArg is Int
                    || asmArg is Float
                    || asmArg is Long
                    || asmArg is Double
                    || asmArg is String) {
                codeAPIArgsList.add(asmArg)
            } else if (asmArg is Type) {

                val className = asmArg.className

                if (className != null) {
                    // Class
                    codeAPIArgsList.add(typeResolver.resolveUnknown(className))
                } else {
                    // Method
                    codeAPIArgsList.add(DescriptionHelper.toTypeSpec(asmArg.descriptor, typeResolver))
                }


            } else if (asmArg is Handle) {
                codeAPIArgsList.add(specFromHandle(asmArg, typeResolver))
            } else {
                throw IllegalArgumentException("Unsupported ASM BSM Argument: " + asmArg)
            }
        }

        return codeAPIArgsList.toTypedArray()
    }

}
