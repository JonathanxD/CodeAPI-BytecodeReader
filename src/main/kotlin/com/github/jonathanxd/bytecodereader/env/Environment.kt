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
package com.github.jonathanxd.bytecodereader.env

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.common.Data
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.type.PlainCodeType
import com.github.jonathanxd.codeapi.util.SimpleResolver
import com.github.jonathanxd.codeapi.util.TypeResolver
import java.util.*

/**
 * Bytecode reading environment

 * Helper class.
 */
class Environment : TypeResolver {

    val data = Data()
    private val types = HashMap<String, CodeType>()
    val typeResolver: TypeResolver = SimpleResolver(TypeResolver { str, isInterface -> this.getType(str, isInterface) }, false)

    fun getType(str: String): CodeType {
        return this.getType(str, false)
    }

    fun getType(str: String, isInterface: Boolean): CodeType {
        return this.getType0(str, isInterface)
    }

    private fun getType0(str: String, isInterface: Boolean): CodeType {

        val type: CodeType

        val types = this.getTypes()

        if (types.containsKey(str)) {
            type = types[str]!!
        } else {

            val aClass: Class<*>? = this.check(str)

            if (aClass != null) {
                type = CodeAPI.getJavaType(aClass)
            } else {
                type = BytecodeCodeType(str, isInterface)
            }

            types.put(str, type)
        }

        if (type is BytecodeCodeType) {
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

    override fun resolveUnknown(name: String): CodeType {
        return this.typeResolver.resolveUnknown(name)
    }

    override fun resolve(name: String, isInterface: Boolean): CodeType {
        return this.typeResolver.resolve(name, isInterface)
    }

    override fun resolveInterface(type: String): CodeType {
        return this.typeResolver.resolveInterface(type)
    }

    override fun resolveClass(type: String): CodeType {
        return this.typeResolver.resolveClass(type)
    }

    protected fun getTypes(): MutableMap<String, CodeType> {
        return this.types
    }

    internal class BytecodeCodeType : PlainCodeType {

        override var isInterface: Boolean = false

        constructor(canonicalName: String) : super(canonicalName) {
            this.isInterface = false
        }

        constructor(canonicalName: String, isInterface: Boolean) : super(canonicalName, isInterface) {
            this.isInterface = isInterface
        }

        override fun equals(obj: Any?): Boolean {

            if (obj is CodeType)
                return this.compareTo((obj as CodeType?)!!) == 0

            return super.equals(obj)
        }
    }
}
