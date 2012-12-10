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
function newOptions(lzpLowContextLength, lzpLowMaskSize, lzpHighContextLength,
                    lzpHighMaskSize, literalCoderOrder, literalCoderInit,
                    literalCoderStep, literalCoderLimit) {
    var self = {};

    self.isValid = function() {
        return lzpLowContextLength > literalCoderOrder
            && lzpLowContextLength <= lzpHighContextLength
            && lzpHighContextLength <= 8
            && lzpLowMaskSize >= 15
            && lzpLowMaskSize <= 30
            && lzpHighMaskSize >= 15
            && lzpHighMaskSize <= 30
            && literalCoderOrder >= 1
            && literalCoderOrder <= 2
            && literalCoderInit >= 1
            && literalCoderInit <= 127
            && literalCoderStep >= 1
            && literalCoderStep <= 127
            && literalCoderLimit >= literalCoderInit * 256
            && literalCoderLimit <= 32767 - literalCoderStep;
    };

    self.toPacked = function() {
        var a = (lzpLowContextLength << 8) + (lzpLowMaskSize);
        var b = (lzpHighContextLength << 8) + (lzpHighMaskSize);
        var c = ((literalCoderOrder - 1) << 15) + (literalCoderInit << 8)
            + (literalCoderStep);
        var d = (literalCoderLimit);
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

    self.getLiteralCoderOrder = function() {
        return literalCoderOrder;
    };

    self.getLiteralCoderInit = function() {
        return literalCoderInit;
    };

    self.getLiteralCoderStep = function() {
        return literalCoderStep;
    };

    self.getLiteralCoderLimit = function() {
        return literalCoderLimit;
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
    return newOptions((a & 0xff00) >> 8, a & 0xff, (b & 0xff00) >> 8, b & 0xff,
        ((c & 0x8000) >> 15) + 1, (c & 0x7f00) >> 8, c & 0xff, d);
};
