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

import com.github.jonathanxd.iutils.description.DescriptionUtil
import com.github.jonathanxd.kores.Instruction
import com.github.jonathanxd.kores.Types
import com.github.jonathanxd.kores.base.InvokeDynamic
import com.github.jonathanxd.kores.base.InvokeType
import com.github.jonathanxd.kores.base.Operate
import com.github.jonathanxd.kores.base.TypeSpec
import com.github.jonathanxd.kores.common.DynamicMethodSpec
import com.github.jonathanxd.kores.common.MethodInvokeSpec
import com.github.jonathanxd.kores.common.MethodTypeSpec
import com.github.jonathanxd.kores.common.Void
import com.github.jonathanxd.kores.factory.cast
import com.github.jonathanxd.kores.factory.returnValue
import com.github.jonathanxd.kores.helper.OperateHelper
import com.github.jonathanxd.kores.literal.Literal
import com.github.jonathanxd.kores.literal.Literals
import com.github.jonathanxd.kores.operator.Operator
import com.github.jonathanxd.kores.operator.Operators
import com.github.jonathanxd.kores.type.KoresType
import com.github.jonathanxd.kores.type.TypeRef
import com.github.jonathanxd.kores.type.javaSpecName
import com.github.jonathanxd.kores.typeOrNull
import com.github.jonathanxd.kores.util.TypeResolver
import com.github.jonathanxd.kores.util.resolveUnknown
import com.github.jonathanxd.kores.util.toTypeSpec
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

typealias ReflectType = java.lang.reflect.Type

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

    fun handleReturn(opcode: Int, input: Instruction?): Instruction {

        if (opcode == Opcodes.RETURN)
        // No computation is needed for void return.
            return returnValue(Types.VOID, Void)

        // Note that Java bytecode cover a limited set of types, is better to Kores infer the types
        // Example, types like Boolean, Character, Short, etc... is covered by IRETURN
        // and Kores works better with the original types (Kores don't like if you
        // return a boolean value using INT type like: Kores.returnValue(Types.INT, Literals.FALSE)).
        // TODO: Sometimes a forwarding lookup may be needed for booleans in following cases:
        // TODO: - Used as argument of a method
        // TODO: - Used as return type of a method
        // TODO: - Used as value of a field
        // TODO: In these cases a lookup is required, which traverses all the bytecode to find the correct type.
        var type: ReflectType? = input?.typeOrNull

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
                    throw IllegalArgumentException("Cannot handle return opcode: '${opcode.opcodeName}'")
                }
            }
        }

        return returnValue(
            type,
            input ?: throw IllegalArgumentException("Missing return value for ${opcode.opcodeName}")
        )
    }

    fun handleMathAndBitwise(opcode: Int, input1: Instruction, input2: Instruction): Instruction {
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

        return Operate(input1, operation, input2)
    }

    fun handleNegation(opcode: Int, input: Instruction): Instruction {
        return OperateHelper.builder(input)
            .neg()
            .build()
    }

    fun handleConversion(opcode: Int, input: Instruction): Instruction {
        val from: KoresType
        val to: KoresType

        when (opcode) {
            Opcodes.I2L -> {
                from = Types.INT
                to = Types.LONG
            } // TODO START
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

        return cast(from, to, input)
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
            else -> throw RuntimeException("Cannot determine InvokeType of opcode '${opcode.opcodeName}'")
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
            else -> throw RuntimeException("Cannot determine InvokeType of opcode '${opcode.opcodeName}'")
        }
    }


    @Suppress("NAME_SHADOWING")
    fun typeSpecFromDesc(
        resolver: TypeResolver,
        typeDeclarationRef: TypeRef,
        methodName: String,
        desc: String
    ): TypeSpec {
        val desc = "${typeDeclarationRef.javaSpecName}:$methodName$desc"

        val parameterTypes = DescriptionUtil.getParameterTypes(desc)
        val returnType = DescriptionUtil.getType(desc)

        return TypeSpec(resolver.resolveUnknown(returnType),
            parameterTypes
                .map { resolver.resolveUnknown(it) })
    }

    fun specFromHandle(handle: Handle, typeResolver: TypeResolver): MethodInvokeSpec {
        val invokeType = fromAsm_H(handle.tag)

        val owner = typeResolver.resolveUnknown(handle.owner)
        val desc = "${owner.javaSpecName}:${handle.name}${handle.desc}"

        val description = DescriptionUtil.parseDescription(desc)

        return MethodInvokeSpec(
            invokeType = invokeType,
            methodTypeSpec = MethodTypeSpec(
                localization = owner,
                methodName = handle.name,
                typeSpec = TypeSpec(
                    returnType = typeResolver.resolveUnknown(description.type),
                    parameterTypes = description.parameterTypes.map { typeResolver.resolveUnknown(it) }
                )
            )
        )
    }

    fun fromHandle(
        handle: Handle,
        args: Array<Any>,
        invocation: DynamicMethodSpec,
        typeResolver: TypeResolver
    ): InvokeDynamic {
        val fullMethodSpec = specFromHandle(handle, typeResolver)

        return InvokeDynamic(
            bootstrap = fullMethodSpec,
            dynamicMethod = invocation,
            bootstrapArgs = bsmArgsFromAsm(args, typeResolver)
        )
    }

    fun bsmArgsFromAsm(asmArgs: Array<Any>?, typeResolver: TypeResolver): List<Any> {
        if (asmArgs == null || asmArgs.isEmpty())
            return emptyList()

        val koresArgsList = mutableListOf<Any>()

        for (asmArg in asmArgs) {
            if (asmArg is Int
                    || asmArg is Float
                    || asmArg is Long
                    || asmArg is Double
                    || asmArg is String
            ) {
                koresArgsList.add(asmArg)
            } else if (asmArg is Type) {

                val className = asmArg.className

                if (className != null) {
                    // Class
                    koresArgsList.add(typeResolver.resolveUnknown(className))
                } else {
                    // Method
                    koresArgsList.add(toTypeSpec(asmArg.descriptor, typeResolver))
                }


            } else if (asmArg is Handle) {
                koresArgsList.add(specFromHandle(asmArg, typeResolver))
            } else {
                throw IllegalArgumentException("Unsupported ASM BSM Argument: " + asmArg)
            }
        }

        return koresArgsList
    }

}
