package io.github.janlely.jparser;

import java.util.Optional;

/**
 * Buffer interface used by Parser
 */
public interface IBuffer {
    /**
     * @param n byte counts to move backward
     */
    void backward(int n);

    /**
     * @return remaining bytes
     */
    int remaining();

    /**
     * @return head byte
     */
    Optional<Byte> head();

    /**
     * @param n byte counts to head
     * @return leading n bytes
     */
    byte[] headN(int n);

    /**
     * @param n byte counts to move forward
     */
    void forward(int n);

    /**
     * @return current position of the cursor
     */
    int getPos();

    /**
     * @param idx position to split at
     * @return two buffers
     */
    IBuffer[] splitAt(int idx);

    /**
     * @return remaining bytes
     */
    byte[] remainContent();
}
