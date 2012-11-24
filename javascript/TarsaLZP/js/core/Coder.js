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
Coder = (function() {
    var self = {};

    var HeaderValue = new Long(0x208b, 0xbb9f, 0x5b12, 0x98be);

    self.getHeaderValue = function() {
        return Object.create(HeaderValue);
    };

    self.getOptions = function(inputStream) {
        var header = new Long(0, 0, 0, 0);
        var i;
        for (i = 0; i < 8; i++) {
            header.shl8();
            var inputByte = inputStream.read();
            if (inputByte == -1) {
                throw "Unexpected end of file.";
            }
            header.d |= inputByte;
        }
        if (header.a != HeaderValue.a || header.b != HeaderValue.b
            || header.c != HeaderValue.c || header.d != HeaderValue.d) {
            throw "Wrong file header. Probably not a compressed file.";
        }
        return self.getOptionsHeaderless(inputStream);
    };

    self.getOptionsHeaderless = function(inputStream) {
        var packedOptions = new Long(0, 0, 0, 0);
        var i;
        for (i = 0; i < 8; i++) {
            packedOptions.shl8();
            var inputByte = inputStream.read();
            if (inputByte == -1) {
                throw "Unexpected end of file.";
            }
            packedOptions.d |= inputByte;
        }
        var result = Options.fromPacked(packedOptions);
        if (result == null) {
            throw "Invalid compression options.";
        } else {
            return result;
        }
    };

    function _checkInterval(intervalLength) {
        if (intervalLength <= 0) {
            throw "Interval length has to be positive.";
        }
    }

    self.decode = function(inputStream, outputStream, callback,
                           intervalLength) {
        _checkInterval(intervalLength);
        var options = self.getOptions(inputStream);
        self.decodeRaw(inputStream, outputStream, callback, intervalLength,
            options);
    };

    self.decodeRaw = function(inputStream, outputStream, callback,
                              intervalLength, options) {
        _checkInterval(intervalLength);
        var decoder = newDecoder(inputStream, outputStream, options);
        var amountProcessed = 0;
        while (!decoder.decode(intervalLength)) {
            amountProcessed += intervalLength;
            if (callback != null) {
                callback(amountProcessed);
            }
        }
    };

    self.encode = function(inputStream, outputStream, callback, intervalLength,
                           options) {
        _checkInterval(intervalLength);
        var encoder = newEncoder(inputStream, outputStream, options);
        var header = self.getHeaderValue();
        var i;
        for (i = 0; i < 8; i++) {
            outputStream.write((header.a & 0xff00) >> 8);
            header.shl8();
        }
        var packedOptions = options.toPacked();
        for (i = 0; i < 8; i++) {
            outputStream.write((packedOptions.a & 0xff00) >> 8);
            packedOptions.shl8();
        }
        _doEncode(encoder, callback, intervalLength);
    };

    self.encodeRaw = function(inputStream, outputStream, callback,
                              intervalLength, options) {
        _checkInterval(intervalLength);
        var encoder = newEncoder(inputStream, outputStream, options);
        _doEncode(encoder, callback, intervalLength);
    };

    function _doEncode(encoder, callback, intervalLength) {
        var amountProcessed = 0;
        while (!encoder.encode(intervalLength)) {
            amountProcessed += intervalLength;
            if (callback != null) {
                callback(amountProcessed);
            }
        }
        encoder.flush();
    }

    return self;
})();
