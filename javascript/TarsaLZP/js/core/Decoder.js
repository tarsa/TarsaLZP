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
        self.computeHashes();
        var modelLowFrequency = self.getSeeLow(self.getLzpStateLow(self.getLastHashLow()));
        var predictedSymbol = self.getLzpPredictedSymbolLow(self.getLastHashLow());
        var match = _decodeFlag(modelLowFrequency);
        self.updateSeeLow(self.getLzpStateLow(self.getLastHashLow()), match);
        var nextSymbol = match ? predictedSymbol : _decodeSymbol(predictedSymbol);
        self.updateLzpStateLow(self.getLastHashLow(), nextSymbol, match);
        self.updateContext(nextSymbol);
        return nextSymbol;
    }

    function _decodeSingle() {
        self.computeHashes();
        var modelLowFrequency = self.getSeeLow(self.getLzpStateLow(self.getLastHashLow()));
        var modelHighFrequency = self.getSeeHigh(self.getLzpStateHigh(self.getLastHashHigh()));
        var match;
        var predictedSymbol;
        var nextSymbol;
        if (modelLowFrequency >= modelHighFrequency) {
            predictedSymbol = self.getLzpPredictedSymbolLow(self.getLastHashLow());
            match = _decodeFlag(modelLowFrequency);
            self.updateSeeLow(self.getLzpStateLow(self.getLastHashLow()), match);
            if (match) {
                nextSymbol = predictedSymbol;
            } else {
                nextSymbol = _decodeSymbol(predictedSymbol);
            }
            self.updateLzpStateLow(self.getLastHashLow(), nextSymbol, match);
            predictedSymbol = self.getLzpPredictedSymbolHigh(self.getLastHashHigh());
            match = nextSymbol == predictedSymbol;
            self.updateSeeHistoryHigh(match);
            self.updateLzpStateHigh(self.getLastHashHigh(), nextSymbol, match);
        } else {
            predictedSymbol = self.getLzpPredictedSymbolHigh(self.getLastHashHigh());
            match = _decodeFlag(modelHighFrequency);
            self.updateSeeHigh(self.getLzpStateHigh(self.getLastHashHigh()), match);
            if (match) {
                nextSymbol = predictedSymbol;
            } else {
                nextSymbol = _decodeSymbol(predictedSymbol);
            }
            self.updateLzpStateHigh(self.getLastHashHigh(), nextSymbol, match);
            predictedSymbol = self.getLzpPredictedSymbolLow(self.getLastHashLow());
            match = nextSymbol == predictedSymbol;
            self.updateSeeHistoryLow(match);
            self.updateLzpStateLow(self.getLastHashLow(), nextSymbol, match);
        }
        self.updateContext(nextSymbol);
        return nextSymbol;
    }

    function _decodeSymbol(mispredictedSymbol) {
        _normalize();
        self.computePpmContext();
        var mispredictedSymbolFrequency = self.getRangesSingle()[(self.getLastPpmContext() << 8) + mispredictedSymbol];
        rcRange = rcRange / (self.getRangesTotal()[self.getLastPpmContext()] - mispredictedSymbolFrequency) >> 0;
        self.getRangesSingle()[(self.getLastPpmContext() << 8) + mispredictedSymbol] = 0;
        self.getRangesGrouped()[((self.getLastPpmContext() << 8) + mispredictedSymbol) >> 4] -=
            mispredictedSymbolFrequency;
        var rcHelper = rcBuffer / rcRange >> 0;
        var cumulativeFrequency = rcHelper;
        var index;
        for (index = self.getLastPpmContext() << 4; rcHelper >= self.getRangesGrouped()[index]; index++) {
            rcHelper -= self.getRangesGrouped()[index];
        }
        for (index <<= 4; rcHelper >= self.getRangesSingle()[index]; index++) {
            rcHelper -= self.getRangesSingle()[index];
        }
        rcBuffer -= (cumulativeFrequency - rcHelper) * rcRange;
        rcRange *= self.getRangesSingle()[index];
        var nextSymbol = index & 0xff;
        self.getRangesSingle()[(self.getLastPpmContext() << 8) + mispredictedSymbol] = mispredictedSymbolFrequency;
        self.getRangesGrouped()[((self.getLastPpmContext() << 8) + mispredictedSymbol) >> 4] +=
            mispredictedSymbolFrequency;
        self.updatePpm(index);
        return nextSymbol;
    }

    self.decode = function(limit) {
        if (!started) {
            _init();
        }
        var endReached = false;
        var i;
        for (i = 0; i < limit; i++) {
            endReached = !_decodeSkewed();
            if (!endReached) {
                var symbol = self.isOnlyLowLzp() ? _decodeSingleOnlyLowLzp() : _decodeSingle();
                outputStream.write(symbol);
            } else {
                break;
            }
        }
        return endReached;
    };

    return self;
}