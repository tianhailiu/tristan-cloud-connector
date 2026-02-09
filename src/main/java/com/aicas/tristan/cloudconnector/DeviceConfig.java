/*------------------------------------------------------------------------*
 * Copyright 2026, aicas GmbH; all rights reserved.
 * This header, including copyright notice, may not be altered or removed.
 *------------------------------------------------------------------------*/
package com.aicas.tristan.cloudconnector;

public class DeviceConfig
{
  private String name;
  private String token;
  private String trace;
  private long delay;

  public String getName()
  {
    return this.name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public String getToken()
  {
    return this.token;
  }

  public void setToken(String token)
  {
    this.token = token;
  }

  public String getTrace()
  {
    return this.trace;
  }

  public void setTrace(String trace)
  {
    this.trace = trace;
  }

  public long getDelay()
  {
    return this.delay;
  }

  public void setDelay(long delay)
  {
    this.delay = delay;
  }
}
