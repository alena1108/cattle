package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.core.addon.ExternalCredential;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Region;

import java.util.List;
import java.util.Map;

public interface RegionService {

    public void reconcileExternalLinks(long accountId);

    List<ExternalCredential> getExternalCredentials(Account account, Agent agent);

    boolean deactivateAndRemoveExtenralAgent(Agent agent, Region localRegion, Map<String, Region> regions, ExternalCredential cred);
}
