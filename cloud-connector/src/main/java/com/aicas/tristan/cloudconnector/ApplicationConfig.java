package com.aicas.tristan.cloudconnector;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApplicationConfig
{
  @JsonProperty("edg.server.uri")
  private String edgServerUri;

  private List<DeviceConfig> deviceConfigs;
}


