package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.RegionTable.*;

import io.cattle.platform.agent.instance.service.AgentMetadataService;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.AccountLink;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Region;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.service.RegionService;
import io.cattle.platform.util.type.Priority;

import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class RegionsCredentialsReconcile extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    private static final Logger log = LoggerFactory.getLogger(RegionsCredentialsReconcile.class);
    @Inject
    AgentMetadataService agentMetadataService;
    @Inject
    RegionService regionService;
    @Inject
    ObjectManager objectManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { AccountConstants.PROCESS_ACCOUNT_LINK_REMOVE, AccountConstants.PROCESS_ACCOUNT_LINK_CREATE, AgentConstants.PROCESS_ACTIVATE,
                AgentConstants.PROCESS_REMOVE,
                InstanceConstants.PROCESS_START };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        // Exercise the hanlder only when regions are set
        if (objectManager.findAny(Region.class, REGION.REMOVED, new Condition(ConditionType.NULL)) == null) {
            return null;
        }
        Long accountId = getAccountId(state.getResource());
        if (accountId == null) {
            return null;
        }
        // 1. reconcile the credentials
        boolean result = regionService.reconcileAgentsExternalCredentials(accountId);
        if (!result) {
            log.error(String.format("Fail to reconcile credentials for account[%d]", accountId));
        }
        // 2. update metadata
        agentMetadataService.updateMetadata(accountId);
        return null;
    }

    private Long getAccountId(Object object) {
        Long accountId = null;
        if (object instanceof AccountLink) {
            // 1. External account link create/remove trigger
            AccountLink link = ((AccountLink) object);
            if (link.getLinkedAccount() != null) {
                accountId = link.getAccountId();
            }
        } else if (object instanceof Agent) {
            // 2. External agent create/remove trigger
            Agent agent = (Agent) object;
            if (agent.getExternalId() != null) {
                accountId = DataAccessor.fieldLong(agent, AgentConstants.DATA_AGENT_RESOURCES_ACCOUNT_ID);
            }
        } else {
            // 3. Metadata instance start
            Instance instance = (Instance) object;
            if (instance.getAgentId() != null) {
                Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
                if (labels.containsKey(SystemLabels.LABEL_AGENT_SERVICE_METADATA)) {
                    accountId = instance.getAccountId();
                }
            }
        }
        return accountId;
    }

    @Override
    public int getPriority() {
        return 0;
    }

}
