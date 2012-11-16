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

#ifndef STREAMS_H
#define	STREAMS_H

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#ifdef	__cplusplus
extern "C" {
#endif
    
    FILE * input;
    FILE * output;
    
    #define inputBufferSize (64 * 1024)
    uint8_t inputBuffer[inputBufferSize];
    int32_t inputBufferPosition;
    int32_t inputBufferLimit;
    bool inputReadEnded;
    #define outputBufferSize (64 * 1024)
    uint8_t outputBuffer[outputBufferSize];
    int32_t outputBufferPosition;
    int32_t outputBufferLimit;
    bool outputWriteStarted;
    bool outputWriteToFile;
    char const * outputWriteFilename;
    
    void __inputReader__() {
        inputBufferPosition = 0;
        inputBufferLimit = 0;
        inputReadEnded = false;
    }
    
    int32_t inputRead() {
        if (inputBufferPosition < inputBufferLimit) {
            return inputBuffer[inputBufferPosition++];
        } else if (inputReadEnded) {
            return -1;
        } else {
            inputBufferPosition = 0;
            inputBufferLimit = fread(inputBuffer, 1, inputBufferSize, input);
            if (inputBufferLimit == 0) {
                inputReadEnded = true;
            }
            return inputRead();
        }
    }
    
    void __outputWriter__() {
        outputBufferPosition = 0;
        outputBufferLimit = outputBufferSize;
        outputWriteStarted = false;
        outputWriteToFile = false;
        outputWriteFilename = NULL;
    }

    void outputFlush() {
        if ((!outputWriteStarted) && outputWriteToFile) {
            output = fopen(outputWriteFilename, "wb");
        }
        outputWriteStarted = true;
        if (fwrite(outputBuffer, 1, outputBufferPosition, output)
                != outputBufferPosition) {
            fputs("Error while writing to output.", stderr);
            exit(EXIT_FAILURE);
        };
        outputBufferPosition = 0;
    }

    void outputWrite(int32_t const byte) {
        if (outputBufferPosition < outputBufferLimit) {
            outputBuffer[outputBufferPosition++] = byte;
        } else {
            outputFlush();
            outputWrite(byte);
        }
    }
    
#ifdef	__cplusplus
}
#endif

#endif	/* STREAMS_H */

