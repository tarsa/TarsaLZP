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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 *
 * @author Piotr Tarsa
 */
abstract class Common {

    // streams section
    final InputStream inputStream;
    final OutputStream outputStream;
    // options section
    private final int lzpLowContextLength;
    private final int lzpLowMaskSize;
    private final int lzpHighContextLength;
    private final int lzpHighMaskSize;
    private final short literalCoderOrder;
    private final short literalCoderInit;
    private final short literalCoderStep;
    private final short literalCoderLimit;
    // Lempel-Ziv Predictive section
    final boolean onlyLowLzp;
    private final int lzpLowMask;
    private final int lzpHighMask;
    private final short[] lzpLow;
    private final short[] lzpHigh;
    // Literal coder section
    private final int CostScale = 7;
    private final int literalCoderContextMaskSize;
    private final int literalCoderContextMask;
    final short[] rangesSingle;
    final short[] rangesGrouped;
    final short[] rangesTotal;
    private int recentCost;
    // Contexts and hashes section
    private int lastLiteralCoderContext;
    private long context;
    private int hashLow;
    private int hashHigh;
    private final int[] precomputedHashes = new int[256];
    // Adaptive probability map section
    private final short[] apmLow;
    private final short[] apmHigh;

    public Common(final InputStream inputStream,
            final OutputStream outputStream, final Options options) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        lzpLowContextLength = (int) options.getLzpLowContextLength();
        lzpLowMaskSize = (int) options.getLzpLowMaskSize();
        lzpHighContextLength = (int) options.getLzpHighContextLength();
        lzpHighMaskSize = (int) options.getLzpHighMaskSize();
        literalCoderOrder = (short) options.getLiteralCoderOrder();
        literalCoderInit = (short) options.getLiteralCoderInit();
        literalCoderStep = (short) options.getLiteralCoderStep();
        literalCoderLimit = (short) options.getLiteralCoderLimit();
        // Lempel-Ziv Predictive init
        final int lzpLowCount = 1 << lzpLowMaskSize;
        final int lzpHighCount = 1 << lzpHighMaskSize;
        lzpLowMask = lzpLowCount - 1;
        lzpHighMask = lzpHighCount - 1;
        lzpLow = new short[lzpLowCount];
        Arrays.fill(lzpLow, (short) 0xffb5);
        onlyLowLzp = lzpLowContextLength == lzpHighContextLength
                && lzpLowMaskSize == lzpHighMaskSize;
        if (onlyLowLzp) {
            lzpHigh = null;
        } else {
            lzpHigh = new short[lzpHighCount];
            Arrays.fill(lzpHigh, (short) 0xffb5);
        }
        // Literal coder init
        literalCoderContextMaskSize = 8 * literalCoderOrder;
        literalCoderContextMask = (1 << literalCoderContextMaskSize) - 1;
        rangesSingle = new short[1 << literalCoderContextMaskSize + 8];
        rangesGrouped = new short[1 << literalCoderContextMaskSize + 4];
        rangesTotal = new short[1 << literalCoderContextMaskSize];
        Arrays.fill(rangesSingle, (short) (literalCoderInit));
        Arrays.fill(rangesGrouped, (short) (literalCoderInit * 16));
        Arrays.fill(rangesTotal, (short) (literalCoderInit * 256));
        recentCost = 8 << CostScale + 14;
        // Adaptive probability map init
        apmLow = new short[16 * 256];
        Arrays.fill(apmLow, (short) 0x4000);
        if (onlyLowLzp) {
            apmHigh = null;
        } else {
            apmHigh = new short[16 * 256];
            Arrays.fill(apmHigh, (short) 0x4000);
        }
        // Contexts and hashes init
        lastLiteralCoderContext = 0;
        context = 0;
        hashLow = 0;
        hashHigh = 0;
        for (int i = 0; i < 256; i++) {
            int hash = -2128831035;
            hash *= 16777619;
            hash ^= i;
            hash *= 16777619;
            precomputedHashes[i] = hash;
        }
    }
    // <editor-fold defaultstate="collapsed" desc="Contexts and hashes">
    void updateContext(final int input) {
        context <<= 8;
        context |= input;
    }

    void computeLiteralCoderContext() {
        lastLiteralCoderContext = (int) (context & literalCoderContextMask);
    }

    void computeHashesOnlyLowLzp() {        
        long localContext = context >>> 8;
        int hash = precomputedHashes[(int)(context & 0xFF)];
        int i = 1;
        while (true) {
            hash ^= (int) (localContext & 0xFF);
            localContext >>= 8;
            if (++i == lzpLowContextLength) {
                break;
            }
            hash *= 16777619;
        }
        hashLow = hash & lzpLowMask;
    }

    void computeHashes() {
        long localContext = context >>> 8;
        int hash = precomputedHashes[(int)(context & 0xFF)];
        int i = 1;
        while (true) {
            hash ^= (int) (localContext & 0xFF);
            localContext >>>= 8;
            if (++i == lzpLowContextLength) {
                break;
            }
            hash *= 16777619;
        }
        hashLow = hash & lzpLowMask;
        while (i++ < lzpHighContextLength) {
            hash *= 16777619;
            hash ^= (int) (localContext & 0xFF);
            localContext >>>= 8;
        }
        hashHigh = hash & lzpHighMask;
    }

    int getLastLiteralCoderContext() {
        return lastLiteralCoderContext;
    }// </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Calculating states">
    private static final byte[] stateTable = new FsmGenerator().getStateTable();

    int getNextState(final int state, final boolean match) {
        return stateTable[state * 2 +(match ? 1 : 0)] & 0xFF;
    }// </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Lempel-Ziv Predictive stuff">

    int getLzpStateLow() {
        return (lzpLow[hashLow] >> 8) & 0xff;
    }

    int getLzpStateHigh() {
        return (lzpHigh[hashHigh] >> 8) & 0xff;
    }

    int getLzpPredictedSymbolLow() {
        return lzpLow[hashLow] & 0xff;
    }

    int getLzpPredictedSymbolHigh() {
        return lzpHigh[hashHigh] & 0xff;
    }

    void updateLzpStateLow(final int lzpStateLow, final int input,
            final boolean match) {
        lzpLow[hashLow] = (short) ((getNextState(lzpStateLow, match) << 8)
                + input);
    }

    void updateLzpStateHigh(final int lzpStateHigh, final int input,
            final boolean match) {
        lzpHigh[hashHigh] = (short) ((getNextState(lzpStateHigh, match) << 8)
                + input);
    }// </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Adaptive prob. map stuff">
    private int historyLow = 0;
    private int historyHigh = 0;
    private final int historyLowMask = 15;
    private final int historyHighMask = 15;

    int getApmLow(final int state) {
        return apmLow[(historyLow << 8) + state];
    }

    int getApmHigh(final int state) {
        return apmHigh[(historyHigh << 8) + state];
    }

    void updateApmHistoryLow(final boolean match) {
        historyLow = ((historyLow << 1) + (match ? 0 : 1)) & historyLowMask;
    }

    void updateApmHistoryHigh(final boolean match) {
        historyHigh = ((historyHigh << 1) + (match ? 0 : 1)) & historyHighMask;
    }

    void updateApmLow(final int state, final boolean match) {
        final int index = (historyLow << 8) + state;
        if (match) {
            apmLow[index] += ((1 << 15) - apmLow[index]) >> 7;
        } else {
            apmLow[index] -= apmLow[index] >> 7;
        }
        updateApmHistoryLow(match);
    }

    void updateApmHigh(final int state, final boolean match) {
        final int index = (historyHigh << 8) + state;
        if (match) {
            apmHigh[index] += ((1 << 15) - apmHigh[index]) >> 7;
        } else {
            apmHigh[index] -= apmHigh[index] >> 7;
        }
        updateApmHistoryHigh(match);
    }// </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Literal coder stuff">

    private void rescaleLiteralCoder() {
        for (int indexCurrent = getLastLiteralCoderContext() << 8; indexCurrent
                < (getLastLiteralCoderContext() + 1) << 8; indexCurrent++) {
            rangesSingle[indexCurrent] -= rangesSingle[indexCurrent] >> 1;
        }
        short totalFrequency = 0;
        for (int groupCurrent = getLastLiteralCoderContext() << 4; groupCurrent
                < (getLastLiteralCoderContext() + 1) << 4; groupCurrent++) {
            short groupFrequency = 0;
            for (int indexCurrent = groupCurrent << 4; indexCurrent
                    < (groupCurrent + 1) << 4; indexCurrent++) {
                groupFrequency += rangesSingle[indexCurrent];
            }
            rangesGrouped[groupCurrent] = groupFrequency;
            totalFrequency += groupFrequency;
        }
        rangesTotal[getLastLiteralCoderContext()] = totalFrequency;
    }

    void updateLiteralCoder(final int index) {
        rangesSingle[index] += literalCoderStep;
        rangesGrouped[index >> 4] += literalCoderStep;
        rangesTotal[getLastLiteralCoderContext()] += literalCoderStep;
        if (rangesTotal[getLastLiteralCoderContext()] > literalCoderLimit) {
            rescaleLiteralCoder();
        }
    }

    boolean useFixedProbabilities() {
        return recentCost > 8 << CostScale + 14;
    }

    void updateRecentCost(final int symbolFrequency, final int totalFrequency) {
        recentCost -= recentCost >> CostScale;
        recentCost += Lg2.nLog2(totalFrequency);
        recentCost -= Lg2.nLog2(symbolFrequency);
    }
    // </editor-fold>  
}
