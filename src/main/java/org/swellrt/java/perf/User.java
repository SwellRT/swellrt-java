package org.swellrt.java.perf;

import java.lang.reflect.Constructor;


public abstract class User implements Runnable {

  private static int counter = 0;

  public static User fromClass(String className, UserParemeters parameters, String name) {

    User user = null;

    try {
      @SuppressWarnings("rawtypes")
      Class userClass = Class.forName(className);
      @SuppressWarnings({ "unchecked", "rawtypes" })
      Constructor ctor = userClass.getConstructor();
      user = (User) ctor.newInstance();
      user.parameters = parameters;
      user.name = name;
      user.index = new Integer(counter++);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return user;

  }

  private UserParemeters parameters = null;
  private String name;
  private Integer index;

  protected void setName(String name) {
    this.name = name;
  }

  protected String getName() {
    return name;
  }

  protected int getIndex() {
    return index;
  }

  protected UserParemeters getParameters() {
    return parameters;
  }

  protected void log(String msg, long time) {
    long now = System.currentTimeMillis();
    System.out.printf("%d - %s - %s - %d - %s %n", now, name, Thread.currentThread().getName(),
        time, msg);
    System.out.flush();
  }

  protected void log(String msg) {
    long now = System.currentTimeMillis();
    System.out.printf("%d - %s - %s - %s %n", now, name, Thread.currentThread().getName(), msg);
    System.out.flush();
  }

  @Override
  public void run() {

    log("Start");
    behaviour();
    log("End");

  }

  public abstract void behaviour();

}
