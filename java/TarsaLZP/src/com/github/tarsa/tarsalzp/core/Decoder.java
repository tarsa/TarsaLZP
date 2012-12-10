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
package com.github.tarsa.tarsalzp.core;

import com.github.tarsa.tarsalzp.Options;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author Piotr Tarsa
 */
public final class Decoder extends Common {

    private int rcBuffer;
    private int rcRange;
    private boolean started;
    private int nextHighBit = 0;

    public Decoder(final InputStream inputStream,
            final OutputStream outputStream, final Options options) {
        super(inputStream, outputStream, options);
        started = false;
    }

    private int inputByte() throws IOException {
        final int inputByte = inputStream.read();
        if (inputByte == -1) {
            throw new IOException("Unexpected end of file.");
        }
        final int currentByte = (inputByte >> 1) + (nextHighBit << 7);
        nextHighBit = inputByte & 1;
        return currentByte;
    }

    private void init() throws IOException {
        rcBuffer = 0;
        for (int i = 0; i < 4; i++) {
            rcBuffer = (rcBuffer << 8) + inputByte();
        }
        rcRange = 0x7FFFFFFF;
        started = true;
    }

    private void normalize() throws IOException {
        while (rcRange < 0x00800000) {
            rcBuffer = (rcBuffer << 8) + inputByte();
            rcRange <<= 8;
        }
    }

    private boolean decodeFlag(final int probability) throws IOException {
        normalize();
        final int rcHelper = (rcRange >> 15) * probability;
        if (rcHelper > rcBuffer) {
            rcRange = rcHelper;
            return true;
        } else {
            rcRange -= rcHelper;
            rcBuffer -= rcHelper;
            return false;
        }
    }

    private boolean decodeSkewed() throws IOException {
        normalize();
        if (rcBuffer < rcRange - 1) {
            rcRange--;
            return true;
        } else {
            rcBuffer = 0;
            rcRange = 1;
            return false;
        }
    }

    private int decodeSingleOnlyLowLzp() throws IOException {
        computeHashesOnlyLowLzp();
        final int lzpStateLow = getLzpStateLow();
        final int predictedSymbolLow = getLzpPredictedSymbolLow();
        final int modelLowFrequency = getApmLow(lzpStateLow);
        final boolean matchLow = decodeFlag(modelLowFrequency);
        updateApmLow(lzpStateLow, matchLow);
        final int nextSymbol = matchLow ? predictedSymbolLow
                : decodeSymbol(predictedSymbolLow);
        updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
        updateContext(nextSymbol);
        return nextSymbol;
    }

    private int decodeSingle() throws IOException {
        computeHashes();
        final int lzpStateLow = getLzpStateLow();
        final int predictedSymbolLow = getLzpPredictedSymbolLow();
        final int modelLowFrequency = getApmLow(lzpStateLow);
        final int lzpStateHigh = getLzpStateHigh();
        final int predictedSymbolHigh = getLzpPredictedSymbolHigh();
        final int modelHighFrequency = getApmHigh(lzpStateHigh);
        int nextSymbol;
        if (modelLowFrequency >= modelHighFrequency) {
            final boolean matchLow = decodeFlag(modelLowFrequency);
            updateApmLow(lzpStateLow, matchLow);
            nextSymbol = matchLow ? predictedSymbolLow
                    : decodeSymbol(predictedSymbolLow);
            updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
            final boolean matchHigh = nextSymbol == predictedSymbolHigh;
            updateApmHistoryHigh(matchHigh);
            updateLzpStateHigh(lzpStateHigh, nextSymbol, matchHigh);
        } else {
            final boolean matchHigh = decodeFlag(modelHighFrequency);
            updateApmHigh(lzpStateHigh, matchHigh);
            nextSymbol = matchHigh ? predictedSymbolHigh
                    : decodeSymbol(predictedSymbolHigh);
            updateLzpStateHigh(lzpStateHigh, nextSymbol, matchHigh);
            final boolean matchLow = nextSymbol == predictedSymbolLow;
            updateApmHistoryLow(matchLow);
            updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
        }
        updateContext(nextSymbol);
        return nextSymbol;
    }

    private int decodeSymbol(final int mispredictedSymbol) throws IOException {
        normalize();
        computeLiteralCoderContext();
        int index;
        int nextSymbol;
        if (!useFixedProbabilities()) {
            final short mispredictedSymbolFrequency = rangesSingle[
                    (getLastLiteralCoderContext() << 8) + mispredictedSymbol];
            rcRange /= rangesTotal[getLastLiteralCoderContext()]
                    - mispredictedSymbolFrequency;
            rangesSingle[(getLastLiteralCoderContext() << 8)
                    + mispredictedSymbol] = 0;
            rangesGrouped[((getLastLiteralCoderContext() << 8)
                    + mispredictedSymbol) >> 4] -= mispredictedSymbolFrequency;
            int rcHelper = rcBuffer / rcRange;
            final int cumulativeFrequency = rcHelper;
            for (index = getLastLiteralCoderContext() << 4; rcHelper
                    >= rangesGrouped[index]; index++) {
                rcHelper -= rangesGrouped[index];
            }
            for (index <<= 4; rcHelper >= rangesSingle[index]; index++) {
                rcHelper -= rangesSingle[index];
            }
            rcBuffer -= (cumulativeFrequency - rcHelper) * rcRange;
            rcRange *= rangesSingle[index];
            nextSymbol = index & 0xff;
            rangesSingle[(getLastLiteralCoderContext() << 8)
                    + mispredictedSymbol] = mispredictedSymbolFrequency;
            rangesGrouped[((getLastLiteralCoderContext() << 8)
                    + mispredictedSymbol) >> 4] += mispredictedSymbolFrequency;
        } else {
            rcRange /= 255;
            final int rcHelper = rcBuffer / rcRange;
            rcBuffer -= rcHelper * rcRange;
            nextSymbol = rcHelper + (rcHelper >= mispredictedSymbol ? 1 : 0);
            index = (getLastLiteralCoderContext() << 8) + nextSymbol;
        }
        updateRecentCost(rangesSingle[index],
                rangesTotal[getLastLiteralCoderContext()]);
        updateLiteralCoder(index);
        return nextSymbol;
    }

    long decode(final long limit) throws IOException {
        if (!started) {
            init();
        }
        for (long processed = 0; processed < limit; processed++) {
            if (decodeSkewed()) {
                final int symbol = onlyLowLzp ? decodeSingleOnlyLowLzp()
                        : decodeSingle();
                outputStream.write(symbol);
            } else {
                return processed;
            }
        }
        return limit;
    }
}
