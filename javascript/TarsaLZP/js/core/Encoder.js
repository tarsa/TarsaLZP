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
function newEncoder(inputStream, outputStream, options) {
    var rcBuffer = 0;
    var rcRange = 0x7fffffff;
    var xFFRunLength = 0;
    var lastOutputByte = 0;
    var delay = false;
    var carry = false;

    var _super = newCommon(options);
    var self = Object.create(_super);

    function _outputByte(octet) {
        if (octet != 0xff) {
            if (delay) {
                outputStream.write(lastOutputByte + (carry ? 1 : 0));
            }
            while (xFFRunLength > 0) {
                xFFRunLength--;
                outputStream.write(carry ? 0x00 : 0xff);
            }
            lastOutputByte = octet;
            delay = true;
            carry = false;
        } else {
            xFFRunLength++;
        }
    }

    function _normalize() {
        while (rcRange < 0x00800000) {
            _outputByte((rcBuffer & 0x7f800000) >> 23);
            rcBuffer = (rcBuffer & 0x007fffff) << 8;
            rcRange <<= 8;
        }
    }

    function _addWithCarry(value) {
        rcBuffer += value;
        if (rcBuffer > 0x7fffffff) {
            rcBuffer &= 0x7fffffff;
            carry = true;
        }
    }

    function _encodeFlag(probability, match) {
        _normalize();
        var rcHelper = (rcRange >> 15) * probability;
        if (match) {
            rcRange = rcHelper;
        } else {
            _addWithCarry(rcHelper);
            rcRange -= rcHelper;
        }
    }

    function _encodeSkewed(flag) {
        _normalize();
        if (flag) {
            rcRange--;
        } else {
            _addWithCarry(rcRange - 1);
            rcRange = 1;
        }
    }

    function _encodeSingleOnlyLowLzp(nextSymbol) {
        self.computeHashesOnlyLowLzp();
        var lzpStateLow = self.getLzpStateLow();
        var predictedSymbolLow = self.getLzpPredictedSymbolLow();
        var modelLowFrequency = self.getSeeLow(lzpStateLow);
        var matchLow = nextSymbol == predictedSymbolLow;
        _encodeFlag(modelLowFrequency, matchLow);
        self.updateSeeLow(lzpStateLow, matchLow);
        self.updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
        if (!matchLow) {
            _encodeSymbol(nextSymbol, predictedSymbolLow);
        }
        self.updateContext(nextSymbol);
    }

    function _encodeSingle(nextSymbol) {
        self.computeHashes();
        var lzpStateLow = self.getLzpStateLow();
        var predictedSymbolLow = self.getLzpPredictedSymbolLow();
        var modelLowFrequency = self.getSeeLow(lzpStateLow);
        var lzpStateHigh = self.getLzpStateHigh();
        var predictedSymbolHigh = self.getLzpPredictedSymbolHigh();
        var modelHighFrequency = self.getSeeHigh(lzpStateHigh);
        var matchLow, matchHigh;
        if (modelLowFrequency >= modelHighFrequency) {
            matchHigh = nextSymbol == predictedSymbolHigh;
            self.updateSeeHistoryHigh(matchHigh);
            self.updateLzpStateHigh(lzpStateHigh, nextSymbol, matchHigh);
            matchLow = nextSymbol == predictedSymbolLow;
            _encodeFlag(modelLowFrequency, matchLow);
            self.updateSeeLow(lzpStateLow, matchLow);
            self.updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
            if (!matchLow) {
                _encodeSymbol(nextSymbol, predictedSymbolLow);
            }
        } else {
            matchLow = nextSymbol == predictedSymbolLow;
            self.updateSeeHistoryLow(matchLow);
            self.updateLzpStateLow(lzpStateLow, nextSymbol, matchLow);
            matchHigh = nextSymbol == predictedSymbolHigh;
            _encodeFlag(modelHighFrequency, matchHigh);
            self.updateSeeHigh(lzpStateHigh, matchHigh);
            self.updateLzpStateHigh(lzpStateHigh, nextSymbol, matchHigh);
            if (!matchHigh) {
                _encodeSymbol(nextSymbol, predictedSymbolHigh);
            }
        }
        self.updateContext(nextSymbol);
    }

    function _encodeSymbol(nextSymbol, mispredictedSymbol) {
        _normalize();
        self.computePpmContext();
        var index = (self.getLastPpmContext() << 8) + nextSymbol;
        var cumulativeExclusiveFrequency = 0;
        var symbolGroup = index >> 4;
        var indexPartial;
        for (indexPartial = self.getLastPpmContext() << 4;
             indexPartial < symbolGroup; indexPartial++) {
            cumulativeExclusiveFrequency +=
                self.getRangesGrouped()[indexPartial];
        }
        for (indexPartial = symbolGroup << 4; indexPartial < index;
             indexPartial++) {
            cumulativeExclusiveFrequency +=
                self.getRangesSingle()[indexPartial];
        }
        var mispredictedSymbolFrequency = self.getRangesSingle()[
            (self.getLastPpmContext() << 8) + mispredictedSymbol];
        if (nextSymbol > mispredictedSymbol) {
            cumulativeExclusiveFrequency -= mispredictedSymbolFrequency;
        }
        var rcHelper = rcRange / (self.getRangesTotal()[
            self.getLastPpmContext()] - mispredictedSymbolFrequency) >> 0;
        _addWithCarry(rcHelper * cumulativeExclusiveFrequency);
        rcRange = rcHelper * self.getRangesSingle()[index];
        self.updatePpm(index);
    }

    self.flush = function() {
        _encodeSkewed(false);
        var i;
        for (i = 0; i < 5; i++) {
            _outputByte((rcBuffer >> 23) & 0xff);
            rcBuffer = (rcBuffer & 0x007fffff) << 8;
        }
    };

    self.encode = function(limit) {
        var endReached = false;
        var i;
        for (i = 0; i < limit; i++) {
            var symbol = inputStream.read();
            if (symbol == -1) {
                endReached = true;
                break;
            }
            _encodeSkewed(true);
            if (self.isOnlyLowLzp()) {
                _encodeSingleOnlyLowLzp(symbol);
            } else {
                _encodeSingle(symbol);
            }
        }
        return endReached;
    };

    return self;
}
