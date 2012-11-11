#
# Copyright (c) 2012, Piotr Tarsa
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# Redistributions of source code must retain the above copyright notice, this
# list of conditions and the following disclaimer.
#
# Redistributions in binary form must reproduce the above copyright notice,
# this list of conditions and the following disclaimer in the documentation
# and/or other materials provided with the distribution.
#
# Neither the name of the author nor the names of its contributors may be used
# to endorse or promote products derived from this software without specific
# prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#

from Common import Common

__author__ = 'Piotr Tarsa'

class Encoder(Common):
    def __init__(self, inputStream, outputStream, options):
        Common.__init__(self, inputStream, outputStream, options)
        self.rcBuffer = 0
        self.rcRange = 0x7fffffff
        self.xFFRunLength = 0
        self.lastOutputByte = 0
        self.delay = False
        self.carry = False

    def outputByte(self, octet):
        if octet != 0xff:
            if self.delay:
                self.outputStream.writeByte(self.lastOutputByte +
                                            (1 if self.carry else 0))
            while self.xFFRunLength > 0:
                self.xFFRunLength -= 1
                self.outputStream.writeByte(0x00 if self.carry else 0xff)
            self.lastOutputByte = octet
            self.delay = True
            self.carry = False
        else:
            self.xFFRunLength += 1

    def normalize(self):
        while self.rcRange < 0x00800000:
            self.outputByte(self.rcBuffer >> 23)
            self.rcBuffer = (self.rcBuffer & 0x007fffff) << 8
            self.rcRange <<= 8

    def addWithCarry(self, value):
        distanceToMax = 0x7fffffff - self.rcBuffer
        if value > distanceToMax:
            self.rcBuffer = ((self.rcBuffer & 0x3fffffff) +
            (value & 0x3fffffff)) ^ ((self.rcBuffer ^ value) & 0x40000000)
            self.carry = True
        else:
            self.rcBuffer += value

    def encodeFlag(self, probability, match):
        self.normalize()
        rcHelper = (self.rcRange >> 15) * probability
        if match:
            self.rcRange = rcHelper
        else:
            self.addWithCarry(rcHelper)
            self.rcRange -= rcHelper

    def encodeSkewed(self, flag):
        self.normalize()
        if flag:
            self.rcRange -= 1
        else:
            self.addWithCarry(self.rcRange - 1)
            self.rcRange = 1

    def encodeSingleOnlyLowLzp(self, nextSymbol):
        self.computeHashesOnlyLowLzp()
        lzpStateLow = self.getLzpStateLow()
        predictedSymbolLow = self.getLzpPredictedSymbolLow()
        modelLowFrequency = self.getSeeLow(lzpStateLow)
        matchLow = nextSymbol == predictedSymbolLow
        self.encodeFlag(modelLowFrequency, matchLow)
        self.updateSeeLow(lzpStateLow, matchLow)
        self.updateLzpStateLow(lzpStateLow, nextSymbol, matchLow)
        if not matchLow:
            self.encodeSymbol(nextSymbol, predictedSymbolLow)
        self.updateContext(nextSymbol)

    def encodeSingle(self, nextSymbol):
        self.computeHashes()
        lzpStateLow = self.getLzpStateLow()
        predictedSymbolLow = self.getLzpPredictedSymbolLow()
        modelLowFrequency = self.getSeeLow(lzpStateLow)
        lzpStateHigh = self.getLzpStateHigh()
        predictedSymbolHigh = self.getLzpPredictedSymbolHigh()
        modelHighFrequency = self.getSeeHigh(lzpStateHigh)
        if modelLowFrequency >= modelHighFrequency:
            matchHigh = nextSymbol == predictedSymbolHigh
            self.updateSeeHistoryHigh(matchHigh)
            self.updateLzpStateHigh(lzpStateHigh, nextSymbol, matchHigh)
            matchLow = nextSymbol == predictedSymbolLow
            self.encodeFlag(modelLowFrequency, matchLow)
            self.updateSeeLow(lzpStateLow, matchLow)
            self.updateLzpStateLow(lzpStateLow, nextSymbol, matchLow)
            if not matchLow:
                self.encodeSymbol(nextSymbol, predictedSymbolLow)
        else:
            matchLow = nextSymbol == predictedSymbolLow
            self.updateSeeHistoryLow(matchLow)
            self.updateLzpStateLow(lzpStateLow, nextSymbol, matchLow)
            matchHigh = nextSymbol == predictedSymbolHigh
            self.encodeFlag(modelHighFrequency, matchHigh)
            self.updateSeeHigh(lzpStateHigh, matchHigh)
            self.updateLzpStateHigh(lzpStateHigh, nextSymbol, matchHigh)
            if not matchHigh:
                self.encodeSymbol(nextSymbol, predictedSymbolHigh)
        self.updateContext(nextSymbol)

    def encodeSymbol(self, nextSymbol, mispredictedSymbol):
        self.normalize()
        self.computePpmContext()
        index = (self.lastPpmContext << 8) + nextSymbol
        cumulativeExclusiveFrequency = 0
        symbolGroup = index >> 4
        for indexPartial in range(self.lastPpmContext << 4, symbolGroup):
            cumulativeExclusiveFrequency += self.rangesGrouped[indexPartial]
        for indexPartial in range(symbolGroup << 4, index):
            cumulativeExclusiveFrequency += self.rangesSingle[indexPartial]
        mispredictedSymbolFrequency = \
        self.rangesSingle[(self.lastPpmContext << 8) + mispredictedSymbol]
        if nextSymbol > mispredictedSymbol:
            cumulativeExclusiveFrequency -= mispredictedSymbolFrequency
        rcHelper = self.rcRange / (self.rangesTotal[self.lastPpmContext]
                                   - mispredictedSymbolFrequency)
        self.addWithCarry(rcHelper * cumulativeExclusiveFrequency)
        self.rcRange = rcHelper * self.rangesSingle[index]
        self.updatePpm(index)

    def flush(self):
        self.encodeSkewed(False)
        for i in range(0, 5):
            self.outputByte((self.rcBuffer >> 23) & 0xff)
            self.rcBuffer = (self.rcBuffer & 0x007fffff) << 8

    def encode(self, limit):
        endReached = False
        for i in range(0, limit):
            symbol = self.inputStream.readByte()
            if symbol == -1:
                endReached = True
                break
            self.encodeSkewed(True)
            if self.onlyLowLzp:
                self.encodeSingleOnlyLowLzp(symbol)
            else:
                self.encodeSingle(symbol)
        return endReached
