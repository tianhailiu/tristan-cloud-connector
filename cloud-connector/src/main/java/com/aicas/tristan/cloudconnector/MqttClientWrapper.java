package com.aicas.tristan.cloudconnector;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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

import java.nio.charset.StandardCharsets;

/**
 * The {@code MqttClientWrapper} class encapsulates operations for connecting to,
 * publishing messages to, and disconnecting from an MQTT server using the Eclipse Paho MQTT client.
 * It simplifies the MQTT client's usage by providing a higher-level interface for MQTT operations.
 */
@Slf4j
@Getter
@Setter
public class MqttClientWrapper
{
  private final String serverUri;
  private final String accessToken;
  private MqttAsyncClient client;

  /**
   * Constructs an instance of the MQTT client wrapper.
   *
   * @param serverUri the URI of the MQTT server to connect to.
   * @param accessToken the access token for authenticating with the MQTT server.
   */
  public MqttClientWrapper(String serverUri, String accessToken)
  {
    this.serverUri = serverUri;
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
        System.out.println("Connection lost: " + me.getMessage());
      }

      @Override
      public void messageArrived(String s, MqttMessage mqttMessage)
        throws
        Exception
      {
        System.out.println("Message arrived: " + s);
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken)
      {
        MqttDeliveryToken token = (MqttDeliveryToken) iMqttDeliveryToken;
        try
        {
          System.out.println("Message delivered id: " + token.getMessage());
        }
        catch (MqttException e)
        {
          throw new RuntimeException(e);
        }
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
        System.out.println("Connected to Cloud.");
      }

      @Override
      public void onFailure(IMqttToken iMqttToken, Throwable e)
      {
        System.out.println("Failed to connect to Cloud: " + e.getMessage());
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
      System.out.println("Disconnected from Cloud.");
    }
  }

}
