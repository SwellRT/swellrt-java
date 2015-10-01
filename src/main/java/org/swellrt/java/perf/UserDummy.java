package org.swellrt.java.perf;

public class UserDummy extends User {

  @Override
  public void behaviour() {

    for (int i = 0; i < 10; i++) {
      log("Doing something #" + i);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        log("Interrupted");
        return;
      }
    }
  }


}
