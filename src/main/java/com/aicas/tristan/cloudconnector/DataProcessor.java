/*------------------------------------------------------------------------*
 * Copyright 2024, aicas GmbH; all rights reserved.
 * This header, including copyright notice, may not be altered or removed.
 *------------------------------------------------------------------------*/
package com.aicas.tristan.cloudconnector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code DataProcessor} class is responsible for connecting to the MQTT server,
 * loading trace data from a specified source, and publishing this data to a specified topic.
 * It implements the {@code Runnable} interface, allowing it to be executed in a separate thread.
 */
public class DataProcessor implements Runnable
{
  private static final Logger log =
    org.slf4j.LoggerFactory.getLogger(DataProcessor.class);
  private final MqttClientWrapper mqttClient;
  private final String traceFile;
  private final int topN;

  /**
   * Constructs an instance of the DataProcessor.
   *
   * @param mqttClient the MQTT client wrapper to use for publishing messages.
   * @param traceFile  the path to the trace file.
   */
  public DataProcessor(MqttClientWrapper mqttClient, String traceFile)
  {
    this(mqttClient, traceFile, 0);
  }

  /**
   * Constructs an instance of the DataProcessor with a configurable top-N mode.
   * When {@code topN} is greater than zero, only the first N scalar (non-array)
   * signals from each data point are published.
   *
   * @param mqttClient the MQTT client wrapper to use for publishing messages.
   * @param traceFile  the path to the trace file.
   * @param topN       the number of scalar signals to include (0 or negative means all).
   */
  public DataProcessor(MqttClientWrapper mqttClient, String traceFile, int topN)
  {
    this.mqttClient = mqttClient;
    this.traceFile = traceFile;
    this.topN = topN;
  }

  /**
   * The run method invoked when the DataProcessor thread is started. It connects to
   * the MQTT server, loads trace data, and publishes it at configured intervals.
   */
  @Override
  public void run()
  {
    try
    {
      log.info("Connect {} to aicas EDG {}", mqttClient.getDeviceName(), mqttClient.getServerUri());
      mqttClient.connect();
      log.info("Load automotive traces {}", traceFile);
      List<Map<String, Object>> traceData = loadTraceData(traceFile);
      log.info("Sending data to aicas EDG");
      ObjectMapper mapper = new ObjectMapper();
      for (Map<String, Object> dataPoint : traceData)
      {
        Map<String, Object> effectivePayload =
          topN > 0 ? selectTopNScalars(dataPoint, topN) : dataPoint;
        String payload = mapper.writeValueAsString(effectivePayload);
        log.trace("{} publishes a message {}", mqttClient.getDeviceName(),
                  payload);
        mqttClient.publish("v1/devices/me/telemetry", payload);
        Thread.sleep(1000);
      }
    }
    catch (Exception e)
    {
      log.error("DataProcessor failed", e);
    }
    finally
    {
      try
      {
        log.info("Disconnect MQTT connection");
        mqttClient.disconnect();
      }
      catch (MqttException e)
      {
        log.warn("Failed to disconnect cleanly", e);
      }
    }
  }

  /**
   * Loads trace data from the specified file path. If the file exists locally,
   * it's loaded directly; otherwise, the method attempts to load an embedded
   * resource with the same name.
   *
   * @param traceFilePath the path to the trace file.
   * @return a List of Maps representing the trace data.
   * @throws Exception if there is an error loading the trace data.
   */
  private List<Map<String, Object>> loadTraceData(String traceFilePath)
    throws
    Exception
  {
    File traceFile = new File(traceFilePath);
    ObjectMapper objectMapper = new ObjectMapper();
    if (traceFile.exists())
    {
      log.info("loading automotive trace file from path {}", traceFile);
      return objectMapper.readValue(traceFile,
                                    new TypeReference<List<Map<String, Object>>>()
                                    {
                                    });
    }
    else
    {
      log.info("loading automotive trace file from stream {}", traceFile);
      InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream(traceFilePath);
      if (inputStream == null)
      {
        throw new IOException(
          "Embedded trace file not found: " + traceFilePath);
      }
      return objectMapper.readValue(inputStream,
                                    new TypeReference<List<Map<String, Object>>>()
                                    {
                                    });
    }
  }

  /**
   * Selects the first {@code n} scalar (non-collection) signals from the given
   * data point map, preserving the original insertion order.
   * Entries whose values are arrays/lists are skipped and not counted.
   *
   * @param dataPoint the original data point.
   * @param n         the maximum number of scalar signals to include.
   * @return a new map containing at most {@code n} scalar entries.
   */
  static Map<String, Object> selectTopNScalars(Map<String, Object> dataPoint,
                                               int n)
  {
    Map<String, Object> result = new LinkedHashMap<>();
    int count = 0;
    for (Map.Entry<String, Object> entry : dataPoint.entrySet())
    {
      if (count >= n)
      {
        break;
      }
      if (entry.getValue() instanceof List || entry.getValue() instanceof Object[])
      {
        continue; // skip array-like values and don't count them
      }
      result.put(entry.getKey(), entry.getValue());
      count++;
    }
    return result;
  }
}
