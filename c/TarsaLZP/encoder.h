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

#include "common.h"
#include "streams.h"

#ifdef	__cplusplus
extern "C" {
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
        computeHashesForNextIteration(0);
    }

    void encoderOutputByte(int32_t const octet) {
        if (octet != 0xff) {
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
        computePpmContext();
        int32_t const index = (lastPpmContext << 8) + nextSymbol;
        int16_t cumulativeExclusiveFrequency = 0;
        int32_t const symbolGroup = index >> 4;
        for (int32_t indexPartial = lastPpmContext << 4;
                indexPartial < symbolGroup; indexPartial++) {
            cumulativeExclusiveFrequency += rangesGrouped[indexPartial];
        }
        for (int32_t indexPartial = symbolGroup << 4; indexPartial < index;
                indexPartial++) {
            cumulativeExclusiveFrequency += rangesSingle[indexPartial];
        }
        int16_t const mispredictedSymbolFrequency =
                rangesSingle[(lastPpmContext << 8) + mispredictedSymbol];
        if (nextSymbol > mispredictedSymbol) {
            cumulativeExclusiveFrequency -= mispredictedSymbolFrequency;
        }
        int32_t const rcHelper = rcRange / (rangesTotal[lastPpmContext]
                - mispredictedSymbolFrequency);
        encoderAddWithCarry(rcHelper * cumulativeExclusiveFrequency);
        rcRange = rcHelper * rangesSingle[index];
        updatePpm(index);
    }

    void encodeSingleOnlyLowLzp(int32_t const nextSymbol) {
        shiftHashesOnlyLowLzp();
        computeHashesOnlyLowLzpForNextIteration(nextSymbol);
        int32_t const lzpStateLow = getLzpStateLow();
        int32_t const predictedSymbolLow = getLzpPredictedSymbolLow();
        int32_t const modelLowFrequency = getSeeLow(lzpStateLow);
        bool const matchLow = nextSymbol == predictedSymbolLow;
        encodeFlag(modelLowFrequency, matchLow);
        updateSeeLow(lzpStateLow, matchLow);
        updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
        if (!matchLow) {
            encodeSymbol(nextSymbol, predictedSymbolLow);
        }
        updateContext(nextSymbol);
    }

    void encodeSingle(int32_t const nextSymbol) {
        shiftHashes();
        computeHashesForNextIteration(nextSymbol);
        int32_t const lzpStateLow = getLzpStateLow();
        int32_t const predictedSymbolLow = getLzpPredictedSymbolLow();
        int32_t const modelLowFrequency = getSeeLow(lzpStateLow);
        int32_t const lzpStateHigh = getLzpStateHigh();
        int32_t const predictedSymbolHigh = getLzpPredictedSymbolHigh();
        int32_t const modelHighFrequency = getSeeHigh(lzpStateHigh);
        if (modelLowFrequency >= modelHighFrequency) {
            bool const matchHigh = nextSymbol == predictedSymbolHigh;
            updateSeeHistoryHigh(matchHigh);
            updateLzpStateHigh(lzpStateHigh, nextSymbol, matchHigh);
            bool const matchLow = nextSymbol == predictedSymbolLow;
            encodeFlag(modelLowFrequency, matchLow);
            updateSeeLow(lzpStateLow, matchLow);
            updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
            if (!matchLow) {
                encodeSymbol(nextSymbol, predictedSymbolLow);
            }
        } else {
            bool const matchLow = nextSymbol == predictedSymbolLow;
            updateSeeHistoryLow(matchLow);
            updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
            bool const matchHigh = nextSymbol == predictedSymbolHigh;
            encodeFlag(modelHighFrequency, matchHigh);
            updateSeeHigh(lzpStateHigh, matchHigh);
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

