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
            final long lzpHighMaskSize, final long ppmOrder, final long ppmInit,
            final long ppmStep, final long ppmLimit) {
        final boolean valid = lzpLowContextLength > ppmOrder
                && lzpLowContextLength <= lzpHighContextLength
                && lzpHighContextLength <= 8
                && lzpLowMaskSize >= 15
                && lzpLowMaskSize <= 30
                && lzpHighMaskSize >= 15
                && lzpHighMaskSize <= 30
                && ppmOrder >= 1
                && ppmOrder <= 2
                && ppmInit >= 1
                && ppmInit <= 127
                && ppmStep >= 1
                && ppmStep <= 127
                && ppmLimit >= ppmInit * 256
                && ppmLimit <= 32767 - ppmStep;
        if (valid) {
            return new Options(lzpLowContextLength, lzpLowMaskSize,
                    lzpHighContextLength, lzpHighMaskSize, ppmOrder, ppmInit,
                    ppmStep, ppmLimit);
        } else {
            return null;
        }
    }
    private final long lzpLowContextLength;
    private final long lzpLowMaskSize;
    private final long lzpHighContextLength;
    private final long lzpHighMaskSize;
    private final long ppmOrder;
    private final long ppmInit;
    private final long ppmStep;
    private final long ppmLimit;

    private Options(final long lzpLowContextLength, final long lzpLowMaskSize,
            final long lzpHighContextLength, final long lzpHighMaskSize,
            final long ppmOrder, final long ppmInit, final long ppmStep,
            final long ppmLimit) {
        this.lzpLowContextLength = lzpLowContextLength;
        this.lzpLowMaskSize = lzpLowMaskSize;
        this.lzpHighContextLength = lzpHighContextLength;
        this.lzpHighMaskSize = lzpHighMaskSize;
        this.ppmOrder = ppmOrder;
        this.ppmInit = ppmInit;
        this.ppmStep = ppmStep;
        this.ppmLimit = ppmLimit;
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
                + (((ppmOrder - 1) & 0x01) << 31)
                + ((ppmInit & 0x7f) << 24)
                + ((ppmStep & 0xff) << 16)
                + (ppmLimit & 0xffff);
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

    public long getPpmOrder() {
        return ppmOrder;
    }

    public long getPpmInit() {
        return ppmInit;
    }

    public long getPpmStep() {
        return ppmStep;
    }

    public long getPpmLimit() {
        return ppmLimit;
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
        hash = 31 * hash + (int) (this.ppmOrder ^ (this.ppmOrder >>> 32));
        hash = 31 * hash + (int) (this.ppmInit ^ (this.ppmInit >>> 32));
        hash = 31 * hash + (int) (this.ppmStep ^ (this.ppmStep >>> 32));
        hash = 31 * hash + (int) (this.ppmLimit ^ (this.ppmLimit >>> 32));
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
        if (this.ppmOrder != other.ppmOrder) {
            return false;
        }
        if (this.ppmInit != other.ppmInit) {
            return false;
        }
        if (this.ppmStep != other.ppmStep) {
            return false;
        }
        if (this.ppmLimit != other.ppmLimit) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Options[" + "lzpLowContextLength=" + lzpLowContextLength
                + ", lzpLowMaskSize=" + lzpLowMaskSize
                + ", lzpHighContextLength=" + lzpHighContextLength
                + ", lzpHighMaskSize=" + lzpHighMaskSize + ", ppmOrder="
                + ppmOrder + ", ppmInit=" + ppmInit + ", ppmStep=" + ppmStep
                + ", ppmLimit=" + ppmLimit + ']';
    }
}
