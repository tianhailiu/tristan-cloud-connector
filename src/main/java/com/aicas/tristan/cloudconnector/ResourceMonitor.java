/*------------------------------------------------------------------------*
 * Copyright 2024, aicas GmbH; all rights reserved.
 * This header, including copyright notice, may not be altered or removed.
 *------------------------------------------------------------------------*/
package com.aicas.tristan.cloudconnector;

import org.slf4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import com.sun.management.OperatingSystemMXBean;

/**
 * Monitors and logs CPU and memory usage. Tracks cumulative values
 * to compute running averages across sampling points.
 */
class ResourceMonitor
{
  private static final Logger log =
    org.slf4j.LoggerFactory.getLogger(ResourceMonitor.class);

  private final OperatingSystemMXBean osMxBean;
  private final MemoryMXBean memoryMxBean;

  private long sampleCount = 0;
  private double cumulativeCpuPercent = 0.0;
  private long cumulativeUsedMemoryBytes = 0;

  ResourceMonitor()
  {
    osMxBean = (OperatingSystemMXBean)
      ManagementFactory.getOperatingSystemMXBean();
    memoryMxBean = ManagementFactory.getMemoryMXBean();
  }

  /**
   * Samples current CPU and memory usage, updates running averages,
   * and logs both the current and average values.
   *
   * @param label a descriptive label for this sampling point (e.g. "before publishing",
   *              "message #5").
   */
  void sample(String label)
  {
    double cpuLoad = osMxBean.getProcessCpuLoad();
    double cpuPercent = cpuLoad >= 0 ? cpuLoad * 100.0 : 0.0;

    MemoryUsage heapUsage = memoryMxBean.getHeapMemoryUsage();
    long usedMemoryBytes = heapUsage.getUsed();
    long maxMemoryBytes = heapUsage.getMax();

    sampleCount++;
    cumulativeCpuPercent += cpuPercent;
    cumulativeUsedMemoryBytes += usedMemoryBytes;

    double avgCpuPercent = cumulativeCpuPercent / sampleCount;
    long avgUsedMemoryBytes = cumulativeUsedMemoryBytes / sampleCount;

    log.info("[{}] CPU: {}, avg CPU: {} | Memory used: {}, avg used: {}, max: {}",
             label,
             formatPercent(cpuPercent),
             formatPercent(avgCpuPercent),
             formatBytes(usedMemoryBytes),
             formatBytes(avgUsedMemoryBytes),
             formatBytes(maxMemoryBytes));
  }

  /**
   * Logs a final summary of the average CPU and memory usage over all samples.
   */
  void logSummary()
  {
    if (sampleCount == 0)
    {
      log.info("Resource monitor: no samples collected.");
      return;
    }
    double avgCpuPercent = cumulativeCpuPercent / sampleCount;
    long avgUsedMemoryBytes = cumulativeUsedMemoryBytes / sampleCount;

    log.info("Resource monitor summary over {} samples — avg CPU: {}, avg memory used: {}",
             sampleCount,
             formatPercent(avgCpuPercent),
             formatBytes(avgUsedMemoryBytes));
  }

  private static String formatPercent(double percent)
  {
    return String.format("%.2f%%", percent);
  }

  private static String formatBytes(long bytes)
  {
    if (bytes < 1024)
    {
      return bytes + " B";
    }
    else if (bytes < 1024 * 1024)
    {
      return String.format("%.1f KB", bytes / 1024.0);
    }
    else
    {
      return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
  }
}
