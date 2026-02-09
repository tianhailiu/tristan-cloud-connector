/*------------------------------------------------------------------------*
 * Copyright 2026, aicas GmbH; all rights reserved.
 * This header, including copyright notice, may not be altered or removed.
 *------------------------------------------------------------------------*/
package com.aicas.tristan.cloudconnector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ApplicationConfig
{
  @JsonProperty("edg.server.uri")
  private String edgServerUri;

  private List<DeviceConfig> deviceConfigs;

  public String getEdgServerUri()
  {
    return this.edgServerUri;
  }

  public void setEdgServerUri(String edgServerUri)
  {
    this.edgServerUri = edgServerUri;
  }

  public List<DeviceConfig> getDeviceConfigs()
  {
    return this.deviceConfigs;
  }

  public void setDeviceConfigs(List<DeviceConfig> deviceConfigs)
  {
    this.deviceConfigs = deviceConfigs;
  }
}


