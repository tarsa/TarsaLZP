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
package com.github.tarsa.tarsalzp;

import com.github.tarsa.tarsalzp.core.Coder;
import com.github.tarsa.tarsalzp.gui.MainFrame;
import com.github.tarsa.tarsalzp.gui.OptionsBean;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * Entry point.
 *
 * @author Piotr Tarsa
 */
public class Main {

    private void err(final String output) {
        System.err.println(output);
    }

    private void printHelp() {
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
        final OptionsBean options = new OptionsBean();
        err("\tlzpLowContextLength=" + options.getLzpLowContextLength());
        err("\tlzpLowMaskSize=" + options.getLzpLowMaskSize());
        err("\tlzpHighContextLength=" + options.getLzpHighContextLength());
        err("\tlzpHighMaskSize=" + options.getLzpHighMaskSize());
        err("\tppmOrder=" + options.getPpmOrder());
        err("\tppmInit=" + options.getPpmInit());
        err("\tppmStep=" + options.getPpmStep());
        err("\tppmLimit=" + options.getPpmLimit());
        err("Example program invocation (with increased heap size): ");
        err("\tjava -Xmx3500m -jar TarsaLZP.jar encode lzpHighMaskSize=30 "
                + "< input > output");
    }

    private void printError(final String cause) {
        err("Error happened.");
        if (cause != null && !cause.trim().isEmpty()) {
            err(cause);
        }
        err("");
        printHelp();
    }

    private Map<String, String> convertOptions(final String[] args) {
        final Map<String, String> optionsMap = new HashMap<String, String>();
        for (int i = 1; i < args.length; i++) {
            final String arg = args[i];
            final int splitPoint = arg.indexOf("=");
            if (splitPoint == -1) {
                return null;
            }
            if (optionsMap.put(arg.substring(0, splitPoint).toLowerCase(),
                    arg.substring(splitPoint + 1)) != null) {
                return null;
            }
        }
        return optionsMap;
    }

    private static class EncoderCallback implements Coder.Callback {

        private final long fileSize;

        public EncoderCallback(final long fileSize) {
            this.fileSize = fileSize;
        }

        @Override
        public void progressChanged(long processedSymbols) {
            System.err.printf("\r%5.2f", 100. * processedSymbols / fileSize);
            System.err.flush();
        }
    }
    
    private static class DecoderCallback implements Coder.Callback {
        
        private final FileChannel fileChannel;
        private final long fileSize;

        public DecoderCallback(final FileChannel fileChannel, 
                final long fileSize) {
            this.fileChannel = fileChannel;
            this.fileSize = fileSize;
        }

        @Override
        public void progressChanged(long processedSymbols) {
            try {
                System.err.printf("\r%5.2f", 100. * fileChannel.position() 
                        / fileSize);
                System.err.flush();
            } catch (final IOException ex) {
            }
        }
    }

    private void encode(final Map<String, String> optionsMap)
            throws IOException {
        InputStream input = new BufferedInputStream(System.in, 64 * 1024);
        OutputStream output = new BufferedOutputStream(System.out, 64 * 1024);
        boolean standardInput = true;
        boolean standardOutput = true;
        Coder.Callback callback = null;
        final OptionsBean optionsBean = new OptionsBean();
        for (final String option : optionsMap.keySet()) {
            if ("fi".equalsIgnoreCase(option)) {
                input = new BufferedInputStream(new FileInputStream(
                        optionsMap.get(option)), 64 * 1024);
                standardInput = false;
                final long fileSize = new File(optionsMap.get(option)).length();
                callback = new EncoderCallback(fileSize);
            } else if ("fo".equalsIgnoreCase(option)) {
                output = new BufferedOutputStream(new DelayedFileOutputStream(
                        optionsMap.get(option)), 64 * 1024);
                standardOutput = false;
            } else if ("lzpLowContextLength".equalsIgnoreCase(option)) {
                optionsBean.setLzpLowContextLength(
                        Integer.parseInt(optionsMap.get(option)));
            } else if ("lzpLowMaskSize".equalsIgnoreCase(option)) {
                optionsBean.setLzpLowMaskSize(
                        Integer.parseInt(optionsMap.get(option)));
            } else if ("lzpHighContextLength".equalsIgnoreCase(option)) {
                optionsBean.setLzpHighContextLength(
                        Integer.parseInt(optionsMap.get(option)));
            } else if ("lzpHighMaskSize".equalsIgnoreCase(option)) {
                optionsBean.setLzpHighMaskSize(
                        Integer.parseInt(optionsMap.get(option)));
            } else if ("ppmOrder".equalsIgnoreCase(option)) {
                optionsBean.setPpmOrder(
                        Integer.parseInt(optionsMap.get(option)));
            } else if ("ppmInit".equalsIgnoreCase(option)) {
                optionsBean.setPpmInit(
                        Integer.parseInt(optionsMap.get(option)));
            } else if ("ppmStep".equalsIgnoreCase(option)) {
                optionsBean.setPpmStep(
                        Integer.parseInt(optionsMap.get(option)));
            } else if ("ppmLimit".equalsIgnoreCase(option)) {
                optionsBean.setPpmLimit(
                        Integer.parseInt(optionsMap.get(option)));
            } else {
                printError("Not suitable or unknown option: " + option);
                return;
            }
        }
        final Options options = optionsBean.toOptions();
        if (options == null) {
            printError("Wrong encoding options combination.");
            return;
        }
        Coder.encode(input, output, callback, 64 * 1024, options);
        output.flush();
        if (callback != null) {
            System.err.println("\rCompleted!");
        }
        if (!standardInput) {
            input.close();
        }
        if (!standardOutput) {
            output.close();
        }
    }

    private void decode(final Map<String, String> optionsMap) 
            throws FileNotFoundException, IOException {
        InputStream input = new BufferedInputStream(System.in, 64 * 1024);
        OutputStream output = new BufferedOutputStream(System.out,
                64 * 1024);
        boolean standardInput = true;
        boolean standardOutput = true;
        Coder.Callback callback = null;
        for (final String option : optionsMap.keySet()) {
            if ("fi".equalsIgnoreCase(option)) {
                final FileInputStream fileInputStream = new FileInputStream(
                        optionsMap.get(option));
                input = new BufferedInputStream(fileInputStream, 64 * 1024);
                standardInput = false;
                final long fileSize = new File(optionsMap.get(option)).length();
                final FileChannel fileChannel = fileInputStream.getChannel();
                callback = new DecoderCallback(fileChannel, fileSize);
            } else if ("fo".equalsIgnoreCase(option)) {
                output = new BufferedOutputStream(
                        new DelayedFileOutputStream(optionsMap.get(option)),
                        64 * 1024);
                standardOutput = false;
            } else {
                printError("Not suitable or unknown option: " + option);
                return;
            }
        }
        Coder.decode(input, output, callback, 64 * 1024);
        output.flush();
        final boolean allDecoded = input.read() == -1;
        if (!standardInput) {
            input.close();
        }
        if (!standardOutput) {
            output.close();
        }
        if (!allDecoded) {
            throw new IOException("Not entire input was decoded.");
        }
    }

    private void showOptions(final Map<String, String> optionsMap) 
            throws FileNotFoundException, IOException {
        InputStream input = System.in;
        boolean standardInput = true;
        for (final String option : optionsMap.keySet()) {
            if ("fi".equalsIgnoreCase(option)) {
                input = new FileInputStream(optionsMap.get(option));
                standardInput = false;
            } else {
                printError("Not suitable or unknown option: " + option);
                return;
            }
        }
        final Options options = Coder.getOptions(input);
        err(options.toString());
        if (!standardInput) {
            input.close();
        }
    }

    private void dispatchCommand(final String[] args) throws IOException {
        final String command = args[0];
        final Map<String, String> optionsMap = convertOptions(args);
        if (optionsMap == null) {
            printError("Duplicated or wrongly formatted options.");
        } else if ("encode".equalsIgnoreCase(command)) {
            encode(optionsMap);
        } else if ("decode".equalsIgnoreCase(command)) {
            decode(optionsMap);
        } else if ("showOptions".equalsIgnoreCase(command)) {
            showOptions(optionsMap);
        } else if ("gui".equalsIgnoreCase(command)) {
            printError("`gui` command do not expect options.");
        } else {
            printError("Unknown command: " + command);
        }
    }

    private void run(final String[] args) {
        err("TarsaLZP");
        err("Author: Piotr Tarsa");
        err("");
        if (args.length == 0) {
            printHelp();
            MainFrame.main(args);
        } else {
            try {
                dispatchCommand(args);
            } catch (final OutOfMemoryError e) {
                System.err.println("Out of memory error - try increasing heap "
                        + "size or lowering mask sizes.");
            } catch (final Exception e) {
                System.err.println("Exception thrown: " + e.getMessage());
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        new Main().run(args);
    }
}
