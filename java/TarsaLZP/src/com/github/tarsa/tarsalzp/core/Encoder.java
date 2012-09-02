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
    private int xFFRunLength = 0;
    private int lastOutputByte = 0;
    private boolean delay = false;
    private boolean carry = false;

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

    private void encodeFlag(final int probability, final boolean match)
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

    private void encodeSkewed(final boolean flag) throws IOException {
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
        updatePpm(index);
    }

    void flush() throws IOException {
        encodeSkewed(false);
        for (int i = 0; i < 5; i++) {
            outputByte(((int) (rcBuffer >> 24)) & 0xFF);
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
