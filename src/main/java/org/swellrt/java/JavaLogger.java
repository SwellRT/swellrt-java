package org.swellrt.java;

import org.slf4j.LoggerFactory;
import org.waveprotocol.wave.common.logging.AbstractLogger.Level;
import org.waveprotocol.wave.common.logging.Logger;


public class JavaLogger implements Logger {

  final static org.slf4j.Logger Log = LoggerFactory.getLogger(JavaLogger.class);

  private final Level level;
  private final String tag;


  public JavaLogger(String tag, Level level) {
    this.level = level;
    this.tag = tag;
  }

  public JavaLogger(String tag) {
    this.level = null;
    this.tag = tag;
  }

  private String stringifyLogObject(Object o) {
    if (o instanceof Object[]) {
      StringBuilder builder = new StringBuilder();
      Object[] objects = (Object[]) o;
      for (Object object : objects) {
        builder.append(object.toString());
      }
      return builder.toString();
    } else {
      return o.toString();
    }
  }

  @Override
  public void log(String msg) {

    if (level == null) {
      Log.info("[" + tag + "] " + msg);
    } else if (level.equals(Level.ERROR)) {
      Log.warn("[" + tag + "] " + msg);
    } else if (level.equals(Level.FATAL)) {
      Log.error("[" + tag + "] " + msg);
    } else if (level.equals(Level.TRACE)) {
      Log.trace("[" + tag + "] " + msg);
    }

  }

  protected void logThrowable(String msg, Throwable t) {

    if (level == null) {
      Log.info("[" + tag + "] " + msg, t);
    } else if (level.equals(Level.ERROR)) {
      Log.warn("[" + tag + "] " + msg, t);
    } else if (level.equals(Level.FATAL)) {
      Log.error("[" + tag + "] " + msg, t);
    } else if (level.equals(Level.TRACE)) {
      Log.trace("[" + tag + "] " + msg, t);
    }
  }


  @Override
  public void log(Object... messages) {
    log(stringifyLogObject(messages));
  }

  @Override
  public void logPlainText(String msg) {
    log(msg);
  }

  @Override
  public void logPlainText(String msg, Throwable t) {
    logThrowable(msg, t);
  }

  @Override
  public void logXml(String xml) {
    log(xml);
  }

  @Override
  public void log(String label, Object o) {
    log(label + ": " + stringifyLogObject(o));
  }

  @Override
  public void log(Throwable t) {
    logThrowable("", t);
  }

  @Override
  public void log(String label, Throwable t) {
    logThrowable(label, t);
  }

  @Override
  public void logLazyObjects(Object... objects) {
    log(stringifyLogObject(objects));
  }

  @Override
  public boolean shouldLog() {
    return true;
  }

}
