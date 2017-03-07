package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.servicediscovery.upgrade.UpgradeManager;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceRemove extends AbstractObjectProcessHandler {

    @Inject
    DeploymentManager deploymentMgr;

    @Inject
    ServiceDiscoveryService sdService;
    
    @Inject
    UpgradeManager upgradeMgr;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_SERVICE_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        
        upgradeMgr.finishUpgrade(service, false);
        deploymentMgr.remove(service);

        sdService.releaseVip(service);

        sdService.releasePorts(service);

        sdService.removeServiceIndexes(service);

        return null;
    }
}
