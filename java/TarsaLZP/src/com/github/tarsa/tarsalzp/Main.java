package com.github.tarsa.tarsalzp;

import com.github.tarsa.tarsalzp.core.Coder;
import com.github.tarsa.tarsalzp.gui.MainFrame;
import com.github.tarsa.tarsalzp.gui.OptionsBean;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    private void printBanner() {
        err("TarsaLZP");
        err("Author: Piotr Tarsa");
    }

    private void printHelp() {
        err("Syntax: command [option=value]*");
        err("Commands:");
        err("\tgui           - show GUI");
        err("\tencode        - encode input");
        err("\tdecode        - decode compressed stream");
        err("\tshowOptions   - read and show compression options only");
        err("General options:");
        err("\tfi=fileName   - read from file `fileName` (all modes)");
        err("\tfo=fileName   - write to file `fileName` (encode and decode)");
        err("Encoding only options:");
        err("\tlzpLowContextLength=value");
        err("\tlzpLowMaskSize=value");
        err("\tlzpHighContextLength=value");
        err("\tlzpHighMaskSize=value");
        err("\tppmOrder=value");
        err("\tppmInit=value");
        err("\tppmStep=value");
        err("\tppmLimit=value");
        err("Default compression options:");
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
        if (cause != null && !cause.trim().isEmpty()) {
            err(cause);
        }
        printBanner();
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

    private void encode(final Map<String, String> optionsMap)
            throws IOException {
        InputStream input = System.in;
        OutputStream output = System.out;
        final OptionsBean optionsBean = new OptionsBean();
        for (final String option : optionsMap.keySet()) {
            if ("fi".equalsIgnoreCase(option)) {
                input = new FileInputStream(optionsMap.get(option));
            } else if ("fo".equalsIgnoreCase(option)) {
                output = new DelayedFileOutputStream(
                        optionsMap.get(option));
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
        Coder.encode(input, output, null, 64 * 1024, options);
        output.flush();
    }

    private void run(final String[] args) throws IOException {
        if (args.length == 0) {
            printHelp();
        } else {
            final String command = args[0];
            Map<String, String> optionsMap = convertOptions(args);
            if (optionsMap == null) {
                printError("Duplicated or wrongly formatted options.");
            } else if ("encode".equalsIgnoreCase(command)) {
                encode(optionsMap);
            } else if ("decode".equalsIgnoreCase(command)) {
                InputStream input = System.in;
                OutputStream output = System.out;
                for (final String option : optionsMap.keySet()) {
                    if ("fi".equalsIgnoreCase(option)) {
                        input = new FileInputStream(optionsMap.get(option));
                    } else if ("fo".equalsIgnoreCase(option)) {
                        output = new DelayedFileOutputStream(
                                optionsMap.get(option));
                    } else {
                        printError("Not suitable or unknown option: " + option);
                        return;
                    }
                }
                Coder.decode(input, output, null, 64 * 1024);
                output.flush();
            } else if ("showOptions".equalsIgnoreCase(command)) {
                InputStream input = System.in;
                for (final String option : optionsMap.keySet()) {
                    if ("fi".equalsIgnoreCase(option)) {
                        input = new FileInputStream(optionsMap.get(option));
                    } else {
                        printError("Not suitable or unknown option: " + option);
                        return;
                    }
                }
                final Options options = Coder.getOptions(input);
                err(options.toString());
            } else if ("gui".equalsIgnoreCase(command)) {
                printError("`gui` command do not expect options.");
            } else {
                printError("Unknown command: " + command);
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        if (args.length == 1 && "gui".equalsIgnoreCase(args[0])) {
            MainFrame.main(args);
        } else {
            try {
                new Main().run(args);
            } catch (final OutOfMemoryError e) {
                System.err.println("Out of memory error - try increasing heap "
                        + "size or lowering mask sizes.");
            } catch (final Exception e) {
                System.err.println("Exception thrown: " + e.getMessage());
            }
        }
    }
}
