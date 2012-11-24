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
function newDecoder(inputStream, outputStream, options) {
    var rcBuffer;
    var rcRange;
    var started = false;
    var nextHighBit = 0;

    var _super = newCommon(options);
    var self = Object.create(_super);

    function _inputByte() {
        var inputByte = inputStream.read();
        if (inputByte == -1) {
            throw "Unexpected end of file.";
        }
        var currentByte = (inputByte >> 1) + (nextHighBit << 7);
        nextHighBit = inputByte & 1;
        return currentByte;
    }

    function _init() {
        rcBuffer = 0;
        var i;
        for (i = 0; i < 4; i++) {
            rcBuffer = (rcBuffer << 8) + _inputByte();
        }
        rcRange = 0x7fffffff;
        started = true;
    }

    function _normalize() {
        while (rcRange < 0x00800000) {
            rcBuffer = (rcBuffer << 8) + _inputByte();
            rcRange <<= 8;
        }
    }

    function _decodeFlag(probability) {
        _normalize();
        var rcHelper = (rcRange >> 15) * probability;
        if (rcHelper > rcBuffer) {
            rcRange = rcHelper;
            return true;
        } else {
            rcRange -= rcHelper;
            rcBuffer -= rcHelper;
            return false;
        }
    }

    function _decodeSkewed() {
        _normalize();
        if (rcBuffer < rcRange - 1) {
            rcRange--;
            return true;
        } else {
            rcBuffer = 0;
            rcRange = 1;
            return false;
        }
    }

    function _decodeSingleOnlyLowLzp() {
        self.computeHashesOnlyLowLzp();
        var lzpStateLow = self.getLzpStateLow();
        var predictedSymbolLow = self.getLzpPredictedSymbolLow();
        var modelLowFrequency = self.getSeeLow(lzpStateLow);
        var matchLow = _decodeFlag(modelLowFrequency);
        self.updateSeeLow(lzpStateLow, matchLow);
        var nextSymbol = matchLow ? predictedSymbolLow
            : _decodeSymbol(predictedSymbolLow);
        self.updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
        self.updateContext(nextSymbol);
        return nextSymbol;
    }

    function _decodeSingle() {
        self.computeHashes();
        var lzpStateLow = self.getLzpStateLow();
        var predictedSymbolLow = self.getLzpPredictedSymbolLow();
        var modelLowFrequency = self.getSeeLow(lzpStateLow);
        var lzpStateHigh = self.getLzpStateHigh();
        var predictedSymbolHigh = self.getLzpPredictedSymbolHigh();
        var modelHighFrequency = self.getSeeHigh(lzpStateHigh);
        var nextSymbol;
        var matchLow, matchHigh;
        if (modelLowFrequency >= modelHighFrequency) {
            matchLow = _decodeFlag(modelLowFrequency);
            self.updateSeeLow(lzpStateLow, matchLow);
            nextSymbol = matchLow ? predictedSymbolLow
                : _decodeSymbol(predictedSymbolLow);
            self.updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
            matchHigh = nextSymbol == predictedSymbolHigh;
            self.updateSeeHistoryHigh(matchHigh);
            self.updateLzpStateHigh(lzpStateHigh, nextSymbol, matchHigh);
        } else {
            matchHigh = _decodeFlag(modelHighFrequency);
            self.updateSeeHigh(lzpStateHigh, matchHigh);
            nextSymbol = matchHigh ? predictedSymbolHigh
                : _decodeSymbol(predictedSymbolHigh);
            self.updateLzpStateHigh(lzpStateHigh, nextSymbol, matchHigh);
            matchLow = nextSymbol == predictedSymbolLow;
            self.updateSeeHistoryLow(matchLow);
            self.updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
        }
        self.updateContext(nextSymbol);
        return nextSymbol;
    }

    function _decodeSymbol(mispredictedSymbol) {
        _normalize();
        self.computePpmContext();
        var index;
        var nextSymbol;
        var rcHelper;
        if (!self.useFixedProbabilities()) {
            var mispredictedSymbolFrequency = self.getRangesSingle()[
                (self.getLastPpmContext() << 8) + mispredictedSymbol];
            rcRange = rcRange / (self.getRangesTotal()[self.getLastPpmContext()]
                - mispredictedSymbolFrequency) >> 0;
            self.getRangesSingle()[(self.getLastPpmContext() << 8)
                + mispredictedSymbol] = 0;
            self.getRangesGrouped()[((self.getLastPpmContext() << 8)
                + mispredictedSymbol) >> 4] -=
                mispredictedSymbolFrequency;
            rcHelper = rcBuffer / rcRange >> 0;
            var cumulativeFrequency = rcHelper;
            for (index = self.getLastPpmContext() << 4;
                 rcHelper >= self.getRangesGrouped()[index]; index++) {
                rcHelper -= self.getRangesGrouped()[index];
            }
            for (index <<= 4; rcHelper >= self.getRangesSingle()[index];
                 index++) {
                rcHelper -= self.getRangesSingle()[index];
            }
            rcBuffer -= (cumulativeFrequency - rcHelper) * rcRange;
            rcRange *= self.getRangesSingle()[index];
            nextSymbol = index & 0xff;
            self.getRangesSingle()[(self.getLastPpmContext() << 8)
                + mispredictedSymbol] = mispredictedSymbolFrequency;
            self.getRangesGrouped()[((self.getLastPpmContext() << 8)
                + mispredictedSymbol) >> 4] += mispredictedSymbolFrequency;
        } else {
            rcRange = rcRange / 255 >> 0;
            rcHelper = rcBuffer / rcRange >> 0;
            rcBuffer -= rcHelper * rcRange;
            nextSymbol = rcHelper + (rcHelper >= mispredictedSymbol ? 1 : 0);
            index = (self.getLastPpmContext() << 8) + nextSymbol;
        }
        self.updateRecentCost(self.getRangesSingle()[index],
            self.getRangesTotal()[self.getLastPpmContext()]);
        self.updatePpm(index);
        return nextSymbol;
    }

    self.decode = function (limit) {
        if (!started) {
            _init();
        }
        var endReached = false;
        var i;
        for (i = 0; i < limit; i++) {
            endReached = !_decodeSkewed();
            if (!endReached) {
                var symbol = self.isOnlyLowLzp() ? _decodeSingleOnlyLowLzp() :
                    _decodeSingle();
                outputStream.write(symbol);
            } else {
                break;
            }
        }
        return endReached;
    };

    return self;
}
