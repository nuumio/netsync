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

import fi.nuumio.netsync.client.ClientStartFailedException;
import fi.nuumio.netsync.client.GroupJoinException;
import fi.nuumio.netsync.client.GroupLeaveException;
import fi.nuumio.netsync.client.GroupSyncException;
import fi.nuumio.netsync.client.SyncClient;
import fi.nuumio.netsync.client.SyncGroup;
import fi.nuumio.netsync.protocol.message.group.SyncType;
import fi.nuumio.netsync.util.ClientSettings;
import fi.nuumio.netsync.util.NodeId;
import fi.nuumio.netsync.util.Token;

/**
 * Example client application.
 * <p>
 * Shows basic usage of SyncClient and SyncGroup.
 */
public class NetSyncClientExample {
    private static final HelpFormatter formatter = new HelpFormatter();

    private NetSyncClientExample() {
        // Not to be constructed
    }

    public static void main(final String[] args) {
        final ClientSettings settings = new ClientSettings();
        final Options options = new Options();
        final Option port = Option.builder("p").argName("port")
                .hasArg()
                .longOpt("port")
                .desc("connect to given server port")
                .build();
        final Option timeoutOpt = Option.builder("t").argName("timeout")
                .hasArg()
                .longOpt("timeout")
                .desc("client wait timeout in ms")
                .build();
        options.addOption(port);
        options.addOption(timeoutOpt);
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
        long timeout = 30000;
        if (line.hasOption("timeout")) {
            try {
                timeout = Long.valueOf(line.getOptionValue("timeout"));
            } catch (final NumberFormatException e) {
                System.err.println("Cannot make long from " + line.getOptionValue("port"));
                printUsage(System.err, options);
                System.exit(3);
            }
        }

        final String[] progArgs = line.getArgs();
        if (null == progArgs || progArgs.length != 6) {
            printUsage(System.err, options);
            System.exit(4);
        }
        int members = 0;
        try {
            members = Integer.parseInt(progArgs[5]);
        } catch (final NumberFormatException e) {
            System.err.println("Cannot make integer from " + progArgs[5]);
            printUsage(System.err, options);
            System.exit(5);
        }

        int ret = clientExample(progArgs[0], progArgs[1], settings, progArgs[2], progArgs[3],
                progArgs[4], members, timeout);
        System.exit(ret);
    }

    private static int clientExample(final String server, final String clientId,
                                     final ClientSettings settings,
                                     final String groupName, final String groupToken,
                                     final String syncPoint,
                                     final int memberCount, final long timeout) {
        final SyncClient client = new SyncClient(server, new NodeId(clientId), settings);
        try {
            client.start();
        } catch (final ClientStartFailedException e) {
            System.err.println("Failed to start client");
            e.printStackTrace();
            return 6;
        }
        final SyncGroup group = client.createGroup(new NodeId(groupName), new Token(groupToken));
        try {
            group.join();
        } catch (final GroupJoinException e) {
            System.err.println("Failed to join group");
            e.printStackTrace();
            return 7;
        }

        final boolean gotMembers;
        try {
            gotMembers = group.waitMembers(SyncGroup.MemberCount.atLeast(group, memberCount), timeout);
        } catch (GroupSyncException e) {
            System.err.println("Failed to wait for other members");
            e.printStackTrace();
            return 8;
        }
        if (!gotMembers) {
            System.err.println("Didn't get others to join in time. Needed at least " +
                    memberCount + " members");
        } else {
            boolean synced = false;
            try {
                synced = group.waitSync(syncPoint, SyncType.ALL, timeout);
            } catch (final GroupSyncException e) {
                System.err.println("Failed to create sync point");
                e.printStackTrace();
            }
            if (synced) {
                System.out.println("Success! All members synced!");
            } else {
                System.out.println("FAIL! All members didn't get to sync point!");
            }
        }
        if (group.isJoined()) {
            try {
                group.leave();
            } catch (final GroupLeaveException e) {
                System.err.println("Failed to leave group");
                e.printStackTrace();
            }
        }
        client.stop();
        return 0;
    }

    private static void printUsage(final PrintStream stream, final Options options) {
        final PrintWriter writer = new PrintWriter(stream);
        formatter.printHelp(writer, 72, "NetSyncClientExample server client_name group_name group_token " +
                "sync_point member_count", "", options, 4, 4, "", true);
        writer.flush();
        writer.close();
    }
}
