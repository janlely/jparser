package org.jay.parser;

import java.util.Optional;

public interface IBuffer {
    void backward(int n);

    int remaining();

    Optional<Byte> head();

    byte[] headN(int n);

    void forward(int n);

    int getPos();

    IBuffer[] splitAt(int idx);

    byte[] remainContent();
}
