/*
 * Copyright 2017 Jari Hämäläinen / https://github.com/nuumio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.nuumio.netsync;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.PrintStream;
import java.io.PrintWriter;

import fi.nuumio.netsync.server.ServerStartException;
import fi.nuumio.netsync.server.SyncServer;
import fi.nuumio.netsync.util.ServerSettings;

/**
 * Executable NetSyncServer.
 */
public class NetSyncServer {
    private static final HelpFormatter formatter = new HelpFormatter();

    private NetSyncServer() {
        // Not to be constructed
    }

    public static void main(final String[] args) {
        final ServerSettings settings = new ServerSettings();
        final Options options = new Options();
        final Option port = Option.builder("p").argName("port")
                .hasArg()
                .longOpt("port")
                .desc("bind to given port")
                .build();
        options.addOption(port);
        final CommandLineParser parser = new DefaultParser();
        CommandLine line = null;
        try {
            line = parser.parse(options, args);
        } catch (final ParseException e) {
            System.err.println("Parsing failed.  Reason: " + e.getMessage());
            printUsage(System.err, options);
            System.exit(1);
        }
        if (line.hasOption("port")) {
            try {
                settings.setServerPort(Integer.valueOf(line.getOptionValue("port")));
            } catch (final NumberFormatException e) {
                System.err.println("Cannot make integer from " + line.getOptionValue("port"));
                printUsage(System.err, options);
                System.exit(2);
            }
        }

        final SyncServer server = new SyncServer(settings);
        try {
            server.start();
        } catch (final ServerStartException e) {
            System.err.println("Failed to start server");
            e.printStackTrace();
        }

        synchronized (server) {
            while (server.isRunning()) {
                try {
                    server.wait(5000);
                } catch (final InterruptedException e) {
                    System.out.println("Stopping: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    server.stop();
                }
            }
        }
    }

    private static void printUsage(final PrintStream stream, final Options options) {
        final PrintWriter writer = new PrintWriter(stream);
        formatter.printUsage(writer, 72, "NetSyncServer", options);
        writer.flush();
        writer.close();
    }
}
