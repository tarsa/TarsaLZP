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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

/**
 *
 * @author Piotr Tarsa
 */
public final class ActionBean {

    public enum Action {

        Encode, Decode, ShowOptions
    }
    private transient final PropertyChangeSupport propertyChangeSupport =
            new PropertyChangeSupport(this);
    private Action action = null;
    private OptionsBean optionsBean = null;
    private boolean actionAllowed = false;
    private boolean actionInProgress = false;
    private String inputFilePath;
    private String outputFilePath;
    public static final String PropAction = "action";
    public static final String PropOptionsBean = "optionsBean";
    public static final String PropActionAllowed = "actionAllowed";
    public static final String PropActionInProgress = "actionInProgress";
    public static final String PropInputFilePath = "inputFilePath";
    public static final String PropOutputFilePath = "outputFilePath";
    private final PropertyChangeListener activationListener;

    private class ActivationListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            validate();
        }
    }

    public ActionBean() {
        activationListener = new ActivationListener();
        for (final String prop : new String[]{PropAction, PropActionInProgress,
                    PropInputFilePath, PropOutputFilePath}) {
            propertyChangeSupport.addPropertyChangeListener(prop,
                    activationListener);
        }
    }

    private void validate() {
        boolean localActionAllowed = false;
        localActionAllowed |= action == Action.Decode && inputFilePath != null
                && new File(inputFilePath).isAbsolute();
        localActionAllowed |= action == Action.ShowOptions && inputFilePath
                != null && new File(inputFilePath).isAbsolute();
        localActionAllowed |= action == Action.Encode && inputFilePath != null
                && new File(inputFilePath).isAbsolute() && outputFilePath 
                != null && new File(outputFilePath).isAbsolute() && optionsBean
                != null && optionsBean.isValid();
        localActionAllowed &= !actionInProgress;
        setActionAllowed(localActionAllowed);
    }

    /**
     * Get the value of action
     *
     * @return the value of action
     */
    public Action getAction() {
        return action;
    }

    /**
     * Get the value of optionsBean
     *
     * @return the value of optionsBean
     */
    public OptionsBean getOptionsBean() {
        return optionsBean;
    }

    /**
     * Get the value of actionAllowed
     *
     * @return the value of actionAllowed
     */
    public boolean isActionAllowed() {
        return actionAllowed;
    }

    /**
     * Get the value of actionInProgress
     *
     * @return the value of actionInProgress
     */
    public boolean isActionInProgress() {
        return actionInProgress;
    }

    /**
     * Get the value of inputFilePath
     *
     * @return the value of inputFilePath
     */
    public String getInputFilePath() {
        return inputFilePath;
    }

    /**
     * Get the value of outputFilePath
     *
     * @return the value of outputFilePath
     */
    public String getOutputFilePath() {
        return outputFilePath;
    }

    /**
     * Set the value of action
     *
     * @param action new value of action
     */
    public void setAction(final Action action) {
        final Action oldAction = this.action;
        this.action = action;
        propertyChangeSupport.firePropertyChange(PropAction, oldAction, action);
    }

    /**
     * Set the value of optionsBean
     *
     * @param optionsBean new value of optionsBean
     */
    public void setOptionsBean(final OptionsBean optionsBean) {
        final OptionsBean oldOptionsBean = this.optionsBean;
        if (oldOptionsBean != null) {
            oldOptionsBean.removePropertyChangeListener(activationListener);
        }
        this.optionsBean = optionsBean;
        if (optionsBean != null) {
            optionsBean.addPropertyChangeListener(activationListener);
        }
        propertyChangeSupport.firePropertyChange(PropOptionsBean,
                oldOptionsBean, optionsBean);
    }

    /**
     * Set the value of actionAllowed
     *
     * @param actionAllowed new value of actionAllowed
     */
    public void setActionAllowed(final boolean actionAllowed) {
        final boolean oldActionAllowed = this.actionAllowed;
        this.actionAllowed = actionAllowed;
        propertyChangeSupport.firePropertyChange(PropActionAllowed,
                oldActionAllowed, actionAllowed);
    }

    /**
     * Set the value of actionInProgress
     *
     * @param actionInProgress new value of actionInProgress
     */
    public void setActionInProgress(final boolean actionInProgress) {
        final boolean oldActionInProgress = this.actionInProgress;
        this.actionInProgress = actionInProgress;
        propertyChangeSupport.firePropertyChange(PropActionInProgress,
                oldActionInProgress, actionInProgress);
    }

    /**
     * Set the value of inputFilePath
     *
     * @param inputFilePath new value of inputFilePath
     */
    public void setInputFilePath(final String inputFilePath) {
        final String oldInputFilePath = this.inputFilePath;
        this.inputFilePath = inputFilePath;
        propertyChangeSupport.firePropertyChange(PropInputFilePath,
                oldInputFilePath, inputFilePath);
    }

    /**
     * Set the value of outputFilePath
     *
     * @param outputFilePath new value of outputFilePath
     */
    public void setOutputFilePath(final String outputFilePath) {
        final String oldOutputFilePath = this.outputFilePath;
        this.outputFilePath = outputFilePath;
        propertyChangeSupport.firePropertyChange(PropOutputFilePath,
                oldOutputFilePath, outputFilePath);
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
}
