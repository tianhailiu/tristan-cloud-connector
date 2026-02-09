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
    printDescription();
    String serverUri = System.getProperty("edg.server.uri",
                                          "tcp://demo-jamaicaedg.aicas.com:1883");
    String deviceName = System.getProperty("edg.device.name",
                                           "Tristan-CloudConnector-Demo-Device");
    String deviceToken = System.getProperty("edg.device.token", "fake-token");
    int topN = Integer.parseInt(System.getProperty("edg.top.n", "0"));
    String trustStorePath = System.getProperty("edg.truststore.path");
    String trustStorePassword = System.getProperty("edg.truststore.password");
    double frequencyHz =
      Double.parseDouble(System.getProperty("edg.frequency", "1.0"));

    executorService = Executors.newCachedThreadPool();
    mqttClient = new MqttClientWrapper(serverUri, deviceName, deviceToken,
                                       trustStorePath, trustStorePassword);
    DataProcessor dataProcessor =
      new DataProcessor(mqttClient, "automotive-trace.json", topN, frequencyHz);
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

  private void printDescription()
  {
    String description = "TRISTAN Cloud Connector\n" +
            "========================\n" +
            "Tristan Cloud Connector is an OSGi bundle developed by aicas as part of the contributions to the EU-funded project TRISTAN. The bundle connects to the Edge Data Gateway (aicas EDG) to manage automotive data streams. This bundle is designed to run within an OSGi container, such as aicas JamaicaAMS and Apache Felix, but can also be packaged as a fat JAR for standalone execution.\n" +
            "Copyright(c) 2024, aicas GmbH; all rights reserved.";
    System.out.println(description);
  }
}
