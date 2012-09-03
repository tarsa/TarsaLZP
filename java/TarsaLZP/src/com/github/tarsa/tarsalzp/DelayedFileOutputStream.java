package com.github.tarsa.tarsalzp;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Piotr Tarsa
 */
public final class DelayedFileOutputStream extends OutputStream {

    private enum Type {

        TypeFile, TypeFileDescriptor
    }
    private final Type type;
    private final File targetFile;
    private final boolean targetFileAppend;
    private final FileDescriptor targetFileDescriptor;
    private FileOutputStream targetFileOutputStream;
    private boolean initialized = false;

    public DelayedFileOutputStream(final String name) {
        this(name, false);
    }

    public DelayedFileOutputStream(final String name, final boolean append) {
        this(name == null ? null : new File(name), append);
    }

    public DelayedFileOutputStream(final File file) {
        this(file, false);
    }

    public DelayedFileOutputStream(final File file, final boolean append) {
        type = Type.TypeFile;
        targetFile = file;
        targetFileAppend = append;
        targetFileDescriptor = null;
    }

    public DelayedFileOutputStream(final FileDescriptor fdObj) {
        type = Type.TypeFileDescriptor;
        targetFile = null;
        targetFileAppend = false;
        targetFileDescriptor = fdObj;
    }

    private void init() throws FileNotFoundException {
        if (!initialized) {
            targetFileOutputStream = type == Type.TypeFile
                    ? new FileOutputStream(targetFile, targetFileAppend)
                    : new FileOutputStream(targetFileDescriptor);
            initialized = true;
        }
    }

    @Override
    public void close() throws IOException {
        if (!initialized) {
            init();
        }
        targetFileOutputStream.close();
    }

    @Override
    public void flush() throws IOException {
        if (!initialized) {
            init();
        }
        targetFileOutputStream.flush();
    }

    @Override
    public void write(final byte[] b) throws IOException {
        if (!initialized) {
            init();
        }
        targetFileOutputStream.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) 
            throws IOException {
        if (!initialized) {
            init();
        }
        targetFileOutputStream.write(b, off, len);
    }

    @Override
    public void write(final int b) throws IOException {
        if (!initialized) {
            init();
        }
        targetFileOutputStream.write(b);
    }
}
