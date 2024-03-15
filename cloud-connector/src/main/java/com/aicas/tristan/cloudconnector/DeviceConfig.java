/*------------------------------------------------------------------------*
 * Copyright 2024, aicas GmbH; all rights reserved.
 * This header, including copyright notice, may not be altered or removed.
 *------------------------------------------------------------------------*/
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
