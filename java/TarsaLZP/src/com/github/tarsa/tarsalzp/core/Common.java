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

    // stream section
    final InputStream inputStream;
    final OutputStream outputStream;
    // options section
    private final int lzpLowContextLength;
    private final int lzpLowMaskSize;
    private final int lzpHighContextLength;
    private final int lzpHighMaskSize;
    private final short ppmOrder;
    private final short ppmInit;
    private final short ppmStep;
    private final short ppmLimit;
    // LZP section
    final boolean onlyLowLzp;
    private final int lzpLowMask;
    private final int lzpHighMask;
    private final short[] lzpLow;
    private final short[] lzpHigh;
    // PPM section
    private final int CostScale = 7;
    private final int ppmMaskSize;
    private final int ppmMask;
    final short[] rangesSingle;
    final short[] rangesGrouped;
    final short[] rangesTotal;
    private int recentCost;
    // Contexts and hashes section
    private int lastPpmContext;
    private long context;
    private int hashLow;
    private int hashHigh;
    private final int[] precomputedHashes = new int[256];
    // SEE section
    private final short[] seeLow;
    private final short[] seeHigh;

    public Common(final InputStream inputStream,
            final OutputStream outputStream, final Options options) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        lzpLowContextLength = (int) options.getLzpLowContextLength();
        lzpLowMaskSize = (int) options.getLzpLowMaskSize();
        lzpHighContextLength = (int) options.getLzpHighContextLength();
        lzpHighMaskSize = (int) options.getLzpHighMaskSize();
        ppmOrder = (short) options.getPpmOrder();
        ppmInit = (short) options.getPpmInit();
        ppmStep = (short) options.getPpmStep();
        ppmLimit = (short) options.getPpmLimit();
        // LZP init
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
        // PPM init
        ppmMaskSize = 8 * ppmOrder;
        ppmMask = (1 << ppmMaskSize) - 1;
        rangesSingle = new short[1 << ppmMaskSize + 8];
        rangesGrouped = new short[1 << ppmMaskSize + 4];
        rangesTotal = new short[1 << ppmMaskSize];
        Arrays.fill(rangesSingle, (short) (ppmInit));
        Arrays.fill(rangesGrouped, (short) (ppmInit * 16));
        Arrays.fill(rangesTotal, (short) (ppmInit * 256));
        recentCost = 8 << CostScale + 14;
        // SEE init
        seeLow = new short[16 * 256];
        Arrays.fill(seeLow, (short) 0x4000);
        if (onlyLowLzp) {
            seeHigh = null;
        } else {
            seeHigh = new short[16 * 256];
            Arrays.fill(seeHigh, (short) 0x4000);
        }
        // Contexts and hashes init
        lastPpmContext = 0;
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

    void computePpmContext() {
        lastPpmContext = (int) (context & ppmMask);
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

    int getLastPpmContext() {
        return lastPpmContext;
    }// </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Calculating states">
    private static final byte[] stateTable = new FsmGenerator().getStateTable();

    int getNextState(final int state, final boolean match) {
        return stateTable[state * 2 +(match ? 1 : 0)] & 0xFF;
    }// </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="LZP stuff">

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
    // <editor-fold defaultstate="collapsed" desc="SEE stuff">
    private int historyLow = 0;
    private int historyHigh = 0;
    private final int historyLowMask = 15;
    private final int historyHighMask = 15;

    int getSeeLow(final int state) {
        return seeLow[(historyLow << 8) + state];
    }

    int getSeeHigh(final int state) {
        return seeHigh[(historyHigh << 8) + state];
    }

    void updateSeeHistoryLow(final boolean match) {
        historyLow = ((historyLow << 1) + (match ? 0 : 1)) & historyLowMask;
    }

    void updateSeeHistoryHigh(final boolean match) {
        historyHigh = ((historyHigh << 1) + (match ? 0 : 1)) & historyHighMask;
    }

    void updateSeeLow(final int state, final boolean match) {
        final int index = (historyLow << 8) + state;
        if (match) {
            seeLow[index] += ((1 << 15) - seeLow[index]) >> 7;
        } else {
            seeLow[index] -= seeLow[index] >> 7;
        }
        updateSeeHistoryLow(match);
    }

    void updateSeeHigh(final int state, final boolean match) {
        final int index = (historyHigh << 8) + state;
        if (match) {
            seeHigh[index] += ((1 << 15) - seeHigh[index]) >> 7;
        } else {
            seeHigh[index] -= seeHigh[index] >> 7;
        }
        updateSeeHistoryHigh(match);
    }// </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="PPM stuff">

    private void rescalePpm() {
        for (int indexCurrent = getLastPpmContext() << 8; indexCurrent
                < (getLastPpmContext() + 1) << 8; indexCurrent++) {
            rangesSingle[indexCurrent] -= rangesSingle[indexCurrent] >> 1;
        }
        short totalFrequency = 0;
        for (int groupCurrent = getLastPpmContext() << 4; groupCurrent
                < (getLastPpmContext() + 1) << 4; groupCurrent++) {
            short groupFrequency = 0;
            for (int indexCurrent = groupCurrent << 4; indexCurrent
                    < (groupCurrent + 1) << 4; indexCurrent++) {
                groupFrequency += rangesSingle[indexCurrent];
            }
            rangesGrouped[groupCurrent] = groupFrequency;
            totalFrequency += groupFrequency;
        }
        rangesTotal[getLastPpmContext()] = totalFrequency;
    }

    void updatePpm(final int index) {
        rangesSingle[index] += ppmStep;
        rangesGrouped[index >> 4] += ppmStep;
        rangesTotal[getLastPpmContext()] += ppmStep;
        if (rangesTotal[getLastPpmContext()] > ppmLimit) {
            rescalePpm();
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
