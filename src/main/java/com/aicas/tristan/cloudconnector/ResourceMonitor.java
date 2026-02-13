/*------------------------------------------------------------------------*
 * Copyright 2026, aicas GmbH; all rights reserved.
 * This header, including copyright notice, may not be altered or removed.
 *------------------------------------------------------------------------*/
package com.aicas.tristan.cloudconnector;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Locale;

/**
 * Monitors and logs CPU and heap usage with JamaicaVM-friendly fallbacks.
 *
 * CPU:
 *  - JamaicaVM may not provide getProcessCpuLoad(); in that case this class
 *    computes CPU% from Linux /proc/self/stat (utime+stime) deltas.
 *  - Because /proc CPU time is tick-based (CLK_TCK=100 -> 10ms resolution),
 *    CPU% is computed over a minimum time window (default 1000ms) to avoid 0.00%.
 *
 * Memory:
 *  - Prefer RTSJ HeapMemory (javax.realtime.HeapMemory) if available.
 *  - Fallback to MemoryMXBean heap usage, then Runtime total-free.
 *
 * Optional JVM/system properties:
 *  -Dresource.monitor.cpu.window.ms=1000     (min window for CPU%, default 1000ms)
 *  -Dresource.monitor.clk_tck=100            (CLK_TCK, default 100; you confirmed 100)
 */
class ResourceMonitor
{
  private static final Logger log =
    org.slf4j.LoggerFactory.getLogger(ResourceMonitor.class);

  // ---- sampling state for averages ----
  private long sampleCount = 0;
  private double cumulativeCpuPercent = 0.0;
  private long cumulativeUsedHeapBytes = 0;

  // ---- CPU ----
  private final int cores;
  private final CpuLoadSource cpuLoadSource; // likely none on JamaicaVM
  private final CpuTimeTicksSource cpuTicksSource; // /proc/self/stat
  private final long cpuWindowNs;
  private final int clkTck;

  private long lastWallNs;
  private long lastCpuTicks;
  private long accWallNs = 0;
  private long accCpuTicks = 0;
  private double lastCpuPercent = 0.0;

  // Optional: CPU time per confirmed msg if you want later (left here for future)
  // private long lastConfirmedCount = 0;

  // ---- Memory ----
  private final MemorySource memorySource;

  ResourceMonitor()
  {
    this.cores = Math.max(1, Runtime.getRuntime().availableProcessors());

    // Try load source first (works on many HotSpot JVMs). JamaicaVM often doesn't provide it.
    this.cpuLoadSource = CpuLoadSource.tryCreate();

    // Always available on Linux: /proc/self/stat ticks (utime+stime).
    this.cpuTicksSource = new ProcSelfStatCpuTicksSource();

    this.clkTck = Integer.getInteger("resource.monitor.clk_tck", 100);
    long windowMs = Long.getLong("resource.monitor.cpu.window.ms", 1000L);
    this.cpuWindowNs = Math.max(100L, windowMs) * 1_000_000L;

    this.memorySource = MemorySource.createBestEffort();

    this.lastWallNs = System.nanoTime();
    this.lastCpuTicks = cpuTicksSource.readProcessCpuTicks();

    log.info("ResourceMonitor CPU load source: {}, CPU ticks source: {}, Memory source: {}, cores={}, CLK_TCK={}, cpuWindow={}ms",
             cpuLoadSource != null ? cpuLoadSource.name() : "none",
             cpuTicksSource.name(),
             memorySource.name(),
             cores,
             clkTck,
             windowMs);
  }

  /**
   * Samples current CPU and heap usage, updates running averages,
   * and logs both current and average values.
   */
  void sample(String label)
  {
    double cpuPercent = readCpuPercent();

    long usedHeapBytes = memorySource.readUsedHeapBytes();
    long maxHeapBytes = memorySource.readMaxHeapBytes();

    sampleCount++;
    cumulativeCpuPercent += cpuPercent;
    cumulativeUsedHeapBytes += usedHeapBytes;

    double avgCpuPercent = cumulativeCpuPercent / sampleCount;
    long avgUsedHeapBytes = cumulativeUsedHeapBytes / sampleCount;

    log.info("[{}] CPU: {}, avg CPU: {} | Heap used: {}, avg used: {}, max: {}",
             label,
             formatPercent(cpuPercent),
             formatPercent(avgCpuPercent),
             formatBytes(usedHeapBytes),
             formatBytes(avgUsedHeapBytes),
             (maxHeapBytes > 0 ? formatBytes(maxHeapBytes) : "n/a"));
  }

  /**
   * Logs a final summary of the average CPU and heap usage over all samples.
   */
  void logSummary()
  {
    if (sampleCount == 0)
    {
      log.info("Resource monitor: no samples collected.");
      return;
    }
    double avgCpuPercent = cumulativeCpuPercent / sampleCount;
    long avgUsedHeapBytes = cumulativeUsedHeapBytes / sampleCount;

    log.info("Resource monitor summary over {} samples — avg CPU: {}, avg heap used: {}",
             sampleCount,
             formatPercent(avgCpuPercent),
             formatBytes(avgUsedHeapBytes));
  }

  // --------------------------------------------------------------------------
  // CPU
  // --------------------------------------------------------------------------

  private double readCpuPercent()
  {
    // 1) If processCpuLoad exists and returns a valid value, use it.
    // Note: it is a rate over time; may return 0 or -1 early/too frequently.
    if (cpuLoadSource != null)
    {
      double load = cpuLoadSource.readProcessCpuLoad(); // 0..1 or -1
      if (load >= 0.0)
      {
        double p = load * 100.0;
        if (p < 0.0) p = 0.0;
        if (p > 100.0) p = 100.0;
        lastCpuPercent = p;
        return lastCpuPercent;
      }
    }

    // 2) JamaicaVM path: derive CPU% from /proc/self/stat ticks over a time window.
    long nowWallNs = System.nanoTime();
    long nowCpuTicks = cpuTicksSource.readProcessCpuTicks();

    long dWallNs = nowWallNs - lastWallNs;
    long dCpuTicks = nowCpuTicks - lastCpuTicks;

    lastWallNs = nowWallNs;
    lastCpuTicks = nowCpuTicks;

    if (dWallNs <= 0 || dCpuTicks < 0)
    {
      return lastCpuPercent;
    }

    // Accumulate until window is large enough (prevents 0.00% due to tick granularity)
    accWallNs += dWallNs;
    accCpuTicks += dCpuTicks;

    if (accWallNs < cpuWindowNs)
    {
      return lastCpuPercent; // keep previous value
    }

    long accCpuNs = (accCpuTicks * 1_000_000_000L) / (long) clkTck;

    double p = (accCpuNs / (double) (accWallNs * (long) cores)) * 100.0;
    if (p < 0.0) p = 0.0;
    if (p > 100.0) p = 100.0;

    lastCpuPercent = p;

    // reset window
    accWallNs = 0;
    accCpuTicks = 0;

    return lastCpuPercent;
  }

  // --------------------------------------------------------------------------
  // CPU load source via MXBean (optional)
  // --------------------------------------------------------------------------

  private interface CpuLoadSource
  {
    double readProcessCpuLoad(); // 0..1 or -1 if unsupported
    String name();

    static CpuLoadSource tryCreate()
    {
      try
      {
        Object mx = ManagementFactory.getOperatingSystemMXBean();
        final java.lang.reflect.Method m = mx.getClass().getMethod("getProcessCpuLoad");

        // smoke test call
        Object v = m.invoke(mx);
        if (!(v instanceof Double)) return null;

        return new CpuLoadSource()
        {
          @Override
          public double readProcessCpuLoad()
          {
            try
            {
              Object x = m.invoke(mx);
              if (x instanceof Double) return (Double) x;
            }
            catch (Throwable ignored) {}
            return -1.0;
          }

          @Override
          public String name()
          {
            return "OperatingSystemMXBean.getProcessCpuLoad";
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
  // CPU ticks source (/proc/self/stat)
  // --------------------------------------------------------------------------

  private interface CpuTimeTicksSource
  {
    long readProcessCpuTicks(); // utime+stime in ticks
    String name();
  }

  private static final class ProcSelfStatCpuTicksSource implements CpuTimeTicksSource
  {
    @Override
    public long readProcessCpuTicks()
    {
      try (BufferedReader br = new BufferedReader(new FileReader("/proc/self/stat")))
      {
        String s = br.readLine();
        if (s == null) return 0L;

        // handle "(comm with spaces)" by slicing after last ')'
        int rparen = s.lastIndexOf(')');
        String after = (rparen >= 0 && rparen + 2 < s.length())
          ? s.substring(rparen + 2).trim()
          : s.trim();

        String[] parts = after.split("\\s+");
        // after removing pid+comm, parts[11]=utime(field14), parts[12]=stime(field15)
        if (parts.length < 13) return 0L;

        long utime = Long.parseLong(parts[11]);
        long stime = Long.parseLong(parts[12]);
        return utime + stime;
      }
      catch (Exception e)
      {
        return 0L;
      }
    }

    @Override
    public String name()
    {
      return "/proc/self/stat (utime+stime ticks)";
    }
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

        @Override public String name() { return "Runtime.total-free"; }
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
              if (c instanceof Long && r instanceof Long) return (Long) c + (Long) r;
            }
            catch (Throwable ignored) {}
            return -1L;
          }

          @Override public String name() { return "RTSJ HeapMemory.memoryConsumed"; }
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

          @Override public String name() { return "MemoryMXBean.getHeapMemoryUsage"; }
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

  private static String formatPercent(double percent)
  {
    // Use 4 decimals to avoid rounding tiny values to 0.00%
    return String.format(Locale.ROOT, "%.2f%%", percent);
  }

  private static String formatBytes(long bytes)
  {
    if (bytes < 0) return "n/a";
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
    if (bytes < 1024L * 1024L * 1024L) return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    return String.format(Locale.ROOT, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
  }
}
