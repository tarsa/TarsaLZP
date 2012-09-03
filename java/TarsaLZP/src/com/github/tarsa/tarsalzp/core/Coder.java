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
        final Options result = Options.fromPacked(header);
        if (result == null) {
            throw new IllegalArgumentException("Invalid compression options.");
        } else {
            return result;
        }
    }

    public static void decode(final InputStream inputStream,
            final OutputStream outputStream, final Callback callback,
            final long callbackPeriod) throws IOException {
        final Options options = getOptions(inputStream);
        decodeRaw(inputStream, outputStream, callback, callbackPeriod, options);
    }

    public static void decodeRaw(final InputStream inputStream,
            final OutputStream outputStream, final Callback callback,
            final long callbackPeriod, final Options options)
            throws IOException {
        final Decoder decoder = new Decoder(inputStream, outputStream, options);
        long amountProcessed = 0;
        while (!decoder.decode(callbackPeriod)) {
            amountProcessed += callbackPeriod;
            if (callback != null) {
                callback.progressChanged(amountProcessed);
            }
        }
    }

    public static void encode(final InputStream inputStream,
            final OutputStream outputStream, final Callback callback,
            final long callbackPeriod, final Options options)
            throws IOException {
        final Encoder encoder = new Encoder(inputStream, outputStream, options);
        long header = options.toPacked();
        for (int i = 0; i < 8; i++) {
            outputStream.write((int) (header >>> 56) & 0xff);
            header <<= 8;
        }
        doEncode(encoder, callback, callbackPeriod);
        encoder.flush();
    }

    public static void encodeRaw(final InputStream inputStream,
            final OutputStream outputStream, final Callback callback,
            final long callbackPeriod, final Options options)
            throws IOException {
        final Encoder encoder = new Encoder(inputStream, outputStream, options);
        doEncode(encoder, callback, callbackPeriod);
        encoder.flush();
    }
    
    private static void doEncode(final Encoder encoder, final Callback callback,
            final long callbackPeriod) throws IOException {
        long amountProcessed = 0;
        while (!encoder.encode(callbackPeriod)) {
            amountProcessed += callbackPeriod;
            if (callback != null) {
                callback.progressChanged(amountProcessed);
            }
        }
    }
}
