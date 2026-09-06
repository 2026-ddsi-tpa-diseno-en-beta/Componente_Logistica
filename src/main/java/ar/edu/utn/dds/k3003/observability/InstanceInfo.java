package ar.edu.utn.dds.k3003.observability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InstanceInfo {

  private final String instanceId;
  private final String component;

  public InstanceInfo(
      @Value("${app.instance-id:logistica-local}") String instanceId,
      @Value("${app.component:api}") String component) {
    this.instanceId = instanceId;
    this.component = component;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getComponent() {
    return component;
  }
}
