package org.swellrt.java.perf;

import java.net.MalformedURLException;
import java.util.concurrent.CountDownLatch;

import org.swellrt.java.WaveWebSocketClient.ConnectionListener;
import org.swellrt.java.client.Channel;
import org.swellrt.java.client.Channel.ChannelOperationCallback;
import org.swellrt.java.client.Session;
import org.swellrt.model.generic.Model;
import org.swellrt.model.generic.TextType;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;

public class UserSimpleWriter extends User implements UnsavedDataListener {

  private final static String NETWORK_CONNECTED = "connected";
  private final static String NETWORK_DISCONNECTED = "disconnected";

  private CountDownLatch channelLatch = new CountDownLatch(1);
  private CountDownLatch openModelLatch = new CountDownLatch(1);

  private String networkStatus = NETWORK_DISCONNECTED;

  private Model model;

  @Override
  public void behaviour() {

    try {

      char c = (char) (65 + getIndex());
      char[] cs = { ' ', c, c, c };
      String s = new String(cs);
      int p = 5 + (getIndex() * 20);

      p = 5;

      log("Using text " + s);

      String server = getParameters().getProperty("server");
      String user = getParameters().getProperty("user");
      String password = getParameters().getProperty("password");

      Session session = Session.create(server, user, password);

      log("Session is " + session.getSessionId());

      Channel channel = Channel.open(getParameters().getProperty("domain"), session,
          new ConnectionListener() {

            @Override
            public void onReconnect() {
              networkStatus = NETWORK_CONNECTED;
            }

            @Override
            public void onDisconnect() {
              networkStatus = NETWORK_DISCONNECTED;
            }

            @Override
            public void onConnect() {
              networkStatus = NETWORK_CONNECTED;
              log("Channel Connected");
              channelLatch.countDown();
            }
          });

      channelLatch.await();

      channel.openModel(getParameters().getProperty("waveid"), new ChannelOperationCallback() {

        @Override
        public void onSuccess(Model model) {
          log("Data model opened");
          UserSimpleWriter.this.model = model;
          openModelLatch.countDown();
        }

        @Override
        public void onFailure() {
        }

      }, this);

      openModelLatch.await();

      TextType text = (TextType) model.getRoot().get("doc");

      for (int i = 0; i < 5; i++) {
        Thread.sleep(4000);
        if (networkStatus.equals(NETWORK_CONNECTED)) {
          log("Insert text #" + i);
          text.insertText(p + (i * 4), s);
        } else {
          log("Unable to insert text, network disconnected");
        }
      }


    } catch (MalformedURLException | InvalidParticipantAddress e) {
      log("Error creating session");
    } catch (InterruptedException e) {
      log("Thread interrupted");
    }

  }

  @Override
  public void onUpdate(UnsavedDataInfo unsavedDataInfo) {

    int inFlight = unsavedDataInfo.inFlightSize();
    int inQueue = unsavedDataInfo.estimateUnacknowledgedSize() - inFlight;
    int ackNotCommited = unsavedDataInfo.estimateUncommittedSize()
        - unsavedDataInfo.estimateUnacknowledgedSize();

    log("OnUpdate " + inQueue + "/" + inFlight + "/" + ackNotCommited + "("
        + unsavedDataInfo.laskAckVersion() + ":" + unsavedDataInfo.lastCommitVersion() + ")");
  }

  @Override
  public void onClose(boolean everythingCommitted) {

  }

}
