package org.swellrt.java;



import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.Socket;
import org.atmosphere.wasync.impl.AtmosphereClient;
import org.atmosphere.wasync.impl.AtmosphereRequest.AtmosphereRequestBuilder;
import org.atmosphere.wasync.impl.DefaultOptionsBuilder;
import org.slf4j.LoggerFactory;
import org.waveprotocol.wave.model.util.Base64DecoderException;
import org.waveprotocol.wave.model.util.CharBase64;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

/**
 * A WaveSocket implementation backed by an Atmosphere/WAsync client.
 *
 * Atmosphere server and client must support following features:
 * <ul>
 * <li>Heart beat messages</li>
 * <li>Track message size + Base64 message encoding</li>
 * </ul>
 *
 * @author Pablo Ojanguren (pablojan@gmail.com)
 *
 */
public class WaveSocketWAsync implements WaveSocket {

  final static org.slf4j.Logger Log = LoggerFactory.getLogger(WaveSocketWAsync.class);

  /**
   *  See {@WaveSocketCallbackSwellRT} for more info
   */
  final static int WAVE_MESSAGE_SEPARATOR = '|';
  /**
   * See {@WaveSocketCallbackSwellRT} for more info
   */
  final static String WAVE_MESSAGE_END_MARKER = "}|";


  private final String urlBase;
  private Socket socket = null;
  private final WaveSocketCallback callback;
  private String sessionId;
  private String clientVer;

  AsyncHttpClient ahc;
  /**
   *
   * See {@WaveSocketCallbackSwellRT} for more info
   * @param message
   * @return
   */
  private boolean isPackedWaveMessage(String message) {
    return message.indexOf(WAVE_MESSAGE_SEPARATOR) == 0;
  }

  /**
   * See {@WaveSocketCallbackSwellRT} for more info
   * @param packedMessage
   * @return
   */
  private List<String> unpackWaveMessages(String packedMessage) {

    List<String> messages = new ArrayList<String>();

    if (isPackedWaveMessage(packedMessage)) {

      while (packedMessage.indexOf(WAVE_MESSAGE_SEPARATOR) == 0 && packedMessage.length() > 1) {
        packedMessage = packedMessage.substring(1);
        int marker = packedMessage.indexOf(WAVE_MESSAGE_END_MARKER);
        String splitMessage = packedMessage.substring(0, marker + 1);
        messages.add(splitMessage);
        packedMessage = packedMessage.substring(marker + 1);
      }
    }

    return messages;
  }


  protected WaveSocketWAsync(final WaveSocket.WaveSocketCallback callback, String urlBase,
      String sessionId, String clientVersion) {
    this.urlBase = urlBase;
    this.callback = callback;
    this.sessionId = sessionId;
    this.clientVer = clientVersion;
  }


  public static boolean DISABLE_SSL_CHECK = true;

  protected AsyncHttpClientConfig.Builder configureAHC() {

    /*
     * Configure the Grizzly provider in the Async Http Client: <a href=
     * 'http://github.com/Atmosphere/wasync/wiki/Configuring-the-underlying-AHC-provider'>configure
     * AHC</a>
     */

    AsyncHttpClientConfig.Builder ahcConfigBuilder = new AsyncHttpClientConfig.Builder();

    // Allow connections from any server, please use only for debug purpose
    if (DISABLE_SSL_CHECK) {


      HostnameVerifier hostnameVerifier = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      };

      // References to AsycHttpClient, Grizzly and SSL
      // https://groups.google.com/forum/#!topic/asynchttpclient/wgaAs3lszbI
      // http://stackoverflow.com/questions/21833804/how-to-make-https-calls-using-asynchttpclient

      // Issue 93740: Lollipop breaks SSL/TLS connections when using Jetty
      // https://code.google.com/p/android/issues/detail?id=93740

      // Support for SSL connections accepting self signed cert
      SSLContext sslContext = null;
      try {
        sslContext = SSLContext.getInstance("TLS");

        sslContext.init(null, new X509TrustManager[] { new X509TrustManager() {

          public void checkClientTrusted(X509Certificate[] chain, String authType)
              throws CertificateException {
          }

          public void checkServerTrusted(X509Certificate[] chain, String authType)
              throws CertificateException {
          }

          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
          }

        } }, new SecureRandom());

      } catch (Exception e) {
        Log.error("Error creating SSLContext", e);
      }

      ahcConfigBuilder.setSSLContext(sslContext).setHostnameVerifier(hostnameVerifier);
    }

      return ahcConfigBuilder;

  }


  @Override
  public void connect() {

    // Use Grizzly only with WAsync 2.1.2
    /*
     * ahc = new AsyncHttpClient(new GrizzlyAsyncHttpProvider( new
     * AsyncHttpClientConfig.Builder().build()));
     */

    AsyncHttpClientConfig ahcConfig = configureAHC().build();
    ahc = new AsyncHttpClient(ahcConfig);

    AtmosphereClient client = ClientFactory.getDefault().newClient(AtmosphereClient.class);

    DefaultOptionsBuilder optionsBuilder = client.newOptionsBuilder();
    optionsBuilder.runtime(ahc);

    socket = client.create(optionsBuilder.build())

    .on(Event.STATUS.name(), new Function<String>() {

      @Override
      public void on(String status) {
        Log.info("STATUS");
      }

    })

    .on(Event.OPEN.name(), new Function<String>() {

      @Override
      public void on(String arg0) {
        Log.info("CONNECT");
        callback.onConnect();
      }

    }).on(Event.CLOSE.name(), new Function<String>() {

      @Override
      public void on(String arg0) {
        Log.info("CLOSE");
        callback.onDisconnect();
      }

    }).on(Event.REOPENED.name(), new Function<String>() {

      @Override
      public void on(String arg0) {
        Log.info("REOPENED");
        callback.onConnect();
      }

    }).on(Event.MESSAGE.name(), new Function<String>() {

      @Override
      public void on(String encodedMessage) {

        String message = null;
        try {
          message = new String(CharBase64.decode(encodedMessage));
        } catch (Base64DecoderException e) {
          return;
        }

          // Ignore heart-beat messages
          // NOTE: is heart beat string always " "?
        if (message == null || message.isEmpty() || message.startsWith(" ")
            || message.startsWith("  "))
            return;

        if (isPackedWaveMessage(message)) {
          List<String> unpacked = unpackWaveMessages(message);
            for (String s : unpacked) {
              callback.onMessage(s);
            }

          } else {
          callback.onMessage(message);
          }

      }

    }).on(new Function<Throwable>() {

      @Override
      public void on(Throwable t) {
        Log.info("ON Exception");
        callback.onDisconnect();
      }

    });


    AtmosphereRequestBuilder requestBuilder = client.newRequestBuilder()
        .method(Request.METHOD.GET)
        .uri(urlBase)
        .enableProtocol(true)
        .queryString("X-client-version", clientVer)
        .trackMessageLength(true)
        // .transport(Request.TRANSPORT.LONG_POLLING)
        .header("Cookie", "WSESSIONID=" + sessionId).transport(Request.TRANSPORT.WEBSOCKET);





    // Using waitBeforeUnlocking(2000) option to avoid high delay on
    // long-polling connection


    try {
      socket.open(requestBuilder.build());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void disconnect() {
    socket.close();
    ahc.getProvider().close();
    ahc.close();
  }



  @Override
  public void sendMessage(final String message) {
    try {
      socket.fire(message);
    } catch (IOException e) {
      Log.error("Error sending message " + e);
    }
  }

}



