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
function newOptions(lzpLowContextLength, lzpLowMaskSize, lzpHighContextLength, lzpHighMaskSize,
                    ppmOrder, ppmInit, ppmStep, ppmLimit) {
    var self = {};

    self.isValid = function() {
        return lzpLowContextLength > ppmOrder
            && lzpLowContextLength <= lzpHighContextLength
            && lzpHighContextLength <= 8
            && lzpLowMaskSize >= 15
            && lzpLowMaskSize <= 30
            && lzpHighMaskSize >= 15
            && lzpHighMaskSize <= 30
            && ppmOrder >= 1
            && ppmOrder <= 2
            && ppmInit >= 1
            && ppmInit <= 127
            && ppmStep >= 1
            && ppmStep <= 127
            && ppmLimit >= ppmInit * 256
            && ppmLimit <= 32767 - ppmStep;
    };

    self.toPacked = function() {
        var a = (lzpLowContextLength << 8) + (lzpLowMaskSize);
        var b = (lzpHighContextLength << 8) + (lzpHighMaskSize);
        var c = ((ppmOrder - 1) << 15) + (ppmInit << 8) + (ppmStep);
        var d = (ppmLimit);
        return new Long(a, b, c, d);
    };

    self.getLzpLowContextLength = function() {
        return lzpLowContextLength;
    };

    self.getLzpLowMaskSize = function() {
        return lzpLowMaskSize;
    };

    self.getLzpHighContextLength = function() {
        return lzpHighContextLength;
    };

    self.getLzpHighMaskSize = function() {
        return lzpHighMaskSize;
    };

    self.getPpmOrder = function() {
        return ppmOrder;
    };

    self.getPpmInit = function() {
        return ppmInit;
    };

    self.getPpmStep = function() {
        return ppmStep;
    };

    self.getPpmLimit = function() {
        return ppmLimit;
    };

    if (self.isValid()) {
        return self;
    } else {
        return null;
    }
}

Options = {};

Options.fromPacked = function(packed) {
    var a = packed.a;
    var b = packed.b;
    var c = packed.c;
    var d = packed.d;
    return newOptions((a & 0xff00) >> 8, a & 0xff, (b & 0xff00) >> 8, b & 0xff, ((c & 0x8000) >> 15) + 1,
        (c & 0x7f00) >> 8, c & 0xff, d);
};