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

  /**
   * Constructs an instance of the DataProcessor.
   *
   * @param mqttClient the MQTT client wrapper to use for publishing messages.
   */
  public DataProcessor(MqttClientWrapper mqttClient, String traceFile)
  {
    this.mqttClient = mqttClient;
    this.traceFile = traceFile;
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
      log.info("Connect {} to MQTT broker", mqttClient.getDeviceName());
      mqttClient.connect();
      log.info("Load automotive traces");
      List<Map<String, Object>> traceData = loadTraceData(traceFile);
      log.info("Sending data to JamaicaEDG");
      ObjectMapper mapper = new ObjectMapper();
      for (Map<String, Object> dataPoint : traceData)
      {
        String payload = mapper.writeValueAsString(dataPoint);
        log.trace("{} publishes message {}", mqttClient.getDeviceName(),
                  payload);
        mqttClient.publish("v1/devices/me/telemetry", payload);
        Thread.sleep(1000);
      }
    }
    catch (Exception e)
    {
      log.error(e.getMessage());
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
        throw new RuntimeException(e);
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
      return objectMapper.readValue(traceFile,
                                    new TypeReference<List<Map<String, Object>>>()
                                    {
                                    });
    }
    else
    {
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
}
