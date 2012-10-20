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
function newOptionsBean() {
    var valid = true;
    var lzpLowContextLength = 4;
    var lzpLowMaskSize = 24;
    var lzpHighContextLength = 8;
    var lzpHighMaskSize = 27;
    var ppmOrder = 2;
    var ppmInit = 1;
    var ppmStep = 60;
    var ppmLimit = 30000;
    var listeners = [];

    var self = {};

    self.addChangeListener = function (listener) {
        listeners.push(listener);
    };

    self.removeChangeListener = function (listener) {
        var index = listeners.indexOf(listener);
        if (index != -1) {
            listeners.splice(index, 1);
        }
    };

    function firePropertyChange(propertyName) {
        listeners.forEach(function (listener) {
            listener(propertyName);
        });
    }

    self.isValid = function () {
        return valid;
    };

    self.getLzpLowContextLength = function () {
        return lzpLowContextLength;
    };

    self.getLzpLowMaskSize = function () {
        return lzpLowMaskSize;
    };

    self.getLzpHighContextLength = function () {
        return lzpHighContextLength;
    };

    self.getLzpHighMaskSize = function () {
        return lzpHighMaskSize;
    };

    self.getPpmOrder = function () {
        return ppmOrder;
    };

    self.getPpmInit = function () {
        return ppmInit;
    };

    self.getPpmStep = function () {
        return ppmStep;
    };

    self.getPpmLimit = function () {
        return ppmLimit;
    };

    self.setLzpLowContextLength = function (_lzpLowContextLength) {
        if (lzpLowContextLength != _lzpLowContextLength) {
            lzpLowContextLength = _lzpLowContextLength;
            firePropertyChange("lzpLowContextLength");
        }
    };

    self.setLzpLowMaskSize = function (_lzpLowMaskSize) {
        if (lzpLowMaskSize != _lzpLowMaskSize) {
            lzpLowMaskSize = _lzpLowMaskSize;
            firePropertyChange("lzpLowMaskSize");
        }
    };

    self.setLzpHighContextLength = function (_lzpHighContextLength) {
        if (lzpHighContextLength != _lzpHighContextLength) {
            lzpHighContextLength = _lzpHighContextLength;
            firePropertyChange("lzpHighContextLength");
        }
    };

    self.setLzpHighMaskSize = function (_lzpHighMaskSize) {
        if (lzpHighMaskSize != _lzpHighMaskSize) {
            lzpHighMaskSize = _lzpHighMaskSize;
            firePropertyChange("lzpHighMaskSize");
        }
    };

    self.setPpmOrder = function (_ppmOrder) {
        if (ppmOrder != _ppmOrder) {
            ppmOrder = _ppmOrder;
            firePropertyChange("ppmOrder");
        }
    };

    self.setPpmInit = function (_ppmInit) {
        if (ppmInit != _ppmInit) {
            ppmInit = _ppmInit;
            firePropertyChange("ppmInit");
        }
    };

    self.setPpmStep = function (_ppmStep) {
        if (ppmStep != _ppmStep) {
            ppmStep = _ppmStep;
            firePropertyChange("ppmStep");
        }
    };

    self.setPpmLimit = function (_ppmLimit) {
        if (ppmLimit != _ppmLimit) {
            ppmLimit = _ppmLimit;
            firePropertyChange("ppmLimit");
        }
    };

    self.addChangeListener(function (propertyName) {
        if (propertyName != "valid") {
            valid = self.toOptions() != null;
            firePropertyChange("valid");
        }
    });

    self.toOptions = function() {
        return newOptions(lzpLowContextLength, lzpLowMaskSize, lzpHighContextLength, lzpHighMaskSize, ppmOrder, ppmInit,
            ppmStep, ppmLimit);
    };

    return self;
}