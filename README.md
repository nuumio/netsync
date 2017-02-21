# NetSync
NetSync is a tool for your synchronization needs. It was created from a need to get somekind of synchronization done between (Java) nodes in a network where broadcast and multicast didn't work and so a client/server solution was needed.

Basic idea is this:
1. All clients register to server
	```
    SyncClient client = new SyncClient("server", clientId, settings);
    client.start();
    ```
2. All clients join a SyncGroup
	```
    SyncGroup group = client.createGroup(groupId, groupToken);
    group.join();
    ```
3. All clients (optionally) wait for enough members to join
	```
    boolean gotThem = group.waitMembers(SyncGroup.MemberCount.atLeast(group, count), timeout);
    ```
4. All clients wait for others in the group to arrive at same SyncPoint
	```
    boolean synced = group.waitSync(syncPoint, SyncType.ALL, timeout);
    ```
5. More waiting in different SyncPoints...
6. All clients leave the group and stop.
	```
    group.leave();
    client.stop();
    ```
For a full example code how to create your own client code see `NetSyncClientExample` class and it's `clientExample()` method.

## Installation
TODO: Getting jars when binaries are available...

## Building

Clone repo and build:
```
$ git clone https://github.com/nuumio/netsync.git
$ cd netsync
$ ./gradlew build
```

## Usage
Run `NetSyncServer` (get commons-cli-1.3.1.jar first to `locallibs` directory, see [Download Apache Commons CLI](https://commons.apache.org/proper/commons-cli/download_cli.cgi)):
```
$ ./runserver.sh
```

Test server by running `NetSyncClientExample` in two separate terminals.
```
$ ./runclient.sh localhost client-1
```
```
$ ./runclient.sh localhost client-2
```

For a full example code how to create your own client code see `NetSyncClientExample` class and it's `clientExample()` method.

## License

The Apache License, Version 2.0