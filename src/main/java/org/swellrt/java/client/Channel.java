package org.swellrt.java.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import org.swellrt.java.RemoteViewServiceMultiplexer;
import org.swellrt.java.WaveLoader;
import org.swellrt.java.WaveWebSocketClient;
import org.swellrt.java.WaveWebSocketClient.ConnectionListener;
import org.swellrt.java.wave.client.concurrencycontrol.MuxConnector.Command;
import org.swellrt.model.generic.Model;
import org.swellrt.model.generic.TextType;
import org.swellrt.model.generic.TypeIdGenerator;
import org.swellrt.model.shared.ModelUtils;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.concurrencycontrol.wave.CcDataDocumentImpl;
import org.waveprotocol.wave.model.document.WaveContext;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdGeneratorImpl;
import org.waveprotocol.wave.model.id.IdGeneratorImpl.Seed;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.WaveRef;

public class Channel {

  public interface ChannelOperationCallback {

    public void onSuccess(Model model);

    public void onFailure();
  }

  public static Channel open(final String domain, final Session session,
      final ConnectionListener listener) {

    String webSocketUrl = session.getServerURL().getProtocol() + "://"
        + session.getServerURL().getHost()
        + (session.getServerURL().getPort() != -1 ? ":" + session.getServerURL().getPort() : "")
        + "/atmosphere";

    IdGenerator idGenerator = new IdGeneratorImpl(domain, new Seed() {

      @Override
      public String get() {

        return session.getSessionId().substring(0, 5);
      }
    });

    TypeIdGenerator typeIdGenerator = TypeIdGenerator.get(idGenerator);

    WaveWebSocketClient webSocketClient = new WaveWebSocketClient(webSocketUrl,
        session.getSessionId());
    webSocketClient.connect(listener);

    RemoteViewServiceMultiplexer remoteServiceChannel = new RemoteViewServiceMultiplexer(
        webSocketClient,
        session.getParticipantId().getAddress());

    Channel channel = new Channel(domain, session, typeIdGenerator, idGenerator,
        remoteServiceChannel,
        webSocketClient);
    session.register(channel);

    return channel;
  }


  private Channel(String domain, Session session, TypeIdGenerator typeIdGenerator,
      IdGenerator idGenerator,
      RemoteViewServiceMultiplexer remoteServiceChannel,
      WaveWebSocketClient webSocketClient) {

    this.domain = domain;
    this.session = session;
    this.typeIdGenerator = typeIdGenerator;
    this.idGenerator = idGenerator;
    this.remoteServiceChannel = remoteServiceChannel;
    this.webSocketClient = webSocketClient;

    waveStore = new HashMap<WaveRef, Pair<WaveLoader, Model>>();
    timer = new Timer(Thread.currentThread().getName());
  }

  private final String domain;
  private final Session session;
  private final TypeIdGenerator typeIdGenerator;
  private final IdGenerator idGenerator;
  private final RemoteViewServiceMultiplexer remoteServiceChannel;
  private final WaveWebSocketClient webSocketClient;

  private final Map<WaveRef, Pair<WaveLoader, Model>> waveStore;
  private final Timer timer;

  private boolean isClosed = false;

  public void openModel(String modelId, final ChannelOperationCallback callback,
      final UnsavedDataListener dataListener) {

    Preconditions.checkArgument(!isClosed, "Channel is closed");

    final WaveRef waveRef = WaveRef.of(WaveId.deserialise(modelId));

    if (waveStore.containsKey(waveRef)) {
      callback.onSuccess(waveStore.get(waveRef).getSecond());
    }

    final WaveLoader loader = WaveLoader.create(false, waveRef, remoteServiceChannel,
        session.getParticipantId(), Collections.<ParticipantId> emptySet(), idGenerator,
        dataListener, timer);

    loader.init(new Command() {

      @Override
      public void execute() {

        WaveContext wave = loader.getWaveContext();
        Model model = Model.create(wave.getWave(), domain, session.getParticipantId(), false,
            idGenerator);
        waveStore.put(waveRef, new Pair<WaveLoader, Model>(loader, model));

        callback.onSuccess(model);

      }
    });

  }

  public void createModel(final ChannelOperationCallback callback,
      final UnsavedDataListener dataListener) {

    Preconditions.checkArgument(!isClosed, "Channel is closed");

    WaveId newWaveId = typeIdGenerator.newWaveId();
    final WaveRef waveRef = WaveRef.of(newWaveId);

    final WaveLoader loader = WaveLoader.create(true, waveRef, remoteServiceChannel,
        session.getParticipantId(), Collections.<ParticipantId> emptySet(), idGenerator,
        dataListener,
        timer);

    loader.init(new Command() {

      @Override
      public void execute() {

        WaveContext wave = loader.getWaveContext();
        Model model = Model.create(wave.getWave(), domain, session.getParticipantId(), true,
            idGenerator);

        waveStore.put(waveRef, new Pair<WaveLoader, Model>(loader, model));

        callback.onSuccess(model);
      }
    });
  }

  public void closeModel(String modelId) {

    Preconditions.checkArgument(!isClosed, "Channel is closed");

    WaveRef waveRef = WaveRef.of(WaveId.deserialise(modelId));
    Preconditions.checkState(waveStore.containsKey(waveRef), "Trying to close a not opened Model");

    WaveLoader loader = waveStore.get(waveRef).getFirst();
    loader.close();

    waveStore.remove(waveRef);
  }

  public Model getModel(String modelId) {

    Preconditions.checkArgument(!isClosed, "Channel is closed");

    WaveRef waveRef = WaveRef.of(WaveId.deserialise(modelId));
    if (!waveStore.containsKey(waveRef))
      return null;

    return waveStore.get(waveRef).getSecond();
  }

  public CcDataDocumentImpl getReadableDocument(TextType text) {

    Preconditions.checkArgument(!isClosed, "Channel is closed");

    WaveRef waveRef = WaveRef.of(text.getModel().getWaveId());
    if (!waveStore.containsKey(waveRef))
      return null;

    return waveStore.get(waveRef).getFirst().getDocumentRegistry()
        .getBlipDocument(ModelUtils.serialize(text.getModel().getWaveletId()), text.getDocumentId());
  }

  public void close() {

    Preconditions.checkArgument(!isClosed, "Channel is closed");

    webSocketClient.disconnect();

    for (WaveRef waveRef : waveStore.keySet()) {
      WaveLoader loader = waveStore.get(waveRef).getFirst();
      // remoteServiceChannel.close(id, stream);
      loader.close();
    }

    waveStore.clear();
    isClosed = true;

  }

}
