package com.aicas.tristan.cloudconnector;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceConfig
{
  private String name;
  private String token;
  private String trace;
  private long delay;
}
