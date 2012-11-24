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
public class Lg2 {

    private static final byte[] lgLut = new byte[256];

    static {
        lgLut[0] = -1;
        for (int i = 1; i < lgLut.length; i++) {
            lgLut[i] = (byte) (1 + lgLut[i / 2]);
        }
    }

    public static byte iLog2(final int value) {
        if (value >= 256 && value < 65536) {
            return (byte) (8 + lgLut[value >> 8]);
        } else if (value < 256) {
            return lgLut[value];
        } else {
            return (byte) (16 + iLog2(value >>> 16));
        }
    }

    /**
     * Approximate logatihm base 2 scaled by 2^14, Works only for positive
     * values lower than 2^15.
     *
     * @param value
     * @return
     */
    public static int nLog2(final int value) {
        final byte ilog = iLog2(value);
        final int norm = value << 14 - ilog;
        return (ilog - 1 << 14) + norm;
    }
}
