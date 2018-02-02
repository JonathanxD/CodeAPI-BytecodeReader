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
package com.github.jonathanxd.kores.bytecodereader.env

import com.github.jonathanxd.iutils.data.TypedData
import com.github.jonathanxd.kores.type.KoresType
import com.github.jonathanxd.kores.type.PlainKoresType
import com.github.jonathanxd.kores.util.*
import java.lang.reflect.Type
import java.util.*

/**
 * Bytecode reading environment

 * Helper class.
 */
class Environment : TypeResolver {

    val data = TypedData()
    private val types = HashMap<String, Type>()
    val typeResolver: TypeResolver = SimpleResolver(object : TypeResolver {
        override fun resolve(name: String, isInterface: Boolean): Type =
            this@Environment.getType(name, isInterface)
    }, false)

    fun getType(str: String): Type {
        return this.getType(str, false)
    }

    fun getType(str: String, isInterface: Boolean): Type {
        return this.getType0(str, isInterface)
    }

    private fun getType0(str: String, isInterface: Boolean): Type {

        val type: Type

        val types = this.getTypes()

        if (types.containsKey(str)) {
            type = types[str]!!
        } else {

            val aClass: Class<*>? = this.check(str)

            if (aClass != null) {
                type = aClass
            } else {
                type = BytecodeKoresType(str, isInterface)
            }

            types.put(str, type)
        }

        if (type is BytecodeKoresType) {
            if (!type.isInterface && isInterface) {
                type.isInterface = true
            }
        }


        return type
    }

    private fun check(str: String): Class<*>? {
        try {
            return Class.forName(str)
        } catch (e: ClassNotFoundException) {
            return null
        }

    }

    fun resolveUnknown(name: String): Type {
        return this.typeResolver.resolveUnknown(name)
    }

    override fun resolve(name: String, isInterface: Boolean): Type {
        return this.typeResolver.resolve(name, isInterface)
    }

    fun resolveInterface(type: String): Type {
        return this.typeResolver.resolveInterface(type)
    }

    fun resolveClass(type: String): Type {
        return this.typeResolver.resolveClass(type)
    }

    private fun getTypes(): MutableMap<String, Type> {
        return this.types
    }

    internal class BytecodeKoresType : PlainKoresType {

        override var isInterface: Boolean = false

        constructor(canonicalName: String) : super(canonicalName) {
            this.isInterface = false
        }

        constructor(canonicalName: String, isInterface: Boolean) : super(
            canonicalName,
            isInterface
        ) {
            this.isInterface = isInterface
        }

        override fun hashCode(): Int {
            return this.hash()
        }

        override fun equals(other: Any?): Boolean {

            if (other is KoresType)
                return this.compareTo((other as KoresType?)!!) == 0

            return super.equals(other)
        }
    }
}
