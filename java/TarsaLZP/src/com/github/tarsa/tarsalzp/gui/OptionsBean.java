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
package com.github.tarsa.tarsalzp.gui;

import com.github.tarsa.tarsalzp.Options;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

/**
 *
 * @author Piotr Tarsa
 */
public final class OptionsBean implements Serializable {

    private static final long serialVersionUID = -30172670080052117L;
    private transient final PropertyChangeSupport propertyChangeSupport =
            new PropertyChangeSupport(this);
    private boolean valid = true;
    private int lzpLowContextLength = 4;
    private int lzpLowMaskSize = 24;
    private int lzpHighContextLength = 8;
    private int lzpHighMaskSize = 27;
    private int literalCoderOrder = 2;
    private int literalCoderInit = 1;
    private int literalCoderStep = 60;
    private int literalCoderLimit = 30000;
    public static final String PropValid = "valid";
    public static final String PropLzpLowContextLength =
            "lzpLowContextLength";
    public static final String PropLzpLowMaskSize =
            "lzpLowMaskSize";
    public static final String PropLzpHighContextLength =
            "lzpHighContextLength";
    public static final String PropLzpHighMaskSize =
            "lzpHighMaskSize";
    public static final String PropLiteralCoderOrder = "literalCoderOrder";
    public static final String PropLiteralCoderInit = "literalCoderInit";
    public static final String PropLiteralCoderStep = "literalCoderStep";
    public static final String PropLiteralCoderLimit = "literalCoderLimit";

    private class ValidatingListener implements PropertyChangeListener {

        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
            if (!PropValid.equals(evt.getPropertyName())) {
                setValid(toOptions() != null);
            }
        }
    }

    public OptionsBean() {
        propertyChangeSupport.addPropertyChangeListener(
                new ValidatingListener());
    }

    /**
     * Get the value of valid
     *
     * @return the value of valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Get the value of lzpLowContextLength
     *
     * @return the value of lzpLowContextLength
     */
    public int getLzpLowContextLength() {
        return lzpLowContextLength;
    }

    /**
     * Get the value of lzpLowMaskSize
     *
     * @return the value of lzpLowMaskSize
     */
    public int getLzpLowMaskSize() {
        return lzpLowMaskSize;
    }

    /**
     * Get the value of lzpHighContextLength
     *
     * @return the value of lzpHighContextLength
     */
    public int getLzpHighContextLength() {
        return lzpHighContextLength;
    }

    /**
     * Get the value of lzpHighMaskSize
     *
     * @return the value of lzpHighMaskSize
     */
    public int getLzpHighMaskSize() {
        return lzpHighMaskSize;
    }

    /**
     * Get the value of literalCoderOrder
     *
     * @return the value of literalCoderOrder
     */
    public int getLiteralCoderOrder() {
        return literalCoderOrder;
    }

    /**
     * Get the value of literalCoderInit
     *
     * @return the value of literalCoderInit
     */
    public int getLiteralCoderInit() {
        return literalCoderInit;
    }

    /**
     * Get the value of literalCoderStep
     *
     * @return the value of literalCoderStep
     */
    public int getLiteralCoderStep() {
        return literalCoderStep;
    }

    /**
     * Get the value of literalCoderLimit
     *
     * @return the value of literalCoderLimit
     */
    public int getLiteralCoderLimit() {
        return literalCoderLimit;
    }

    /**
     * Set the valud of valid
     *
     * @param valid new value of valid
     */
    private void setValid(final boolean valid) {
        final boolean oldValid = this.valid;
        this.valid = valid;
        propertyChangeSupport.firePropertyChange(PropValid, oldValid, valid);
    }

    /**
     * Set the value of lzpLowContextLength
     *
     * @param lzpLowContextLength new value of lzpLowContextLength
     */
    public void setLzpLowContextLength(final int lzpLowContextLength) {
        final int oldLzpLowContextLength = this.lzpLowContextLength;
        this.lzpLowContextLength = lzpLowContextLength;
        propertyChangeSupport.firePropertyChange(PropLzpLowContextLength,
                oldLzpLowContextLength, lzpLowContextLength);
    }

    /**
     * Set the value of lzpLowMaskSize
     *
     * @param lzpLowMaskSize new value of lzpLowMaskSize
     */
    public void setLzpLowMaskSize(final int lzpLowMaskSize) {
        final int oldLzpLowMaskSize = this.lzpLowMaskSize;
        this.lzpLowMaskSize = lzpLowMaskSize;
        propertyChangeSupport.firePropertyChange(PropLzpLowMaskSize,
                oldLzpLowMaskSize, lzpLowMaskSize);
    }

    /**
     * Set the value of lzpHighContextLength
     *
     * @param lzpHighContextLength new value of lzpHighContextLength
     */
    public void setLzpHighContextLength(final int lzpHighContextLength) {
        final int oldLzpHighContextLength = this.lzpHighContextLength;
        this.lzpHighContextLength = lzpHighContextLength;
        propertyChangeSupport.firePropertyChange(PropLzpHighContextLength,
                oldLzpHighContextLength, lzpHighContextLength);
    }

    /**
     * Set the value of lzpHighMaskSize
     *
     * @param lzpHighMaskSize new value of lzpHighMaskSize
     */
    public void setLzpHighMaskSize(final int lzpHighMaskSize) {
        final int oldLzpHighMaskSize = this.lzpHighMaskSize;
        this.lzpHighMaskSize = lzpHighMaskSize;
        propertyChangeSupport.firePropertyChange(PropLzpHighMaskSize,
                oldLzpHighMaskSize, lzpHighMaskSize);
    }

    /**
     * Set the value of literalCoderOrder
     *
     * @param literalCoderOrder new value of literalCoderOrder
     */
    public void setLiteralCoderOrder(final int literalCoderOrder) {
        final int oldLiteralCoderOrder = this.literalCoderOrder;
        this.literalCoderOrder = literalCoderOrder;
        propertyChangeSupport.firePropertyChange(PropLiteralCoderOrder,
                oldLiteralCoderOrder, literalCoderOrder);
    }

    /**
     * Set the value of literalCoderInit
     *
     * @param literalCoderInit new value of literalCoderInit
     */
    public void setLiteralCoderInit(final int literalCoderInit) {
        final int oldLiteralCoderInit = this.literalCoderInit;
        this.literalCoderInit = literalCoderInit;
        propertyChangeSupport.firePropertyChange(PropLiteralCoderInit,
                oldLiteralCoderInit, literalCoderInit);
    }

    /**
     * Set the value of literalCoderStep
     *
     * @param literalCoderStep new value of literalCoderStep
     */
    public void setLiteralCoderStep(final int literalCoderStep) {
        final int oldLiteralCoderStep = this.literalCoderStep;
        this.literalCoderStep = literalCoderStep;
        propertyChangeSupport.firePropertyChange(PropLiteralCoderStep,
                oldLiteralCoderStep, literalCoderStep);
    }

    /**
     * Set the value of literalCoderLimit
     *
     * @param literalCoderLimit new value of literalCoderLimit
     */
    public void setLiteralCoderLimit(final int literalCoderLimit) {
        final int oldLiteralCoderLimit = this.literalCoderLimit;
        this.literalCoderLimit = literalCoderLimit;
        propertyChangeSupport.firePropertyChange(PropLiteralCoderLimit,
                oldLiteralCoderLimit, literalCoderLimit);
    }

    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    public void addPropertyChangeListener(
            final PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    public void removePropertyChangeListener(
            final PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Converts bean to immutable structure.
     *
     * @return immutable structure.
     */
    public Options toOptions() {
        return Options.create(lzpLowContextLength, lzpLowMaskSize,
                lzpHighContextLength, lzpHighMaskSize, literalCoderOrder, 
                literalCoderInit, literalCoderStep, literalCoderLimit);
    }
}
