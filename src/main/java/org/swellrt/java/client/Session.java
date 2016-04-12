package org.swellrt.java.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swellrt.java.WaveHttpLogin;
import org.swellrt.java.WaveWebSocketClient.ConnectionListener;
import org.swellrt.java.client.Channel.ChannelOperationCallback;
import org.swellrt.model.generic.Model;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

public class Session {

  public static final String USER_ANONYMOUS = "_anonymous_";

  final static Logger Log = LoggerFactory.getLogger(Session.class);

  public static void disableSSLcheck() {

    //
    // Global -------------------------------------------
    //

    // Create a trust manager that does not validate certificate chains
    SSLContext ctx = null;
    TrustManager[] trustAllCerts = new X509TrustManager[] { new X509TrustManager() {
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      public void checkClientTrusted(X509Certificate[] certs, String authType) {
      }

      public void checkServerTrusted(X509Certificate[] certs, String authType) {
      }
    } };
    try {
      ctx = SSLContext.getInstance("SSL");
      ctx.init(null, trustAllCerts, null);
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      Log.info("Error loading ssl context {}", e.getMessage());
    }

    SSLContext.setDefault(ctx);
    //
    // --------------------------------------------------
    //

  }

  public static Session create(String host, String username, String password)
      throws MalformedURLException, InvalidParticipantAddress {

    // Validate parameters
    URL hostURL = new URL(host);

    ParticipantId participantId = null;

    if (!USER_ANONYMOUS.equals(username))
      participantId = ParticipantId.of(username);

    // Perform Http Call
    WaveHttpLogin httpLogin = new WaveHttpLogin(hostURL.toString(), username, password);
    WaveHttpLogin.Response response = httpLogin.execute();

    if (response.sessionId == null || response.userId == null)
      return null;
    else {
      participantId = ParticipantId.of(response.userId);
      return new Session(hostURL, participantId, response.sessionId);
    }
  }

  private Session(URL serverURL, ParticipantId participantId, String sessionId) {
    this.serverURL = serverURL;
    this.sessionId = sessionId;
    this.participantId = participantId;
  }

  private final URL serverURL;
  private final String sessionId;
  private final ParticipantId participantId;

  public String getSessionId() {
    return sessionId;
  }

  public ParticipantId getParticipantId() {
    return participantId;
  }

  public URL getServerURL() {
    return serverURL;
  }

  public void close() {

  }

  protected void register(Channel channel) {

  }

  protected void unRegister(Channel channel) {

  }

  public static void main(String[] args) {

    try {

      Session session = Session.create("http://localhost:9898", "_anonymous_", "");

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

      channel.createModel(new ChannelOperationCallback() {

        @Override
        public void onSuccess(Model model) {
          Log.info("Model opened");
          for (String s : model.getRoot().keySet())
            System.out.print(s + " ");

          System.out.println();
        }

        @Override
        public void onFailure() {
        }
      }, null);

    } catch (MalformedURLException e) {

      e.printStackTrace();

    } catch (InvalidParticipantAddress e) {

      e.printStackTrace();
    }

  }

}
