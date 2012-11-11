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

from prelude.Long import Long

__author__ = 'Piotr Tarsa'

class Options(object):
    def __init__(self, lzpLowContextLength, lzpLowMaskSize,
                 lzpHighContextLength, lzpHighMaskSize, ppmOrder, ppmInit,
                 ppmStep, ppmLimit):
        self.lzpLowContextLength = lzpLowContextLength
        self.lzpLowMaskSize = lzpLowMaskSize
        self.lzpHighContextLength = lzpHighContextLength
        self.lzpHighMaskSize = lzpHighMaskSize
        self.ppmOrder = ppmOrder
        self.ppmInit = ppmInit
        self.ppmStep = ppmStep
        self.ppmLimit = ppmLimit

    def isValid(self):
        return (self.lzpLowContextLength > self.ppmOrder)\
        & (self.lzpLowContextLength <= self.lzpHighContextLength)\
        & (self.lzpHighContextLength <= 8)\
        & (self.lzpLowMaskSize >= 15)\
        & (self.lzpLowMaskSize <= 30)\
        & (self.lzpHighMaskSize >= 15)\
        & (self.lzpHighMaskSize <= 30)\
        & (self.ppmOrder >= 1)\
        & (self.ppmOrder <= 2)\
        & (self.ppmInit >= 1)\
        & (self.ppmInit <= 127)\
        & (self.ppmStep >= 1)\
        & (self.ppmStep <= 127)\
        & (self.ppmLimit >= self.ppmInit * 256)\
        & (self.ppmLimit <= 32767 - self.ppmStep)

    def toPacked(self):
        a = (self.lzpLowContextLength << 8) + self.lzpLowMaskSize
        b = (self.lzpHighContextLength << 8) + self.lzpHighMaskSize
        c = ((self.ppmOrder - 1) << 15) + (self.ppmInit << 8) + self.ppmStep
        d = self.ppmLimit
        return Long(a, b, c, d)

    @staticmethod
    def fromPacked(packed):
        a = packed.a
        b = packed.b
        c = packed.c
        d = packed.d
        options = Options((a & 0xff00) >> 8, a & 0xff, (b & 0xff00) >> 8,
            b & 0xff, ((c & 0x8000) >> 15) + 1, (c & 0x7f00) >> 8, c & 0xff, d)
        return options if options.isValid() else None

    @staticmethod
    def getDefault():
        lzpLowContextLength = 4
        lzpLowMaskSize = 24
        lzpHighContextLength = 8
        lzpHighMaskSize = 27
        ppmOrder = 2
        ppmInit = 1
        ppmStep = 60
        ppmLimit = 30000
        return Options(lzpLowContextLength, lzpLowMaskSize,
            lzpHighContextLength, lzpHighMaskSize, ppmOrder, ppmInit, ppmStep,
            ppmLimit)
