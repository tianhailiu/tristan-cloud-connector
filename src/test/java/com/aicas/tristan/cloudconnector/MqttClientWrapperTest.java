/*------------------------------------------------------------------------*
 * Copyright 2024, aicas GmbH; all rights reserved.
 * This header, including copyright notice, may not be altered or removed.
 *------------------------------------------------------------------------*/
package com.aicas.tristan.cloudconnector;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MqttClientWrapperTest
{
  @Test
  void testPublish()
    throws
    Exception
  {
    MqttAsyncClient mockClient = mock(MqttAsyncClient.class);
    MqttClientWrapper wrapper =
      new MqttClientWrapper("tcp://test-server:1883", "deviceName", "deviceToken");
    wrapper.setClient(mockClient);

    doReturn(mock(IMqttDeliveryToken.class)).when(mockClient)
      .publish(anyString(), any(MqttMessage.class));

    wrapper.publish("test/topic", "test message");

    verify(mockClient, times(1)).publish(eq("test/topic"),
                                         any(MqttMessage.class));
  }
}
