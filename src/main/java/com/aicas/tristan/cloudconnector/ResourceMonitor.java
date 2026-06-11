/*------------------------------------------------------------------------*
 * Copyright 2026, aicas GmbH; all rights reserved.
 * This header, including copyright notice, may not be altered or removed.
 *------------------------------------------------------------------------*/
package com.aicas.tristan.cloudconnector;

import org.slf4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Locale;

/**
 * Monitors and logs Java heap usage.
 *
 * Memory usage:
 *  - Prefer RTSJ HeapMemory (JamaicaVM) if available.
 *  - Fallback to MemoryMXBean.
 *  - Final fallback: Runtime totalMemory - freeMemory.
 *
 * Tracks:
 *  - Current heap usage
 *  - Running average heap usage
 *  - Peak heap usage
 */
class ResourceMonitor
{
  private static final Logger log =
    org.slf4j.LoggerFactory.getLogger(ResourceMonitor.class);

  private final MemorySource memorySource;

  private long sampleCount = 0;
  private long cumulativeUsedHeapBytes = 0;
  private long peakHeapBytes = 0;

  ResourceMonitor()
  {
    this.memorySource = MemorySource.createBestEffort();

    log.info("ResourceMonitor using Memory source: {}",
             memorySource.name());
  }

  /**
   * Sample current heap usage and log current, average, and peak values.
   */
  void sample(String label)
  {
    long usedHeapBytes = memorySource.readUsedHeapBytes();
    long maxHeapBytes  = memorySource.readMaxHeapBytes();

    sampleCount++;
    cumulativeUsedHeapBytes += usedHeapBytes;

    if (usedHeapBytes > peakHeapBytes)
    {
      peakHeapBytes = usedHeapBytes;
    }

    long avgHeapBytes = cumulativeUsedHeapBytes / sampleCount;

    log.debug("[{}] Heap used: {}, avg used: {}, peak: {}, max: {}",
             label,
             formatBytes(usedHeapBytes),
             formatBytes(avgHeapBytes),
             formatBytes(peakHeapBytes),
             (maxHeapBytes > 0 ? formatBytes(maxHeapBytes) : "n/a"));
  }

  /**
   * Log final summary.
   */
  void logSummary()
  {
    if (sampleCount == 0)
    {
      log.info("Resource monitor: no samples collected.");
      return;
    }

    long avgHeapBytes = cumulativeUsedHeapBytes / sampleCount;

    log.info("Resource monitor summary over {} samples — avg heap: {}, peak heap: {}",
             sampleCount,
             formatBytes(avgHeapBytes),
             formatBytes(peakHeapBytes));
  }

  // --------------------------------------------------------------------------
  // Memory sources
  // --------------------------------------------------------------------------

  private interface MemorySource
  {
    long readUsedHeapBytes();
    long readMaxHeapBytes();
    String name();

    static MemorySource createBestEffort()
    {
      MemorySource rtsj = tryRtsjHeapMemory();
      if (rtsj != null) return rtsj;

      MemorySource mx = tryMemoryMxBean();
      if (mx != null) return mx;

      return new MemorySource()
      {
        @Override public long readUsedHeapBytes()
        {
          Runtime rt = Runtime.getRuntime();
          return rt.totalMemory() - rt.freeMemory();
        }

        @Override public long readMaxHeapBytes()
        {
          return Runtime.getRuntime().maxMemory();
        }

        @Override public String name()
        {
          return "Runtime.totalMemory-freeMemory";
        }
      };
    }

    static MemorySource tryRtsjHeapMemory()
    {
      try
      {
        Class<?> heapMemCls = Class.forName("javax.realtime.HeapMemory");
        java.lang.reflect.Method instanceM = heapMemCls.getMethod("instance");
        Object heapMem = instanceM.invoke(null);

        java.lang.reflect.Method consumedM = heapMemCls.getMethod("memoryConsumed");
        java.lang.reflect.Method remainingM = heapMemCls.getMethod("memoryRemaining");

        return new MemorySource()
        {
          @Override public long readUsedHeapBytes()
          {
            try
            {
              Object v = consumedM.invoke(heapMem);
              return (v instanceof Long) ? (Long) v : -1L;
            }
            catch (Throwable ignored) { return -1L; }
          }

          @Override public long readMaxHeapBytes()
          {
            try
            {
              Object c = consumedM.invoke(heapMem);
              Object r = remainingM.invoke(heapMem);
              if (c instanceof Long && r instanceof Long)
              {
                return (Long) c + (Long) r;
              }
            }
            catch (Throwable ignored) {}
            return -1L;
          }

          @Override public String name()
          {
            return "RTSJ HeapMemory.memoryConsumed";
          }
        };
      }
      catch (Throwable t)
      {
        return null;
      }
    }

    static MemorySource tryMemoryMxBean()
    {
      try
      {
        final MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        return new MemorySource()
        {
          @Override public long readUsedHeapBytes()
          {
            MemoryUsage u = mem.getHeapMemoryUsage();
            return u != null ? u.getUsed() : -1L;
          }

          @Override public long readMaxHeapBytes()
          {
            MemoryUsage u = mem.getHeapMemoryUsage();
            return u != null ? u.getMax() : -1L;
          }

          @Override public String name()
          {
            return "MemoryMXBean.getHeapMemoryUsage";
          }
        };
      }
      catch (Throwable t)
      {
        return null;
      }
    }
  }

  // --------------------------------------------------------------------------
  // Formatting helpers
  // --------------------------------------------------------------------------

  private static String formatBytes(long bytes)
  {
    if (bytes < 0) return "n/a";
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024)
      return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
    if (bytes < 1024L * 1024L * 1024L)
      return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    return String.format(Locale.ROOT, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
  }
}
