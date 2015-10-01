package org.swellrt.java;



import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.Socket;
import org.atmosphere.wasync.impl.AtmosphereClient;
import org.atmosphere.wasync.impl.AtmosphereRequest.AtmosphereRequestBuilder;
import org.slf4j.LoggerFactory;
import org.waveprotocol.wave.model.util.Base64DecoderException;
import org.waveprotocol.wave.model.util.CharBase64;

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

  /**
   * Configure this provider before using the client
   */
  public static AHCProvider asyncHttpClientProvider = new DefaultAHCProvider();

  private final String urlBase;
  private Socket socket = null;
  private final WaveSocketCallback callback;
  private String sessionId;

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


  protected WaveSocketWAsync(final WaveSocket.WaveSocketCallback callback, String urlBase, String sessionId) {
    this.urlBase = urlBase;
    this.callback = callback;
    this.sessionId = sessionId;
  }



  @Override
  public void connect() {


    AtmosphereClient client = ClientFactory.getDefault().newClient(AtmosphereClient.class);

    AtmosphereRequestBuilder requestBuilder = client.newRequestBuilder().method(Request.METHOD.GET)
        .uri(urlBase)
        .header("Cookie", "WSESSIONID=" + sessionId)
        .enableProtocol(true)
        .trackMessageLength(true)
        .transport(Request.TRANSPORT.WEBSOCKET);
        //.transport(Request.TRANSPORT.LONG_POLLING);




    // Using waitBeforeUnlocking(2000) option to avoid high delay on
    // long-polling connection


    socket = client
        .create(
            client.newOptionsBuilder().runtime(asyncHttpClientProvider.getClient())
                .runtimeShared(asyncHttpClientProvider.isSharedClient()).build())
        .on(Event.OPEN.name(), new Function<String>() {

          @Override
          public void on(String arg0) {
            callback.onConnect();
          }

        }).on(Event.CLOSE.name(), new Function<String>() {

          @Override
          public void on(String arg0) {
            callback.onDisconnect();
          }

        }).on(Event.REOPENED.name(), new Function<String>() {

          @Override
          public void on(String arg0) {
            callback.onConnect();
          }


        }).on(Event.MESSAGE.name(), new Function<String>() {

          @Override
          public void on(String message) {

            // Log.info("Raw message: " + message);

            try {

              // Decode from Base64 because of Atmosphere Track Message Lenght server
              // feauture
              // NOTE: no Charset is specified, so this relies on UTF-8 as default
              // charset
              String decoded = new String(CharBase64.decode(message));
              // Log.info("Decoded message: " + decoded);

              // Ignore heart-beat messages
              // NOTE: is heart beat string always " "?
              if (decoded == null || decoded.isEmpty() || decoded.startsWith(" ")
                  || decoded.startsWith("  ")) return;


              if (isPackedWaveMessage(decoded)) {
                List<String> unpacked = unpackWaveMessages(decoded);
                for (String s : unpacked) {
                  callback.onMessage(s);
                }

              } else {
                // Filter non JSON messages
                // TODO remove, use atmosphere client properly
                if (decoded.startsWith("{"))
                  callback.onMessage(decoded);
              }

            } catch (Base64DecoderException e) {
              Log.error("Error decoding Base64 message from WebSocket", e);
            }

          }

        }).on(new Function<Throwable>() {

          @Override
          public void on(Throwable t) {
            callback.onDisconnect();
          }

        });

    try {
      socket.open(requestBuilder.build(), -1, TimeUnit.MILLISECONDS);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void disconnect() {
    socket.close();
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



