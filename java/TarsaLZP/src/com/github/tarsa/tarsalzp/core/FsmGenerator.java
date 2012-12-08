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
package com.github.tarsa.tarsalzp.core;

/**
 *
 * @author Piotr Tarsa
 */
public class FsmGenerator {

    private final byte[] stateTable = new byte[512];
    private final int LimitX = 20;
    private final int LimitY = 20;
    private int p = 0;
    private int freqmask[] = new int[(LimitX + 1) * (LimitY + 1) * 3 * 3];
    
    public FsmGenerator() {
        for (int i = 0; i < (LimitX + 1) * (LimitY + 1) * 3 * 3; i++) {
            freqmask[i] = -1;
        }
        p = 0;
        initStates(0, 0, 2, 2);
    }
    
    public byte[] getStateTable() {
        return stateTable;
    }

    private int divisor(int a, int b) {
        return (Lg2.nLog2(b) >> 3) + (Lg2.nLog2(1950) >> 3) - (12 << 11);
    }

    private int repeated(int a, int b) {
        return b > 0 && divisor(a, b) > (1200)
                ? ((a + 1) * 1950) / divisor(a, b) : a + 1;
    }

    private int opposite(int a, int b) {
        return b > 0 && divisor(a, b) > (1200)
                ? (b * 1950) / divisor(a, b) : b;
    }

    private byte initStates(int x, int y, int h1, int h0) {
        x = Math.min(x, LimitX);
        y = Math.min(y, LimitY);
        final int index = ((y * (LimitX + 1) + x) * 3 + h1) * 3 + h0;
        if (freqmask[index] == -1) {
            freqmask[index] = p;
            int c = p++;
            stateTable[c * 2 + 0] = initStates(repeated(x, y),
                    opposite(x, y), h0, 0);
            stateTable[c * 2 + 1] = initStates(opposite(y, x),
                    repeated(y, x), h0, 1);
        }
        return (byte) freqmask[index];
    }
}
