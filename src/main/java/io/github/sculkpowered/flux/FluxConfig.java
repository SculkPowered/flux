package io.github.sculkpowered.flux;

public final class FluxConfig {

  private final String organization;
  private final String bucket;
  private final String url;
  private final String token;
  private final String server;

  public FluxConfig() {
    this.organization = "Organization";
    this.bucket = "Bucket";
    this.url = "http://127.0.0.1:8086";
    this.token = "";
    this.server = "server1";
  }

  public String organization() {
    return this.organization;
  }

  public String bucket() {
    return this.bucket;
  }

  public String url() {
    return this.url;
  }

  public String token() {
    return this.token;
  }

  public String server() {
    return this.server;
  }
}
