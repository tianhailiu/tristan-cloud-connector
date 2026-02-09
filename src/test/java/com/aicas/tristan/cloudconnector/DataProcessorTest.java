/*------------------------------------------------------------------------*
 * Copyright 2024, aicas GmbH; all rights reserved.
 * This header, including copyright notice, may not be altered or removed.
 *------------------------------------------------------------------------*/
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class DataProcessorTest
{

  private MqttClientWrapper mockMqttClient;
  private DataProcessor dataProcessor;

  @BeforeEach
  void setUp()
  {
    mockMqttClient = mock(MqttClientWrapper.class);
    dataProcessor = new DataProcessor(mockMqttClient, "testTrace.json");
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

  @Test
  void testSelectTopNScalarsReturnsOnlyScalarEntries()
  {
    Map<String, Object> dataPoint = new LinkedHashMap<>();
    dataPoint.put("motor-torque-left", 22.93);
    dataPoint.put("P-min-index", 9515.28);
    dataPoint.put("trip-battery-stress", 0.0068);
    dataPoint.put("P-total", Arrays.asList(1477946.65, 1408618.32)); // array, should be skipped
    dataPoint.put("engine-torque", 11.46);
    dataPoint.put("P-batt", Arrays.asList(-1.49, -1.42)); // array, should be skipped

    Map<String, Object> result = DataProcessor.selectTopNScalars(dataPoint, 2);

    assertEquals(2, result.size());
    assertEquals(22.93, result.get("motor-torque-left"));
    assertEquals(9515.28, result.get("P-min-index"));
    assertFalse(result.containsKey("P-total"));
    assertFalse(result.containsKey("P-batt"));
  }

  @Test
  void testSelectTopNScalarsSkipsAllArrays()
  {
    Map<String, Object> dataPoint = new LinkedHashMap<>();
    dataPoint.put("P-total", Arrays.asList(1.0, 2.0));
    dataPoint.put("P-batt", Arrays.asList(3.0, 4.0));
    dataPoint.put("speed-mph", 55.0);

    Map<String, Object> result = DataProcessor.selectTopNScalars(dataPoint, 5);

    assertEquals(1, result.size());
    assertEquals(55.0, result.get("speed-mph"));
  }

  @Test
  void testSelectTopNScalarsWithZeroReturnsEmpty()
  {
    Map<String, Object> dataPoint = new LinkedHashMap<>();
    dataPoint.put("speed-mph", 55.0);
    dataPoint.put("engine-torque", 11.0);

    Map<String, Object> result = DataProcessor.selectTopNScalars(dataPoint, 0);

    assertEquals(0, result.size());
  }

  @Test
  void testSelectTopNScalarsLargerThanAvailable()
  {
    Map<String, Object> dataPoint = new LinkedHashMap<>();
    dataPoint.put("speed-mph", 55.0);
    dataPoint.put("P-total", Arrays.asList(1.0));
    dataPoint.put("engine-torque", 11.0);

    Map<String, Object> result = DataProcessor.selectTopNScalars(dataPoint, 10);

    assertEquals(2, result.size());
    assertEquals(55.0, result.get("speed-mph"));
    assertEquals(11.0, result.get("engine-torque"));
  }
}
