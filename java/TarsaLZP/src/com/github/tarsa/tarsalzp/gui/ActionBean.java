package com.github.tarsa.tarsalzp.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 * @author Piotr Tarsa
 */
public class ActionBean {

    public enum Action {

        Encode, Decode, ShowOptions
    }
    private transient final PropertyChangeSupport propertyChangeSupport =
            new PropertyChangeSupport(this);
    private Action action = null;
    private OptionsBean optionsBean = null;
    private boolean valid = false;
    public static final String PropAction = "action";
    public static final String PropOptionsBean = "optionsBean";
    public static final String PropValid = "valid";
    private final PropertyChangeListener validationListener;

    public ActionBean() {
        validationListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                if (OptionsBean.PropValid.equals(evt.getPropertyName())) {
                    validate();
                }
            }
        };
    }

    private void validate() {
        setValid(action == Action.Decode || action == Action.ShowOptions
                || (action == Action.Encode && optionsBean != null
                && optionsBean.isValid()));
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
     * Get the value of valid
     *
     * @return the value of valid
     */
    public boolean isValid() {
        return valid;
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
        validate();
    }

    /**
     * Set the value of optionsBean
     *
     * @param optionsBean new value of optionsBean
     */
    public void setOptionsBean(final OptionsBean optionsBean) {
        final OptionsBean oldOptionsBean = this.optionsBean;
        if (oldOptionsBean != null) {
            oldOptionsBean.removePropertyChangeListener(validationListener);
        }
        this.optionsBean = optionsBean;
        if (optionsBean != null) {
            optionsBean.addPropertyChangeListener(validationListener);
        }
        propertyChangeSupport.firePropertyChange(PropOptionsBean,
                oldOptionsBean, optionsBean);
    }

    /**
     * Set the value of valid
     *
     * @param valid new value of valid
     */
    public void setValid(final boolean valid) {
        final boolean oldValid = this.valid;
        this.valid = valid;
        propertyChangeSupport.firePropertyChange(PropValid, oldValid, valid);
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
