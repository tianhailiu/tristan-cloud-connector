/*------------------------------------------------------------------------*
 * Copyright 2024, aicas GmbH; all rights reserved.
 * This header, including copyright notice, may not be altered or removed.
 *------------------------------------------------------------------------*/
package com.aicas.tristan.cloudconnector;

import lombok.extern.slf4j.Slf4j;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class Activator implements BundleActivator
{
  private ExecutorService executorService;
  private List<MqttClientWrapper> mqttClients;

  @Override
  public void start(BundleContext context)
    throws
    Exception
  {
    executorService = Executors.newCachedThreadPool();
    mqttClients = new ArrayList<>();

    ApplicationConfig config =
      ConfigurationLoader.loadApplicationConfig("config.json");

    for (DeviceConfig deviceConfig : config.getDeviceConfigs())
    {
      MqttClientWrapper mqttClient =
        new MqttClientWrapper(config.getEdgServerUri(),
                              deviceConfig.getToken());
      DataProcessor dataProcessor = new DataProcessor(mqttClient, deviceConfig);
      mqttClients.add(mqttClient);
      executorService.submit(dataProcessor);
    }
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

    for (MqttClientWrapper client : mqttClients)
    {
      try
      {
        client.disconnect();
      }
      catch (Exception e)
      {
        log.error("Exception happens when disconnecting MQTT client", e);
      }
    }
  }
}

