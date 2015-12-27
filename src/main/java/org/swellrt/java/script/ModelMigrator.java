package org.swellrt.java.script;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.swellrt.java.WaveSocketWAsync;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class ModelMigrator {


  JsonElement source;
  String url;
  String domain;
  PrintStream out;

  List<PerWaveMigrator> cleanQueue = new ArrayList<PerWaveMigrator>();


  public ModelMigrator(JsonElement source, String url, String domain, PrintStream out) {
    super();
    this.source = source;
    this.url = url;
    this.domain = domain;
    this.out = out;
  }

  private String getValidParticipant(JsonArray participantsItem) {
    String participant = null;

    for (int j = 0; j < participantsItem.size(); j++) {
      String asString = participantsItem.get(j).getAsString();

      if (!asString.startsWith("@") && !asString.startsWith("_anonymous_")) {
        participant = asString;
        break;
      } else if (asString.startsWith("@")) {
        participant = "_anonymous_";
      }

    }

    return participant;
  }


  public void run() {

    JsonArray sourceArray = source.getAsJsonArray();

    int max = sourceArray.size();
    for (int i = 0; i < max; i++) {



      String waveId = null;
      String participant = null;

      try {

        JsonObject sourceItem = sourceArray.get(i).getAsJsonObject();

        waveId = sourceItem.get("wave_id").getAsString();
        participant = getValidParticipant(sourceItem.getAsJsonArray("participants"));

        if (waveId == null)
          throw new RuntimeException("Wave id not found");

        if (participant == null)
          throw new RuntimeException("Participant address not found");

        PerWaveMigrator migrator = new PerWaveMigrator(""+i, url, domain, waveId, participant);

        Thread t = new Thread(migrator);
        t.start();
        t.join();
        out.println(migrator.getLog());

      } catch (InterruptedException e) {
        out.println("Item " + i + ":" + waveId + ":" + participant + ":InterruptedException:"
            + e.getMessage());
      } catch (Exception e) {
        out.println("Item " + i + ":" + waveId + ":" + participant + ":Exception:" + e.getMessage());
      }


    }

    WaveSocketWAsync.asyncHttpClientProvider.getClient().closeAsynchronously();
  }

  private static void logInfo(String s) {
    System.out.println(s);
  }

  private static void logError(String s) {
    System.err.println(s);
  }

  public static void main(String[] args) throws MalformedURLException, InvalidParticipantAddress,
      InterruptedException {

    logInfo("SwellRT model migrator to version 1.0");

    String url = args[0];
    String domain = args[1];
    String inputFile = args[2];

    JsonElement jsonRootElement = null;
    try {
      FileReader reader;
      reader = new FileReader(inputFile);

      JsonParser jsonParser = new JsonParser();
      jsonRootElement = jsonParser.parse(reader);

    } catch (FileNotFoundException e) {
      logError(e.getMessage());
      return;
    } catch (JsonParseException e) {
      logError(e.getMessage());
      return;
    }

    if (!jsonRootElement.isJsonArray()) {
      logError("Expected Json array not found as root object");
      return;
    }

    ModelMigrator m = new ModelMigrator(jsonRootElement, url, domain, System.out);
    m.run();

    /*
     * ModelMigrator m = new ModelMigrator(null, "http://localhost:9898",
     * "prototype.p2pvalue.eu", System.out);
     */

    /*
     * try {
     * 
     * PerWaveMigrator migrator = new PerWaveMigrator("",
     * "http://localhost:9898", "prototype.p2pvalue.eu",
     * "prototype.p2pvalue.eu/s+xxx", "xxx@prototype.p2pvalue.eu");
     * 
     * Thread t = new Thread(migrator); t.start();
     * 
     * t.join();
     * 
     * System.out.println(migrator.getLog()); migrator.close();
     * 
     * } catch (InterruptedException e) { System.out.println("Item " +
     * ":InterruptedException:" + e.getMessage()); } catch (Exception e) {
     * System.out.println("Item " + ":Exception:" + e.getMessage()); }
     * 
     * WaveSocketWAsync.asyncHttpClientProvider.getClient().closeAsynchronously()
     * ;
     */
  }


}
