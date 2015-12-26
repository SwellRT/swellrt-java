package org.swellrt.java;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class DefaultAHCProvider implements AHCProvider {

  final static org.slf4j.Logger Log = LoggerFactory.getLogger(DefaultAHCProvider.class);

  public static boolean DISABLE_SSL_CHECK = false;


  private static AsyncHttpClient createInstance() {
    /*
     * Configure the Grizzly provider in the Async Http Client: <a href=
     * 'http://github.com/Atmosphere/wasync/wiki/Configuring-the-underlying-AHC-provider'>configure
     * AHC</a>
     */

    AsyncHttpClientConfig.Builder ahcConfigBuilder = new AsyncHttpClientConfig.Builder();

    // Allow connections from any server, please use only for debug purpose
    if (DISABLE_SSL_CHECK) {

      HostnameVerifier hostnameVerifier = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      };

      // References to AsycHttpClient, Grizzly and SSL
      // https://groups.google.com/forum/#!topic/asynchttpclient/wgaAs3lszbI
      // http://stackoverflow.com/questions/21833804/how-to-make-https-calls-using-asynchttpclient

      // Issue 93740: Lollipop breaks SSL/TLS connections when using Jetty
      // https://code.google.com/p/android/issues/detail?id=93740

      // Support for SSL connections accepting self signed cert
      SSLContext sslContext = null;
      try {
        sslContext = SSLContext.getInstance("TLS");

        sslContext.init(null, new X509TrustManager[] { new X509TrustManager() {

          public void checkClientTrusted(X509Certificate[] chain, String authType)
              throws CertificateException {
          }

          public void checkServerTrusted(X509Certificate[] chain, String authType)
              throws CertificateException {
          }

          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
          }

        } }, new SecureRandom());

      } catch (Exception e) {
        Log.error("Error creating SSLContext", e);
      }

      ahcConfigBuilder.setSSLContext(sslContext).setHostnameVerifier(hostnameVerifier);

    }

    // Using Grizzly Async Http Provider
    // AsyncHttpClient ahc = new AsyncHttpClient(new
    // GrizzlyAsyncHttpProvider(ahcConfig), ahcConfig);

    return new AsyncHttpClient(ahcConfigBuilder.build());

  }

  private static AsyncHttpClient INSTANCE = null;

  @Override
  public AsyncHttpClient getClient() {
    /*
     * if (INSTANCE == null) INSTANCE = createInstance();
     */
    return createInstance();
  }

  @Override
  public boolean isSharedClient() {
    return false;
  }

}
