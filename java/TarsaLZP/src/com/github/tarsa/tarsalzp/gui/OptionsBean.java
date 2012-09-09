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
    private int ppmOrder = 2;
    private int ppmInit = 1;
    private int ppmStep = 60;
    private int ppmLimit = 30000;
    public static final String PropValid = "valid";
    public static final String PropLzpLowContextLength =
            "lzpLowContextLength";
    public static final String PropLzpLowMaskSize =
            "lzpLowMaskSize";
    public static final String PropLzpHighContextLength =
            "lzpHighContextLength";
    public static final String PropLzpHighMaskSize =
            "lzpHighMaskSize";
    public static final String PropPpmOrder = "ppmOrder";
    public static final String PropPpmInit = "ppmInit";
    public static final String PropPpmStep = "ppmStep";
    public static final String PropPpmLimit = "ppmLimit";

    private class ValidatingListener implements PropertyChangeListener {

        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
            if (!PropValid.equals(evt.getPropertyName())) {
                setValid(Options.create(lzpLowContextLength, lzpLowMaskSize,
                        lzpHighContextLength, lzpHighMaskSize, ppmOrder,
                        ppmInit, ppmStep, ppmLimit) != null);
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
     * Get the value of ppmOrder
     *
     * @return the value of ppmOrder
     */
    public int getPpmOrder() {
        return ppmOrder;
    }

    /**
     * Get the value of ppmInit
     *
     * @return the value of ppmInit
     */
    public int getPpmInit() {
        return ppmInit;
    }

    /**
     * Get the value of ppmStep
     *
     * @return the value of ppmStep
     */
    public int getPpmStep() {
        return ppmStep;
    }

    /**
     * Get the value of ppmLimit
     *
     * @return the value of ppmLimit
     */
    public int getPpmLimit() {
        return ppmLimit;
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
     * Set the value of ppmOrder
     *
     * @param ppmOrder new value of ppmOrder
     */
    public void setPpmOrder(final int ppmOrder) {
        final int oldPpmOrder = this.ppmOrder;
        this.ppmOrder = ppmOrder;
        propertyChangeSupport.firePropertyChange(PropPpmOrder,
                oldPpmOrder, ppmOrder);
    }

    /**
     * Set the value of ppmInit
     *
     * @param ppmInit new value of ppmInit
     */
    public void setPpmInit(final int ppmInit) {
        final int oldPpmInit = this.ppmInit;
        this.ppmInit = ppmInit;
        propertyChangeSupport.firePropertyChange(PropPpmInit,
                oldPpmInit, ppmInit);
    }

    /**
     * Set the value of ppmStep
     *
     * @param ppmStep new value of ppmStep
     */
    public void setPpmStep(final int ppmStep) {
        final int oldPpmStep = this.ppmStep;
        this.ppmStep = ppmStep;
        propertyChangeSupport.firePropertyChange(PropPpmStep,
                oldPpmStep, ppmStep);
    }

    /**
     * Set the value of ppmLimit
     *
     * @param ppmLimit new value of ppmLimit
     */
    public void setPpmLimit(final int ppmLimit) {
        final int oldPpmLimit = this.ppmLimit;
        this.ppmLimit = ppmLimit;
        propertyChangeSupport.firePropertyChange(PropPpmLimit,
                oldPpmLimit, ppmLimit);
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
                lzpHighContextLength, lzpHighMaskSize, ppmOrder, ppmInit,
                ppmStep, ppmLimit);
    }
}
