package com.github.tarsa.tarsalzp.core;

import com.github.tarsa.tarsalzp.Options;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author Piotr Tarsa
 */
public final class Coder {

    public static final long HeaderValue = 2345174324078614718l;

    public interface Callback {

        void progressChanged(final long processedSymbols);
    }

    public static Options getOptions(final InputStream inputStream)
            throws IOException {
        long header = 0;
        for (int i = 0; i < 8; i++) {
            header <<= 8;
            final int inputByte = inputStream.read();
            if (inputByte == -1) {
                throw new IOException("Unexpected end of file.");
            }
            header |= inputByte;
        }
        if (header != HeaderValue) {
            throw new IOException("Wrong file header. Probably not a "
                    + "compressed file.");
        }
        return getOptionsHeaderless(inputStream);
    }

    public static Options getOptionsHeaderless(final InputStream inputStream)
            throws IOException {
        long packedOptions = 0;
        for (int i = 0; i < 8; i++) {
            packedOptions <<= 8;
            final int inputByte = inputStream.read();
            if (inputByte == -1) {
                throw new IOException("Unexpected end of file.");
            }
            packedOptions |= inputByte;
        }
        final Options result = Options.fromPacked(packedOptions);
        if (result == null) {
            throw new IllegalArgumentException("Invalid compression options.");
        } else {
            return result;
        }
    }

    public static void decode(final InputStream inputStream,
            final OutputStream outputStream, final Callback callback,
            final long intervalLength) throws IOException {
        if (intervalLength <= 0) {
            throw new IllegalArgumentException(
                    "Interval length has to be positive.");
        }
        final Options options = getOptions(inputStream);
        decodeRaw(inputStream, outputStream, callback, intervalLength, options);
    }

    public static void decodeRaw(final InputStream inputStream,
            final OutputStream outputStream, final Callback callback,
            final long intervalLength, final Options options)
            throws IOException {
        if (intervalLength <= 0) {
            throw new IllegalArgumentException(
                    "Interval length has to be positive.");
        }
        final Decoder decoder = new Decoder(inputStream, outputStream, options);
        long amountProcessed = 0;
        while (!decoder.decode(intervalLength)) {
            amountProcessed += intervalLength;
            if (callback != null) {
                callback.progressChanged(amountProcessed);
            }
        }
    }

    public static void encode(final InputStream inputStream,
            final OutputStream outputStream, final Callback callback,
            final long intervalLength, final Options options)
            throws IOException {
        if (intervalLength <= 0) {
            throw new IllegalArgumentException(
                    "Interval length has to be positive.");
        }
        final Encoder encoder = new Encoder(inputStream, outputStream, options);
        long header = HeaderValue;
        for (int i = 0; i < 8; i++) {
            encoder.outputStream.write((int) (header >>> 56) & 0xff);
            header <<= 8;
        }
        long packedOptions = options.toPacked();
        for (int i = 0; i < 8; i++) {
            outputStream.write((int) (packedOptions >>> 56) & 0xff);
            packedOptions <<= 8;
        }
        doEncode(encoder, callback, intervalLength);
    }

    public static void encodeRaw(final InputStream inputStream,
            final OutputStream outputStream, final Callback callback,
            final long intervalLength, final Options options)
            throws IOException {
        if (intervalLength <= 0) {
            throw new IllegalArgumentException(
                    "Interval length has to be positive.");
        }
        final Encoder encoder = new Encoder(inputStream, outputStream, options);
        doEncode(encoder, callback, intervalLength);
    }

    private static void doEncode(final Encoder encoder, final Callback callback,
            final long intervalLength) throws IOException {
        long amountProcessed = 0;
        while (!encoder.encode(intervalLength)) {
            amountProcessed += intervalLength;
            if (callback != null) {
                callback.progressChanged(amountProcessed);
            }
        }
        encoder.flush();
    }
}
