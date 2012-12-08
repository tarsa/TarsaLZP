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
function newStateTable() {
    var stateTable = new Uint8Array(512);
    var LimitX = 20;
    var LimitY = 20;
    var p = 0;
    var freqMask = new Int32Array((LimitX + 1) * (LimitY + 1) * 3 * 3);

    function _divisor(a, b) {
        return (Lg2.nLog2(b) >> 3) + (Lg2.nLog2(1950) >> 3) - (12 << 11);
    }

    function _repeated(a, b) {
        return (b > 0 && _divisor(a, b) > (1200))
            ? (((a + 1) * 1950) / _divisor(a, b)) >> 0 : a + 1;
    }

    function _opposite(a, b) {
        return (b > 0 && _divisor(a, b) > (1200))
            ? ((b * 1950) / _divisor(a, b)) >> 0 : b;
    }

    function _initStates(x, y, h1, h0) {
        x = Math.min(x, LimitX);
        y = Math.min(y, LimitY);
        var index = ((y * (LimitX + 1) + x) * 3 + h1) * 3 + h0;
        if (freqMask[index] == -1) {
            freqMask[index] = p;
            var c = p++;
            stateTable[c * 2 + 0] = _initStates(_repeated(x, y),
                _opposite(x, y), h0, 0);
            stateTable[c * 2 + 1] = _initStates(_opposite(y, x),
                _repeated(y, x), h0, 1);
        }
        return freqMask[index];
    }

    fillArray(freqMask, -1);
    _initStates(0, 0, 2, 2);
    return stateTable;
}