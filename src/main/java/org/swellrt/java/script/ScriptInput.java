package org.swellrt.java.script;

import com.google.gson.JsonObject;

public interface ScriptInput {

  public boolean open();

  public JsonObject next();

  public void close();

}
