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

        boolean gen0 = true;
        InputStream resourceAsStream = TestRead.class.getResourceAsStream("/EventSys_listener_if_test.class");
        InputStream resourceAsStream2 = TestRead.class.getResourceAsStream("/lvl2/SimpleIfZ.class");

        byte[] bytes = new byte[0];
        byte[] bytes2;

        try {
            if(gen0)
                bytes = TestRead.toByteArray(resourceAsStream);
            bytes2 = TestRead.toByteArray(resourceAsStream2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BytecodeReader bytecodeReader = new BytecodeReader();

        TypeDeclaration read = null;

        if(gen0)
            read = bytecodeReader.read(bytes);

        TypeDeclaration read2 = bytecodeReader.read(bytes2);


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
                KeysKt.getAPPENDER().getOrNull(data).append("Magic["+part.getObj().toString()+"]");
            }

            @Override
            public void endProcess(MagicPart part, @NotNull TypedData data, @NotNull CodeProcessor<?> codeProcessor) {

            }
        }, MagicPart.class);

        if(gen0) {
            String gen = plainSourceGenerator.process(read);
            System.out.println(gen);
            System.out.println("====================================================================");
        }

        String gen2 = plainSourceGenerator.process(read2);

        System.out.println(gen2);
    }

    private String print(CodePart part, boolean printType) {
        StringBuilder sb = new StringBuilder();
        sb.append(part.toString());

        if (part instanceof BodyHolder) {

            if (part instanceof TypeDeclaration && !printType)
                return sb.toString();

            sb.append("\n{\n");
            CodeSource body = ((BodyHolder) part).getBody();
            sb.append("    ").append(body.stream().map(codePart -> print(codePart, false)).collect(Collectors.joining(",    \n    ")));

            sb.append("\n}");

        }

        return sb.toString();
    }

}
