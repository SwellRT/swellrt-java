package org.swellrt.java;

import org.waveprotocol.wave.common.logging.AbstractLogger.Level;
import org.waveprotocol.wave.common.logging.Logger;
import org.waveprotocol.wave.common.logging.LoggerBundle;

public class JavaLoggerBundle implements LoggerBundle {

  private JavaLogger traceLogger;
  private JavaLogger errorLogger;
  private JavaLogger fatalLogger;
  private JavaLogger infoLogger; // Hacking original implementation

  private final String tag;
  private final boolean toInfoLogger;

  public JavaLoggerBundle(String tag) {
    this.tag = tag;
    this.toInfoLogger = false;
  }

  public JavaLoggerBundle(String tag, boolean toInfoLogger) {
    this.tag = tag;
    this.toInfoLogger = toInfoLogger;
  }

  @Override
  public void log(Level level, Object... messages) {

    if (toInfoLogger) {
      info().log(messages);
    } else if (level.equals(Level.ERROR)) {
      error().log(messages);
    } else if (level.equals(Level.TRACE)) {
      trace().log(messages);
    } else if (level.equals(Level.FATAL)) {
      fatal().log(messages);
    }

  }

  @Override
  public Logger trace() {

    if (traceLogger == null) {
      if (toInfoLogger)
        traceLogger = new JavaLogger(tag);
      else
        traceLogger = new JavaLogger(tag, Level.TRACE);
    }

    return traceLogger;
  }

  @Override
  public Logger error() {

    if (errorLogger == null)
      if (toInfoLogger)
        errorLogger = new JavaLogger(tag);
      else
        errorLogger = new JavaLogger(tag, Level.ERROR);

    return errorLogger;
  }

  @Override
  public Logger fatal() {

    if (fatalLogger == null)
      if (toInfoLogger)
        fatalLogger = new JavaLogger(tag);
      else
        fatalLogger = new JavaLogger(tag, Level.FATAL);

    return fatalLogger;

  }

  private Logger info() {
    if (infoLogger == null)
      infoLogger = new JavaLogger(tag);

    return info();
  }

  @Override
  public boolean isModuleEnabled() {
    return true;
  }

}
