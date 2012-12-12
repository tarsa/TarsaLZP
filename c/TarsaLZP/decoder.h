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

#ifndef DECODER_H
#define	DECODER_H

#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>

#include "common.h"
#include "err.h"
#include "streams.h"

#ifdef	__cplusplus
extern "C" {
#endif


    bool started;
    int32_t nextHighBit;

    void __decoder__() {
        __common__();
        started = false;
        nextHighBit = 0;
#ifndef NO_PREFETCH
        computeHashesForNextIteration(0);
#endif        
    }

    int32_t decoderInputByte() {
        int32_t const inputByte = inputRead();
        if (inputByte == -1) {
            err("Unexpected end of file.");
            exit(EXIT_FAILURE);
        }
        int32_t const currentByte = (inputByte >> 1) + (nextHighBit << 7);
        nextHighBit = inputByte & 1;
        return currentByte;
    }

    void decoderInit() {
        rcBuffer = 0;
        for (int32_t i = 0; i < 4; i++) {
            rcBuffer = (rcBuffer << 8) + decoderInputByte();
        }
        rcRange = 0x7FFFFFFF;
        started = true;
    }

    void decoderNormalize() {
        while (rcRange < 0x00800000) {
            rcBuffer = (rcBuffer << 8) + decoderInputByte();
            rcRange <<= 8;
        }
    }

    bool decodeFlag(int32_t const probability) {
        decoderNormalize();
        int32_t const rcHelper = (rcRange >> 15) * probability;
        if (rcHelper > rcBuffer) {
            rcRange = rcHelper;
            return true;
        } else {
            rcRange -= rcHelper;
            rcBuffer -= rcHelper;
            return false;
        }
    }

    bool decodeSkewed() {
        decoderNormalize();
        if (rcBuffer < rcRange - 1) {
            rcRange--;
            return true;
        } else {
            rcBuffer = 0;
            rcRange = 1;
            return false;
        }
    }

    int32_t decodeSymbol(int32_t const mispredictedSymbol) {
        decoderNormalize();
        computeLiteralCoderContext();
        int16_t * const rangesGrouped = rangesGroupedAndSingle 
                + lastLiteralCoderContext * 272;
        int16_t * const rangesSingle = rangesGrouped + 16;
        int32_t nextSymbol;
        if (!useFixedProbabilities()) {
            int16_t const mispredictedSymbolFrequency = rangesSingle[
                    mispredictedSymbol];
            rcRange /= rangesTotal[lastLiteralCoderContext]
                    - mispredictedSymbolFrequency;
            rangesSingle[mispredictedSymbol] = 0;
            rangesGrouped[mispredictedSymbol >> 4] -= 
                    mispredictedSymbolFrequency;
            int32_t rcHelper = rcBuffer / rcRange;
            int32_t const cumulativeFrequency = rcHelper;
            int32_t nextSymbolGroup;
            for (nextSymbolGroup = 0; rcHelper 
                    >= rangesGrouped[nextSymbolGroup]; nextSymbolGroup++) {
                rcHelper -= rangesGrouped[nextSymbolGroup];
            }
            for (nextSymbol = nextSymbolGroup << 4; rcHelper 
                    >= rangesSingle[nextSymbol]; nextSymbol++) {
                rcHelper -= rangesSingle[nextSymbol];
            }
            rcBuffer -= (cumulativeFrequency - rcHelper) * rcRange;
            rcRange *= rangesSingle[nextSymbol];
            rangesSingle[mispredictedSymbol] = mispredictedSymbolFrequency;
            rangesGrouped[mispredictedSymbol >> 4] += 
                    mispredictedSymbolFrequency;
        } else {
            rcRange /= 255;
            int32_t const rcHelper = rcBuffer / rcRange;
            rcBuffer -= rcHelper * rcRange;
            nextSymbol = rcHelper + (rcHelper >= mispredictedSymbol ? 1 : 0);
        }
#ifndef NO_PREFETCH    
        if (onlyLowLzp) {
            computeHashesOnlyLowLzpForNextIteration(nextSymbol);
        } else {
            computeHashesForNextIteration(nextSymbol);
        }
#endif         
        updateRecentCost(rangesSingle[nextSymbol],
                rangesTotal[lastLiteralCoderContext]);
        updateLiteralCoder(nextSymbol);
        return nextSymbol;
    }

    int32_t decodeSingleOnlyLowLzp() {
#ifndef NO_PREFETCH
        shiftHashesOnlyLowLzp();
#else
        computeHashesOnlyLowLzp();
#endif        
        int32_t const lzpStateLow = getLzpStateLow();
        int32_t const predictedSymbolLow = getLzpPredictedSymbolLow();
        int32_t const modelLowFrequency = getApmLow(lzpStateLow);
        bool const matchLow = decodeFlag(modelLowFrequency);
#ifndef NO_PREFETCH            
        if (matchLow) {
            computeHashesOnlyLowLzpForNextIteration(predictedSymbolLow);
        }
#endif  
        int32_t const nextSymbol = matchLow ? predictedSymbolLow
                : decodeSymbol(predictedSymbolLow);
        updateApmLow(lzpStateLow, matchLow);
        updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
        updateContext(nextSymbol);
        return nextSymbol;
    }

    int32_t decodeSingle() {
#ifndef NO_PREFETCH
        shiftHashes();
#else
        computeHashes();
#endif        
        int32_t const lzpStateLow = getLzpStateLow();
        int32_t const predictedSymbolLow = getLzpPredictedSymbolLow();
        int32_t const modelLowFrequency = getApmLow(lzpStateLow);
        int32_t const lzpStateHigh = getLzpStateHigh();
        int32_t const predictedSymbolHigh = getLzpPredictedSymbolHigh();
        int32_t const modelHighFrequency = getApmHigh(lzpStateHigh);
        int32_t nextSymbol;
        if (modelLowFrequency >= modelHighFrequency) {
            bool const matchLow = decodeFlag(modelLowFrequency);
#ifndef NO_PREFETCH            
            if (matchLow) {
                computeHashesForNextIteration(predictedSymbolLow);
            }
#endif            
            nextSymbol = matchLow ? predictedSymbolLow
                    : decodeSymbol(predictedSymbolLow);
            updateApmLow(lzpStateLow, matchLow);
            updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
            bool const matchHigh = nextSymbol == predictedSymbolHigh;
            updateApmHistoryHigh(matchHigh);
            updateLzpStateHigh(lzpStateHigh, nextSymbol, matchHigh);
        } else {
            bool const matchHigh = decodeFlag(modelHighFrequency);
#ifndef NO_PREFETCH            
            if (matchHigh) {
                computeHashesForNextIteration(predictedSymbolHigh);
            }
#endif
            nextSymbol = matchHigh ? predictedSymbolHigh
                    : decodeSymbol(predictedSymbolHigh);
            updateApmHigh(lzpStateHigh, matchHigh);
            updateLzpStateHigh(lzpStateHigh, nextSymbol, matchHigh);
            bool const matchLow = nextSymbol == predictedSymbolLow;
            updateApmHistoryLow(matchLow);
            updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
        }
        updateContext(nextSymbol);
        return nextSymbol;
    }

    void decode() {
        if (!started) {
            decoderInit();
        }
        if (onlyLowLzp) {
            while (decodeSkewed()) {
                outputWrite(decodeSingleOnlyLowLzp());
            }
        } else {
            while (decodeSkewed()) {
                outputWrite(decodeSingle());
            }
        }
    }

#ifdef	__cplusplus
}
#endif

#endif	/* DECODER_H */
