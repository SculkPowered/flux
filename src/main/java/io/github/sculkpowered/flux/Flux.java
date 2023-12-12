package io.github.sculkpowered.flux;

import com.google.gson.GsonBuilder;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.client.WriteApi;
import com.influxdb.client.write.Point;
import com.sun.management.OperatingSystemMXBean;
import io.github.sculkpowered.server.event.Subscribe;
import io.github.sculkpowered.server.event.lifecycle.ServerInitializeEvent;
import io.github.sculkpowered.server.event.lifecycle.ServerShutdownEvent;
import io.github.sculkpowered.server.plugin.Plugin;
import io.github.sculkpowered.server.plugin.PluginDescription;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

@PluginDescription(name = "flux", version = "1.0.0")
public final class Flux extends Plugin {

  private FluxConfig config;
  private InfluxDBClient client;
  private WriteApi writeApi;

  @Subscribe
  public void handle(final ServerInitializeEvent event) {
    try {
      if (Files.notExists(this.dataDirectory())) {
        Files.createDirectory(this.dataDirectory());
      }
      final var gson = new GsonBuilder().setPrettyPrinting().create();
      final var path = this.dataDirectory().resolve("config.json");
      if (Files.notExists(path)) {
        this.config = new FluxConfig();
        try (final var writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE_NEW)) {
          writer.write(gson.toJson(this.config));
        }
      } else {
        try (final var reader = Files.newBufferedReader(path)) {
          this.config = gson.fromJson(reader, FluxConfig.class);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    this.client = InfluxDBClientFactory.create(InfluxDBClientOptions.builder()
        .url(this.config.url())
        .authenticateToken(this.config.token().toCharArray())
        .org(this.config.organization())
        .bucket(this.config.bucket())
        .build());
    this.writeApi = this.client.makeWriteApi();

    this.writeApi.writePoint(this.point("start_time")
        .addField("gauge", System.currentTimeMillis()));

    final var managementFactory = ManagementFactory.getMemoryMXBean();
    final var operatingBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    final var threadBean = ManagementFactory.getThreadMXBean();

    this.server().scheduler().newTask(this, () -> {
      this.writeApi.writePoint(this.point("player_count")
          .addField("gauge", this.server().playerCount()));

      // memory
      final var heap = managementFactory.getHeapMemoryUsage();
      final var nonHeap = managementFactory.getNonHeapMemoryUsage();

      this.writeApi.writePoint(this.point("memory_used")
          .addTag("area", "heap")
          .addField("gauge", heap.getUsed()));
      this.writeApi.writePoint(this.point("memory_max")
          .addTag("area", "heap")
          .addField("gauge", heap.getMax()));

      this.writeApi.writePoint(this.point("memory_used")
          .addTag("area", "nonheap")
          .addField("gauge", nonHeap.getUsed()));
      this.writeApi.writePoint(this.point("memory_max")
          .addTag("area", "nonheap")
          .addField("gauge", nonHeap.getMax()));

      //  cpu
      this.writeApi.writePoint(this.point("process_cpu_load")
          .addField("gauge", operatingBean.getProcessCpuLoad()));

      // thread
      this.writeApi.writePoint(this.point("threads_count")
          .addField("gauge", threadBean.getThreadCount()));
      this.writeApi.writePoint(this.point("threads_daemon_count")
          .addField("gauge", threadBean.getDaemonThreadCount()));
      this.writeApi.writePoint(this.point("threads_started")
          .addField("gauge", threadBean.getTotalStartedThreadCount()));
    }).repeat(10, TimeUnit.SECONDS).schedule();
  }

  @Subscribe
  public void handle(final ServerShutdownEvent event) {
    this.writeApi.close();
    this.client.close();
  }

  private Point point(final String measurement) {
    return new Point(measurement)
        .addTag("server", this.config.server());
  }
}
