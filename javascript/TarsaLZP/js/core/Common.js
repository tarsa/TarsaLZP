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
function newCommon(options) {
    var lzpLowContextLength = options.getLzpLowContextLength();
    var lzpLowMaskSize = options.getLzpLowMaskSize();
    var lzpHighContextLength = options.getLzpHighContextLength();
    var lzpHighMaskSize = options.getLzpHighMaskSize();
    var ppmOrder = options.getPpmOrder();
    var ppmInit = options.getPpmInit();
    var ppmStep = options.getPpmStep();
    var ppmLimit = options.getPpmLimit();

    /// LZP section
    var lzpLowCount = 1 << lzpLowMaskSize;
    var lzpHighCount = 1 << lzpHighMaskSize;
    var lzpLowMask = lzpLowCount - 1;
    var lzpHighMask = lzpHighCount - 1;
    var lzpLow = new Uint16Array(lzpLowCount);
    var lzpHigh;
    fillArray(lzpLow, 0xffb5);
    var onlyLowLzp = lzpLowContextLength == lzpHighContextLength
        && lzpLowMaskSize == lzpHighMaskSize;
    if (onlyLowLzp) {
        lzpHigh = null;
    } else {
        lzpHigh = new Uint16Array(lzpHighCount);
        fillArray(lzpHigh, 0xffb5);
    }
    // PPM section
    var ppmMaskSize = ppmOrder * 8;
    var ppmMask = (1 << ppmMaskSize) - 1;
    var rangesSingle = new Int16Array(1 << ppmMaskSize + 8);
    fillArray(rangesSingle, ppmInit);
    var rangesGrouped = new Int16Array(1 << ppmMaskSize + 4);
    fillArray(rangesGrouped, ppmInit * 16);
    var rangesTotal = new Int16Array(1 << ppmMaskSize);
    fillArray(rangesTotal, ppmInit * 256);
    // SEE section
    var seeLow = new Int16Array(16 * 256);
    fillArray(seeLow, 0x4000);
    var seeHigh = new Int16Array(16 * 256);
    fillArray(seeHigh, 0x4000);
    var historyLow = 0;
    var historyLowMask = 15;
    var historyHigh = 0;
    var historyHighMask = 15;
    // Contexts and hashes
    var lastPpmContext = 0;
    var context = new Long(0, 0, 0, 0);
    var hashLow = 0;
    var hashHigh = 0;
    // Calculating states
    var StateTable = [
        [1, 241, 0, 0, 0, 0],
        [2, 227, 1, 0, 1, 1],
        [3, 213, 2, 0, 2, 2],
        [4, 202, 3, 0, 3, 3],
        [5, 45, 4, 0, 4, 4],
        [6, 23, 5, 0, 5, 5],
        [7, 23, 6, 0, 6, 6],
        [8, 23, 7, 0, 7, 7],
        [9, 23, 8, 0, 8, 8],
        [10, 23, 9, 0, 9, 9],
        [11, 23, 10, 0, 10, 10],
        [12, 23, 11, 0, 11, 11],
        [13, 23, 12, 0, 12, 12],
        [14, 23, 13, 0, 13, 13],
        [15, 42, 14, 0, 14, 14],
        [16, 42, 15, 0, 15, 15],
        [17, 42, 16, 0, 16, 16],
        [18, 42, 17, 0, 17, 17],
        [19, 42, 18, 0, 18, 18],
        [20, 42, 19, 0, 19, 19],
        [20, 21, 20, 0, 20, 20],
        [22, 44, 6, 0, 6, 21],
        [8, 23, 7, 0, 7, 22],
        [24, 25, 4, 0, 4, 23],
        [6, 23, 5, 0, 5, 24],
        [26, 46, 4, 1, 5, 25],
        [27, 45, 5, 1, 6, 26],
        [28, 45, 6, 1, 7, 27],
        [29, 45, 7, 1, 8, 28],
        [30, 45, 8, 1, 9, 29],
        [31, 23, 9, 1, 10, 30],
        [32, 23, 10, 1, 11, 31],
        [33, 23, 11, 1, 12, 32],
        [34, 23, 12, 1, 13, 33],
        [35, 23, 13, 1, 14, 34],
        [36, 42, 14, 1, 15, 35],
        [37, 42, 15, 1, 16, 36],
        [38, 42, 16, 1, 17, 37],
        [39, 42, 17, 1, 18, 38],
        [40, 42, 18, 1, 19, 39],
        [41, 42, 19, 1, 20, 40],
        [41, 21, 20, 1, 21, 41],
        [43, 44, 5, 0, 5, 42],
        [7, 23, 6, 0, 6, 43],
        [24, 25, 4, 0, 4, 44],
        [26, 46, 4, 1, 5, 45],
        [47, 66, 4, 2, 6, 46],
        [48, 65, 5, 2, 7, 47],
        [49, 65, 6, 2, 8, 48],
        [50, 45, 7, 2, 9, 49],
        [51, 45, 8, 2, 10, 50],
        [52, 45, 9, 2, 11, 51],
        [53, 45, 10, 2, 12, 52],
        [54, 45, 11, 2, 13, 53],
        [55, 45, 12, 2, 14, 54],
        [56, 45, 13, 2, 15, 55],
        [57, 63, 14, 2, 16, 56],
        [58, 63, 15, 2, 17, 57],
        [59, 42, 16, 2, 18, 58],
        [60, 42, 17, 2, 19, 59],
        [61, 42, 18, 2, 20, 60],
        [62, 42, 19, 2, 21, 61],
        [62, 21, 20, 2, 22, 62],
        [64, 25, 5, 1, 6, 63],
        [28, 45, 6, 1, 7, 64],
        [47, 66, 4, 2, 6, 65],
        [67, 86, 4, 3, 7, 66],
        [68, 85, 5, 3, 8, 67],
        [69, 65, 6, 3, 9, 68],
        [70, 65, 7, 3, 10, 69],
        [71, 65, 8, 3, 11, 70],
        [72, 45, 9, 3, 12, 71],
        [73, 45, 10, 3, 13, 72],
        [74, 45, 11, 3, 14, 73],
        [75, 45, 12, 3, 15, 74],
        [76, 45, 13, 3, 16, 75],
        [77, 63, 14, 3, 17, 76],
        [78, 63, 15, 3, 18, 77],
        [79, 63, 16, 3, 19, 78],
        [80, 63, 17, 3, 20, 79],
        [81, 63, 18, 3, 21, 80],
        [82, 63, 19, 3, 22, 81],
        [82, 83, 20, 3, 23, 82],
        [84, 25, 6, 1, 7, 83],
        [29, 45, 7, 1, 8, 84],
        [67, 86, 4, 3, 7, 85],
        [87, 103, 4, 4, 8, 86],
        [88, 102, 5, 4, 9, 87],
        [89, 85, 6, 4, 10, 88],
        [90, 65, 7, 4, 11, 89],
        [91, 65, 8, 4, 12, 90],
        [92, 65, 9, 4, 13, 91],
        [93, 65, 10, 4, 14, 92],
        [94, 65, 11, 4, 15, 93],
        [95, 45, 12, 4, 16, 94],
        [96, 45, 13, 4, 17, 95],
        [97, 63, 14, 4, 18, 96],
        [98, 63, 15, 4, 19, 97],
        [99, 63, 16, 4, 20, 98],
        [100, 63, 17, 4, 21, 99],
        [101, 63, 18, 4, 22, 100],
        [101, 83, 20, 4, 24, 101],
        [87, 103, 4, 4, 8, 102],
        [104, 107, 4, 5, 9, 103],
        [105, 106, 4, 4, 8, 104],
        [88, 102, 5, 4, 9, 105],
        [104, 107, 4, 5, 9, 106],
        [108, 189, 4, 6, 10, 107],
        [109, 110, 3, 4, 7, 108],
        [105, 106, 4, 4, 8, 109],
        [108, 111, 3, 5, 8, 110],
        [112, 173, 3, 6, 9, 111],
        [113, 114, 2, 4, 6, 112],
        [109, 110, 3, 4, 7, 113],
        [112, 115, 2, 5, 7, 114],
        [112, 116, 2, 6, 8, 115],
        [117, 158, 2, 7, 9, 116],
        [118, 119, 1, 4, 5, 117],
        [113, 114, 2, 4, 6, 118],
        [117, 120, 1, 5, 6, 119],
        [117, 121, 1, 6, 7, 120],
        [117, 122, 1, 7, 8, 121],
        [117, 123, 1, 8, 9, 122],
        [124, 147, 1, 9, 10, 123],
        [125, 126, 0, 4, 4, 124],
        [118, 119, 1, 4, 5, 125],
        [124, 127, 0, 5, 5, 126],
        [124, 128, 0, 6, 6, 127],
        [124, 129, 0, 7, 7, 128],
        [124, 130, 0, 8, 8, 129],
        [124, 131, 0, 9, 9, 130],
        [124, 132, 0, 10, 10, 131],
        [124, 133, 0, 11, 11, 132],
        [124, 134, 0, 12, 12, 133],
        [124, 135, 0, 13, 13, 134],
        [136, 139, 0, 14, 14, 135],
        [137, 138, 0, 5, 5, 136],
        [125, 126, 0, 4, 4, 137],
        [124, 128, 0, 6, 6, 138],
        [136, 140, 0, 15, 15, 139],
        [136, 141, 0, 16, 16, 140],
        [136, 142, 0, 17, 17, 141],
        [136, 143, 0, 18, 18, 142],
        [136, 144, 0, 19, 19, 143],
        [145, 144, 0, 20, 20, 144],
        [137, 146, 0, 6, 6, 145],
        [124, 129, 0, 7, 7, 146],
        [124, 148, 1, 10, 11, 147],
        [124, 149, 1, 11, 12, 148],
        [124, 150, 1, 12, 13, 149],
        [124, 151, 1, 13, 14, 150],
        [136, 152, 1, 14, 15, 151],
        [136, 153, 1, 15, 16, 152],
        [136, 154, 1, 16, 17, 153],
        [136, 155, 1, 17, 18, 154],
        [136, 156, 1, 18, 19, 155],
        [136, 157, 1, 19, 20, 156],
        [145, 157, 1, 20, 21, 157],
        [117, 159, 2, 8, 10, 158],
        [117, 160, 2, 9, 11, 159],
        [117, 161, 2, 10, 12, 160],
        [117, 162, 2, 11, 13, 161],
        [117, 163, 2, 12, 14, 162],
        [117, 164, 2, 13, 15, 163],
        [165, 167, 2, 14, 16, 164],
        [125, 166, 1, 5, 6, 165],
        [117, 121, 1, 6, 7, 166],
        [165, 168, 2, 15, 17, 167],
        [136, 169, 2, 16, 18, 168],
        [136, 170, 2, 17, 19, 169],
        [136, 171, 2, 18, 20, 170],
        [136, 172, 2, 19, 21, 171],
        [145, 172, 2, 20, 22, 172],
        [112, 174, 3, 7, 10, 173],
        [112, 175, 3, 8, 11, 174],
        [117, 176, 3, 9, 12, 175],
        [117, 177, 3, 10, 13, 176],
        [117, 178, 3, 11, 14, 177],
        [117, 179, 3, 12, 15, 178],
        [117, 180, 3, 13, 16, 179],
        [165, 181, 3, 14, 17, 180],
        [165, 182, 3, 15, 18, 181],
        [165, 183, 3, 16, 19, 182],
        [165, 184, 3, 17, 20, 183],
        [165, 185, 3, 18, 21, 184],
        [165, 186, 3, 19, 22, 185],
        [187, 186, 3, 20, 23, 186],
        [125, 188, 1, 6, 7, 187],
        [117, 122, 1, 7, 8, 188],
        [112, 190, 4, 7, 11, 189],
        [112, 191, 4, 8, 12, 190],
        [112, 192, 4, 9, 13, 191],
        [112, 193, 4, 10, 14, 192],
        [112, 194, 4, 11, 15, 193],
        [117, 195, 4, 12, 16, 194],
        [117, 196, 4, 13, 17, 195],
        [165, 197, 4, 14, 18, 196],
        [165, 198, 4, 15, 19, 197],
        [165, 199, 4, 16, 20, 198],
        [165, 200, 4, 17, 21, 199],
        [165, 201, 4, 18, 22, 200],
        [187, 201, 4, 20, 24, 201],
        [203, 205, 3, 1, 4, 202],
        [204, 65, 4, 1, 5, 203],
        [27, 45, 5, 1, 6, 204],
        [206, 208, 3, 2, 5, 205],
        [207, 85, 4, 2, 6, 206],
        [48, 65, 5, 2, 7, 207],
        [209, 211, 3, 3, 6, 208],
        [210, 102, 4, 3, 7, 209],
        [68, 85, 5, 3, 8, 210],
        [104, 212, 3, 4, 7, 211],
        [108, 111, 3, 5, 8, 212],
        [214, 217, 2, 1, 3, 213],
        [215, 216, 3, 1, 4, 214],
        [204, 65, 4, 1, 5, 215],
        [206, 208, 3, 2, 5, 216],
        [218, 221, 2, 2, 4, 217],
        [219, 220, 3, 2, 5, 218],
        [207, 85, 4, 2, 6, 219],
        [209, 211, 3, 3, 6, 220],
        [222, 225, 2, 3, 5, 221],
        [223, 224, 3, 3, 6, 222],
        [210, 102, 4, 3, 7, 223],
        [104, 212, 3, 4, 7, 224],
        [108, 226, 2, 4, 6, 225],
        [112, 115, 2, 5, 7, 226],
        [228, 231, 1, 1, 2, 227],
        [229, 230, 2, 1, 3, 228],
        [215, 216, 3, 1, 4, 229],
        [218, 221, 2, 2, 4, 230],
        [232, 235, 1, 2, 3, 231],
        [233, 234, 2, 2, 4, 232],
        [219, 220, 3, 2, 5, 233],
        [222, 225, 2, 3, 5, 234],
        [236, 239, 1, 3, 4, 235],
        [237, 238, 2, 3, 5, 236],
        [223, 224, 3, 3, 6, 237],
        [108, 226, 2, 4, 6, 238],
        [112, 240, 1, 4, 5, 239],
        [117, 120, 1, 5, 6, 240],
        [242, 245, 0, 1, 1, 241],
        [243, 244, 1, 1, 2, 242],
        [229, 230, 2, 1, 3, 243],
        [232, 235, 1, 2, 3, 244],
        [246, 249, 0, 2, 2, 245],
        [247, 248, 1, 2, 3, 246],
        [233, 234, 2, 2, 4, 247],
        [236, 239, 1, 3, 4, 248],
        [250, 253, 0, 3, 3, 249],
        [251, 252, 1, 3, 4, 250],
        [237, 238, 2, 3, 5, 251],
        [112, 240, 1, 4, 5, 252],
        [117, 254, 0, 4, 4, 253],
        [124, 127, 0, 5, 5, 254],
        [0, 0, 0, 0, 0, 255]
    ];

    var self = {};

// Contexts and Hashes

    self.updateContext = function (input) {
        context.shl8();
        context.d |= input;
    };

    self.computePpmContext = function () {
        lastPpmContext = context.d & ppmMask;
    };

    self.computeHashes = function () {
        var localContext = Object.create(context);
        var hash = 18652613;
        var i;
        for (i = 0; i < lzpLowContextLength; i++) {
            var newHash = hash;
            newHash &= 0x3fffffff;
            newHash += (hash & 0x1fffffff) << 1;
            newHash &= 0x3fffffff;
            newHash += (hash & 0x03ffffff) << 4;
            newHash &= 0x3fffffff;
            newHash += (hash & 0x007fffff) << 7;
            newHash &= 0x3fffffff;
            newHash += (hash & 0x003fffff) << 8;
            newHash &= 0x3fffffff;
            newHash += (hash & 0x0000003f) << 24;
            newHash &= 0x3fffffff;
            newHash ^= localContext.d & 0xff;
            newHash &= 0x3fffffff;
            hash = newHash;
            localContext.shr8();
        }
        hashLow = hash & lzpLowMask;
        for (i = lzpLowContextLength; i < lzpHighContextLength; i++) {
            var newHash = hash;
            newHash &= 0x3fffffff;
            newHash += (hash & 0x1fffffff) << 1;
            newHash &= 0x3fffffff;
            newHash += (hash & 0x03ffffff) << 4;
            newHash &= 0x3fffffff;
            newHash += (hash & 0x007fffff) << 7;
            newHash &= 0x3fffffff;
            newHash += (hash & 0x003fffff) << 8;
            newHash &= 0x3fffffff;
            newHash += (hash & 0x0000003f) << 24;
            newHash &= 0x3fffffff;
            newHash ^= localContext.d & 0xff;
            newHash &= 0x3fffffff;
            hash = newHash;
            localContext.shr8();
        }
        hashHigh = hash & lzpHighMask;
    };

    self.getLastPpmContext = function () {
        return lastPpmContext;
    };

    self.getLastHashLow = function () {
        return hashLow;
    };

    self.getLastHashHigh = function () {
        return hashHigh;
    };

// Calculating states

    self.getNextState = function (state, match) {
        return StateTable[state][match ? 0 : 1];
    };

// LZP section

    self.isOnlyLowLzp = function() {
        return onlyLowLzp;
    };

    self.getLzpStateLow = function (hash) {
        return (lzpLow[hash] & 0xff00) >> 8;
    };

    self.getLzpStateHigh = function (hash) {
        return (lzpHigh[hash] & 0xff00) >> 8;
    };

    self.getLzpPredictedSymbolLow = function (hash) {
        return lzpLow[hash] & 0xff;
    };

    self.getLzpPredictedSymbolHigh = function (hash) {
        return lzpHigh[hash] & 0xff;
    };

    self.updateLzpStateLow = function (hash, input, match) {
        lzpLow[hash] = (self.getNextState(self.getLzpStateLow(hash), match) << 8) + input;
    };

    self.updateLzpStateHigh = function (hash, input, match) {
        lzpHigh[hash] = (self.getNextState(self.getLzpStateHigh(hash), match) << 8) + input;
    };

// SEE section

    self.getSeeLow = function (state) {
        return seeLow[(historyLow << 8) + state];
    };

    self.getSeeHigh = function (state) {
        return seeHigh[(historyHigh << 8) + state];
    };

    self.updateSeeHistoryLow = function (match) {
        historyLow = ((historyLow << 1) + (match ? 0 : 1)) & historyLowMask;
    };

    self.updateSeeHistoryHigh = function (match) {
        historyHigh = ((historyHigh << 1) + (match ? 0 : 1)) & historyHighMask;
    };

    self.updateSeeLow = function (state, match) {
        var index = (historyLow << 8) + state;
        if (match) {
            seeLow[index] += (((1 << 15) - seeLow[index]) & 0xff80) >> 7;
        } else {
            seeLow[index] -= (seeLow[index] & 0xff80) >> 7;
        }
        self.updateSeeHistoryLow(match);
    };

    self.updateSeeHigh = function (state, match) {
        var index = (historyHigh << 8) + state;
        if (match) {
            seeHigh[index] += (((1 << 15) - seeHigh[index]) & 0xff80) >> 7;
        } else {
            seeHigh[index] -= (seeHigh[index] & 0xff80) >> 7;
        }
        self.updateSeeHistoryHigh(match);
    };

// PPM section

    self.getRangesSingle = function() {
        return rangesSingle;
    };

    self.getRangesGrouped = function() {
        return rangesGrouped;
    };

    self.getRangesTotal = function() {
        return rangesTotal;
    };

    self.rescalePpm = function () {
        var indexCurrent, groupCurrent;
        for (indexCurrent = self.getLastPpmContext() << 8; indexCurrent < (self.getLastPpmContext() + 1) << 8;
             indexCurrent++) {
            rangesSingle[indexCurrent] -= (rangesSingle[indexCurrent] & 0xfffe) >> 1;
        }
        var totalFrequency = 0;
        for (groupCurrent = self.getLastPpmContext() << 4; groupCurrent < (self.getLastPpmContext() + 1) << 4;
             groupCurrent++) {
            var groupFrequency = 0;
            for (indexCurrent = groupCurrent << 4; indexCurrent < (groupCurrent + 1) << 4; indexCurrent++) {
                groupFrequency += rangesSingle[indexCurrent];
            }
            rangesGrouped[groupCurrent] = groupFrequency;
            totalFrequency += groupFrequency;
        }
        rangesTotal[self.getLastPpmContext()] = totalFrequency;
    };

    self.updatePpm = function (index) {
        rangesSingle[index] += ppmStep;
        rangesGrouped[index >> 4] += ppmStep;
        rangesTotal[self.getLastPpmContext()] += ppmStep;
        if (rangesTotal[self.getLastPpmContext()] > ppmLimit) {
            self.rescalePpm();
        }
    };

    return self;
}