package com.aicas.tristan.cloudconnector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * The {@code DataProcessor} class is responsible for connecting to the MQTT server,
 * loading trace data from a specified source, and publishing this data to a specified topic.
 * It implements the {@code Runnable} interface, allowing it to be executed in a separate thread.
 */
@Slf4j
public class DataProcessor implements Runnable
{
  private final MqttClientWrapper mqttClient;
  private final DeviceConfig deviceConfig;

  /**
   * Constructs an instance of the DataProcessor.
   *
   * @param mqttClient the MQTT client wrapper to use for publishing messages.
   * @param deviceConfig the configuration for the device, including trace file and delay.
   */
  public DataProcessor(MqttClientWrapper mqttClient, DeviceConfig deviceConfig)
  {
    this.mqttClient = mqttClient;
    this.deviceConfig = deviceConfig;
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
      mqttClient.connect();
      List<Map<String, Object>> traceData =
        loadTraceData(deviceConfig.getTrace());
      ObjectMapper mapper = new ObjectMapper();
      for (Map<String, Object> dataPoint : traceData)
      {
        String payload = mapper.writeValueAsString(dataPoint);
        mqttClient.publish("v1/devices/me/telemetry", payload);
        Thread.sleep(deviceConfig.getDelay());
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      try
      {
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
        throw new RuntimeException(
          "Embedded trace file not found: " + traceFilePath);
      }
      return objectMapper.readValue(inputStream,
                                    new TypeReference<List<Map<String, Object>>>()
                                    {
                                    });
    }
  }
}
