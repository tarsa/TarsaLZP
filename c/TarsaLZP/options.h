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

#ifndef OPTIONS_H
#define	OPTIONS_H

#include <stdbool.h>
#include <stdint.h>

#ifdef	__cplusplus
extern "C" {
#endif

    int32_t lzpLowContextLength;
    int32_t lzpLowMaskSize;
    int32_t lzpHighContextLength;
    int32_t lzpHighMaskSize;
    int32_t literalCoderOrder;
    int32_t literalCoderInit;
    int32_t literalCoderStep;
    int32_t literalCoderLimit;

    void __options__() {
        lzpLowContextLength = 4;
        lzpLowMaskSize = 24;
        lzpHighContextLength = 8;
        lzpHighMaskSize = 27;
        literalCoderOrder = 2;
        literalCoderInit = 1;
        literalCoderStep = 60;
        literalCoderLimit = 30000;
    }

    bool optionsValid() {
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
    }

    void optionsLoad(uint64_t const packed) {
        lzpLowContextLength = packed >> 56 & 0xff;
        lzpLowMaskSize = packed >> 48 & 0xff;
        lzpHighContextLength = packed >> 40 & 0xff;
        lzpHighMaskSize = packed >> 32 & 0xff;
        literalCoderOrder = (packed >> 31 & 0x01) + 1;
        literalCoderInit = packed >> 24 & 0x7f;
        literalCoderStep = packed >> 16 & 0xff;
        literalCoderLimit = packed & 0xffff;
    }

    uint64_t optionsSave() {
        return ((uint64_t) (lzpLowContextLength & 0xff) << 56)
                + ((uint64_t) (lzpLowMaskSize & 0xff) << 48)
                + ((uint64_t) (lzpHighContextLength & 0xff) << 40)
                + ((uint64_t) (lzpHighMaskSize & 0xff) << 32)
                + ((uint64_t) ((literalCoderOrder - 1) & 0x01) << 31)
                + ((uint64_t) (literalCoderInit & 0x7f) << 24)
                + ((uint64_t) (literalCoderStep & 0xff) << 16)
                + ((uint64_t) literalCoderLimit & 0xffff);
    }


#ifdef	__cplusplus
}
#endif

#endif	/* OPTIONS_H */
