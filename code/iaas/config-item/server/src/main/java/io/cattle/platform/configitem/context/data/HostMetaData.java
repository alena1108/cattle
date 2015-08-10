package io.cattle.platform.configitem.context.data;

import java.util.Map;

public class HostMetaData {
    String agent_ip;
    String name;
    Map<String, String> labels;

    public String getAgent_ip() {
        return agent_ip;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public HostMetaData(String agent_ip, String name, Map<String, String> labels) {
        super();
        this.agent_ip = agent_ip;
        this.name = name;
        this.labels = labels;
    }

}
