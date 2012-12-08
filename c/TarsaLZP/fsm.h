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

#ifndef FSM_H
#define	FSM_H

#include <stdint.h>

#include "lg2.h"

#ifdef	__cplusplus
extern "C" {
#endif

    uint8_t stateTable[256][2];
    int32_t const LimitX = 20;
    int32_t const LimitY = 20;

    int32_t divisor(int32_t const a, int32_t const b) {
        return (nLog2(b) >> 3) + (nLog2(1950) >> 3) - (12 << 11);
    }

    int32_t repeated(int32_t const a, int32_t const b) {
        return b > 0 && divisor(a, b) > (1200)
                ? ((a + 1) * 1950) / divisor(a, b) : a + 1;
    }

    int32_t opposite(int32_t const a, int32_t const b) {
        return b > 0 && divisor(a, b) > (1200)
                ? (b * 1950) / divisor(a, b) : b;
    }

    uint8_t initStates(int32_t const x, int32_t const y, int32_t const h1,
            int32_t const h0, int32_t * const freqmask, int32_t * const p) {
        int32_t const xc = x < LimitX ? x : LimitX;
        int32_t const yc = y < LimitY ? y : LimitY;
        int32_t const index = ((yc * (LimitX + 1) + xc) * 3 + h1) * 3 + h0;
        if (freqmask[index] == -1) {
            freqmask[index] = *p;
            int32_t const c = (*p)++;
            stateTable[c][0] = initStates(repeated(xc, yc),
                    opposite(xc, yc), h0, 0, freqmask, p);
            stateTable[c][1] = initStates(opposite(yc, xc),
                    repeated(yc, xc), h0, 1, freqmask, p);
        }
        return (uint8_t) freqmask[index];
    }

    void __fsm__() {
        int32_t freqmask[(LimitX + 1) * (LimitY + 1) * 3 * 3];
        int32_t p = 0;
        for (int32_t i = 0; i < (LimitX + 1) * (LimitY + 1) * 3 * 3; i++) {
            freqmask[i] = -1;
        }
        initStates(0, 0, 2, 2, freqmask, &p);
    }


#ifdef	__cplusplus
}
#endif

#endif	/* FSM_H */
