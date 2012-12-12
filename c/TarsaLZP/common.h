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

#ifndef COMMON_H
#define	COMMON_H

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>

#include "err.h"
#include "fsm.h"
#include "lg2.h"
#include "options.h"

#ifdef	__cplusplus
extern "C" {
#endif

#define CostScale 7

    // Lempel-Ziv Predictive section
    bool onlyLowLzp;
    int32_t lzpLowMask;
    int32_t lzpHighMask;
    uint16_t * lzpLow;
    uint16_t * lzpHigh;
    // Literal coder section
    int32_t literalCoderContextMaskSize;
    int32_t literalCoderContextMask;
    int16_t * rangesGroupedAndSingle;
    int16_t * rangesTotal;
    int32_t recentCost;
    // Adaptive probability map section
    int32_t historyLow;
    int32_t historyHigh;
    int32_t const historyLowMask = 15;
    int32_t const historyHighMask = 15;
    int16_t apmLow[16 * 256];
    int16_t apmHigh[16 * 256];
    // Contexts and hashes
    int32_t lastLiteralCoderContext;
    uint64_t context;
    int32_t hashLow;
    int32_t hashHigh;
    int32_t hashLowNext;
    int32_t hashHighNext;
    int32_t precomputedHashes[256];

    // Coder section
    int32_t rcBuffer;
    int32_t rcRange;

    void __common__() {
        // State tables init
        __fsm__();
        // Lempel-Ziv Predictive init
        int32_t const lzpLowCount = 1 << lzpLowMaskSize;
        int32_t const lzpHighCount = 1 << lzpHighMaskSize;
        lzpLowMask = lzpLowCount - 1;
        lzpHighMask = lzpHighCount - 1;
        lzpLow = malloc(sizeof (uint16_t) * lzpLowCount);
        if (lzpLow == NULL) {
            err("Memory allocation failure.");
            exit(EXIT_FAILURE);
        }
        for (int32_t i = 0; i < lzpLowCount; i++) {
            lzpLow[i] = 0xffb5;
        }
        onlyLowLzp = lzpLowContextLength == lzpHighContextLength
                && lzpLowMaskSize == lzpHighMaskSize;
        if (onlyLowLzp) {
            lzpHigh = NULL;
        } else {
            lzpHigh = malloc(sizeof (uint16_t) * lzpHighCount);
            if (lzpHigh == NULL) {
                err("Memory allocation failure.");
                exit(EXIT_FAILURE);
            }
            for (int32_t i = 0; i < lzpHighCount; i++) {
                lzpHigh[i] = 0xffb5;
            }
        }
        // Literal coder init
        literalCoderContextMaskSize = 8 * literalCoderOrder;
        literalCoderContextMask = (1 << literalCoderContextMaskSize) - 1;
        int32_t const rangesGroupedAndSingleCount =
                (1 << literalCoderContextMaskSize) * 272;
        rangesGroupedAndSingle = malloc(sizeof (int16_t)
                * rangesGroupedAndSingleCount);
        if (rangesGroupedAndSingle == NULL) {
            err("Memory allocation failure.");
            exit(EXIT_FAILURE);
        }
        int32_t const rangesTotalCount = 1 << literalCoderContextMaskSize;
        rangesTotal = malloc(sizeof (int16_t) * rangesTotalCount);
        if (rangesTotal == NULL) {
            err("Memory allocation failure.");
            exit(EXIT_FAILURE);
        }
        for (int32_t i = 0; i < rangesTotalCount; i++) {
            rangesTotal[i] = literalCoderInit * 256;
            int16_t * const rangesGrouped = rangesGroupedAndSingle + i * 272;
            for (int32_t j = 0; j < 16; j++) {
                rangesGrouped[j] = literalCoderInit * 16;
            }
            int16_t * const rangesSingle = rangesGrouped + 16;
            for (int32_t j = 0; j < 256; j++) {
                rangesSingle[j] = literalCoderInit;
            }
        }
        recentCost = 8 << CostScale + 14;
        // Adaptive probability map init
        historyLow = 0;
        historyHigh = 0;
        for (int32_t i = 0; i < 16 * 256; i++) {
            apmLow[i] = 0x4000;
        }
        if (!onlyLowLzp) {
            for (int32_t i = 0; i < 16 * 256; i++) {
                apmHigh[i] = 0x4000;
            }
        }
        // Contexts and hashes init
        lastLiteralCoderContext = 0;
        context = 0;
        hashLow = 0;
        hashHigh = 0;
        for (int32_t i = 0; i < 256; i++) {
            int32_t hash = -2128831035;
            hash *= 16777619;
            hash ^= i;
            hash *= 16777619;
            precomputedHashes[i] = hash;
        }
    }

    // Contexts and hashes

    void updateContext(int32_t const input) {
        context <<= 8;
        context |= input;
    }

    void computeLiteralCoderContext() {
        lastLiteralCoderContext = (int32_t) (context & literalCoderContextMask);
    }

    void computeHashesOnlyLowLzp() {
        uint64_t localContext = context >> 8;
        int32_t hash = precomputedHashes[context & 0xFF];
        int32_t i = 1;
        while (true) {
            hash ^= (int32_t) (localContext & 0xFF);
            localContext >>= 8;
            if (++i == lzpLowContextLength) {
                break;
            }
            hash *= 16777619;
        }
        hashLow = hash & lzpLowMask;
    }

    void computeHashesOnlyLowLzpForNextIteration(int32_t const nextSymbol) {
        uint64_t localContext = context;
        int32_t hash = precomputedHashes[nextSymbol];
        int32_t i = 1;
        while (true) {
            hash ^= (int32_t) (localContext & 0xFF);
            localContext >>= 8;
            if (++i == lzpLowContextLength) {
                break;
            }
            hash *= 16777619;
        }
        hashLowNext = hash & lzpLowMask;
#ifndef NO_PREFETCH
        __builtin_prefetch(lzpLow + hashLowNext, 0, 2);
#endif
    }

    void computeHashes() {
        uint64_t localContext = context >> 8;
        int32_t hash = precomputedHashes[context & 0xFF];
        int32_t i = 1;
        while (true) {
            hash ^= (int32_t) (localContext & 0xFF);
            localContext >>= 8;
            if (++i == lzpLowContextLength) {
                break;
            }
            hash *= 16777619;
        }
        hashLow = hash & lzpLowMask;
        while (i++ < lzpHighContextLength) {
            hash *= 16777619;
            hash ^= (int32_t) (localContext & 0xFF);
            localContext >>= 8;
        }
        hashHigh = hash & lzpHighMask;
    }

    void computeHashesForNextIteration(int32_t const nextSymbol) {
        uint64_t localContext = context;
        int32_t hash = precomputedHashes[nextSymbol];
        int32_t i = 1;
        while (true) {
            hash ^= (int32_t) (localContext & 0xFF);
            localContext >>= 8;
            if (++i == lzpLowContextLength) {
                break;
            }
            hash *= 16777619;
        }
        hashLowNext = hash & lzpLowMask;
#ifndef NO_PREFETCH
        __builtin_prefetch(lzpLow + hashLowNext);
#endif
        while (i++ < lzpHighContextLength) {
            hash *= 16777619;
            hash ^= (int32_t) (localContext & 0xFF);
            localContext >>= 8;
        }
        hashHighNext = hash & lzpHighMask;
#ifndef NO_PREFETCH
        __builtin_prefetch(lzpHigh + hashHighNext);
#endif
    }

    void shiftHashes() {
        hashLow = hashLowNext;
        hashHigh = hashHighNext;
    }

    void shiftHashesOnlyLowLzp() {
        hashLow = hashLowNext;
    }

    // Calculating states

    int32_t getNextState(int32_t const state, bool const match) {
        return stateTable[state][match ? 0 : 1];
    }

    // Lempel-Ziv Predictive stuff

    int32_t getLzpStateLow() {
        return (lzpLow[hashLow] >> 8) & 0xff;
    }

    int32_t getLzpStateHigh() {
        return (lzpHigh[hashHigh] >> 8) & 0xff;
    }

    int32_t getLzpPredictedSymbolLow() {
        return lzpLow[hashLow] & 0xff;
    }

    int32_t getLzpPredictedSymbolHigh() {
        return lzpHigh[hashHigh] & 0xff;
    }

    void updateLzpStateLow(int32_t const lzpStateLow, int32_t const input,
            bool const match) {
        lzpLow[hashLow] = (uint16_t) ((getNextState(lzpStateLow, match) << 8)
                + input);
    }

    void updateLzpStateHigh(int32_t const lzpStateHigh, int32_t const input,
            bool const match) {
        lzpHigh[hashHigh] = (uint16_t) ((getNextState(lzpStateHigh, match) << 8)
                + input);
    }

    // Adaptive probability map stuff

    int32_t getApmLow(int32_t const state) {
        return apmLow[(historyLow << 8) + state];
    }

    int32_t getApmHigh(int32_t const state) {
        return apmHigh[(historyHigh << 8) + state];
    }

    void updateApmHistoryLow(bool const match) {
        historyLow = ((historyLow << 1) + (match ? 0 : 1)) & historyLowMask;
    }

    void updateApmHistoryHigh(bool const match) {
        historyHigh = ((historyHigh << 1) + (match ? 0 : 1)) & historyHighMask;
    }

    void updateApmLow(int32_t const state, bool const match) {
        int32_t const index = (historyLow << 8) + state;
        if (match) {
            apmLow[index] += ((1 << 15) - apmLow[index]) >> 7;
        } else {
            apmLow[index] -= apmLow[index] >> 7;
        }
        updateApmHistoryLow(match);
    }

    void updateApmHigh(int32_t const state, bool const match) {
        int32_t const index = (historyHigh << 8) + state;
        if (match) {
            apmHigh[index] += ((1 << 15) - apmHigh[index]) >> 7;
        } else {
            apmHigh[index] -= apmHigh[index] >> 7;
        }
        updateApmHistoryHigh(match);
    }

    // Literal coder stuff

    void rescaleLiteralCoder() {
        int16_t * const rangesGrouped = rangesGroupedAndSingle
                + lastLiteralCoderContext * 272;
        int16_t * const rangesSingle = rangesGrouped + 16;
        int16_t totalFrequency = 0;
        for (int32_t groupCurrent = 0; groupCurrent < 16; groupCurrent++) {
            int16_t groupFrequency = 0;
            for (int32_t indexCurrent = groupCurrent << 4; indexCurrent
                    < (groupCurrent + 1) << 4; indexCurrent++) {
                rangesSingle[indexCurrent] -= rangesSingle[indexCurrent] >> 1;
                groupFrequency += rangesSingle[indexCurrent];
            }
            rangesGrouped[groupCurrent] = groupFrequency;
            totalFrequency += groupFrequency;
        }
        rangesTotal[lastLiteralCoderContext] = totalFrequency;
    }

    void updateLiteralCoder(int32_t const symbol) {
        int32_t const baseIndex = lastLiteralCoderContext * 272;
        rangesGroupedAndSingle[baseIndex + (symbol >> 4)] += literalCoderStep;
        rangesGroupedAndSingle[baseIndex + 16 + symbol] += literalCoderStep;
        rangesTotal[lastLiteralCoderContext] += literalCoderStep;
        if (rangesTotal[lastLiteralCoderContext] > literalCoderLimit) {
            rescaleLiteralCoder();
        }
    }

    bool useFixedProbabilities() {
        return recentCost > 8 << CostScale + 14;
    }

    void updateRecentCost(int32_t const symbolFrequency,
            int32_t const totalFrequency) {
        recentCost -= recentCost >> CostScale;
        recentCost += nLog2(totalFrequency);
        recentCost -= nLog2(symbolFrequency);
    }

#ifdef	__cplusplus
}
#endif

#endif	/* COMMON_H */
