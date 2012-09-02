package com.github.tarsa.tarsalzp.core;

import com.github.tarsa.tarsalzp.Options;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author Piotr Tarsa
 */
public final class Decoder extends Common {

    private long rcBuffer;
    private long rcRange;
    boolean started;

    public Decoder(final InputStream inputStream,
            final OutputStream outputStream, final Options options) {
        super(inputStream, outputStream, options);
        started = false;
    }

    private void init() throws IOException {
        rcBuffer = 0;
        for (int i = 0; i < 4; i++) {
            final int inputByte = inputStream.read();
            if (inputByte == -1) {
                throw new IOException("Unexpected end of file.");
            }
            rcBuffer <<= 8;
            rcBuffer |= inputByte;
        }
        rcRange = 0xFFFFFFFFl;
        started = true;
    }

    private void normalize() throws IOException {
        while (rcRange < 0x01000000) {
            final int inputByte = inputStream.read();
            if (inputByte == -1) {
                throw new IOException("Unexpected end of file.");
            }
            rcBuffer <<= 8;
            rcBuffer |= inputByte;
            rcRange <<= 8;
        }
    }

    boolean decodeFlag(final int probability) throws IOException {
        normalize();
        final long rcHelper = (rcRange >> 16) * probability;
        if (rcHelper > rcBuffer) {
            rcRange = rcHelper;
            return true;
        } else {
            rcRange -= rcHelper;
            rcBuffer -= rcHelper;
            return false;
        }
    }

    boolean decodeSkewed() throws IOException {
        normalize();
        if (rcBuffer < rcRange - 1) {
            rcRange--;
            return true;
        } else {
            rcBuffer = 0;
            rcRange = 1;
            return false;
        }
    }

    int decodeSingle() throws IOException {
        computeHashes();
        final int model4Frequency = getSEE4(getLZPState4(getLastHash4()));
        final int model8Frequency = getSEE8(getLZPState8(getLastHash8()));
        boolean match;
        int predictedSymbol;
        int nextSymbol;
        if (model4Frequency >= model8Frequency) {
            predictedSymbol = getLZPPredictedSymbol4(getLastHash4());
            match = decodeFlag(model4Frequency);
            updateSEE4(getLZPState4(getLastHash4()), match);
            if (match) {
                nextSymbol = predictedSymbol;
            } else {
                nextSymbol = decodeSymbol(predictedSymbol);
            }
            updateLZPState4(getLastHash4(), nextSymbol, match);
            predictedSymbol = getLZPPredictedSymbol8(getLastHash8());
            match = nextSymbol == predictedSymbol;
            updateSEEHistory8(match);
            updateLZPState8(getLastHash8(), nextSymbol, match);
        } else {
            predictedSymbol = getLZPPredictedSymbol8(getLastHash8());
            match = decodeFlag(model8Frequency);
            updateSEE8(getLZPState8(getLastHash8()), match);
            if (match) {
                nextSymbol = predictedSymbol;
            } else {
                nextSymbol = decodeSymbol(predictedSymbol);
            }
            updateLZPState8(getLastHash8(), nextSymbol, match);
            predictedSymbol = getLZPPredictedSymbol4(getLastHash4());
            match = nextSymbol == predictedSymbol;
            updateSEEHistory4(match);
            updateLZPState4(getLastHash4(), nextSymbol, match);
        }
        updateContext(nextSymbol);
        return nextSymbol;
    }

    int decodeSymbol(final int mispredictedSymbol) throws IOException {
        normalize();
        computePpmContext();
        final short mispredictedSymbolFrequency = 
                rangesSingle[(getLastPpmContext() << 8) + mispredictedSymbol];
        rcRange = rcRange / (rangesTotal[getLastPpmContext()]
                - mispredictedSymbolFrequency);
        rangesSingle[(getLastPpmContext() << 8) + mispredictedSymbol] = 0;
        rangesGrouped[((getLastPpmContext() << 8) + mispredictedSymbol) >> 4] -=
                mispredictedSymbolFrequency;
        long rcHelper = rcBuffer / rcRange;
        final long cumulativeFrequency = rcHelper;
        int index;
        for (index = getLastPpmContext() << 4; rcHelper >= rangesGrouped[index]; 
                index++) {
            rcHelper -= rangesGrouped[index];
        }
        for (index <<= 4; rcHelper >= rangesSingle[index]; index++) {
            rcHelper -= rangesSingle[index];
        }
        rcBuffer -= (cumulativeFrequency - rcHelper) * rcRange;
        rcRange *= rangesSingle[index];
        final int nextSymbol = index & 0xff;
        rangesSingle[(getLastPpmContext() << 8) + mispredictedSymbol] =
                mispredictedSymbolFrequency;
        rangesGrouped[((getLastPpmContext() << 8) + mispredictedSymbol) >> 4] +=
                mispredictedSymbolFrequency;
        rangesSingle[index] += ppmStep;
        rangesGrouped[index >> 4] += ppmStep;
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
        return nextSymbol;
    }

    public boolean decode(final long limit) throws IOException {
        if (!started) {
            init();
        }
        boolean endReached = false;
        for (int i = 0; i < limit; i++) {
            endReached = !decodeSkewed();
            if (!endReached) {
                final int symbol = decodeSingle();
                outputStream.write(symbol);
            } else {
                break;
            }
        }
        return endReached;
    }
}
