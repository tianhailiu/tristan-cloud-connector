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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
  private final long publishIntervalMs;

  /**
   * Constructs an instance of the DataProcessor.
   *
   * @param mqttClient the MQTT client wrapper to use for publishing messages.
   * @param traceFile  the path to the trace file.
   */
  public DataProcessor(MqttClientWrapper mqttClient, String traceFile)
  {
    this(mqttClient, traceFile, 0, 1.0);
  }

  /**
   * Constructs an instance of the DataProcessor with a configurable top-N mode.
   *
   * @param mqttClient the MQTT client wrapper to use for publishing messages.
   * @param traceFile  the path to the trace file.
   * @param topN       the number of scalar signals to include (0 or negative means all).
   */
  public DataProcessor(MqttClientWrapper mqttClient, String traceFile, int topN)
  {
    this(mqttClient, traceFile, topN, 1.0);
  }

  /**
   * Constructs an instance of the DataProcessor with configurable top-N mode
   * and publish frequency.
   *
   * @param mqttClient  the MQTT client wrapper to use for publishing messages.
   * @param traceFile   the path to the trace file.
   * @param topN        the number of scalar signals to include (0 or negative means all).
   * @param frequencyHz the publishing frequency in messages per second (e.g. 2.0 = 2 msg/s).
   *                    Must be positive. Default is 1.0 (one message per second).
   */
  public DataProcessor(MqttClientWrapper mqttClient, String traceFile,
                       int topN, double frequencyHz)
  {
    if (frequencyHz <= 0)
    {
      throw new IllegalArgumentException(
        "Frequency must be positive, got: " + frequencyHz);
    }
    this.mqttClient = mqttClient;
    this.traceFile = traceFile;
    this.topN = topN;
    this.publishIntervalMs = Math.round(1000.0 / frequencyHz);
  }

  /**
   * The run method invoked when the DataProcessor thread is started. It connects to
   * the MQTT server, loads trace data, and publishes it at configured intervals.
   */
  @Override
  public void run()
  {
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    try
    {
      log.info("Connect {} to aicas EDG {}", mqttClient.getDeviceName(),
               mqttClient.getServerUri());
      mqttClient.connect();
      log.info("Load automotive traces {}", traceFile);
      List<Map<String, Object>> traceData = loadTraceData(traceFile);
      log.info("Sending data to aicas EDG at interval {} ms",
               publishIntervalMs);

      ObjectMapper mapper = new ObjectMapper();
      Iterator<Map<String, Object>> iterator = traceData.iterator();
      CountDownLatch doneLatch = new CountDownLatch(1);

      scheduler.scheduleAtFixedRate(() ->
      {
        if (!iterator.hasNext())
        {
          doneLatch.countDown();
          return;
        }
        try
        {
          Map<String, Object> dataPoint = iterator.next();
          Map<String, Object> effectivePayload =
            topN > 0 ? selectTopNScalars(dataPoint, topN) : dataPoint;
          String payload = mapper.writeValueAsString(effectivePayload);
          log.trace("{} publishes a message {}",
                    mqttClient.getDeviceName(), payload);
          mqttClient.publish("v1/devices/me/telemetry", payload);
        }
        catch (Exception e)
        {
          log.warn("Failed to publish message, will retry on next tick", e);
        }
      }, 0, publishIntervalMs, TimeUnit.MILLISECONDS);

      doneLatch.await();
    }
    catch (Exception e)
    {
      log.error("DataProcessor failed", e);
    }
    finally
    {
      scheduler.shutdownNow();
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
