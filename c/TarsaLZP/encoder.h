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

#ifndef ENCODER_H
#define	ENCODER_H

#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#ifndef NO_VECTOR
#include <xmmintrin.h>
#endif

#include "common.h"
#include "streams.h"

#ifdef	__cplusplus
extern "C" {
#endif

#ifndef NO_VECTOR

    union {
        uint16_t w[8];
        __m128i v8;
    } __attribute__((aligned(128))) masksA[16], masksB[16];
#endif

    int32_t xFFRunLength;
    int32_t lastOutputByte;
    bool delay;
    bool carry;

    void __encoder__() {
        __common__();
        rcBuffer = 0;
        rcRange = 0x7FFFFFFF;
        xFFRunLength = 0;
        lastOutputByte = 0;
        delay = false;
        carry = false;
#ifndef NO_PREFETCH
        computeHashesForNextIteration(0);
#endif
#ifndef NO_VECTOR
        for (int32_t i = 0; i < 16; i++) {
            for (int32_t j = 0; j < 8; j++) {
                masksA[i].w[j] = j < i ? 0xffff : 0x0000;
            }
            for (int32_t j = 0; j < 8; j++) {
                masksB[i].w[j] = j < i - 8 ? 0xffff : 0x0000;
            }
        }
#endif
    }

    void encoderOutputByte(int32_t const octet) {
        if (octet != 0xff || carry) {
            if (delay) {
                outputWrite(lastOutputByte + (carry ? 1 : 0));
            }
            while (xFFRunLength > 0) {
                xFFRunLength--;
                outputWrite(carry ? 0x00 : 0xff);
            }
            lastOutputByte = octet;
            delay = true;
            carry = false;
        } else {
            xFFRunLength++;
        }
    }

    void encoderNormalize() {
        while (rcRange < 0x00800000) {
            encoderOutputByte((int32_t) (rcBuffer >> 23));
            rcBuffer = (rcBuffer << 8) & 0x7FFFFFFF;
            rcRange <<= 8;
        }
    }

    void encoderAddWithCarry(int32_t const cumulativeExclusiveFraction) {
        rcBuffer += cumulativeExclusiveFraction;
        if (rcBuffer < 0) {
            carry = true;
            rcBuffer &= 0x7FFFFFFF;
        }
    }

    void encodeFlag(int32_t const probability, bool const match) {
        encoderNormalize();
        int32_t const rcHelper = (rcRange >> 15) * probability;
        if (match) {
            rcRange = rcHelper;
        } else {
            encoderAddWithCarry(rcHelper);
            rcRange -= rcHelper;
        }
    }

    void encodeSkewed(bool const flag) {
        encoderNormalize();
        if (flag) {
            rcRange--;
        } else {
            encoderAddWithCarry(rcRange - 1);
            rcRange = 1;
        }
    }

    void encodeSymbol(int32_t const nextSymbol,
            int32_t const mispredictedSymbol) {
        encoderNormalize();
        computeLiteralCoderContext();
        int16_t * const rangesGrouped = rangesGroupedAndSingle 
                + lastLiteralCoderContext * 272;
        int16_t * const rangesSingle = rangesGrouped + 16;
        if (!useFixedProbabilities()) {
            int16_t cumulativeExclusiveFrequency = 0;
            int32_t const symbolGroup = nextSymbol >> 4;
#ifndef NO_VECTOR

            union {
                __v8hi v8;
                __v16qi v16;
                __m128i m;
                uint32_t v[4];
            } a, b, c;
            a.m = __builtin_ia32_pxor128(a.m, a.m);
            b.m = masksA[symbolGroup].v8;
            c.v8 = *(__v8hi*) (rangesGrouped + 0);
            b.m = __builtin_ia32_pand128(b.m, c.m);
            a.v8 = __builtin_ia32_paddw128(a.v8, b.v8);
            b.m = masksB[symbolGroup].v8;
            c.v8 = *(__v8hi*) (rangesGrouped + 8);
            b.m = __builtin_ia32_pand128(b.m, c.m);
            a.v8 = __builtin_ia32_paddw128(a.v8, b.v8);
            b.m = masksA[nextSymbol & 15].v8;
            c.v8 = *(__v8hi*) (rangesSingle + (symbolGroup << 4));
            b.m = __builtin_ia32_pand128(b.m, c.m);
            a.v8 = __builtin_ia32_paddw128(a.v8, b.v8);
            b.m = masksB[nextSymbol & 15].v8;
            c.v8 = *(__v8hi*) (rangesSingle + (symbolGroup << 4) + 8);
            b.m = __builtin_ia32_pand128(b.m, c.m);
            a.v8 = __builtin_ia32_paddw128(a.v8, b.v8);
            uint32_t const packedSum = a.v[0] + a.v[1] + a.v[2] + a.v[3];
            cumulativeExclusiveFrequency += (packedSum & 0xffff)
                    + (packedSum >> 16);
#else
            for (int32_t group = 0; group < symbolGroup; group++) {
                cumulativeExclusiveFrequency += rangesGrouped[group];
            }
            for (int32_t single = symbolGroup << 4; single < nextSymbol; 
                    single++) {
                cumulativeExclusiveFrequency += rangesSingle[single];
            }
#endif
            int16_t const mispredictedSymbolFrequency = rangesSingle[
                    mispredictedSymbol];
            if (nextSymbol > mispredictedSymbol) {
                cumulativeExclusiveFrequency -= mispredictedSymbolFrequency;
            }
            int32_t const rcHelper = rcRange / (rangesTotal[
                    lastLiteralCoderContext] - mispredictedSymbolFrequency);
            encoderAddWithCarry(rcHelper * cumulativeExclusiveFrequency);
            rcRange = rcHelper * rangesSingle[nextSymbol];
        } else {
            rcRange /= 255;
            encoderAddWithCarry(rcRange *
                    (nextSymbol - (nextSymbol > mispredictedSymbol ? 1 : 0)));
        }
        updateRecentCost(rangesSingle[nextSymbol],
                rangesTotal[lastLiteralCoderContext]);
        updateLiteralCoder(nextSymbol);
    }

    void encodeSingleOnlyLowLzp(int32_t const nextSymbol) {
#ifndef NO_PREFETCH
        shiftHashesOnlyLowLzp();
        computeHashesOnlyLowLzpForNextIteration(nextSymbol);
#else
        computeHashesOnlyLowLzp();
#endif
        int32_t const lzpStateLow = getLzpStateLow();
        int32_t const predictedSymbolLow = getLzpPredictedSymbolLow();
        int32_t const modelLowFrequency = getApmLow(lzpStateLow);
        bool const matchLow = nextSymbol == predictedSymbolLow;
        encodeFlag(modelLowFrequency, matchLow);
        updateApmLow(lzpStateLow, matchLow);
        updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
        if (!matchLow) {
            encodeSymbol(nextSymbol, predictedSymbolLow);
        }
        updateContext(nextSymbol);
    }

    void encodeSingle(int32_t const nextSymbol) {
#ifndef NO_PREFETCH
        shiftHashes();
        computeHashesForNextIteration(nextSymbol);
#else
        computeHashes();
#endif
        int32_t const lzpStateLow = getLzpStateLow();
        int32_t const predictedSymbolLow = getLzpPredictedSymbolLow();
        int32_t const modelLowFrequency = getApmLow(lzpStateLow);
        int32_t const lzpStateHigh = getLzpStateHigh();
        int32_t const predictedSymbolHigh = getLzpPredictedSymbolHigh();
        int32_t const modelHighFrequency = getApmHigh(lzpStateHigh);
        if (modelLowFrequency >= modelHighFrequency) {
            bool const matchHigh = nextSymbol == predictedSymbolHigh;
            updateApmHistoryHigh(matchHigh);
            updateLzpStateHigh(lzpStateHigh, nextSymbol, matchHigh);
            bool const matchLow = nextSymbol == predictedSymbolLow;
            encodeFlag(modelLowFrequency, matchLow);
            updateApmLow(lzpStateLow, matchLow);
            updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
            if (!matchLow) {
                encodeSymbol(nextSymbol, predictedSymbolLow);
            }
        } else {
            bool const matchLow = nextSymbol == predictedSymbolLow;
            updateApmHistoryLow(matchLow);
            updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
            bool const matchHigh = nextSymbol == predictedSymbolHigh;
            encodeFlag(modelHighFrequency, matchHigh);
            updateApmHigh(lzpStateHigh, matchHigh);
            updateLzpStateHigh(lzpStateHigh, nextSymbol, matchHigh);
            if (!matchHigh) {
                encodeSymbol(nextSymbol, predictedSymbolHigh);
            }
        }
        updateContext(nextSymbol);
    }

    void encoderFlush() {
        for (int32_t i = 0; i < 5; i++) {
            encoderOutputByte(((int32_t) (rcBuffer >> 23)) & 0xFF);
            rcBuffer <<= 8;
        }
    }

    void encode() {
        if (onlyLowLzp) {
            while (true) {
                int32_t const symbol = inputRead();
                encodeSkewed(symbol != -1);
                if (symbol != -1) {
                    encodeSingleOnlyLowLzp(symbol);
                } else {
                    break;
                }
            }
        } else {
            while (true) {
                int32_t const symbol = inputRead();
                encodeSkewed(symbol != -1);
                if (symbol != -1) {
                    encodeSingle(symbol);
                } else {
                    break;
                }
            }
        }
    }

#ifdef	__cplusplus
}
#endif

#endif	/* ENCODER_H */
