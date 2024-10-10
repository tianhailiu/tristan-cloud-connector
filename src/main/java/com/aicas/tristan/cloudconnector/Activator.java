/*------------------------------------------------------------------------*
 * Copyright 2024, aicas GmbH; all rights reserved.
 * This header, including copyright notice, may not be altered or removed.
 *------------------------------------------------------------------------*/
package com.aicas.tristan.cloudconnector;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Activator implements BundleActivator
{
  private static final Logger log =
    org.slf4j.LoggerFactory.getLogger(Activator.class);
  private ExecutorService executorService;
  private MqttClientWrapper mqttClient;

  @Override
  public void start(BundleContext context)
    throws
    Exception
  {
    String serverUri = System.getProperty("edg.server.uri",
                                          "tcp://demo-jamaicaedg.aicas.com:1883");
    String deviceName = System.getProperty("edg.device.name",
                                           "Tristan-CloudConnector-Demo-Device");
    String deviceToken = System.getProperty("edg.device.token", "fake-token");

    executorService = Executors.newCachedThreadPool();
    mqttClient = new MqttClientWrapper(serverUri, deviceName, deviceToken);
    DataProcessor dataProcessor = new DataProcessor(mqttClient, "automotive-trace.json");
    executorService.submit(dataProcessor);
  }

  @Override
  public void stop(BundleContext context)
    throws
    Exception
  {
    if (executorService != null)
    {
      executorService.shutdownNow();
    }

    if (mqttClient != null)
    {
      try
      {
        mqttClient.disconnect();
      }
      catch (Exception e)
      {
        log.error("Exception happens when disconnecting MQTT client", e);
      }
    }
  }
}

