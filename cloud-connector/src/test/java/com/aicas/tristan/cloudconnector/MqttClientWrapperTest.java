package com.aicas.tristan.cloudconnector;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MqttClientWrapperTest
{

  @Mock
  private MqttAsyncClient mockClient;

  @Test
  void testPublish()
    throws
    Exception
  {
    MqttClientWrapper wrapper =
      new MqttClientWrapper("tcp://test-server:1883", "token");
    wrapper.setClient(mockClient);

    doNothing().when(mockClient).publish(anyString(), any(MqttMessage.class));

    wrapper.publish("test/topic", "test message");

    verify(mockClient, times(1)).publish(eq("test/topic"),
                                         any(MqttMessage.class));
  }
}
