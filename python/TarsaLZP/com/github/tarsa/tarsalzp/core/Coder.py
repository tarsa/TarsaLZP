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

from ..prelude.Long import Long
from ..Options import Options
from Decoder import Decoder
from Encoder import Encoder

__author__ = 'Piotr Tarsa'

class Coder(object):
    HeaderValue = Long(0x208b, 0xbb9f, 0x5b12, 0x98be)

    @classmethod
    def getOptions(cls, inputStream):
        header = Long(0, 0, 0, 0)
        for i in range(0, 8):
            header.shl8()
            inputByte = inputStream.readByte()
            if inputByte == -1:
                raise IOError("Unexpected end of file.")
            header.d |= inputByte
        if (header.a != cls.HeaderValue.a) | (header.b != cls.HeaderValue.b)\
        | (header.c != cls.HeaderValue.c) | (header.d != cls.HeaderValue.d):
            raise IOError("Wrong file header. Probably not a compressed file.")
        return cls.getOptionsHeaderless(inputStream)

    @classmethod
    def getOptionsHeaderless(cls, inputStream):
        packedOptions = Long(0, 0, 0, 0)
        for i in range(0, 8):
            packedOptions.shl8()
            inputByte = inputStream.readByte()
            if inputByte == -1:
                raise IOError("Unexpected end of file.")
            packedOptions.d |= inputByte
        result = Options.fromPacked(packedOptions)
        if result is None:
            raise ValueError("Invalid compression options.")
        else:
            return result

    @staticmethod
    def checkInterval(intervalLength):
        if intervalLength <= 0:
            raise ValueError("Interval length has to be positive.")

    @classmethod
    def decode(cls, inputStream, outputStream, callback, intervalLength):
        cls.checkInterval(intervalLength)
        options = cls.getOptions(inputStream)
        cls.decodeRaw(inputStream, outputStream, callback, intervalLength,
            options)

    @classmethod
    def decodeRaw(cls, inputStream, outputStream, callback, intervalLength,
                  options):
        cls.checkInterval(intervalLength)
        decoder = Decoder(inputStream, outputStream, options)
        amountProcessed = 0
        while not decoder.decode(intervalLength):
            amountProcessed += intervalLength
            if callback is not None:
                callback(amountProcessed)

    @classmethod
    def encode(cls, inputStream, outputStream, callback, intervalLength,
               options):
        cls.checkInterval(intervalLength)
        encoder = Encoder(inputStream, outputStream, options)
        header = Long(cls.HeaderValue.a, cls.HeaderValue.b, cls.HeaderValue.c,
            cls.HeaderValue.d)
        for i in range(0, 8):
            outputStream.writeByte(header.a >> 8)
            header.shl8()
        packedOptions = options.toPacked()
        for i in range(0, 8):
            outputStream.writeByte(packedOptions.a >> 8)
            packedOptions.shl8()
        cls.doEncode(encoder, callback, intervalLength)

    @classmethod
    def encodeRaw(cls, inputStream, outputStream, callback, intervalLength,
                  options):
        cls.checkInterval(intervalLength)
        encoder = Encoder(inputStream, outputStream, options)
        cls.doEncode(encoder, callback, intervalLength)

    @staticmethod
    def doEncode(encoder, callback, intervalLength):
        amountProcessed = 0
        while not encoder.encode(intervalLength):
            amountProcessed += intervalLength
            if callback is not None:
                callback(amountProcessed)
        encoder.flush()
