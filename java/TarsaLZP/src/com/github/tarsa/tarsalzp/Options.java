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
package com.github.tarsa.tarsalzp;

import java.io.Serializable;

/**
 *
 * @author Piotr Tarsa
 */
public final class Options implements Serializable {

    private static final long serialVersionUID = 6954096913728221462L;

    public static Options create(final long lzpLowContextLength,
            final long lzpLowMaskSize, final long lzpHighContextLength,
            final long lzpHighMaskSize, final long literalCoderOrder, 
            final long literalCoderInit, final long literalCoderStep,
            final long literalCoderLimit) {
        final boolean valid = lzpLowContextLength > literalCoderOrder
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
        if (valid) {
            return new Options(lzpLowContextLength, lzpLowMaskSize,
                    lzpHighContextLength, lzpHighMaskSize, literalCoderOrder, 
                    literalCoderInit, literalCoderStep, literalCoderLimit);
        } else {
            return null;
        }
    }
    private final long lzpLowContextLength;
    private final long lzpLowMaskSize;
    private final long lzpHighContextLength;
    private final long lzpHighMaskSize;
    private final long literalCoderOrder;
    private final long literalCoderInit;
    private final long literalCoderStep;
    private final long literalCoderLimit;

    private Options(final long lzpLowContextLength, final long lzpLowMaskSize,
            final long lzpHighContextLength, final long lzpHighMaskSize,
            final long literalCoderOrder, final long literalCoderInit, 
            final long literalCoderStep, final long literalCoderLimit) {
        this.lzpLowContextLength = lzpLowContextLength;
        this.lzpLowMaskSize = lzpLowMaskSize;
        this.lzpHighContextLength = lzpHighContextLength;
        this.lzpHighMaskSize = lzpHighMaskSize;
        this.literalCoderOrder = literalCoderOrder;
        this.literalCoderInit = literalCoderInit;
        this.literalCoderStep = literalCoderStep;
        this.literalCoderLimit = literalCoderLimit;
    }

    public static Options fromPacked(final long packed) {
        return Options.create(
                packed >> 56 & 0xff,
                packed >> 48 & 0xff,
                packed >> 40 & 0xff,
                packed >> 32 & 0xff,
                (packed >> 31 & 0x01) + 1,
                packed >> 24 & 0x7f,
                packed >> 16 & 0xff,
                packed & 0xffff);
    }

    public long toPacked() {
        return ((lzpLowContextLength & 0xff) << 56)
                + ((lzpLowMaskSize & 0xff) << 48)
                + ((lzpHighContextLength & 0xff) << 40)
                + ((lzpHighMaskSize & 0xff) << 32)
                + (((literalCoderOrder - 1) & 0x01) << 31)
                + ((literalCoderInit & 0x7f) << 24)
                + ((literalCoderStep & 0xff) << 16)
                + (literalCoderLimit & 0xffff);
    }

    public long getLzpLowContextLength() {
        return lzpLowContextLength;
    }

    public long getLzpLowMaskSize() {
        return lzpLowMaskSize;
    }

    public long getLzpHighContextLength() {
        return lzpHighContextLength;
    }

    public long getLzpHighMaskSize() {
        return lzpHighMaskSize;
    }

    public long getLiteralCoderOrder() {
        return literalCoderOrder;
    }

    public long getLiteralCoderInit() {
        return literalCoderInit;
    }

    public long getLiteralCoderStep() {
        return literalCoderStep;
    }

    public long getLiteralCoderLimit() {
        return literalCoderLimit;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (int) (this.lzpLowContextLength
                ^ (this.lzpLowContextLength >>> 32));
        hash = 31 * hash + (int) (this.lzpLowMaskSize
                ^ (this.lzpLowMaskSize >>> 32));
        hash = 31 * hash + (int) (this.lzpHighContextLength
                ^ (this.lzpHighContextLength >>> 32));
        hash = 31 * hash + (int) (this.lzpHighMaskSize
                ^ (this.lzpHighMaskSize >>> 32));
        hash = 31 * hash + (int) (this.literalCoderOrder 
                ^ (this.literalCoderOrder >>> 32));
        hash = 31 * hash + (int) (this.literalCoderInit 
                ^ (this.literalCoderInit >>> 32));
        hash = 31 * hash + (int) (this.literalCoderStep 
                ^ (this.literalCoderStep >>> 32));
        hash = 31 * hash + (int) (this.literalCoderLimit 
                ^ (this.literalCoderLimit >>> 32));
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Options other = (Options) obj;
        if (this.lzpLowContextLength != other.lzpLowContextLength) {
            return false;
        }
        if (this.lzpLowMaskSize != other.lzpLowMaskSize) {
            return false;
        }
        if (this.lzpHighContextLength != other.lzpHighContextLength) {
            return false;
        }
        if (this.lzpHighMaskSize != other.lzpHighMaskSize) {
            return false;
        }
        if (this.literalCoderOrder != other.literalCoderOrder) {
            return false;
        }
        if (this.literalCoderInit != other.literalCoderInit) {
            return false;
        }
        if (this.literalCoderStep != other.literalCoderStep) {
            return false;
        }
        if (this.literalCoderLimit != other.literalCoderLimit) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Options[" + "lzpLowContextLength=" + lzpLowContextLength
                + ", lzpLowMaskSize=" + lzpLowMaskSize
                + ", lzpHighContextLength=" + lzpHighContextLength
                + ", lzpHighMaskSize=" + lzpHighMaskSize 
                + ", literalCoderOrder=" + literalCoderOrder 
                + ", literalCoderInit=" + literalCoderInit 
                + ", literalCoderStep=" + literalCoderStep
                + ", literalCoderLimit=" + literalCoderLimit + ']';
    }
}
