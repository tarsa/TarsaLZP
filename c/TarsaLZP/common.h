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

    // LZP section
    bool onlyLowLzp;
    int32_t lzpLowMask;
    int32_t lzpHighMask;
    uint16_t * lzpLow;
    uint16_t * lzpHigh;
    // PPM section
    int32_t ppmMaskSize;
    int32_t ppmMask;
    int16_t * rangesSingle;
    int16_t * rangesGrouped;
    int16_t * rangesTotal;
    int32_t recentCost;
    // SEE section
    int32_t historyLow;
    int32_t historyHigh;
    int32_t const historyLowMask = 15;
    int32_t const historyHighMask = 15;
    int16_t seeLow[16 * 256];
    int16_t seeHigh[16 * 256];
    // Contexts and hashes
    int32_t lastPpmContext;
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
        // LZP init
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
        // PPM init
        ppmMaskSize = 8 * ppmOrder;
        ppmMask = (1 << ppmMaskSize) - 1;
        int32_t const rangesSingleCount = 1 << ppmMaskSize + 8;
        rangesSingle = malloc(sizeof (int16_t) * rangesSingleCount);
        if (rangesSingle == NULL) {
            err("Memory allocation failure.");
            exit(EXIT_FAILURE);
        }
        for (int32_t i = 0; i < rangesSingleCount; i++) {
            rangesSingle[i] = ppmInit;
        }
        int32_t const rangesGroupedCount = 1 << ppmMaskSize + 4;
        rangesGrouped = malloc(sizeof (int16_t) * rangesGroupedCount);
        if (rangesGrouped == NULL) {
            err("Memory allocation failure.");
            exit(EXIT_FAILURE);
        }
        for (int32_t i = 0; i < rangesGroupedCount; i++) {
            rangesGrouped[i] = ppmInit * 16;
        }
        int32_t const rangesTotalCount = 1 << ppmMaskSize;
        rangesTotal = malloc(sizeof (int16_t) * rangesTotalCount);
        if (rangesTotal == NULL) {
            err("Memory allocation failure.");
            exit(EXIT_FAILURE);
        }
        for (int32_t i = 0; i < rangesTotalCount; i++) {
            rangesTotal[i] = ppmInit * 256;
        }
        recentCost = 8 << CostScale + 14;
        // SEE init
        historyLow = 0;
        historyHigh = 0;
        for (int32_t i = 0; i < 16 * 256; i++) {
            seeLow[i] = 0x4000;
        }
        if (!onlyLowLzp) {
            for (int32_t i = 0; i < 16 * 256; i++) {
                seeHigh[i] = 0x4000;
            }
        }
        // Contexts and hashes init
        lastPpmContext = 0;
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

    void computePpmContext() {
        lastPpmContext = (int32_t) (context & ppmMask);
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

    // LZP stuff

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

    // SEE stuff

    int32_t getSeeLow(int32_t const state) {
        return seeLow[(historyLow << 8) + state];
    }

    int32_t getSeeHigh(int32_t const state) {
        return seeHigh[(historyHigh << 8) + state];
    }

    void updateSeeHistoryLow(bool const match) {
        historyLow = ((historyLow << 1) + (match ? 0 : 1)) & historyLowMask;
    }

    void updateSeeHistoryHigh(bool const match) {
        historyHigh = ((historyHigh << 1) + (match ? 0 : 1)) & historyHighMask;
    }

    void updateSeeLow(int32_t const state, bool const match) {
        int32_t const index = (historyLow << 8) + state;
        if (match) {
            seeLow[index] += ((1 << 15) - seeLow[index]) >> 7;
        } else {
            seeLow[index] -= seeLow[index] >> 7;
        }
        updateSeeHistoryLow(match);
    }

    void updateSeeHigh(int32_t const state, bool const match) {
        int32_t const index = (historyHigh << 8) + state;
        if (match) {
            seeHigh[index] += ((1 << 15) - seeHigh[index]) >> 7;
        } else {
            seeHigh[index] -= seeHigh[index] >> 7;
        }
        updateSeeHistoryHigh(match);
    }

    //PPM stuff

    void rescalePpm() {
        for (int32_t indexCurrent = lastPpmContext << 8; indexCurrent
                < (lastPpmContext + 1) << 8; indexCurrent++) {
            rangesSingle[indexCurrent] -= rangesSingle[indexCurrent] >> 1;
        }
        int16_t totalFrequency = 0;
        for (int32_t groupCurrent = lastPpmContext << 4; groupCurrent
                < (lastPpmContext + 1) << 4; groupCurrent++) {
            int16_t groupFrequency = 0;
            for (int32_t indexCurrent = groupCurrent << 4; indexCurrent
                    < (groupCurrent + 1) << 4; indexCurrent++) {
                groupFrequency += rangesSingle[indexCurrent];
            }
            rangesGrouped[groupCurrent] = groupFrequency;
            totalFrequency += groupFrequency;
        }
        rangesTotal[lastPpmContext] = totalFrequency;
    }

    void updatePpm(int32_t const index) {
        rangesSingle[index] += ppmStep;
        rangesGrouped[index >> 4] += ppmStep;
        rangesTotal[lastPpmContext] += ppmStep;
        if (rangesTotal[lastPpmContext] > ppmLimit) {
            rescalePpm();
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
