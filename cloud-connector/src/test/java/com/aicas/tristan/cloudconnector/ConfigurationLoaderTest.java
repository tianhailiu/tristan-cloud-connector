package com.aicas.tristan.cloudconnector;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationLoaderTest
{

  @Test
  void testLoadApplicationConfigValidFile()
    throws
    IOException
  {
    ApplicationConfig config = ConfigurationLoader.loadApplicationConfig(
      "src/test/resources/testConfig.json");
    assertNotNull(config);
    assertFalse(config.getDeviceConfigs().isEmpty());
    assertEquals("tcp://demo-jamaicaedg.aicas.com:1883",
                 config.getEdgServerUri());
  }

  @Test
  void testLoadApplicationConfigInvalidFile()
  {
    Exception exception = assertThrows(IOException.class, () ->
      ConfigurationLoader.loadApplicationConfig("nonexistent.json")
    );

    assertTrue(
      exception.getMessage().contains("Embedded config file not found"));
  }
}
