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
package com.github.jonathanxd.kores.bytecodereader.test;

import com.github.jonathanxd.iutils.data.TypedData;
import com.github.jonathanxd.kores.Instructions;
import com.github.jonathanxd.kores.KoresPart;
import com.github.jonathanxd.kores.base.BodyHolder;
import com.github.jonathanxd.kores.base.TypeDeclaration;
import com.github.jonathanxd.kores.bytecodereader.BytecodeReader;
import com.github.jonathanxd.kores.bytecodereader.extra.MagicPart;
import com.github.jonathanxd.kores.bytecodereader.extra.UnknownPart;
import com.github.jonathanxd.kores.processor.Processor;
import com.github.jonathanxd.kores.processor.ProcessorManager;
import com.github.jonathanxd.kores.source.process.KeysKt;
import com.github.jonathanxd.kores.source.process.PlainSourceGenerator;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

public class TestRead {


    public static byte[] toByteArray(InputStream input) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        return output.toByteArray();
    }

    @Test
    public void testRead() {

        boolean gen0 = true;
        InputStream resourceAsStream = TestRead.class.getResourceAsStream("/lvl2/SimpleIfZ.class");
        InputStream resourceAsStream2 = TestRead.class.getResourceAsStream("/lvl2/NestedIf.class");

        byte[] bytes = new byte[0];
        byte[] bytes2;

        try {
            if (gen0)
                bytes = TestRead.toByteArray(resourceAsStream);
            bytes2 = TestRead.toByteArray(resourceAsStream2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BytecodeReader bytecodeReader = new BytecodeReader();

        TypeDeclaration read = null;

        if (gen0)
            read = bytecodeReader.read(bytes);

        TypeDeclaration read2 = bytecodeReader.read(bytes2);


        PlainSourceGenerator plainSourceGenerator = new PlainSourceGenerator();

        plainSourceGenerator.registerProcessor(new Processor<UnknownPart>() {
            @Override
            public void process(UnknownPart part,
                                @NotNull TypedData data,
                                @NotNull ProcessorManager<?> processorManager) {
                KeysKt.getAPPENDER().getOrNull(data).append(part.toString());
            }

            @Override
            public void endProcess(UnknownPart part,
                                   @NotNull TypedData data,
                                   @NotNull ProcessorManager<?> processorManager) {

            }
        }, UnknownPart.class);

        plainSourceGenerator.registerProcessor(new Processor<MagicPart>() {
            @Override
            public void process(MagicPart part,
                                @NotNull TypedData data,
                                @NotNull ProcessorManager<?> processorManager) {
                KeysKt.getAPPENDER().getOrNull(data).append("Magic[" + part.getObj().toString() + "]");
            }

            @Override
            public void endProcess(MagicPart part,
                                   @NotNull TypedData data,
                                   @NotNull ProcessorManager<?> processorManager) {

            }
        }, MagicPart.class);

        if (gen0) {
            String gen = plainSourceGenerator.process(read);
            System.out.println(gen);
            System.out.println("====================================================================");
        }

        String gen2 = plainSourceGenerator.process(read2);

        System.out.println(gen2);
    }

    private String print(KoresPart part, boolean printType) {
        StringBuilder sb = new StringBuilder();
        sb.append(part.toString());

        if (part instanceof BodyHolder) {

            if (part instanceof TypeDeclaration && !printType)
                return sb.toString();

            sb.append("\n{\n");
            Instructions body = ((BodyHolder) part).getBody();
            sb.append("    ").append(body.stream().map(koresPart -> print(koresPart, false)).collect(Collectors.joining(",    \n    ")));

            sb.append("\n}");

        }

        return sb.toString();
    }

}
