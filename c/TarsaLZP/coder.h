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

#ifndef CODER_H
#define	CODER_H

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#include "streams.h"
#include "options.h"
#include "encoder.h"
#include "decoder.h"

#ifdef	__cplusplus
extern "C" {
#endif

    uint64_t const HeaderValue = UINT64_C(2345174324078614718);
    
    void coderReadOptionsHeaderless() {
        uint64_t packedOptions = 0;
        for (int32_t i = 0; i < 8; i++) {
            packedOptions <<= 8;
            int32_t const inputByte = inputRead();
            if (inputByte == -1) {
                fputs("Unexpected end of file.", stderr);
                exit(EXIT_FAILURE);
            }
            packedOptions |= inputByte;
        }
        optionsLoad(packedOptions);
        if (!optionsValid()) {
            fputs("Invalid compression options.", stderr);
            exit(EXIT_FAILURE);
        }
    }

    void coderReadOptions() {
        uint64_t header = 0;
        for (int32_t i = 0; i < 8; i++) {
            header <<= 8;
            int32_t const inputByte = inputRead();
            if (inputByte == -1) {
                fputs("Unexpected end of file.", stderr);
                exit(EXIT_FAILURE);
            }
            header |= inputByte;
        }
        if (header != HeaderValue) {
            fputs("Wrong file header. Probably not a compressed file.", stderr);
            exit(EXIT_FAILURE);
        }
        coderReadOptionsHeaderless();
    }
    
    void coderDecodeRaw() {
        __decoder__();
        decode();
    }

    void coderDecode() {
        coderReadOptions();
        coderDecodeRaw();
    }

    void coderEncode() {
        __encoder__();
        uint64_t header = HeaderValue;
        for (int32_t i = 0; i < 8; i++) {
            outputWrite((header >> 56) & 0xff);
            header <<= 8;
        }
        uint64_t packedOptions = optionsSave();
        for (int32_t i = 0; i < 8; i++) {
            outputWrite((packedOptions >> 56) & 0xff);
            packedOptions <<= 8;
        }
        encode();
        encoderFlush();
    }
    
    void coderEncodeRaw() {
        __encoder__();
        encode();
        encoderFlush();
    }

#ifdef	__cplusplus
}
#endif

#endif	/* CODER_H */

