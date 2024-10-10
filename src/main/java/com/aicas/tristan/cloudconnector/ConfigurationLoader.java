/*------------------------------------------------------------------------*
 * Copyright 2024, aicas GmbH; all rights reserved.
 * This header, including copyright notice, may not be altered or removed.
 *------------------------------------------------------------------------*/
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
        throw new IOException(
          "Embedded config file not found: " + configFilePath);
      }
      return objectMapper.readValue(inputStream, ApplicationConfig.class);
    }
  }
}
