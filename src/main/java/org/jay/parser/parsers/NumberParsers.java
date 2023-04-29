package org.jay.parser.parsers;

import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.util.Buffer;
import org.jay.parser.util.ErrorUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class NumberParsers {

    public static Parser intStr(int a) {
        return TextParsers.string(String.valueOf(a)).map(__ -> a);
    }
    public static Parser anyLongBE() {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                byte[] data = buffer.headN(8);
                if (data.length != 4) {
                    return Result.builder()
                            .errorMsg(ErrorUtil.error(buffer.getPos()))
                            .build();
                }
                buffer.forward(8);
                ByteBuffer bf = ByteBuffer.wrap(data);
                bf.order(ByteOrder.BIG_ENDIAN);
                return Result.builder()
                        .length(8)
                        .result(List.of(bf.getLong())).build();
            }
        };
    }

    public static Parser anyLongLE() {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                byte[] data = buffer.headN(8);
                if (data.length != 4) {
                    return Result.builder()
                            .errorMsg(ErrorUtil.error(buffer.getPos()))
                            .build();
                }
                buffer.forward(8);
                ByteBuffer bf = ByteBuffer.wrap(data);
                bf.order(ByteOrder.LITTLE_ENDIAN);
                return Result.builder()
                        .length(8)
                        .result(List.of(bf.getLong())).build();
            }
        };
    }

    public static Parser anyIntBE() {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                byte[] data = buffer.headN(4);
                if (data.length != 4) {
                    return Result.builder()
                            .errorMsg(ErrorUtil.error(buffer.getPos()))
                            .build();
                }
                ByteBuffer bf = ByteBuffer.wrap(data);
                bf.order(ByteOrder.BIG_ENDIAN);
                buffer.forward(4);
                return Result.builder()
                        .length(4)
                        .result(List.of(bf.getInt())).build();
            }
        };
    }

    public static Parser anyIntLE() {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                byte[] data = buffer.headN(4);
                if (data.length != 4) {
                    return Result.builder()
                            .errorMsg(ErrorUtil.error(buffer.getPos()))
                            .build();
                }
                buffer.forward(4);
                ByteBuffer bf = ByteBuffer.wrap(data);
                bf.order(ByteOrder.LITTLE_ENDIAN);
                return Result.builder()
                        .length(4)
                        .result(List.of(bf.getInt())).build();
            }
        };
    }

    public static Parser intLE(int a) {
        return anyIntLE().must(b -> a == (int)((List)b).get(0));
    }
    public static Parser intBE(int a) {
        return anyIntBE().must(b -> a == (int)((List)b).get(0));
    }

    public static Parser longBE(long a) {
        return anyLongBE().must(b -> a == (long)((List)b).get(0));
    }
    public static Parser longLE(long a) {
        return anyLongLE().must(b -> a == (long)((List)b).get(0));
    }


}
