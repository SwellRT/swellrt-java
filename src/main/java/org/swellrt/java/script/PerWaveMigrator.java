package org.swellrt.java.script;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.swellrt.java.WaveWebSocketClient.ConnectionListener;
import org.swellrt.java.client.Channel;
import org.swellrt.java.client.Channel.ChannelOperationCallback;
import org.swellrt.java.client.Session;
import org.swellrt.model.generic.Model;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;

public class PerWaveMigrator implements Runnable {

  private static final String DEFAULT_PASSWORD = "$password$";

  String waveId;
  String participant;
  String url;
  String domain;
  String procId;

  PrintStream out;

  String itemLog = "";

  Model currentModel = null;

  CountDownLatch channelLatch = new CountDownLatch(1);
  CountDownLatch openModelLatch = new CountDownLatch(1);

  Session session;
  Channel channel;

  Long timeLastComplete = new Long(0);

  public PerWaveMigrator(String procId, String url, String domain, String waveId, String participant) {
    super();
    this.procId = procId;
    this.url = url;
    this.domain = domain;
    this.waveId = waveId;
    this.participant = participant;

    itemLog = "Item " + procId + ":" + waveId + ":" + participant;
  }

  @Override
  public void run() {
    channelLatch = new CountDownLatch(1);
    openModelLatch = new CountDownLatch(1);


    try {

      session = Session.create(url, participant, participant.startsWith("_anonymous:") ? ""
          : DEFAULT_PASSWORD);

      channel = Channel.open(domain, session, new ConnectionListener() {

        @Override
        public void onReconnect() {
          System.err.println("Reconnected to server processing " + waveId);
        }

        @Override
        public void onDisconnect() {
          System.err.println("Disconnected to server processing " + waveId);
        }

        @Override
        public void onConnect() {
          System.err.println("Connected to server processing " + waveId);
          channelLatch.countDown();
        }
      });

      channelLatch.await(20000, TimeUnit.MILLISECONDS);

      if (channel == null)
        throw new RuntimeException("Error creating channel");

      channel.openModel(waveId, new ChannelOperationCallback() {

        @Override
        public void onSuccess(Model model) {
          itemLog += ":success";
          PerWaveMigrator.this.currentModel = model;
          openModelLatch.countDown();
        }

        @Override
        public void onFailure() {
          itemLog += ":error";
          openModelLatch.countDown();
        }

      }, new UnsavedDataListener() {

        @Override
        public void onUpdate(UnsavedDataInfo info) {

          if (info.estimateUncommittedSize() == 0 && info.inFlightSize() == 0
              && info.estimateUnacknowledgedSize() == 0
              && info.laskAckVersion() == info.lastCommitVersion()) {

            System.err.println("Unsaved data info: " + info.inFlightSize() + "/"
                + info.estimateUncommittedSize() + "/" + info.estimateUnacknowledgedSize() + " ["
                + info.laskAckVersion() + ":" + info.lastCommitVersion() + "]");

              itemLog += ":all_saved";
            if (timeLastComplete > 0 && (System.currentTimeMillis() - timeLastComplete > 40000)) {
              channel.close();
              session.close();
            }
              else
                timeLastComplete = System.currentTimeMillis();
          }
        }

        @Override
        public void onClose(boolean arg0) {

        }
      });

      openModelLatch.await();


    } catch (MalformedURLException e) {
      throw new RuntimeException(e.getMessage());
    } catch (InvalidParticipantAddress e) {
      throw new RuntimeException(e.getMessage());
    } catch (InterruptedException e) {
      throw new RuntimeException(e.getMessage());
    }

  }

  public void close() {
    channel.close();
    session.close();
  }

  public String getLog() {
    return itemLog;
  }

  public long getLastChangeTime() {
    return timeLastComplete;
  }

}
