package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ServiceMetaData {
    private Long serviceId;
    private boolean isPrimaryConfig;
    
    String name;
    String stack_name;
    String kind;
    String hostname;
    String vip;
    List<String> external_ips = new ArrayList<>();
    List<String> sidekicks;
    List<String> containers = new ArrayList<>();
    Map<String, String> links;
    List<String> ports = new ArrayList<>();
    Map<String, String> labels;

    public ServiceMetaData(Service service, String serviceName, Environment env, Map<String, String> links,
            List<String> sidekicks) {
        this.serviceId = service.getId();
        this.name = serviceName;
        this.stack_name = env.getName();
        this.kind = service.getKind();
        this.sidekicks = sidekicks;
        this.links = links;
        this.vip = service.getVip();
        this.labels = ServiceDiscoveryUtil.getLaunchConfigLabels(service, serviceName);
        this.isPrimaryConfig = service.getName().equalsIgnoreCase(serviceName);
        populateExternalServiceInfo(service);
        populatePortsInfo(service, serviceName);
    }

    @SuppressWarnings("unchecked")
    protected void populatePortsInfo(Service service, String serviceName) {
        Object portsObj = ServiceDiscoveryUtil.getLaunchConfigObject(service, serviceName,
                InstanceConstants.FIELD_PORTS);
        if (portsObj != null) {
            this.ports.addAll((List<String>) portsObj);
        }
    }

    @SuppressWarnings("unchecked")
    protected void populateExternalServiceInfo(Service service) {
        if (kind.equalsIgnoreCase(ServiceDiscoveryConstants.KIND.EXTERNALSERVICE.name())) {
            this.hostname = DataAccessor.fields(service)
                    .withKey(ServiceDiscoveryConstants.FIELD_HOSTNAME).as(String.class);
            external_ips.addAll(DataAccessor.fields(service)
                    .withKey(ServiceDiscoveryConstants.FIELD_EXTERNALIPS).withDefault(Collections.EMPTY_LIST)
                    .as(List.class));
        }
    }

    public String getName() {
        return name;
    }

    public String getStack_name() {
        return stack_name;
    }

    public String getKind() {
        return kind;
    }

    public String getHostname() {
        return hostname;
    }

    public String getVip() {
        return vip;
    }

    public List<String> getExternal_ips() {
        return external_ips;
    }

    public List<String> getSidekicks() {
        return sidekicks;
    }

    public List<String> getContainers() {
        return containers;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public List<String> getPorts() {
        return ports;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void addToContainer(String containerName) {
        this.containers.add(containerName);
    }

    @JsonIgnore
    public Long getServiceId() {
        return serviceId;
    }

    @JsonIgnore
    public boolean isPrimaryConfig() {
        return isPrimaryConfig;
    }
}
