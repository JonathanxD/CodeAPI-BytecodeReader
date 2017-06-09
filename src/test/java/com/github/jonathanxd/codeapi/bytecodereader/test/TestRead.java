package com.github.jonathanxd.codeapi.bytecodereader.test;

import com.github.jonathanxd.codeapi.CodeAPI;
import com.github.jonathanxd.codeapi.CodeInstruction;
import com.github.jonathanxd.codeapi.CodePart;
import com.github.jonathanxd.codeapi.CodeSource;
import com.github.jonathanxd.codeapi.base.BodyHolder;
import com.github.jonathanxd.codeapi.base.TypeDeclaration;
import com.github.jonathanxd.codeapi.bytecodereader.BytecodeReader;
import com.github.jonathanxd.codeapi.bytecodereader.extra.MagicPart;
import com.github.jonathanxd.codeapi.bytecodereader.extra.UnknownPart;
import com.github.jonathanxd.codeapi.factory.Factories;
import com.github.jonathanxd.codeapi.factory.PartFactory;
import com.github.jonathanxd.codeapi.processor.CodeProcessor;
import com.github.jonathanxd.codeapi.processor.Processor;
import com.github.jonathanxd.codeapi.source.process.KeysKt;
import com.github.jonathanxd.codeapi.source.process.PlainSourceGenerator;
import com.github.jonathanxd.iutils.data.TypedData;

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


        InputStream resourceAsStream = TestRead.class.getResourceAsStream("/TryWithResourcesTest_TryWithResourcesTestClass_Result.class");

        byte[] bytes;

        try {
            bytes = TestRead.toByteArray(resourceAsStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BytecodeReader bytecodeReader = new BytecodeReader();

        TypeDeclaration read = bytecodeReader.read(bytes);

        System.out.println(print(read, true));


        PlainSourceGenerator plainSourceGenerator = new PlainSourceGenerator();

        plainSourceGenerator.registerProcessor(new Processor<UnknownPart>() {
            @Override
            public void process(UnknownPart part, @NotNull TypedData data, @NotNull CodeProcessor<?> codeProcessor) {
                KeysKt.getAPPENDER().getOrNull(data).append(part.toString());
            }

            @Override
            public void endProcess(UnknownPart part, @NotNull TypedData data, @NotNull CodeProcessor<?> codeProcessor) {

            }
        }, UnknownPart.class);

        plainSourceGenerator.registerProcessor(new Processor<MagicPart>() {
            @Override
            public void process(MagicPart part, @NotNull TypedData data, @NotNull CodeProcessor<?> codeProcessor) {
                //KeysKt.getAPPENDER().getOrNull(data).append(part.getObj().toString());
            }

            @Override
            public void endProcess(MagicPart part, @NotNull TypedData data, @NotNull CodeProcessor<?> codeProcessor) {

            }
        }, MagicPart.class);

        String gen = plainSourceGenerator.process(read);

        System.out.println(gen);
    }

    private String print(CodePart part, boolean printType) {
        StringBuilder sb = new StringBuilder();
        sb.append(part.toString());

        if (part instanceof BodyHolder) {

            if (part instanceof TypeDeclaration && !printType)
                return sb.toString();

            sb.append("\n{\n");
            CodeSource body = ((BodyHolder) part).getBody();
            sb.append("    " + body.stream().map(codePart -> print(codePart, false)).collect(Collectors.joining(",    \n    ")));

            sb.append("\n}");

        }

        return sb.toString();
    }

}
