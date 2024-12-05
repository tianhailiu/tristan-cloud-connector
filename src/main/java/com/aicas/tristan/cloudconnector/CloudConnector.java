/*------------------------------------------------------------------------*
 * Copyright 2024, aicas GmbH; all rights reserved.
 * This header, including copyright notice, may not be altered or removed.
 *------------------------------------------------------------------------*/
package com.aicas.tristan.cloudconnector;

import org.slf4j.Logger;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@CommandLine.Command(
  footer = "Copyright(c) 2024, aicas GmbH; all rights reserved.",
  description = "Sends automotive data to data visualization server supporting MQTT connection.",
  mixinStandardHelpOptions = true,
  versionProvider = ManifestVersionProvider.class)
public class CloudConnector implements Runnable
{
  @CommandLine.Option(
    names = { "-s", "--server" },
    description = "The MQTT service URI.",
    defaultValue = "tcp://demo-jamaicaedg.aicas.com:1883")
  private String serverURI;

  @CommandLine.Option(
    names = { "-d", "--device" },
    description = "The unique device name registered on the MQTT broker.",
    defaultValue = "Tristan-CloudConnector-Demo-Device")
  private String deviceName;

  @CommandLine.Option(
    names = { "-t", "--token" },
    description = "The token assigned to the device.",
    required = true)
  private String deviceToken;

  private static final Logger log =
    org.slf4j.LoggerFactory.getLogger(CloudConnector.class);

  public static void main(String[] args)
  {
    CloudConnector cloudConnector = new CloudConnector();
    CommandLine commandLine = new CommandLine(cloudConnector);
    cloudConnector.printDescription();
    commandLine.printVersionHelp(System.out);
    System.out.println("-------------------------");
    int exitCode = commandLine.execute(args);
    System.exit(exitCode);
  }

  private void printDescription()
  {
    String description = "TRISTAN Cloud Connector\n" +
      "========================\n" +
      "Tristan Cloud Connector is an OSGi bundle developed by aicas as part of the contributions to the EU-funded project TRISTAN. The bundle connects to the Edge Data Gateway (aicas EDG) to manage automotive data streams. This bundle is designed to run within an OSGi container, such as aicas JamaicaAMS and Apache Felix, but can also be packaged as a fat JAR for standalone execution.\n" +
      "Copyright(c) 2024, aicas GmbH; all rights reserved.";
    System.out.println(description);
  }

  @Override
  public void run()
  {
    ExecutorService executorService = Executors.newCachedThreadPool();
    MqttClientWrapper mqttClient =
      new MqttClientWrapper(serverURI, deviceName, deviceToken);
    DataProcessor dataProcessor =
      new DataProcessor(mqttClient, "automotive-trace.json");
    executorService.submit(dataProcessor);

    Runtime.getRuntime()
      .addShutdownHook(new Thread(executorService::shutdownNow));

    try
    {
      Thread.currentThread().join();
    }
    catch (InterruptedException e)
    {
      throw new RuntimeException(e);
    }
  }

  private void printLogo()
  {
    String artFile = "ascii-text-art.txt";
    try (InputStream inputStream = getClass().getClassLoader()
      .getResourceAsStream(artFile);
         InputStreamReader inputStreamReader = new InputStreamReader(
           Objects.requireNonNull(inputStream));
         BufferedReader reader = new BufferedReader(inputStreamReader))
    {
      while (reader.ready())
      {
        System.out.println(reader.readLine());
      }
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }
}

class ManifestVersionProvider implements CommandLine.IVersionProvider
{
  @Override
  public String[] getVersion()
    throws
    Exception
  {
    String version = VersionUtil.getVersion();
    return new String[]{ "Cloud Connector version: " + version };
  }
}

class VersionUtil
{
  public static String getVersion()
  {
    Package pkg = VersionUtil.class.getPackage();
    return (pkg != null && pkg.getImplementationVersion() != null)
      ? pkg.getImplementationVersion()
      : "Unknown version";
  }
}
