package org.swellrt.java;

import com.ning.http.client.AsyncHttpClient;

public interface AHCProvider {

  public AsyncHttpClient getClient();

  public boolean isSharedClient();

}
