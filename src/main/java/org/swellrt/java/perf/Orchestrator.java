package org.swellrt.java.perf;

import java.util.ArrayList;
import java.util.List;

import org.waveprotocol.wave.concurrencycontrol.channel.ViewChannelImpl;

public class Orchestrator {

  protected String user;
  protected String password;

  protected String userClassName = "org.swellrt.java.perf.UserSimpleWriter";
  protected UserParemeters parameters;
  protected long startIntervalMs = 10000;
  protected int numberOfusers = 2;
  protected List<Thread> threads = new ArrayList<Thread>();

  protected void log(String msg) {
    long now = System.currentTimeMillis();
    System.out.printf("%d - %s - %s - %s %n", now, "Orchestrator",
        Thread.currentThread().getName(), msg);
    System.out.flush();
  }

  public void run() {

    // Set number of concurrent channels per Wave
    ViewChannelImpl.setMaxViewChannelsPerWave(5);

    parameters = new UserParemeters() {

      @Override
      public String getProperty(String name) {

        if (name.equalsIgnoreCase("waveid")) {
          return "local.net/s+DRmEIf7Rs3A";

        } else if (name.equalsIgnoreCase("user")) {
          return user;

        } else if (name.equalsIgnoreCase("password")) {
          return password;

        } else if (name.equalsIgnoreCase("server")) {
          return "http://localhost:9898";

        } else if (name.contentEquals("domain")) {
          return "local.net";
        }

        return null;
      }
    };

    threads.clear();

    log("Let's go!");

    Thread t = new Thread(User.fromClass(userClassName, parameters, "User "));
    t.start();

    try {

      t.join();

      // Launch all virtaul user
      // for (int i = 0; i < numberOfusers; i++) {
      // System.out.println("Creating user #" + i);
      // Thread t = new Thread(User.fromClass(userClassName, parameters,
      // "User #" + i));
      // threads.add(t);
      // t.start();
      // Thread.sleep(startIntervalMs);
      // }

      // Wait for all threads to continue
      // for (Thread t : threads) {
      // t.join();
      // }

      // Close underlying async http client
      // WaveSocketWAsync.asyncHttpClientProvider.getClient().closeAsynchronously();

      log("That's all folks!");

    } catch (InterruptedException e) {
      System.out.println("Orchestrator was interrumpted");
    }


  }

  public static void main(String[] args) {

    Orchestrator orch = new Orchestrator();
    orch.user = args[0];
    orch.password = args[1];
    orch.run();

  }

}
