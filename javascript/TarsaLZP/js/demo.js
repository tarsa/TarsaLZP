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
var lzpLowContextLength = document.getElementById("lzpLowContextLength");
var lzpLowMaskSize = document.getElementById("lzpLowMaskSize");
var lzpHighContextLength = document.getElementById("lzpHighContextLength");
var lzpHighMaskSize = document.getElementById("lzpHighMaskSize");
var ppmOrder = document.getElementById("ppmOrder");
var ppmInit = document.getElementById("ppmInit");
var ppmStep = document.getElementById("ppmStep");
var ppmLimit = document.getElementById("ppmLimit");
var encode = document.getElementById("encode");
var decode = document.getElementById("decode");
var showOptions = document.getElementById("showOptions");

var optionsBean = newOptionsBean();
lzpLowContextLength.value = optionsBean.getLzpLowContextLength();
lzpLowMaskSize.value = optionsBean.getLzpLowMaskSize();
lzpHighContextLength.value = optionsBean.getLzpHighContextLength();
lzpHighMaskSize.value = optionsBean.getLzpHighMaskSize();
ppmOrder.value = optionsBean.getPpmOrder();
ppmInit.value = optionsBean.getPpmInit();
ppmStep.value = optionsBean.getPpmStep();
ppmLimit.value = optionsBean.getPpmLimit();

function statusToString(valid) {
    return valid ? "Valid" : "Invalid";
}

document.getElementById("status").innerText =
    statusToString(optionsBean.isValid());

lzpLowContextLength.addEventListener("input", function () {
    optionsBean.setLzpLowContextLength(parseInt(lzpLowContextLength.value));
}, false);

lzpLowMaskSize.addEventListener("input", function () {
    optionsBean.setLzpLowMaskSize(parseInt(lzpLowMaskSize.value));
}, false);

lzpHighContextLength.addEventListener("input", function () {
    optionsBean.setLzpHighContextLength(parseInt(lzpHighContextLength.value));
}, false);

lzpHighMaskSize.addEventListener("input", function () {
    optionsBean.setLzpHighMaskSize(parseInt(lzpHighMaskSize.value));
}, false);

ppmOrder.addEventListener("input", function () {
    optionsBean.setPpmOrder(parseInt(ppmOrder.value));
}, false);

ppmInit.addEventListener("input", function () {
    optionsBean.setPpmInit(parseInt(ppmInit.value));
}, false);

ppmStep.addEventListener("input", function () {
    optionsBean.setPpmStep(parseInt(ppmStep.value));
}, false);

ppmLimit.addEventListener("input", function () {
    optionsBean.setPpmLimit(parseInt(ppmLimit.value));
}, false);

optionsBean.addChangeListener(function (propertyName) {
    if (propertyName == "valid") {
        document.getElementById("status").innerText =
            statusToString(optionsBean.isValid());
    }
});


var inputFileChooser = document.getElementById("inputFileChooser");

var loadButton = document.getElementById("loadButton");
var processButton = document.getElementById("processButton");
var saveButton = document.getElementById("saveButton");

inputFileChooser.onchange = resetStreams;

loadButton.onclick = loadContents;
processButton.onclick = processData;
saveButton.onclick = saveResults;

function setButtonsState(enabled) {
    loadButton.disabled = !enabled;
    processButton.disabled = !enabled;
    saveButton.disabled = !enabled;
}

var inputStream = null;
var outputStream = null;

function resetStreams() {
    inputStream = null;
    outputStream = null;
}

function loadContents() {
    var file = inputFileChooser.files[0];
    if (!file)
        return;
    var reader = new FileReader();
    setButtonsState(false);
    reader.readAsArrayBuffer(file);
    reader.onload = loaded;
    reader.onloadend = function (/*event*/) {
        setButtonsState(true);
    }
}

function loaded(event) {
    inputStream = newArrayInputStream(new Uint8Array(event.target.result));
}

function processData() {
    var showTime = true;
    setButtonsState(false);
    var startTime = new Date().getTime();
    var options;
    try {
        if (encode.checked) {
            outputStream = newChunksArrayOutputStream();
            options = optionsBean.toOptions();
            if (options == null) {
                alert("Invalid options.");
                showTime = false;
            } else {
                Coder.encode(inputStream, outputStream, null, 100000, options);
            }
            outputStream.flush();
        } else if (decode.checked) {
            outputStream = newChunksArrayOutputStream();
            Coder.decode(inputStream, outputStream, null, 100000);
            outputStream.flush();
        } else if (showOptions.checked) {
            options = Coder.getOptions(inputStream);
            var optionsDescription = "Options:\n"
                + "LZP Low Context Length: "
                + options.getLzpLowContextLength() + "\n"
                + "LZP Low Mask Size: " + options.getLzpLowMaskSize() + "\n"
                + "LZP High Context Length: "
                + options.getLzpHighContextLength() + "\n"
                + "LZP High Mask Size: " + options.getLzpHighMaskSize() + "\n"
                + "PPM Order: " + options.getPpmOrder() + "\n"
                + "PPM Init: " + options.getPpmInit() + "\n"
                + "PPM Step: " + options.getPpmStep() + "\n"
                + "PPM Limit: " + options.getPpmLimit();
            alert(optionsDescription);
            showTime = false;
        } // nothing else should happen
    } catch (error) {
        alert(error);
        showTime = false;
    }
    if (showTime) {
        alert("Time taken: " + (new Date().getTime() - startTime));
    }
    setButtonsState(true);
}

function saveResults() {
    if (outputStream != null) {
        var bb = new BlobBuilder;
        outputStream.getChunksArray().forEach(function (chunk) {
            bb.append(chunk.truncatedBuffer());
        });
        var blob = bb.getBlob("example/binary");
        saveAs(blob, "filename");
    }
}
