package com.github.tarsa.tarsalzp.core;

import com.github.tarsa.tarsalzp.Options;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author Piotr Tarsa
 */
public final class Encoder extends Common {

    private long rcBuffer;
    private long rcRange;
    int xFFRunLength = 0;
    int lastOutputByte = 0;
    boolean delay = false;
    boolean carry = false;

    public Encoder(final InputStream inputStream,
            final OutputStream outputStream, final Options options) {
        super(inputStream, outputStream, options);
        rcBuffer = 0;
        rcRange = 0xFFFFFFFFl;
    }

    private void outputByte(final int octet) throws IOException {
        if (carry) {
            outputStream.write(lastOutputByte + 1);
            for (int i = 0; i < xFFRunLength; i++) {
                outputStream.write(0x00);
            }
            xFFRunLength = 0;
            lastOutputByte = octet;
            delay = true;
            carry = false;
        } else if (octet == 0xff) {
            xFFRunLength++;
        } else {
            if (delay) {
                outputStream.write(lastOutputByte);
            }
            for (int i = 0; i < xFFRunLength; i++) {
                outputStream.write(0xff);
            }
            xFFRunLength = 0;
            lastOutputByte = octet;
            delay = true;
        }
    }

    private void normalize() throws IOException {
        while (rcRange < 0x01000000l) {
            outputByte((int) (rcBuffer >> 24));
            rcBuffer = (rcBuffer << 8) & 0xFFFFFFFFl;
            rcRange <<= 8;
        }
    }

    void encodeFlag(final int probability, final boolean match)
            throws IOException {
        normalize();
        final long rcHelper = (rcRange >> 16) * probability;
        if (match) {
            rcRange = rcHelper;
        } else {
            rcBuffer += rcHelper;
            if (rcBuffer > 0xFFFFFFFFl) {
                carry = true;
                rcBuffer = rcBuffer & 0xFFFFFFFFl;
            }
            rcRange -= rcHelper;
        }
    }

    void encodeSkewed(final boolean flag) throws IOException {
        normalize();
        if (flag) {
            rcRange--;
        } else {
            rcBuffer += rcRange - 1;
            if (rcBuffer > 0xFFFFFFFFl) {
                carry = true;
                rcBuffer = rcBuffer & 0xFFFFFFFFl;
            }
            rcRange = 1;
        }
    }

    void encodeSingle(final int nextSymbol)
            throws IOException {
        computeHashes();
        final int model4Frequency = getSEE4(getLZPState4(getLastHash4()));
        final int model8Frequency = getSEE8(getLZPState8(getLastHash8()));
        boolean match;
        int predictedSymbol;
        if (model4Frequency >= model8Frequency) {
            predictedSymbol = getLZPPredictedSymbol8(getLastHash8());
            match = nextSymbol == predictedSymbol;
            updateSEEHistory8(match);
            updateLZPState8(getLastHash8(), nextSymbol, match);
            predictedSymbol = getLZPPredictedSymbol4(getLastHash4());
            match = nextSymbol == predictedSymbol;
            encodeFlag(model4Frequency, match);
            updateSEE4(getLZPState4(getLastHash4()), match);
            updateLZPState4(getLastHash4(), nextSymbol, match);
        } else {
            predictedSymbol = getLZPPredictedSymbol4(getLastHash4());
            match = nextSymbol == predictedSymbol;
            updateSEEHistory4(match);
            updateLZPState4(getLastHash4(), nextSymbol, match);
            predictedSymbol = getLZPPredictedSymbol8(getLastHash8());
            match = nextSymbol == predictedSymbol;
            encodeFlag(model8Frequency, match);
            updateSEE8(getLZPState8(getLastHash8()), match);
            updateLZPState8(getLastHash8(), nextSymbol, match);
        }
        if (!match) {
            encodeSymbol(nextSymbol, predictedSymbol);
        }
        updateContext(nextSymbol);
    }

    void encodeSymbol(final int nextSymbol, final int mispredictedSymbol)
            throws IOException {
        normalize();
        computePpmContext();
        final int index = (getLastPpmContext() << 8) + nextSymbol;
        short cumulativeFrequency = 0;
        final int symbolGroup = index >> 4;
        for (int indexPartial = getLastPpmContext() << 4;
                indexPartial < symbolGroup; indexPartial++) {
            cumulativeFrequency += rangesGrouped[indexPartial];
        }
        for (int indexPartial = symbolGroup << 4; indexPartial < index;
                indexPartial++) {
            cumulativeFrequency += rangesSingle[indexPartial];
        }
        final short mispredictedSymbolFrequency =
                rangesSingle[(getLastPpmContext() << 8) + mispredictedSymbol];
        if (nextSymbol > mispredictedSymbol) {
            cumulativeFrequency -= mispredictedSymbolFrequency;
        }
        final long rcHelper = rcRange / (rangesTotal[getLastPpmContext()]
                - mispredictedSymbolFrequency);
        rcBuffer += rcHelper * cumulativeFrequency;
        if (rcBuffer > 0xFFFFFFFFl) {
            carry = true;
            rcBuffer = rcBuffer & 0xFFFFFFFFl;
        }
        rcRange = rcHelper * rangesSingle[index];
        rangesSingle[index] += ppmStep;
        rangesGrouped[symbolGroup] += ppmStep;
        rangesTotal[getLastPpmContext()] += ppmStep;
        if (rangesTotal[getLastPpmContext()] > ppmLimit) {
            for (int indexCurrent = getLastPpmContext() << 8; indexCurrent
                    < (getLastPpmContext() + 1) << 8; indexCurrent++) {
                rangesSingle[indexCurrent] -=
                        rangesSingle[indexCurrent] >> 1;
            }
            short totalFrequency = 0;
            for (int groupCurrent = getLastPpmContext() << 4; groupCurrent
                    < (getLastPpmContext() + 1) << 4; groupCurrent++) {
                short groupFrequency = 0;
                for (int indexCurrent = groupCurrent << 4; indexCurrent
                        < (groupCurrent + 1) << 4; indexCurrent++) {
                    groupFrequency += rangesSingle[indexCurrent];
                }
                rangesGrouped[groupCurrent] = groupFrequency;
                totalFrequency += groupFrequency;
            }
            rangesTotal[getLastPpmContext()] = totalFrequency;
        }
    }

    void flush() throws IOException {
        encodeSkewed(false);
        for (int i = 0; i < 5; i++) {
            outputByte(((int) (rcBuffer >> 24)) & 0xFF);
            rcBuffer <<= 8;
        }
    }

    public boolean encode(final long limit) throws IOException {
        boolean endReached = false;
        for (int i = 0; i < limit; i++) {
            final int symbol = inputStream.read();
            if (symbol == -1) {
                endReached = true;
                break;
            }
            encodeSkewed(true);
            encodeSingle(symbol);
        }
        return endReached;
    }
}
