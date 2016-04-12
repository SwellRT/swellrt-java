package org.swellrt.java.script;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.swellrt.java.WaveWebSocketClient.ConnectionListener;
import org.swellrt.java.client.Channel;
import org.swellrt.java.client.Channel.ChannelOperationCallback;
import org.swellrt.java.client.Session;
import org.swellrt.model.generic.ListType;
import org.swellrt.model.generic.Model;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 *
 * A simple Java tool for batch editing of SwellRT objects remotely.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class Launcher {


  String serverUrl;
  String waveDomain;
  PrintStream out;
  PrintStream err = System.err;

  CountDownLatch openChannelLatch;
  CountDownLatch openModelLatch;
  CountDownLatch commitChangesLatch;

  Model cObject;

  Object done = new Boolean(false);

  public Launcher(String serverUrl, String waveDomain, PrintStream out) {
    super();
    this.serverUrl = serverUrl;
    this.waveDomain = waveDomain;
    this.out = out;
  }

  /**
   *
   *
   *
   * @param waveArray
   */
  public void run(String defaultUser, String defaultPassword, Script script,
      ScriptInput input) {

    if (!input.open()) {
      err.println("Error opening input data.");
      return;
    }

    Session.disableSSLcheck();

    JsonObject waveItem = input.next();
    while (waveItem != null) {

      cObject = null;

      String waveId = waveItem.get("wave_id").getAsString();

      String user = defaultUser;
      if (waveItem.has("participant"))
        user = waveItem.get("participant").getAsString();


      String password = defaultPassword;
      if (waveItem.has("password"))
        password = waveItem.get("password").getAsString();


      openChannelLatch = new CountDownLatch(1);
      openModelLatch = new CountDownLatch(1);
      // Wait for commit in 3 wavelets (swl+root, xxx+user, dummy)
      commitChangesLatch = new CountDownLatch(3);

      Session session = null;

      try {

        session = Session.create(serverUrl, user, password);

      } catch (MalformedURLException | InvalidParticipantAddress e) {
        e.printStackTrace(out);
        // continue;
        return;
      }

      Channel channel = Channel.open(waveDomain, session, new ConnectionListener() {

        @Override
        public void onReconnect() {
          err.println("[channel] RECONNECTED");
        }

        @Override
        public void onDisconnect() {
          err.println("[channel] DISCONNECTED");
        }

        @Override
        public void onConnect() {
          err.println("[channel] CONNECTED");
          openChannelLatch.countDown();
        }
      });

      try {
        openChannelLatch.await(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace(out);
        return;
        // continue;
      }

      channel.openModel(waveId, new ChannelOperationCallback() {

        @Override
        public void onSuccess(Model model) {
          Launcher.this.cObject = model;
          openModelLatch.countDown();
        }

        @Override
        public void onFailure() {
          err.println("Error opening Wave!");
          openModelLatch.countDown();
        }

      }, new UnsavedDataListener() {

        @Override
        public void onUpdate(UnsavedDataInfo info) {

          err.println("[In-Flight Data] " + info.inFlightSize() + "/"
              + info.estimateUncommittedSize() + "/" + info.estimateUnacknowledgedSize() + " ["
              + info.laskAckVersion() + ":" + info.lastCommitVersion() + "]");

          if (info.inFlightSize() == 0 && info.estimateUncommittedSize() == 0
              && info.estimateUnacknowledgedSize() == 0
              && (info.laskAckVersion() == info.lastCommitVersion())) {

            commitChangesLatch.countDown();
          }

        }

        @Override
        public void onClose(boolean arg0) {

        }
      });

      String exception = "Error opening wave";

      try {
        openModelLatch.await(20, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace(out);
        exception = "timeout open";
      }

      if (cObject == null) {

        out.println("[wave=" + waveId + "] [status=ERROR] [exception=" + exception + "]");

      } else {

        // Run the script against the collaborative object
        out.println(script.execute(cObject));

        if (!script.wasDataChanged()) {
          commitChangesLatch.countDown();
        }

        try {
          commitChangesLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          e.printStackTrace(out);
        }

        channel.closeModel(waveId);


      }

      channel.close();

      // Next item
      waveItem = input.next();
    }

    input.close();

    out.println("SCRIPT FINISHED!");
  }


  protected static ScriptInput getFileInput(final String fileName) {

    return new ScriptInput() {

      JsonArray inputArray;
      Iterator<JsonElement> iterator;

      @Override
      public boolean open() {

        JsonElement jsonRootElement = null;
        try {
          FileReader reader;
          reader = new FileReader(fileName);

          JsonParser jsonParser = new JsonParser();
          jsonRootElement = jsonParser.parse(reader);
          inputArray = jsonRootElement.getAsJsonArray();
          iterator = inputArray.iterator();

        } catch (FileNotFoundException e) {
          e.printStackTrace();
          return false;
        } catch (JsonParseException e) {
          e.printStackTrace();
          return false;
        } catch (RuntimeException e) {
          e.printStackTrace();
          return false;
        }

        return true;
      }

      @Override
      public JsonObject next() {
        if (iterator.hasNext())
          return iterator.next().getAsJsonObject();
        else
          return null;
      }

      @Override
      public void close() {

      }
    };

  }

  protected static ScriptInput getTestInput() {

    return new ScriptInput() {

      int count = 0;
      final String[] waveIds = { "local.net/s+u28k4lqPkpA", "local.net/s+mWKSf9EyKKA" };

      @Override
      public boolean open() {
        count = 0;
        return true;
      }

      @Override
      public JsonObject next() {

        if (count < waveIds.length) {

          JsonObject jso = new JsonObject();
          jso.add("wave_id", new JsonPrimitive(waveIds[count++]));
          return jso;

        } else
          return null;

      }

      @Override
      public void close() {

      }

    };

  }

  public static void main(String[] args) throws MalformedURLException, InvalidParticipantAddress,
      InterruptedException {

    System.out.println("SwellRT Script Launcher");
    System.out
        .println("Use: java -cp <classpath> org.swellrt.java.script.Launcher <wave server url> <wave domain> <user>:<password> <input file> ");

    String serverUrl = args[0];
    String waveDomain = args[1];

    String[] credentials = args.length > 2 ? args[2].split(":") : new String[0];

    String user = credentials.length > 0 ? credentials[0] : "_anonymous_";
    String password = credentials.length > 1 ? credentials[1] : "";

    final String inputFile = args.length > 3 ? args[3] : "";
    //
    // Example
    //

    Launcher sl = new Launcher(serverUrl, waveDomain, System.out);

    sl.run(user, password, new Script() {

      @Override
      public String execute(Model cObject) {

        if (!cObject.getRoot().keySet().contains("chat")) {
          ListType list = cObject.createList();
          cObject.getRoot().put("chat", list);
        }

        ListType chat = (ListType) cObject.getRoot().get("chat");
        for (int i = 0; i < 10; i++)
          chat.add(cObject.createString("Testing the Script Launcher #" + i));

        return "[" + cObject.getWaveId().serialise() + "] [chat script ended]";

      }

      @Override
      public boolean wasDataChanged() {
        return true;
      }

    }, getTestInput());



  }


}
