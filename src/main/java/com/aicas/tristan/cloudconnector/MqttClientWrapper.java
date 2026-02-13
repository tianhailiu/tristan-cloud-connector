/*------------------------------------------------------------------------*
 * Copyright 2026, aicas GmbH; all rights reserved.
 * This header, including copyright notice, may not be altered or removed.
 *------------------------------------------------------------------------*/
package com.aicas.tristan.cloudconnector;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The {@code MqttClientWrapper} class encapsulates operations for connecting to,
 * publishing messages to, and disconnecting from an MQTT server using the Eclipse Paho MQTT client.
 * It simplifies the MQTT client's usage by providing a higher-level interface for MQTT operations.
 */
public class MqttClientWrapper
{
  private static final Logger log =
    org.slf4j.LoggerFactory.getLogger(MqttClientWrapper.class);
  private final String serverUri;
  private final String deviceName;
  private final String accessToken;
  private final String trustStorePath;
  private final String trustStorePassword;
  private MqttAsyncClient client;
  private final AtomicLong publishedCount = new AtomicLong(0);
  private final AtomicLong confirmedCount = new AtomicLong(0);
  private final AtomicLong failedCount = new AtomicLong(0);
  private final ConcurrentHashMap<IMqttDeliveryToken, Long> publishTimestamps = new ConcurrentHashMap<>();
  private final AtomicLong totalLatencyMs = new AtomicLong(0);
  private final AtomicLong latencySampleCount = new AtomicLong(0);

  /**
   * Constructs an instance of the MQTT client wrapper (plain TCP, no TLS).
   *
   * @param serverUri   the URI of the MQTT server to connect to.
   * @param deviceName  the device name.
   * @param accessToken the access token for authenticating with the MQTT server.
   */
  public MqttClientWrapper(String serverUri, String deviceName,
                           String accessToken)
  {
    this(serverUri, deviceName, accessToken, null, null);
  }

  /**
   * Constructs an instance of the MQTT client wrapper with optional TLS support.
   * When {@code trustStorePath} is non-null, the connection uses SSL/TLS and the
   * server URI should start with {@code ssl://}.
   *
   * @param serverUri          the URI of the MQTT server (e.g. {@code ssl://host:8883}).
   * @param deviceName         the device name.
   * @param accessToken        the access token for authenticating with the MQTT server.
   * @param trustStorePath     path to the JKS truststore containing the server certificate,
   *                           or {@code null} for plain TCP.
   * @param trustStorePassword password for the truststore, or {@code null}.
   */
  public MqttClientWrapper(String serverUri, String deviceName,
                           String accessToken, String trustStorePath,
                           String trustStorePassword)
  {
    this.serverUri = serverUri;
    this.deviceName = deviceName;
    this.accessToken = accessToken;
    this.trustStorePath = trustStorePath;
    this.trustStorePassword = trustStorePassword;
  }

  /**
   * Connects to the MQTT server using the provided server URI and access token.
   *
   * @throws MqttException if there is an error during the connection.
   */
  public void connect()
    throws
    MqttException
  {
    client = new MqttAsyncClient(serverUri, MqttAsyncClient.generateClientId(),
                                 new MemoryPersistence());
    client.setCallback(new MqttCallback()
    {
      @Override
      public void connectionLost(Throwable me)
      {
        log.error("Connection lost", me);
      }

      @Override
      public void messageArrived(String s, MqttMessage mqttMessage) throws Exception
      {
        log.debug("Message arrived: {}", s);
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken)
      {
        MqttDeliveryToken token = (MqttDeliveryToken) iMqttDeliveryToken;
        long confirmed = confirmedCount.incrementAndGet();
        Long publishTimeMs = publishTimestamps.remove(token.getMessageId());
        if (publishTimeMs != null)
        {
          long latencyMs = System.currentTimeMillis() - publishTimeMs;
          totalLatencyMs.addAndGet(latencyMs);
          latencySampleCount.incrementAndGet();
          log.debug("Message delivered id: {}, latency: {} ms, confirmed so far: {}",
                    token.getMessageId(), latencyMs, confirmed);
        }
        else
        {
          log.debug("Message delivered id: {}, confirmed so far: {}",
                    token.getMessageId(), confirmed);
        }
      }
    });

    MqttConnectOptions options = new MqttConnectOptions();
    if (!serverUri.contains("test.mosquitto.org"))
    {
      options.setUserName(accessToken);
    }
    else
    {
      log.info("Anonymous connection to test.mosquitto.org (skipping credentials)");
    }
    options.setAutomaticReconnect(true);
    options.setConnectionTimeout(60);
    options.setKeepAliveInterval(60);

    if (trustStorePath != null)
    {
      try
      {
        options.setSocketFactory(createSslSocketFactory(trustStorePath,
                                                        trustStorePassword));
        log.info("TLS enabled using truststore: {}", trustStorePath);
      }
      catch (Exception e)
      {
        throw new MqttException(e);
      }
    }

    try
    {
      client.connect(options, null, new IMqttActionListener()
      {
        @Override
        public void onSuccess(IMqttToken iMqttToken)
        {
          log.info("Connected to Cloud.");
        }

        @Override
        public void onFailure(IMqttToken iMqttToken, Throwable e)
        {
          log.error("Failed to connect {} to Cloud", deviceName, e);
        }
      }).waitForCompletion();
    }
    catch (MqttException e)
    {
      log.error("Connect failed (reasonCode={}): {}", e.getReasonCode(), e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Publishes a message to the specified MQTT topic.
   *
   * @param topic the MQTT topic to publish the message to.
   * @param payload the payload of the message to publish.
   * @throws MqttException if there is an error during the publishing of the message.
   */
  public void publish(String topic, String payload)
    throws
    MqttException
  {
    MqttMessage message = new MqttMessage(payload.getBytes());
    message.setQos(1);
    try
    {
      IMqttDeliveryToken token = client.publish(topic, message);
      publishTimestamps.put(token, System.currentTimeMillis());
      publishedCount.incrementAndGet();
    }
    catch (MqttException e)
    {
      failedCount.incrementAndGet();
      throw e;
    }
  }

  /**
   * Returns the number of messages successfully submitted for publishing.
   *
   * @return the published message count.
   */
  public long getPublishedCount()
  {
    return publishedCount.get();
  }

  /**
   * Returns the number of messages whose delivery has been confirmed by the broker.
   *
   * @return the confirmed message count.
   */
  public long getConfirmedCount()
  {
    return confirmedCount.get();
  }

  /**
   * Returns the number of messages that failed to be submitted for publishing.
   *
   * @return the failed message count.
   */
  public long getFailedCount()
  {
    return failedCount.get();
  }

  /**
   * Returns the average publish-to-ack latency in microseconds,
   * or -1 if no samples have been recorded.
   *
   * @return the average latency in milliseconds.
   */
  public double getAverageLatencyMs()
  {
    long samples = latencySampleCount.get();
    if (samples == 0)
    {
      return -1;
    }
    return totalLatencyMs.get() / (double) samples;
  }

  /**
   * Returns the number of latency samples recorded.
   *
   * @return the latency sample count.
   */
  public long getLatencySampleCount()
  {
    return latencySampleCount.get();
  }

  /**
   * Disconnects from the MQTT server.
   *
   * @throws MqttException if there is an error during the disconnection.
   */
  public void disconnect()
    throws
    MqttException
  {
    if (client != null && client.isConnected())
    {
      client.disconnect().waitForCompletion();
      log.info("{} disconnected from Cloud.", deviceName);
    }
  }

  public String getServerUri()
  {
    return this.serverUri;
  }

  public String getAccessToken()
  {
    return this.accessToken;
  }

  public String getDeviceName()
  {
    return deviceName;
  }

  public MqttAsyncClient getClient()
  {
    return this.client;
  }

  public void setClient(MqttAsyncClient client)
  {
    this.client = client;
  }

  /**
   * Creates an {@link SSLSocketFactory} that trusts the server certificate(s)
   * stored in the given JKS truststore. This enables one-way TLS where the
   * client verifies the server's identity.
   *
   * @param trustStorePath     path to the JKS truststore file.
   * @param trustStorePassword password for the truststore.
   * @return an SSLSocketFactory configured with the truststore.
   * @throws Exception if the truststore cannot be loaded or the SSL context
   *                   cannot be initialized.
   */
  private static SSLSocketFactory createSslSocketFactory(String trustStorePath,
                                                         String trustStorePassword)
    throws
    Exception
  {
    KeyStore trustStore = KeyStore.getInstance("JKS");
    try (InputStream tsInputStream = new FileInputStream(trustStorePath))
    {
      trustStore.load(tsInputStream,
                      trustStorePassword != null
                        ? trustStorePassword.toCharArray()
                        : null);
    }

    TrustManagerFactory tmf =
      TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(trustStore);

    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
    sslContext.init(null, tmf.getTrustManagers(), null);
    return sslContext.getSocketFactory();
  }
}
