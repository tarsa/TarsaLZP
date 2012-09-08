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

    private int rcBuffer;
    private int rcRange;
    private int xFFRunLength = 0;
    private int lastOutputByte = 0;
    private boolean delay = false;
    private boolean carry = false;

    public Encoder(final InputStream inputStream,
            final OutputStream outputStream, final Options options) {
        super(inputStream, outputStream, options);
        rcBuffer = 0;
        rcRange = 0x7FFFFFFF;
    }

    private void outputByte(final int octet) throws IOException {
        if (octet != 0xff) {
            if (delay) {
                outputStream.write(lastOutputByte + (carry ? 1 : 0));
            }
            while (xFFRunLength > 0) {
                xFFRunLength--;
                outputStream.write(carry ? 0x00 : 0xff);
            }
            lastOutputByte = octet;
            delay = true;
            carry = false;
        } else {
            xFFRunLength++;
        }
    }

    private void normalize() throws IOException {
        while (rcRange < 0x00800000) {
            outputByte((int) (rcBuffer >> 23));
            rcBuffer = (rcBuffer << 8) & 0x7FFFFFFF;
            rcRange <<= 8;
        }
    }

    private void encodeFlag(final int probability, final boolean match)
            throws IOException {
        normalize();
        final int rcHelper = (rcRange >> 15) * probability;
        if (match) {
            rcRange = rcHelper;
        } else {
            rcBuffer += rcHelper;
            if (rcBuffer < 0) {
                carry = true;
                rcBuffer = rcBuffer & 0x7FFFFFFF;
            }
            rcRange -= rcHelper;
        }
    }

    private void encodeSkewed(final boolean flag) throws IOException {
        normalize();
        if (flag) {
            rcRange--;
        } else {
            rcBuffer += rcRange - 1;
            if (rcBuffer < 0) {
                carry = true;
                rcBuffer = rcBuffer & 0x7FFFFFFF;
            }
            rcRange = 1;
        }
    }

    private void encodeSingleOnlyLowLzp(final int nextSymbol)
            throws IOException {
        computeHashes();
        final int modelLowFrequency =
                getSeeLow(getLzpStateLow(getLastHashLow()));
        final int predictedSymbol = getLzpPredictedSymbolLow(getLastHashLow());
        final boolean match = nextSymbol == predictedSymbol;
        encodeFlag(modelLowFrequency, match);
        updateSeeLow(getLzpStateLow(getLastHashLow()), match);
        updateLzpStateLow(getLastHashLow(), nextSymbol, match);
        if (!match) {
            encodeSymbol(nextSymbol, predictedSymbol);
        }
        updateContext(nextSymbol);
    }

    private void encodeSingle(final int nextSymbol) throws IOException {
        computeHashes();
        final int modelLowFrequency =
                getSeeLow(getLzpStateLow(getLastHashLow()));
        final int modelHighFrequency =
                getSeeHigh(getLzpStateHigh(getLastHashHigh()));
        boolean match;
        int predictedSymbol;
        if (modelLowFrequency >= modelHighFrequency) {
            predictedSymbol = getLzpPredictedSymbolHigh(getLastHashHigh());
            match = nextSymbol == predictedSymbol;
            updateSeeHistoryHigh(match);
            updateLzpStateHigh(getLastHashHigh(), nextSymbol, match);
            predictedSymbol = getLzpPredictedSymbolLow(getLastHashLow());
            match = nextSymbol == predictedSymbol;
            encodeFlag(modelLowFrequency, match);
            updateSeeLow(getLzpStateLow(getLastHashLow()), match);
            updateLzpStateLow(getLastHashLow(), nextSymbol, match);
        } else {
            predictedSymbol = getLzpPredictedSymbolLow(getLastHashLow());
            match = nextSymbol == predictedSymbol;
            updateSeeHistoryLow(match);
            updateLzpStateLow(getLastHashLow(), nextSymbol, match);
            predictedSymbol = getLzpPredictedSymbolHigh(getLastHashHigh());
            match = nextSymbol == predictedSymbol;
            encodeFlag(modelHighFrequency, match);
            updateSeeHigh(getLzpStateHigh(getLastHashHigh()), match);
            updateLzpStateHigh(getLastHashHigh(), nextSymbol, match);
        }
        if (!match) {
            encodeSymbol(nextSymbol, predictedSymbol);
        }
        updateContext(nextSymbol);
    }

    private void encodeSymbol(final int nextSymbol,
            final int mispredictedSymbol) throws IOException {
        normalize();
        computePpmContext();
        final int index = (getLastPpmContext() << 8) + nextSymbol;
        short cumulativeExclusiveFrequency = 0;
        final int symbolGroup = index >> 4;
        for (int indexPartial = getLastPpmContext() << 4;
                indexPartial < symbolGroup; indexPartial++) {
            cumulativeExclusiveFrequency += rangesGrouped[indexPartial];
        }
        for (int indexPartial = symbolGroup << 4; indexPartial < index;
                indexPartial++) {
            cumulativeExclusiveFrequency += rangesSingle[indexPartial];
        }
        final short mispredictedSymbolFrequency =
                rangesSingle[(getLastPpmContext() << 8) + mispredictedSymbol];
        if (nextSymbol > mispredictedSymbol) {
            cumulativeExclusiveFrequency -= mispredictedSymbolFrequency;
        }
        final int rcHelper = rcRange / (rangesTotal[getLastPpmContext()]
                - mispredictedSymbolFrequency);
        rcBuffer += rcHelper * cumulativeExclusiveFrequency;
        if (rcBuffer < 0) {
            carry = true;
            rcBuffer = rcBuffer & 0x7FFFFFFF;
        }
        rcRange = rcHelper * rangesSingle[index];
        updatePpm(index);
    }

    void flush() throws IOException {
        encodeSkewed(false);
        for (int i = 0; i < 5; i++) {
            outputByte(((int) (rcBuffer >> 23)) & 0xFF);
            rcBuffer <<= 8;
        }
    }

    boolean encode(final long limit) throws IOException {
        boolean endReached = false;
        for (int i = 0; i < limit; i++) {
            final int symbol = inputStream.read();
            if (symbol == -1) {
                endReached = true;
                break;
            }
            encodeSkewed(true);
            if (onlyLowLzp) {
                encodeSingleOnlyLowLzp(symbol);
            } else {
                encodeSingle(symbol);
            }
        }
        return endReached;
    }
}
