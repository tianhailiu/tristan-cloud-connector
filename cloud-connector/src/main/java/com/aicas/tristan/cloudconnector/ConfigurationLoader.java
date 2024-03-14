package com.aicas.tristan.cloudconnector;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ConfigurationLoader
{

  public static ApplicationConfig loadApplicationConfig(String configFilePath)
    throws
    IOException
  {
    ObjectMapper objectMapper = new ObjectMapper();
    File configFile = new File(configFilePath);
    if (configFile.exists())
    {
      return objectMapper.readValue(configFile, ApplicationConfig.class);
    }
    else
    {
      InputStream inputStream = ApplicationConfig.class.getClassLoader()
        .getResourceAsStream(configFilePath);
      if (inputStream == null)
      {
        throw new RuntimeException(
          "Embedded config file not found: " + configFilePath);
      }
      return objectMapper.readValue(inputStream, ApplicationConfig.class);
    }
  }
}
