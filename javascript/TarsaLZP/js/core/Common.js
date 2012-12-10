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
    var literalCoderOrder = options.getLiteralCoderOrder();
    var literalCoderInit = options.getLiteralCoderInit();
    var literalCoderStep = options.getLiteralCoderStep();
    var literalCoderLimit = options.getLiteralCoderLimit();

    // Lempel-Ziv Predictive section
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
    // Literal coder section
    var CostScale = 7;
    var literalCoderContextMaskSize = literalCoderOrder * 8;
    var rangesSingle = new Int16Array(1 << literalCoderContextMaskSize + 8);
    fillArray(rangesSingle, literalCoderInit);
    var rangesGrouped = new Int16Array(1 << literalCoderContextMaskSize + 4);
    fillArray(rangesGrouped, literalCoderInit * 16);
    var rangesTotal = new Int16Array(1 << literalCoderContextMaskSize);
    fillArray(rangesTotal, literalCoderInit * 256);
    var recentCost = 8 << CostScale + 14;
    // Adaptive probability map section
    var apmLow = new Int16Array(16 * 256);
    fillArray(apmLow, 0x4000);
    var apmHigh = new Int16Array(16 * 256);
    fillArray(apmHigh, 0x4000);
    var historyLow = 0;
    var historyLowMask = 15;
    var historyHigh = 0;
    var historyHighMask = 15;
    // Contexts and hashes
    var lastLiteralCoderContext = 0;
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

    self.computeLiteralCoderContext = function () {
        lastLiteralCoderContext = context[contextIndex];
        if (literalCoderOrder == 2) {
            lastLiteralCoderContext = (lastLiteralCoderContext << 8)
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

    self.getLastLiteralCoderContext = function () {
        return lastLiteralCoderContext;
    };

// Calculating states

    self.getNextState = function (state, match) {
        return stateTable[state * 2 + (match ? 1 : 0)];
    };

// Lempel-Ziv Predictive section

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

// Adaptive probability map section

    self.getApmLow = function (state) {
        return apmLow[(historyLow << 8) + state];
    };

    self.getApmHigh = function (state) {
        return apmHigh[(historyHigh << 8) + state];
    };

    self.updateApmHistoryLow = function (match) {
        historyLow = ((historyLow << 1) + (match ? 0 : 1)) & historyLowMask;
    };

    self.updateApmHistoryHigh = function (match) {
        historyHigh = ((historyHigh << 1) + (match ? 0 : 1)) & historyHighMask;
    };

    self.updateApmLow = function (state, match) {
        var index = (historyLow << 8) + state;
        if (match) {
            apmLow[index] += (((1 << 15) - apmLow[index]) & 0xff80) >> 7;
        } else {
            apmLow[index] -= (apmLow[index] & 0xff80) >> 7;
        }
        self.updateApmHistoryLow(match);
    };

    self.updateApmHigh = function (state, match) {
        var index = (historyHigh << 8) + state;
        if (match) {
            apmHigh[index] += (((1 << 15) - apmHigh[index]) & 0xff80) >> 7;
        } else {
            apmHigh[index] -= (apmHigh[index] & 0xff80) >> 7;
        }
        self.updateApmHistoryHigh(match);
    };

// Literal coder section

    self.getRangesSingle = function() {
        return rangesSingle;
    };

    self.getRangesGrouped = function() {
        return rangesGrouped;
    };

    self.getRangesTotal = function() {
        return rangesTotal;
    };

    self.rescaleLiteralCoder = function () {
        var indexCurrent, groupCurrent;
        for (indexCurrent = self.getLastLiteralCoderContext() << 8;
             indexCurrent < (self.getLastLiteralCoderContext() + 1) << 8;
             indexCurrent++) {
            rangesSingle[indexCurrent] -=
                (rangesSingle[indexCurrent] & 0xfffe) >> 1;
        }
        var totalFrequency = 0;
        for (groupCurrent = self.getLastLiteralCoderContext() << 4; groupCurrent
            < (self.getLastLiteralCoderContext() + 1) << 4; groupCurrent++) {
            var groupFrequency = 0;
            for (indexCurrent = groupCurrent << 4; indexCurrent
                < (groupCurrent + 1) << 4; indexCurrent++) {
                groupFrequency += rangesSingle[indexCurrent];
            }
            rangesGrouped[groupCurrent] = groupFrequency;
            totalFrequency += groupFrequency;
        }
        rangesTotal[self.getLastLiteralCoderContext()] = totalFrequency;
    };

    self.updateLiteralCoder = function (index) {
        rangesSingle[index] += literalCoderStep;
        rangesGrouped[index >> 4] += literalCoderStep;
        rangesTotal[self.getLastLiteralCoderContext()] += literalCoderStep;
        if (rangesTotal[self.getLastLiteralCoderContext()]
            > literalCoderLimit) {
            self.rescaleLiteralCoder();
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
