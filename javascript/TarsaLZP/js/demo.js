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
var literalCoderOrder = document.getElementById("literalCoderOrder");
var literalCoderInit = document.getElementById("literalCoderInit");
var literalCoderStep = document.getElementById("literalCoderStep");
var literalCoderLimit = document.getElementById("literalCoderLimit");
var encode = document.getElementById("encode");
var decode = document.getElementById("decode");
var showOptions = document.getElementById("showOptions");

var optionsBean = newOptionsBean();
lzpLowContextLength.value = optionsBean.getLzpLowContextLength();
lzpLowMaskSize.value = optionsBean.getLzpLowMaskSize();
lzpHighContextLength.value = optionsBean.getLzpHighContextLength();
lzpHighMaskSize.value = optionsBean.getLzpHighMaskSize();
literalCoderOrder.value = optionsBean.getLiteralCoderOrder();
literalCoderInit.value = optionsBean.getLiteralCoderInit();
literalCoderStep.value = optionsBean.getLiteralCoderStep();
literalCoderLimit.value = optionsBean.getLiteralCoderLimit();

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

literalCoderOrder.addEventListener("input", function () {
    optionsBean.setLiteralCoderOrder(parseInt(literalCoderOrder.value));
}, false);

literalCoderInit.addEventListener("input", function () {
    optionsBean.setLiteralCoderInit(parseInt(literalCoderInit.value));
}, false);

literalCoderStep.addEventListener("input", function () {
    optionsBean.setLiteralCoderStep(parseInt(literalCoderStep.value));
}, false);

literalCoderLimit.addEventListener("input", function () {
    optionsBean.setLiteralCoderLimit(parseInt(literalCoderLimit.value));
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
    setButtonsState(false);
    var reader = new FileReader();
    reader.onload = function (event) {
        inputStream = newArrayInputStream(new Uint8Array(event.target.result));
    }
    reader.onloadend = function (/*event*/) {
        setButtonsState(true);
    }
    reader.readAsArrayBuffer(file);
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
            var optionsDescription = "Options:\nLZP Low Context Length: "
                + options.getLzpLowContextLength()
                + "\nLZP Low Mask Size: " + options.getLzpLowMaskSize()
                + "\nLZP High Context Length: "
                + options.getLzpHighContextLength() + "\nLZP High Mask Size: "
                + options.getLzpHighMaskSize() + "\nLiteral Coder Order: "
                + options.getLiteralCoderOrder() + "\nLiteral Coder Init: "
                + options.getLiteralCoderInit() + "\nLiteral Coder Step: "
                + options.getLiteralCoderStep() + "\nLiteral Coder Limit: "
                + options.getLiteralCoderLimit();
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
        var rawChunks = outputStream.getChunksArray();
        var chunks = [];
        rawChunks.forEach(function (rawChunk) {
          chunks.push(rawChunk.truncatedBuffer());
        });
        var blob = new Blob(chunks, { type: 'example/binary' });
        saveAs(blob, "filename");
    }
}
