package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.core.addon.ExternalCredential;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Region;

import java.util.Map;

public interface RegionService {

    public void reconcileExternalLinks(long accountId);

    boolean reconcileAgentsExternalCredentials(long accountId);

    boolean deactivateAndRemoveExtenralAgent(Agent agent, Region localRegion, Map<String, Region> regions, ExternalCredential cred);
}
