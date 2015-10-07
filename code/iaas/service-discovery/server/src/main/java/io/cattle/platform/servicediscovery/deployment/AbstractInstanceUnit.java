package io.cattle.platform.servicediscovery.deployment;

import static io.cattle.platform.core.model.tables.InstanceHostMapTable.INSTANCE_HOST_MAP;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl;

public abstract class AbstractInstanceUnit extends DeploymentUnitInstance implements InstanceUnit {

    protected Instance instance;
    protected boolean startOnce;

    protected AbstractInstanceUnit(DeploymentManagerImpl.DeploymentServiceContext context, String uuid, Service service, String launchConfigName) {
    super(context, uuid, service, launchConfigName);
    }

    @Override
    public Instance getInstance() {
        return instance;
    }

    @Override
    public boolean isUnhealthy() {
        if (instance != null) {
            return instance.getHealthState() != null && (instance.getHealthState().equalsIgnoreCase(
                    HealthcheckConstants.HEALTH_STATE_UNHEALTHY) || instance.getHealthState().equalsIgnoreCase(
                    HealthcheckConstants.HEALTH_STATE_UPDATING_UNHEALTHY));
        }
        return false;
    }

    @Override
    public void stop() {
        if (instance != null && instance.getState().equals(InstanceConstants.STATE_RUNNING)) {
            context.objectProcessManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP, instance,
                    null);
        }
    }

    @Override
    public boolean isHealthCheckInitializing() {
        return instance != null && HealthcheckConstants.HEALTH_STATE_INITIALIZING.equals(instance.getHealthState());
    }

    @Override
    public void waitForAllocate() {
        if (this.instance != null) {
            instance = context.resourceMonitor.waitFor(instance, new ResourcePredicate<Instance>() {
                @Override
                public boolean evaluate(Instance obj) {
                    return context.objectManager.find(InstanceHostMap.class, INSTANCE_HOST_MAP.INSTANCE_ID,
                            instance.getId()).size() > 0;
                }
            });
        }
    }

    @Override
    public DeploymentUnitInstance startImpl() {
        if (instance != null && InstanceConstants.STATE_STOPPED.equals(instance.getState())) {
            context.objectProcessManager.scheduleProcessInstanceAsync(
                    InstanceConstants.PROCESS_START, instance, null);
        }
        return this;
    }
    
    @SuppressWarnings("unchecked")
    protected void setStartOnce() {
        Object labels = ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
                InstanceConstants.FIELD_LABELS);
        if (labels != null) {
            String startOnceLabel = ((Map<String, String>) labels)
                    .get(ServiceDiscoveryConstants.LABEL_SERVICE_CONTAINER_CREATE_ONLY);
            if (StringUtils.equalsIgnoreCase(startOnceLabel, "true")) {
                startOnce = true;
            }
        }
    }
    
    @Override
    public boolean startOnce() {
        return startOnce;
    }

}
