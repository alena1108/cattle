package io.cattle.platform.servicediscovery.service.impl;

import static io.cattle.platform.core.model.tables.AccountLinkTable.*;
import static io.cattle.platform.core.model.tables.RegionTable.*;
import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.addon.ExternalCredential;
import io.cattle.platform.core.addon.LbConfig;
import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AccountLink;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Region;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.iaas.api.filter.apikey.ApiKeyFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.service.RegionService;

import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RegionServiceImpl implements RegionService {
    private static final Logger log = LoggerFactory.getLogger(RegionServiceImpl.class);
    public static final String EXTERNAL_AGENT_URI_PREFIX = "event:///external=";

    @Inject
    ObjectManager objectManager;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Override
    public void reconcileExternalLinks(long accountId) {
        List<Region> regions = objectManager.find(Region.class, REGION.REMOVED, new Condition(ConditionType.NULL));
        if (regions.size() == 0) {
            return;
        }
        Map<String, Region> regionsMap = new HashMap<>();
        Region localRegion = null;
        Account localAccount = objectManager.loadResource(Account.class, accountId);
        for (Region region : regions) {
            regionsMap.put(region.getName(), region);
            if (region.getLocal()) {
                localRegion = region;
            }
        }

        List<? extends AccountLink> existingLinks = objectManager.find(AccountLink.class, ACCOUNT_LINK.ACCOUNT_ID, accountId,
                ACCOUNT_LINK.REMOVED, null, ACCOUNT_LINK.LINKED_ACCOUNT, new Condition(ConditionType.NOTNULL), ACCOUNT_LINK.LINKED_REGION,
                new Condition(ConditionType.NOTNULL));
        Set<String> existingLinksKeys = new HashSet<>();
        for (AccountLink existingLink : existingLinks) {
            existingLinksKeys.add(getUUID(existingLink.getLinkedRegion(), existingLink.getLinkedAccount()));
        }

        List<? extends ServiceConsumeMap> links = objectManager.find(ServiceConsumeMap.class, SERVICE_CONSUME_MAP.ACCOUNT_ID, accountId,
                SERVICE_CONSUME_MAP.REMOVED, null, SERVICE_CONSUME_MAP.CONSUMED_SERVICE, new Condition(ConditionType.NOTNULL));
        List<String> invalidStates = Arrays.asList(CommonStatesConstants.REMOVING, CommonStatesConstants.REMOVED);
        Set<String> toAdd = new HashSet<>();
        for (ServiceConsumeMap link : links) {
            if (invalidStates.contains(link.getState())) {
                continue;
            }
            if (link.getConsumedService() == null) {
                continue;
            }
            String[] splitted = link.getConsumedService().split("/");
            if (splitted.length < 4) {
                continue;
            }
            if (regionsMap.containsKey(splitted[0]) && !invalidStates.contains(localAccount.getState())) {
                toAdd.add(getUUID(splitted[0], splitted[1]));
            }
        }

        List<? extends Service> lbs = objectManager.find(Service.class, SERVICE.ACCOUNT_ID, accountId,
                SERVICE.REMOVED, null, SERVICE.KIND, ServiceConstants.KIND_LOAD_BALANCER_SERVICE);
        for (Service lb : lbs) {
            if (invalidStates.contains(lb.getState())) {
                continue;
            }
            LbConfig lbConfig = DataAccessor.field(lb, ServiceConstants.FIELD_LB_CONFIG, jsonMapper,
                    LbConfig.class);
            if (lbConfig != null && lbConfig.getPortRules() != null) {
                for (PortRule rule : lbConfig.getPortRules()) {
                    String rName = rule.getRegion();
                    String eName = rule.getEnvironment();
                    if (StringUtils.isEmpty(eName)) {
                        continue;
                    }
                    if (StringUtils.isAllLowerCase(rName)) {
                        rName = localRegion.getName();
                    }
                    if (regionsMap.containsKey(rName) && !invalidStates.contains(localAccount.getState())) {
                        toAdd.add(getUUID(rName, eName));
                    }
                }
            }
        }

        List<AccountLink> toRemove = new ArrayList<>();
        List<AccountLink> toUpdate = new ArrayList<>();
        Set<String> toCreate = new HashSet<>();
        for (AccountLink link : existingLinks) {
            if (!toAdd.contains(getUUID(link.getLinkedRegion(), link.getLinkedAccount()))) {
                toRemove.add(link);
            } else {
                toUpdate.add(link);
            }
        }
        for (String item : toAdd) {
            if (!existingLinksKeys.contains(item)) {
                toCreate.add(item);
            }
        }

        for (AccountLink item : toRemove) {
            if (!item.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING)) {
                objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, item, null);
            }
        }

        for (String item : toCreate) {
            String[] splitted = item.split(":");
            String regionName = splitted[0];
            String envName = splitted[1];
            Region region = regionsMap.get(regionName);
            if (region != null) {
                AccountLink link = objectManager.create(AccountLink.class, ACCOUNT_LINK.ACCOUNT_ID,
                        accountId, ACCOUNT_LINK.LINKED_ACCOUNT, envName, ACCOUNT_LINK.LINKED_REGION, regionName,
                        ACCOUNT_LINK.LINKED_REGION_ID, region.getId());
                toUpdate.add(link);
            }
        }
        for (AccountLink item : toUpdate) {
            if (item.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
                objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE, item, null);
            }
        }
    }

    private String getUUID(String regionName, String envName) {
        return String.format("%s:%s", regionName, envName);
    }

    @Override
    public List<ExternalCredential> getExternalCredentials(Account account, Agent agent) {
        Map<Long, Region> regionsIds = new HashMap<>();
        Map<String, Region> regionsNames = new HashMap<>();
        Region localRegion = null;

        for (Region region : objectManager.find(Region.class, REGION.REMOVED, new Condition(ConditionType.NULL))) {
            regionsIds.put(region.getId(), region);
            regionsNames.put(region.getName(), region);
            if (region.getLocal()) {
                localRegion = region;
            }
        }
        // no regions = no external credential management
        if (regionsIds.isEmpty()) {
            return new ArrayList<ExternalCredential>();
        }

        // 1. Get environments
        Set<String> externalLinks = new HashSet<>();
        // 1.1 Get environments linked FROM local
        getEnvironmentsLinkedFromLocal(account.getId(), externalLinks, regionsIds);
        // 1.2 Get environments linked TO local
        for (Region region : regionsNames.values()) {
            try {
                getEnvironmentsLinkedToLocal(region, localRegion.getName(), account.getName(),
                        regionsNames, externalLinks);
            } catch (IOException e) {
                log.error("Failed to fetch environment linked to local from region " + region.getName(), e);
                continue;
            }
        }

        // 2. Set credentials
        Map<String, ExternalCredential> toAdd = new HashMap<>();
        Map<String, ExternalCredential> toRemove = new HashMap<>();
        Map<String, ExternalCredential> toRetain = new HashMap<>();
        setCredentials(agent, externalLinks, toAdd, toRemove, toRetain);

        // 3. Reconcile agents
        return reconcileExternalAgents(account, agent, localRegion, regionsNames, toAdd, toRemove, toRetain);
    }

    private ExternalAgent createExternalAgent(Agent agent, Account account, Region localRegion, Region targetRegion, ExternalCredential cred) {
        // Create external agent with local credentials
        try {
            ExternalProject targetResourceAccount = getTargetProjectByName(targetRegion, cred.getEnvironmentName());
            if (targetResourceAccount == null) {
                log.error(String.format("Failed to find target environment by name [%s] in region [%s]",
                        cred.getEnvironmentName(), localRegion.getName()));
                return null;
            }

            String targetAgentUri = getTargetAgentUri(localRegion.getName(), account.getName(), agent.getUuid(), targetResourceAccount.getUuid());
            log.info(String.format("Creating external agent with uri [%s] in environment [%s] in region [%s]",
                    targetAgentUri,
                    cred.getEnvironmentName(),
                    cred.getRegionName()));
            Map<String, Object> data = new HashMap<>();
            data.put(AgentConstants.DATA_AGENT_RESOURCES_ACCOUNT_ID, targetResourceAccount.getId());
            data.put(CredentialConstants.PUBLIC_VALUE, cred.getPublicValue());
            data.put(CredentialConstants.SECRET_VALUE, cred.getSecretValue());
            data.put(AgentConstants.FIELD_URI, targetAgentUri);
            data.put(AgentConstants.FIELD_EXTERNAL_ID, agent.getUuid());
            Map<String, String> labels = new HashMap<>();
            labels.put(SystemLabels.LABEL_AGENT_SERVICE_METADATA, "true");
            data.put(InstanceConstants.FIELD_LABELS, labels);
            data.put("activateOnCreate", true);
            return createExternalAgent(targetRegion, cred.getEnvironmentName(), data);
        } catch (IOException e) {
            log.error("Failed to create external agent", e);
        }
        return null;
    }

    private List<ExternalCredential> reconcileExternalAgents(Account account, Agent agent,
            Region localRegion,
            Map<String, Region> regions,
            Map<String, ExternalCredential> toAdd,
            Map<String, ExternalCredential> toRemove,
            Map<String, ExternalCredential> toRetain) {
        List<ExternalCredential> toReturn = new ArrayList<>();

        // 1. Check if agent exist for credential to retain; if not, add to remove list
        Iterator<String> it = toRetain.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            ExternalCredential cred = toRetain.get(key);
            Region targetRegion = regions.get(cred.getRegionName());
            if (targetRegion == null) {
                it.remove();
                log.error(String.format("Failed to find target region by name [%s]", cred.getRegionName()));
                continue;
            }
            ExternalAgent externalAgent = null;
            try {
                externalAgent = getExternalAgent(targetRegion, cred.getAgentUuid());
            } catch (IOException e) {
                log.error(String.format("Failed to find external agent by uuid [%s]", cred.getAgentUuid()));
                continue;
            }
            if (externalAgent == null) {
                // regenerate the agent
                String uuid = getUUID(cred.getRegionName(), cred.getEnvironmentName());
                toAdd.put(uuid, new ExternalCredential(cred.getEnvironmentName(), cred.getRegionName(), cred.getPublicValue(), cred.getSecretValue()));
                it.remove();
            }
        }
        toReturn.addAll(toRetain.values());

        // 2. Add missing agents
        for (String key : toAdd.keySet()) {
            ExternalCredential value = toAdd.get(key);
            ExternalAgent externalAgent = createExternalAgent(agent, account, localRegion, regions.get(value.getRegionName()), toAdd.get(key));
            if (externalAgent != null) {
                value.setAgentUuid(externalAgent.getUuid());
                // only add credential of the agent which got created successfully
                toReturn.add(value);
                toRetain.put(key, value);
            }
        }

        // 3. Remove extra agents.
        for (String key : toRemove.keySet()) {
            ExternalCredential value = toRemove.get(key);
            if (!deactivateAndRemoveExtenralAgent(agent, localRegion, regions, value)) {
                // add credential back so it can be cleaned up next time
                toRetain.put(key, value);
            }
        }
        objectManager.setFields(agent, AccountConstants.FIELD_EXTERNAL_CREDENTIALS, toRetain.values());
        return toReturn;
    }

    @Override
    public boolean deactivateAndRemoveExtenralAgent(Agent agent, Region localRegion, Map<String, Region> regions, ExternalCredential cred) {
        Region targetRegion = regions.get(cred.getRegionName());
        if (targetRegion == null) {
            log.error(String.format("Failed to find target region by name [%s]", cred.getRegionName()));
            return true;
        }

        String regionName = cred.getRegionName();
        String envName = cred.getEnvironmentName();
        try {
            log.info(String.format("Removing agent with externalId [%s] in environment [%s] and region [%s]", agent.getUuid(), regionName,
                    envName));
            ExternalProject targetResourceAccount = getTargetProjectByName(targetRegion, envName);
            if (targetResourceAccount == null) {
                log.debug(String.format("Failed to find target environment by name [%s] in region [%s]", envName, regionName));
                return true;
            }
            ExternalAgent externalAgent = getExternalAgent(targetRegion, cred.getAgentUuid());
            if (externalAgent == null) {
                log.debug(String.format("Failed to find agent by externalId [%s] in environment [%s] and region [%s]", agent.getUuid(), regionName,
                        envName));
                return true;
            }
            String uri = String.format("%s/v2-beta/agents/%s", getUrl(targetRegion), externalAgent.getId());
            Request req = Request.Delete(uri);
            setHeaders(req, targetRegion);
            req.execute().handleResponse(new ResponseHandler<ExternalAgent>() {
                @Override
                public ExternalAgent handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode >= 300) {
                        throw new IOException(String.format("Failed to delete external agent externalId=%s, response error code %s", agent.getUuid(),
                                response.getStatusLine().getReasonPhrase()));
                    }

                    return null;
                }
            });
        } catch (IOException e) {
            log.error(String.format("Failed to deactivate agent with externalId [%s] in environment [%s] and region [%s]", agent.getUuid(), regionName,
                    envName));
            return false;
        }
        return true;
    }

    protected ExternalAgent createExternalAgent(Region targetRegion, String targetEnvName, Map<String, Object> params) throws IOException {
        String uri = String.format("%s/v2-beta/agents", getUrl(targetRegion));
        Request req = Request.Post(uri);
        setHeaders(req, targetRegion);
        req.bodyString(jsonMapper.writeValueAsString(params), ContentType.APPLICATION_JSON);
        return req.execute().handleResponse(new ResponseHandler<ExternalAgent>() {
            @Override
            public ExternalAgent handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                if (response.getStatusLine().getStatusCode() >= 300) {
                    throw new IOException(
                            String.format("Failed to create external agent with uri [%s] in environment [%s] in region [%s]: response error code %s",
                            params.get(AgentConstants.FIELD_URI),
                            targetEnvName,
                            targetRegion.getName(),
                            response.getStatusLine().getReasonPhrase()));
                }
                return jsonMapper.readValue(response.getEntity().getContent(), ExternalAgent.class);
            }
        });
    }

    protected ExternalAgent getExternalAgent(Region targetRegion, String uuid)
            throws IOException {
        String uri = String.format("%s/v2-beta/agents?uuid=%s",
                getUrl(targetRegion),
                uuid);
        Request req = Request.Get(uri);
        setHeaders(req, targetRegion);
        return req.execute().handleResponse(new ResponseHandler<ExternalAgent>() {
            @Override
            public ExternalAgent handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                if (response.getStatusLine().getStatusCode() != 200) {
                    return null;
                }
                for (ExternalAgent agent : jsonMapper.readValue(response.getEntity().getContent(), ExternalAgentData.class).data) {
                    List<String> invalidStates = Arrays.asList(CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING);
                    if (invalidStates.contains(agent.getState())) {
                        continue;
                    }
                    return agent;
                }
                return null;
            }
        });
    }

    private String getTargetAgentUri(String localRegionName, String localEnvironmentName, String agentUuid, String targetResourceAccountUuid) {
        return String.format("%s%s_%s_%s_%s", EXTERNAL_AGENT_URI_PREFIX, localRegionName, localEnvironmentName, agentUuid, targetResourceAccountUuid);
    }

    private ExternalProject getTargetProjectByName(Region targetRegion, String accountName) throws IOException {
        String uri = String.format("%s/v2-beta/projects?name=%s&all=true",
                getUrl(targetRegion),
                accountName);
        Request req = Request.Get(uri);
        setHeaders(req, targetRegion);
        return req.execute().handleResponse(new ResponseHandler<ExternalProject>() {
            @Override
            public ExternalProject handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                if (response.getStatusLine().getStatusCode() != 200) {
                    return null;
                }

                ExternalProjectData data = jsonMapper.readValue(response.getEntity().getContent(), ExternalProjectData.class);
                return data.data.size() == 0 ? null : data.data.get(0);
            }
        });
    }

    private void setCredentials(Agent agent, Set<String> externalLinks,
            Map<String, ExternalCredential> toAdd, Map<String, ExternalCredential> toRemove, Map<String, ExternalCredential> toRetain) {
        List<? extends ExternalCredential> existing = DataAccessor.fieldObjectList(agent, AccountConstants.FIELD_EXTERNAL_CREDENTIALS, ExternalCredential.class,
                jsonMapper);

        Map<String, ExternalCredential> existingCredentials = new HashMap<>();
        for (ExternalCredential cred : existing) {
            existingCredentials.put(getUUID(cred.getRegionName(), cred.getEnvironmentName()), cred);
        }

        for (String key : externalLinks) {
            String[] splitted = key.split(":");
            String regionName = splitted[0];
            String envName = splitted[1];
            String uuid = getUUID(regionName, envName);
            if (existingCredentials.containsKey(uuid)) {
                toRetain.put(uuid, existingCredentials.get(uuid));
            } else {
                String[] keys = ApiKeyFilter.generateKeys();
                toAdd.put(uuid, new ExternalCredential(envName, regionName, keys[0], keys[1]));
            }
        }

        for (String key : existingCredentials.keySet()) {
            if (!(toAdd.containsKey(key) || toRetain.containsKey(key))) {
                toRemove.put(key, existingCredentials.get(key));
            }
        }
    }

    protected String getUrl(Region region) {
        return region.getUrl();
    }

    protected void getEnvironmentsLinkedToLocal(Region targetRegion, String localRegionName, String localAccountName,
            Map<String, Region> regions, Set<String> linksToSet)
            throws IOException {
        String uri = String.format("%s/v2-beta/accountLinks?linkedRegion=%s&linkedAccount=%s",
                getUrl(targetRegion),
                localRegionName,
                localAccountName);
        Request req = Request.Get(uri);
        setHeaders(req, targetRegion);
        List<ExternalAccountLink> links = req.execute().handleResponse(new ResponseHandler<List<ExternalAccountLink>>() {
            @Override
            public List<ExternalAccountLink> handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                if (response.getStatusLine().getStatusCode() != 200) {
                    return null;
                }

                ExteranlAccountLinkData linkData = jsonMapper.readValue(response.getEntity().getContent(), ExteranlAccountLinkData.class);
                return linkData.data;
            }
        });
        Map<String, String> accountIdsToRegion = new HashMap<>();
        for (ExternalAccountLink link : links) {
            accountIdsToRegion.put(link.getAccountId(), link.getLinkedRegion());
        }

        for (String accountId : accountIdsToRegion.keySet()) {
            ExternalProject account = getTargetProjectById(targetRegion, accountId);
            if (account == null) {
                continue;
            }
            Region region = regions.get(accountIdsToRegion.get(accountId));
            if (region == null) {
                continue;
            }
            linksToSet.add(getUUID(region.getName(), account.getName()));
        }
    }

    private void getEnvironmentsLinkedFromLocal(long accountId, Set<String> links, Map<Long, Region> regionsIds) {
        List<AccountLink> accountLinks = objectManager.find(AccountLink.class, ACCOUNT_LINK.ACCOUNT_ID,
                accountId, ACCOUNT_LINK.REMOVED, null, ACCOUNT_LINK.LINKED_REGION_ID, new Condition(ConditionType.NOTNULL));

        for (AccountLink link : accountLinks) {
            List<String> invalidStates = Arrays.asList(CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING);
            if (invalidStates.contains(link.getState())) {
                continue;
            }
            Region targetRegion = regionsIds.get(link.getLinkedRegionId());
            if (targetRegion == null) {
                continue;
            }
            links.add(getUUID(targetRegion.getName(), link.getLinkedAccount()));
        }
    }

    private ExternalProject getTargetProjectById(Region targetRegion, String accountId) {
        String uri = String.format("%s/v2-beta/projects/%s",
                getUrl(targetRegion),
                accountId);

        Request req = Request.Get(uri);

        setHeaders(req, targetRegion);
        try {
            return req.execute().handleResponse(new ResponseHandler<ExternalProject>() {
                @Override
                public ExternalProject handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                    if (response.getStatusLine().getStatusCode() != 200) {
                        return null;
                    }

                    ExternalProject data = jsonMapper.readValue(response.getEntity().getContent(), ExternalProject.class);
                    return data.getName() == null ? null : data;
                }
            });
        } catch (IOException e) {
            return null;
        }
    }

    private Request setHeaders(Request req, Region region) {
        String publicKey = region.getPublicValue();
        String secretKey = region.getSecretValue();
        String encodedKeys = Base64.encodeBase64String(String.format("%s:%s", publicKey, secretKey).getBytes());
        String auth = String.format("Basic %s", encodedKeys);
        req.addHeader("Authorization", auth);
        req.addHeader("Content-Type", "application/json");
        req.addHeader("Accept", "application/json");
        return req;
    }

    public static class ExternalAgentData {
        List<ExternalAgent> data;

        public List<ExternalAgent> getData() {
            return data;
        }

        public void setData(List<ExternalAgent> data) {
            this.data = data;
        }
    }

    public static class ExternalAgent {
        String id;
        String uri;
        String name;
        String state;
        String uuid;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class ExteranlAccountLinkData {
        List<ExternalAccountLink> data;

        public List<ExternalAccountLink> getData() {
            return data;
        }

        public void setData(List<ExternalAccountLink> data) {
            this.data = data;
        }
    }

    public static class ExternalAccountLink {
        String accountId;
        String linkedRegion;
        String linkedAccount;

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getLinkedRegion() {
            return linkedRegion;
        }

        public void setLinkedRegion(String linkedRegion) {
            this.linkedRegion = linkedRegion;
        }

        public String getLinkedAccount() {
            return linkedAccount;
        }

        public void setLinkedAccount(String linkedAccount) {
            this.linkedAccount = linkedAccount;
        }
    }

    public static class ExternalProjectData {
        List<ExternalProject> data;

        public List<ExternalProject> getData() {
            return data;
        }

        public void setData(List<ExternalProject> data) {
            this.data = data;
        }
    }

    public static class ExternalProject {
        String id;
        String name;
        String uuid;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }
}
