package com.aicas.tristan.cloudconnector;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@CommandLine.Command(name = "CloudConnector",
  mixinStandardHelpOptions = true,
  version = "CloudConnector 1.0.0-SNAPSHOT",
  description = "Publish MQTT messages to JamaicaEDG Dashboard.")
public class Main implements Callable<Integer>
{
  private static final String CONFIG_FILE_PATH = "config.json";

  public static void main(String[] args)
  {
    new CommandLine(new Main()).execute(args);
  }

  @Override
  public Integer call()
    throws
    Exception
  {
    log.error("Start running");
    ApplicationConfig config =
      ConfigurationLoader.loadApplicationConfig(CONFIG_FILE_PATH);
    ExecutorService executorService =
      Executors.newFixedThreadPool(config.getDeviceConfigs().size());

    for (DeviceConfig deviceConfig : config.getDeviceConfigs())
    {
      MqttClientWrapper mqttClient =
        new MqttClientWrapper(config.getEdgServerUri(),
                              deviceConfig.getToken());
      DataProcessor dataProcessor = new DataProcessor(mqttClient, deviceConfig);
      executorService.submit(dataProcessor);
    }

    Runtime.getRuntime()
      .addShutdownHook(new Thread(executorService::shutdownNow));

    return 0;
  }
}
