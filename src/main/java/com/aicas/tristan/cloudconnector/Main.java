/*------------------------------------------------------------------------*
 * Copyright 2024, aicas GmbH; all rights reserved.
 * This header, including copyright notice, may not be altered or removed.
 *------------------------------------------------------------------------*/
package com.aicas.tristan.cloudconnector;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main
{
  public static void main(String[] args)
    throws
    IOException
  {
    String serverUri = System.getProperty("edg.server.uri",
                                          "tcp://demo-jamaicaedg.aicas.com:1883");
    String deviceName = System.getProperty("edg.device.name",
                                           "Tristan-CloudConnector-Demo-Device");
    String deviceToken = System.getProperty("edg.device.token", "fake-token");

    ExecutorService executorService = Executors.newCachedThreadPool();
    MqttClientWrapper mqttClient =
      new MqttClientWrapper(serverUri, deviceName, deviceToken);
    DataProcessor dataProcessor =
      new DataProcessor(mqttClient, "automotive-trace.json");
    executorService.submit(dataProcessor);

    Runtime.getRuntime()
      .addShutdownHook(new Thread(executorService::shutdownNow));
  }
}
