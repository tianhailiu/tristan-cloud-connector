/*------------------------------------------------------------------------*
 * Copyright 2024, aicas GmbH; all rights reserved.
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

import java.net.ConnectException;

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
  private MqttAsyncClient client;

  /**
   * Constructs an instance of the MQTT client wrapper.
   *
   * @param serverUri the URI of the MQTT server to connect to.
   * @param deviceName the device name
   * @param accessToken the access token for authenticating with the MQTT server.
   */
  public MqttClientWrapper(String serverUri, String deviceName,
                           String accessToken)
  {
    this.serverUri = serverUri;
    this.deviceName = deviceName;
    this.accessToken = accessToken;
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
        log.error("Connection lost: {}", me.getMessage());
      }

      @Override
      public void messageArrived(String s, MqttMessage mqttMessage)
        throws
        Exception
      {
        log.debug("Message arrived: {}", s);
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken)
      {
        MqttDeliveryToken token = (MqttDeliveryToken) iMqttDeliveryToken;
        log.debug("Message delivered id: {}", token.getMessageId());
      }
    });
    MqttConnectOptions options = new MqttConnectOptions();
    options.setUserName(accessToken);
    options.setAutomaticReconnect(true);
    options.setConnectionTimeout(60);
    options.setKeepAliveInterval(60);
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
        log.error("Failed to connect {} to Cloud: {}", deviceName,
                  e.getMessage());
      }
    }).waitForCompletion();
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
    client.publish(topic, message);
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
}
