package org.swellrt.java.examples;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.swellrt.java.WaveWebSocketClient.ConnectionListener;
import org.swellrt.java.client.Channel;
import org.swellrt.java.client.Channel.ChannelOperationCallback;
import org.swellrt.java.client.Session;
import org.swellrt.model.generic.ListType;
import org.swellrt.model.generic.ListType.Listener;
import org.swellrt.model.generic.Model;
import org.swellrt.model.generic.StringType;
import org.swellrt.model.generic.Type;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;

/**
 *
 * Chat example implemented with SwellRT.
 *
 *
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class SimpleChat {

  static final String COBJECT_ID_FILE = "./chat.oid";

  /** Wait for a opened channel with SwellRT server */
  CountDownLatch channelReadyLatch = new CountDownLatch(1);

  /** Wait for the SwellRT data model to be opened **/
  CountDownLatch cObjectReadyLatch = new CountDownLatch(1);

  /** The collaborative object's id */
  String cObjectId = null;

  /** The collaborative object **/
  Model cObject = null;

  /** The domain for c. objects */
  String cObjectDomain = "local.net";

  /** The real-time communication channel with the SwellRT server */
  Channel channel = null;

  public void log(String s) {
    System.out.println(s);
  }

  /**
   * Checks if current folder contains a file with the collaborative object's id
   * supporting the chat. If true, returns de object id. Otherwise returns null.
   *
   * @return
   */
  protected String getCollaborativeObjectId() {

    File f = new File(COBJECT_ID_FILE);
    if (f.canRead()) {

      try {
        FileInputStream fis = new FileInputStream(f);

        @SuppressWarnings("resource")
        Scanner fs = new Scanner(fis).useDelimiter("\\n");

        String objectId = fs.next();

        fs.close();

        return objectId;

      } catch (FileNotFoundException e) {

        System.err.println(e.getMessage());
        return null;

      } catch (IOException e) {
        System.err.println(e.getMessage());
        return null;
      }

    }

    return null;

  }

  /**
   *
   * Write in current folder a file with the collaborative object's id
   * supporting the chat.
   *
   * @return
   */
  protected String setCollaborativeObjectId(String objectId) {

    File f = new File(COBJECT_ID_FILE);

    try {

      if (f.createNewFile()) {
        PrintWriter pw = new PrintWriter(f);
        pw.println(objectId);
        pw.close();
      }

      return null;

    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
    return objectId;
  }


  /**
   *
   * Open an existing collaborative object in the server. Count down the
   * associated latch.
   *
   */
  protected void openCollaborativeObject(String objectId) {

    // OpenModel
    channel.openModel(objectId, new ChannelOperationCallback() {

      @Override
      public void onSuccess(Model model) {
        //
        // Don't make blocking operations in this callback
        //
        cObject = model;
        cObjectReadyLatch.countDown();
      }

      @Override
      public void onFailure() {
        //
        // Don't make blocking operations in this callback
        //
        cObject = null;
        cObjectReadyLatch.countDown();
      }

    },

    new UnsavedDataListener() {

      @Override
      public void onUpdate(UnsavedDataInfo info) {

        /*
         * System.err.println("Unsaved data info: " + info.inFlightSize() + "/"
         * + info.estimateUncommittedSize() + "/" +
         * info.estimateUnacknowledgedSize() + " [" + info.laskAckVersion() +
         * ":" + info.lastCommitVersion() + "]");
         */
      }

      @Override
      public void onClose(boolean arg0) {
        log("Collaborative object closed!");
      }
    });

  }


    /**
   *
   * Create a new collaborative object in the server. Count down the associated
   * latch.
   *
   * @return the object Id
   */
  protected void createCollaborativeObject() {

    // OpenModel
    channel.createModel(new ChannelOperationCallback() {

      @Override
      public void onSuccess(Model model) {

        //
        // Don't make blocking operations in this callback
        //
        cObject = model;
        cObjectId = model.getWaveId().serialise();
        // Allow everybody to join the chat!
        cObject.addParticipant("@" + cObjectDomain);
        cObjectReadyLatch.countDown();
      }

      @Override
      public void onFailure() {

        //
        // Don't make blocking operations in this callback
        //
        cObject = null;
        cObjectReadyLatch.countDown();
      }

    },

    new UnsavedDataListener() {

      @Override
      public void onUpdate(UnsavedDataInfo info) {

        /*
         * System.err.println("Unsaved data info: " + info.inFlightSize() + "/"
         * + info.estimateUncommittedSize() + "/" +
         * info.estimateUnacknowledgedSize() + " [" + info.laskAckVersion() +
         * ":" + info.lastCommitVersion() + "]");
         */
      }

      @Override
      public void onClose(boolean arg0) {
        log("Collaborative object closed!");
      }
    });
  }


  public void run(String serverUrl, String objectDomain) {

    cObjectDomain = objectDomain;

    String user = "_anonymous_";
    String password = "";


    Session.disableSSLcheck();

    Session session = null;

    try {
      session = Session.create(serverUrl, user, password);

    } catch (MalformedURLException e) {
      e.printStackTrace();
      return;

    } catch (InvalidParticipantAddress e) {
      e.printStackTrace();
      return;

    }


    channel = Channel.open(cObjectDomain, session, new ConnectionListener() {

      @Override
      public void onReconnect() {
        log("Reconnected to server");
      }

      @Override
      public void onDisconnect() {
        log("Disconnected from server");
      }

      @Override
      public void onConnect() {
        log("Connected to server");
        channelReadyLatch.countDown();
      }
    });


    // Await for the channel to be open
    try {
      channelReadyLatch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      System.err.println("Error openning a communication channel with the server!");
      return;
    }


    boolean storeObjectId = false;
    cObjectId = getCollaborativeObjectId();

    if (cObjectId == null) {

      storeObjectId = true;
      createCollaborativeObject();


    } else {

      openCollaborativeObject(cObjectId);

    }

    // Await for the collaborative object to load
    try {
      cObjectReadyLatch.await(10, TimeUnit.SECONDS);

      if (cObject == null) {
        System.err.println("Object not loaded!");
        return;
      }

    } catch (InterruptedException e) {
      System.err.println("Time out awaiting object!");
      return;
    }

    if (storeObjectId)
      setCollaborativeObjectId(cObjectId);

    //
    // Here the chat code
    //

    if (!cObject.getRoot().keySet().contains("chat")) {
      ListType list = cObject.createList();
      cObject.getRoot().put("chat", list);
    }

    ListType chat = (ListType) cObject.getRoot().get("chat");

    chat.addListener(new Listener() {

      @Override
      public void onValueAdded(Type arg0) {
        StringType s = (StringType) arg0;
        System.out.println();
        System.out.println(">> " + s.getValue());
      }

      @Override
      public void onValueRemoved(Type arg0) {

      }

    });

    Scanner s = new Scanner(System.in).useDelimiter("\\n");

    String input;
    do {
      System.out.print("you>");
      input = s.next();
      if (input != null) {
        if (input.equalsIgnoreCase("exit;")) {
          break;

        } else if (!input.isEmpty()) {
          chat.add(cObject.createString(input));
        }
      }

    } while (true);

  }


  public static void main(String[] args) {
    SimpleChat chat = new SimpleChat();
    chat.run(args[0], args[1]);
  }

}
