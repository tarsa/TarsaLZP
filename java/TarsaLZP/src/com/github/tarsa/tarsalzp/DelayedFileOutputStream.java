/*
 * Copyright (c) 2012, Piotr Tarsa
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * 
 * Neither the name of the author nor the names of its contributors may be used 
 * to endorse or promote products derived from this software without specific 
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
