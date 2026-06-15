package org.pytenix.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class FastByteArrayOutputStream extends ByteArrayOutputStream {


    public void write(ByteBuffer buffer) {
        if (buffer.hasArray()) {
            this.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
            buffer.position(buffer.limit());
        } else {
            int remaining = buffer.remaining();
            byte[] scratch = new byte[Math.min(remaining, 4096)];
            while (buffer.hasRemaining()) {
                int length = Math.min(buffer.remaining(), scratch.length);
                buffer.get(scratch, 0, length);
                this.write(scratch, 0, length);
            }
        }
    }

    public InputStream toInputStream() {
        return new ByteArrayInputStream(this.buf, 0, this.count);
    }
}
