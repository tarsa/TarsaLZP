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
function newCommon(options) {
    var lzpLowContextLength = options.getLzpLowContextLength();
    var lzpLowMaskSize = options.getLzpLowMaskSize();
    var lzpHighContextLength = options.getLzpHighContextLength();
    var lzpHighMaskSize = options.getLzpHighMaskSize();
    var ppmOrder = options.getPpmOrder();
    var ppmInit = options.getPpmInit();
    var ppmStep = options.getPpmStep();
    var ppmLimit = options.getPpmLimit();

    /// LZP section
    var lzpLowCount = 1 << lzpLowMaskSize;
    var lzpHighCount = 1 << lzpHighMaskSize;
    var lzpLowMask = lzpLowCount - 1;
    var lzpHighMask = lzpHighCount - 1;
    var lzpLow = new Uint16Array(lzpLowCount);
    var lzpHigh;
    fillArray(lzpLow, 0xffb5);
    var onlyLowLzp = lzpLowContextLength == lzpHighContextLength
        && lzpLowMaskSize == lzpHighMaskSize;
    if (onlyLowLzp) {
        lzpHigh = null;
    } else {
        lzpHigh = new Uint16Array(lzpHighCount);
        fillArray(lzpHigh, 0xffb5);
    }
    // PPM section
    var CostScale = 7;
    var ppmMaskSize = ppmOrder * 8;
    var rangesSingle = new Int16Array(1 << ppmMaskSize + 8);
    fillArray(rangesSingle, ppmInit);
    var rangesGrouped = new Int16Array(1 << ppmMaskSize + 4);
    fillArray(rangesGrouped, ppmInit * 16);
    var rangesTotal = new Int16Array(1 << ppmMaskSize);
    fillArray(rangesTotal, ppmInit * 256);
    var recentCost = 8 << CostScale + 14;
    // SEE section
    var seeLow = new Int16Array(16 * 256);
    fillArray(seeLow, 0x4000);
    var seeHigh = new Int16Array(16 * 256);
    fillArray(seeHigh, 0x4000);
    var historyLow = 0;
    var historyLowMask = 15;
    var historyHigh = 0;
    var historyHighMask = 15;
    // Contexts and hashes
    var lastPpmContext = 0;
    var context = new Uint8Array(8);
    fillArray(context, 0);
    var contextIndex = 0;
    var hashLow = 0;
    var hashHigh = 0;
    var precomputedHashes = (function (n) {
        var self = new Int32Array(n);
        var hash, i;
        for (i = 0; i < n; i++) {
            hash = 18652613;
            hash = hash * (1 + 2 + 16 + 128 + 256) + ((hash & 0x3f) << 24);
            hash &= 0x3fffffff;
            hash ^= i;
            hash = hash * (1 + 2 + 16 + 128 + 256) + ((hash & 0x3f) << 24);
            hash &= 0x3fffffff;
            self[i] = hash;
        }
        return self;
    })(256);
    // Calculating states
    var stateTable = newStateTable();

    var self = {};

// Contexts and Hashes

    self.updateContext = function (input) {
        contextIndex = (contextIndex - 1) & 7;
        context[contextIndex] = input;
    };

    self.computePpmContext = function () {
        lastPpmContext = context[contextIndex];
        if (ppmOrder == 2) {
            lastPpmContext = (lastPpmContext << 8)
                + (context[(contextIndex + 1) & 7]);
        }
    };

    self.computeHashesOnlyLowLzp = function() {
        var localIndex = (contextIndex + 1) & 7;
        var hash = precomputedHashes[context[contextIndex]];
        var i = 1;
        while (true) {
            hash ^= context[localIndex];
            localIndex = (localIndex + 1) & 7;
            if (++i == lzpLowContextLength) {
                break;
            }
            hash = hash * (1 + 2 + 16 + 128 + 256) + ((hash & 0x3f) << 24);
            hash &= 0x3fffffff;
        }
        hashLow = hash & lzpLowMask;
    };

    self.computeHashes = function () {
        var localIndex = (contextIndex + 1) & 7;
        var hash = precomputedHashes[context[contextIndex]];
        var i = 1;
        while (true) {
            hash ^= context[localIndex];
            localIndex = (localIndex + 1) & 7;
            if (++i == lzpLowContextLength) {
                break;
            }
            hash = hash * (1 + 2 + 16 + 128 + 256) + ((hash & 0x3f) << 24);
            hash &= 0x3fffffff;
        }
        hashLow = hash & lzpLowMask;
        while (i++ < lzpHighContextLength) {
            hash = hash * (1 + 2 + 16 + 128 + 256) + ((hash & 0x3f) << 24);
            hash &= 0x3fffffff;
            hash ^= context[localIndex];
            localIndex = (localIndex + 1) & 7;
        }
        hashHigh = hash & lzpHighMask;
    };

    self.getLastPpmContext = function () {
        return lastPpmContext;
    };

// Calculating states

    self.getNextState = function (state, match) {
        return stateTable[state * 2 + (match ? 1 : 0)];
    };

// LZP section

    self.isOnlyLowLzp = function() {
        return onlyLowLzp;
    };

    self.getLzpStateLow = function () {
        return (lzpLow[hashLow] & 0xff00) >> 8;
    };

    self.getLzpStateHigh = function () {
        return (lzpHigh[hashHigh] & 0xff00) >> 8;
    };

    self.getLzpPredictedSymbolLow = function () {
        return lzpLow[hashLow] & 0xff;
    };

    self.getLzpPredictedSymbolHigh = function () {
        return lzpHigh[hashHigh] & 0xff;
    };

    self.updateLzpStateLow = function (lzpStateLow, input, match) {
        lzpLow[hashLow] = (self.getNextState(lzpStateLow, match) << 8) + input;
    };

    self.updateLzpStateHigh = function (lzpStateHigh, input, match) {
        lzpHigh[hashHigh] = (self.getNextState(lzpStateHigh, match) << 8)
            + input;
    };

// SEE section

    self.getSeeLow = function (state) {
        return seeLow[(historyLow << 8) + state];
    };

    self.getSeeHigh = function (state) {
        return seeHigh[(historyHigh << 8) + state];
    };

    self.updateSeeHistoryLow = function (match) {
        historyLow = ((historyLow << 1) + (match ? 0 : 1)) & historyLowMask;
    };

    self.updateSeeHistoryHigh = function (match) {
        historyHigh = ((historyHigh << 1) + (match ? 0 : 1)) & historyHighMask;
    };

    self.updateSeeLow = function (state, match) {
        var index = (historyLow << 8) + state;
        if (match) {
            seeLow[index] += (((1 << 15) - seeLow[index]) & 0xff80) >> 7;
        } else {
            seeLow[index] -= (seeLow[index] & 0xff80) >> 7;
        }
        self.updateSeeHistoryLow(match);
    };

    self.updateSeeHigh = function (state, match) {
        var index = (historyHigh << 8) + state;
        if (match) {
            seeHigh[index] += (((1 << 15) - seeHigh[index]) & 0xff80) >> 7;
        } else {
            seeHigh[index] -= (seeHigh[index] & 0xff80) >> 7;
        }
        self.updateSeeHistoryHigh(match);
    };

// PPM section

    self.getRangesSingle = function() {
        return rangesSingle;
    };

    self.getRangesGrouped = function() {
        return rangesGrouped;
    };

    self.getRangesTotal = function() {
        return rangesTotal;
    };

    self.rescalePpm = function () {
        var indexCurrent, groupCurrent;
        for (indexCurrent = self.getLastPpmContext() << 8;
             indexCurrent < (self.getLastPpmContext() + 1) << 8;
             indexCurrent++) {
            rangesSingle[indexCurrent] -=
                (rangesSingle[indexCurrent] & 0xfffe) >> 1;
        }
        var totalFrequency = 0;
        for (groupCurrent = self.getLastPpmContext() << 4; groupCurrent
            < (self.getLastPpmContext() + 1) << 4; groupCurrent++) {
            var groupFrequency = 0;
            for (indexCurrent = groupCurrent << 4; indexCurrent
                < (groupCurrent + 1) << 4; indexCurrent++) {
                groupFrequency += rangesSingle[indexCurrent];
            }
            rangesGrouped[groupCurrent] = groupFrequency;
            totalFrequency += groupFrequency;
        }
        rangesTotal[self.getLastPpmContext()] = totalFrequency;
    };

    self.updatePpm = function (index) {
        rangesSingle[index] += ppmStep;
        rangesGrouped[index >> 4] += ppmStep;
        rangesTotal[self.getLastPpmContext()] += ppmStep;
        if (rangesTotal[self.getLastPpmContext()] > ppmLimit) {
            self.rescalePpm();
        }
    };

    self.useFixedProbabilities = function() {
        return recentCost > 8 << CostScale + 14;
    };

    self.updateRecentCost = function(symbolFrequency, totalFrequency) {
        recentCost -= recentCost >> CostScale;
        recentCost += Lg2.nLog2(totalFrequency);
        recentCost -= Lg2.nLog2(symbolFrequency);
    };

    return self;
}
