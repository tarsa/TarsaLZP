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

#ifndef NO_PREFETCH
#pragma message "Prefetching enabled"
#else
#pragma message "Prefetching disabled"
#endif

#ifndef NO_VECTOR
#pragma message "Vector optimizations enabled"
#else
#pragma message "Vector optimizations disabled"
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <io.h>
#include <fcntl.h>
#endif

#include "coder.h"
#include "err.h"
#include "options.h"
#include "streams.h"

void printHelp() {
    err("Syntax: command [option=value]*");
    err("Commands:");
    err("\t[no command]  - print help and show GUI");
    err("\tencode        - encode input");
    err("\tdecode        - decode compressed stream");
    err("\tshowOptions   - read and show compression options only");
    err("General options:");
    err("\tfi=fileName   - read from file `fileName` (all modes)");
    err("\tfo=fileName   - write to file `fileName` (encode and decode)");
    err("Encoding only options (with default values):");
    __options__();
    fprintf(stderr, "\tlzpLowContextLength=%d\n", lzpLowContextLength);
    fprintf(stderr, "\tlzpLowMaskSize=%d\n", lzpLowMaskSize);
    fprintf(stderr, "\tlzpHighContextLength=%d\n", lzpHighContextLength);
    fprintf(stderr, "\tlzpHighMaskSize=%d\n", lzpHighMaskSize);
    fprintf(stderr, "\tppmOrder=%d\n", ppmOrder);
    fprintf(stderr, "\tppmInit=%d\n", ppmInit);
    fprintf(stderr, "\tppmStep=%d\n", ppmStep);
    fprintf(stderr, "\tppmLimit=%d\n", ppmLimit);
}

void checkParameter(bool * const isSetFlag) {
    if (*isSetFlag) {
        err("Duplicated parameter.");
        exit(EXIT_FAILURE);
    } else {
        *isSetFlag = true;
    }
}

bool checkOptionAndParseIntOrDie(char const * const argument,
        char const * const optionName, bool * const optionFlag,
        int32_t * const option) {
    if (strncmp(argument, optionName, strlen(optionName)) != 0
            || argument[strlen(optionName)] != '=') {
        return false;
    }
    checkParameter(optionFlag);
    char const * const valueAsText = argument + strlen(optionName) + 1;
    *option = atoi(valueAsText);
    return true;
}

void mainEncode(int const argc, char const * const * const argv) {
    __options__();
    bool inputFileSet = false;
    bool outputFileSet = false;
    bool lzpLowContextLengthSet = false;
    bool lzpLowMaskSizeSet = false;
    bool lzpHighContextLengthSet = false;
    bool lzpHighMaskSizeSet = false;
    bool ppmOrderSet = false;
    bool ppmInitSet = false;
    bool ppmStepSet = false;
    bool ppmLimitSet = false;

    input = stdin;
    output = stdout;

    for (int32_t i = 2; i < argc; i++) {
        char const * const arg = argv[i];
        if (strncmp(arg, "fi=", 3) == 0) {
            checkParameter(&inputFileSet);
            input = fopen(arg + 3, "rb");
            if (input == NULL) {
                err("Can't open input file.");
                exit(EXIT_FAILURE);
            }
        } else if (strncmp(arg, "fo=", 3) == 0) {
            checkParameter(&outputFileSet);
            outputWriteFilename = arg + 3;
            outputWriteToFile = true;
        } else {
            char const * const * const optionsNames = (char const * const []){
                "lzpLowContextLength", "lzpLowMaskSize", "lzpHighContextLength",
                "lzpHighMaskSize", "ppmOrder", "ppmInit", "ppmStep", "ppmLimit"
            };
            bool * const * const optionsFlags = (bool * const []){
                &lzpLowContextLengthSet, &lzpLowMaskSizeSet,
                &lzpHighContextLengthSet, &lzpHighMaskSizeSet,
                &ppmOrderSet, &ppmInitSet, &ppmStepSet, &ppmLimitSet
            };
            int32_t * const * const options = (int32_t * const []){
                &lzpLowContextLength, &lzpLowMaskSize, &lzpHighContextLength,
                &lzpHighMaskSize, &ppmOrder, &ppmInit, &ppmStep, &ppmLimit
            };
            bool foundOption = false;
            for (int32_t i = 0; i < 8 && !foundOption; i++) {
                foundOption |= checkOptionAndParseIntOrDie(arg, optionsNames[i],
                        optionsFlags[i], options[i]);
            }
            if (!foundOption) {
                if (strchr(arg, '=') != NULL) {
                    *strchr(arg, '=') = 0;
                }
                fprintf(stderr, "Not suitable or unknown option: %s\n", arg);
                exit(EXIT_FAILURE);
            }
        }
    }
    if (!optionsValid()) {
        err("Wrong encoding options combination.");
        exit(EXIT_FAILURE);
    }
#ifdef _WIN32
    if (input == stdin) {
        setmode(fileno(stdin), O_BINARY);
    }
    if (output == stdout) {
        setmode(fileno(stdout), O_BINARY);
    }
#endif        
    coderEncode();
    outputFlush();
    fclose(input);
    fclose(output);
    err("Completed!");
}

void mainDecode(int const argc, char const * const * const argv) {
    __options__();
    bool inputFileSet = false;
    bool outputFileSet = false;

    input = stdin;
    output = stdout;

    for (int32_t i = 2; i < argc; i++) {
        char const * const arg = argv[i];
        if (strncmp(arg, "fi=", 3) == 0) {
            checkParameter(&inputFileSet);
            input = fopen(arg + 3, "rb");
            if (input == NULL) {
                err("Can't open input file.");
                exit(EXIT_FAILURE);
            }
        } else if (strncmp(arg, "fo=", 3) == 0) {
            checkParameter(&outputFileSet);
            outputWriteFilename = arg + 3;
            outputWriteToFile = true;
        } else {
            fprintf(stderr, "Not suitable or unknown option: %s\n", arg);
            exit(EXIT_FAILURE);
        }
    }
#ifdef _WIN32
    if (input == stdin) {
        setmode(fileno(stdin), O_BINARY);
    }
    if (output == stdout) {
        setmode(fileno(stdout), O_BINARY);
    }
#endif        
    coderDecode();
    outputFlush();
    bool const allDecoded = inputRead() == -1;
    fclose(input);
    fclose(output);
    if (!allDecoded) {
        err("Not entire input was decoded.");
        exit(EXIT_FAILURE);
    }
    err("Completed!");
}

void showOptions(int const argc, char const * const * const argv) {
    __options__();
    bool inputFileSet = false;

    input = stdin;

    for (int32_t i = 2; i < argc; i++) {
        char const * const arg = argv[i];
        if (strncmp(arg, "fi=", 3) == 0) {
            checkParameter(&inputFileSet);
            input = fopen(arg + 3, "rb");
            if (input == NULL) {
                err("Can't open input file.");
                exit(EXIT_FAILURE);
            }
        } else {
            fprintf(stderr, "Not suitable or unknown option: %s\n", arg);
            exit(EXIT_FAILURE);
        }
    }
#ifdef _WIN32
    if (input == stdin) {
        setmode(fileno(stdin), O_BINARY);
    }
#endif        
    coderReadOptions();
    fclose(input);
    fprintf(stderr, "lzpLowContextLength=%d\n", lzpLowContextLength);
    fprintf(stderr, "lzpLowMaskSize=%d\n", lzpLowMaskSize);
    fprintf(stderr, "lzpHighContextLength=%d\n", lzpHighContextLength);
    fprintf(stderr, "lzpHighMaskSize=%d\n", lzpHighMaskSize);
    fprintf(stderr, "ppmOrder=%d\n", ppmOrder);
    fprintf(stderr, "ppmInit=%d\n", ppmInit);
    fprintf(stderr, "ppmStep=%d\n", ppmStep);
    fprintf(stderr, "ppmLimit=%d\n", ppmLimit);
}

void dispatchCommand(int const argc, char const * const * const argv) {
    __inputReader__();
    __outputWriter__();
    char const * const command = argv[1];
    if (strcmp(command, "encode") == 0) {
        mainEncode(argc, argv);
    } else if (strcmp(command, "decode") == 0) {
        mainDecode(argc, argv);
    } else if (strcmp(command, "showOptions") == 0) {
        showOptions(argc, argv);
    } else {
        fprintf(stderr, "Unknown command: %s\n", command);
    }
}

int main(int const argc, char const * const * const argv) {
    err("TarsaLZP");
    err("Author: Piotr Tarsa");
    err("");
    if (argc == 1) {
        printHelp();
    } else {
        dispatchCommand(argc, argv);
    }
    return (EXIT_SUCCESS);
}
