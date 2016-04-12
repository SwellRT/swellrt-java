package org.swellrt.java.script;

import org.swellrt.model.generic.Model;

public interface Script {

  public String execute(Model cObject);

  public boolean wasDataChanged();
}
