package org.jay.parser;

import java.util.Optional;

public interface IBuffer {
    void backward(int n);

    int offset(int n);

    abstract int remaining();

    void jump(int pos);

    Optional<Byte> head();

    byte[] headN(int n);

    void forward(int n);

    int getPos();
}
