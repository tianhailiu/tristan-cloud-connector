package com.aicas.tristan.cloudconnector;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DataProcessorTest
{

  private MqttClientWrapper mockMqttClient;
  private DeviceConfig deviceConfig;
  private DataProcessor dataProcessor;

  @BeforeEach
  void setUp()
  {
    mockMqttClient = mock(MqttClientWrapper.class);
    deviceConfig = new DeviceConfig();
    deviceConfig.setName("testDevice");
    deviceConfig.setToken("testToken");
    deviceConfig.setTrace("testTrace.json");
    deviceConfig.setDelay(500);

    dataProcessor = new DataProcessor(mockMqttClient, deviceConfig);
  }

  @Test
  void testRunSuccessfulPublish()
    throws
    Exception
  {
    // Simulate successful connection and publishing
    doNothing().when(mockMqttClient).connect();
    doNothing().when(mockMqttClient).publish(anyString(), anyString());

    dataProcessor.run();

    verify(mockMqttClient, times(1)).connect();
    // Verify publish was called
    verify(mockMqttClient, atLeastOnce()).publish(anyString(), anyString());
    verify(mockMqttClient, times(1)).disconnect();
  }

  @Test
  void testRunWithConnectionFailure()
    throws
    Exception
  {
    // Simulate a connection failure
    doThrow(
      new MqttException(MqttException.REASON_CODE_BROKER_UNAVAILABLE)).when(
      mockMqttClient).connect();

    dataProcessor.run();

    verify(mockMqttClient, times(1)).connect();
    // Ensure publish is never called due to connection failure
    verify(mockMqttClient, never()).publish(anyString(), anyString());
    // Disconnect may still be called to ensure clean-up
    verify(mockMqttClient, times(1)).disconnect();
  }
}
