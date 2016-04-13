# SwellRT Java Client

This is the native Java client for SwellRT.

SwellRT is a client-server framework to develop real-time applications.

Servers store "collaborative objects" which stores data shared between "participants".

Apps use the SwellRT client to allow "particpants" connect to "collaborative objects".

Participants can read and listen for changes in collaborative objects on real-time.

Use this project directly to develop a SwellRT client app or package it as Jar to include it as dependency in a new project.

The SwellRT Java client project has some examples that can be run:

## Configuring Maven dependencies

To use the client or run the example you must get dependencies from SwellRT's main project:

*At this moment, SwellRT's Maven dependencies are not published on Internet, so you have to build and install them manually as follows:*

```
git clone git@github.com:P2Pvalue/swellrt.git

cd swellrt/

ant get-third-party-depenencies

// build the dependency Jar
ant -f build-swellrt.xml compile  dist-swellrt-client-commons

// install dependency in your local Maven repo
ant -f build-swellrt.xml swellrt-mvn-install-client-commons
```

This tasks will generate and install the dependencies in your local Maven repository.

Take note of the version from the generated Jar artifact `build/swellrt/swellrt-client-commons-XX.YY.ZZ-alpha.jar` and update the `pom.xml` of the Java client project as follows:

```
<dependency>
  <groupId>org.swellrt</groupId>
  <artifactId>swellrt-client-commons</artifactId>
  <version>XX.YY.ZZ-alpha</version>
</dependency>
```

## Run Chat example

We include a simple cmd-line "Chat". By default, it uses the public SwellRT demo server at "https://demo.swellrt.org"

Open two different terminals and execute:

```
./run-chat.sh
```

Just write and get messages between terminals. Use Ctrl+C to end.

You can read the chat example source code in `org.swellrt.java.examples.SimpleChat.java`.



## Understanding the SwellRT API


### Start a session

Sessions manage the participant authentication on a SwellRT server.

```
Session session = Session.create("http://localhost:9898", "tim@local.net", "tim");
```

### Open a channel

Channels manage live communications with the server. To open a channel you must provide the "domain" of your collaborative objects, the current session, and a listener to handle communication events:

```
  Channel channel = Channel.open("local.net", session, new ConnectionListener() {

    @Override
    public void onReconnect() {
      System.out.println("Reconnected to server");
    }

    @Override
    public void onDisconnect() {
      System.out.println("Disconnected from server");
    }

    @Override
    public void onConnect() {
      System.out.println("Connected to server");
    }
  });
```

### Creating collaborative objects

```
  channel.createModel(new ChannelOperationCallback() {

    @Override
    public void onSuccess(Model cObject) {

          // Get the object's Id
          objectId = cObject.getWaveId().serialise();
    }

    @Override
    public void onFailure() {
    }
  });

```

## Opening collaborative objects


```
  channel.openModel("demo.swellrt.org/s+y7Ukjg1ktpA", new ChannelOperationCallback() {

    @Override
    public void onSuccess(Model cObject) {

    }

    @Override
    public void onFailure() {
    }
  });

```

## Using collaborative objects

Visit the [GitHub wiki](https://github.com/P2Pvalue/swellrt/wiki/Collaborative-Data-Models) for more information.

## License

SwellRT and this Client are provided under the Apache License 2.0.
