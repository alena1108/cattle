package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.RegionTable.*;

import io.cattle.platform.core.addon.ExternalCredential;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Region;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.service.RegionService;
import io.cattle.platform.util.type.Priority;

import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class RegionsAgentRemove extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Inject
    RegionService regionService;
    @Inject
    JsonMapper jsonMapper;

    @Inject

    @Override
    public String[] getProcessNames() {
        return new String[] { "agent.remove" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Agent agent = (Agent) state.getResource();
        if (agent.getExternalId() != null) {
            return null;
        }
        List<? extends ExternalCredential> creds = DataAccessor.fieldObjectList(agent, AccountConstants.FIELD_EXTERNAL_CREDENTIALS, ExternalCredential.class,
                jsonMapper);
        if (creds.isEmpty()) {
            return null;
        }
        
        Map<String, Region> regions = new HashMap<>();
        Region localRegion = null;
        for (Region region : objectManager.find(Region.class, REGION.REMOVED, new Condition(ConditionType.NULL))) {
            regions.put(region.getName(), region);
            if (region.getLocal()) {
                localRegion = region;
            }
        }
        for (ExternalCredential cred : creds) {
            regionService.deactivateAndRemoveExtenralAgent(agent, localRegion, regions, cred);
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}

