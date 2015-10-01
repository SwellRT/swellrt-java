# SwellRT native Java client

This is a native Java client of the SwellRT API.

## Build and Run

This is a Maven project, so dependencies are automatically resolved.

In order to open or to create a collaborative data model you must to create a session and then,
to open a live channel to the server as follows:

```
      Session session = Session.create("http://localhost:9898", "tim@local.net", "tim");

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

      channel.openModel("local.net/s+y7Ukjg1ktpA", new ChannelOperationCallback() {

        @Override
        public void onSuccess(Model model) {
            // model operations
        }

        @Override
        public void onFailure() {
        }
      });

```

Please, check `Session.java` for examples.

## Licensing

SwellRT and this Client are provided under the Apache License 2.0. Please check additional licenses for declared dependecies in pom.xml file.


