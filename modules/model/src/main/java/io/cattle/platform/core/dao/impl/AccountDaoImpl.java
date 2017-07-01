package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.AccountLinkTable.*;
import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.CredentialTable.*;
import static io.cattle.platform.core.model.tables.ProjectMemberTable.*;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AccountLink;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Configuration;
import org.jooq.impl.DSL;

public class AccountDaoImpl extends AbstractJooqDao implements AccountDao {

    private static final Set<String> GOOD_STATES = CollectionUtils.set(
            CommonStatesConstants.REGISTERING,
            CommonStatesConstants.ACTIVATING,
            CommonStatesConstants.ACTIVE,
            ServiceConstants.STATE_UPGRADING);

    ObjectProcessManager objectProcessManager;
    ObjectManager objectManager;

    public AccountDaoImpl(Configuration configuration, ObjectManager objectManager,
            ObjectProcessManager objectProcessManager) {
        super(configuration);
        this.objectProcessManager = objectProcessManager;
        this.objectManager = objectManager;
    }

    @Override
    public Account getSystemAccount() {
        Account system = objectManager.findOne(Account.class,
                ACCOUNT.UUID, AccountConstants.SYSTEM_UUID);
        if ( system == null ) {
            throw new IllegalStateException("Failed to find system account");
        }

        return system;
    }

    @Override
    public List<? extends Credential> getApiKeys(Account account, String kind, boolean active) {
        if (kind == null) {
            kind = CredentialConstants.KIND_API_KEY;
        }

        Condition stateCondition = DSL.trueCondition();
        if ( active ) {
            stateCondition = CREDENTIAL.STATE.eq(CommonStatesConstants.ACTIVE);
        }

        return create().selectFrom(CREDENTIAL)
            .where(CREDENTIAL.ACCOUNT_ID.eq(account.getId())
                    .and(CREDENTIAL.REMOVED.isNull())
                    .and(stateCondition)
                    .and(CREDENTIAL.KIND.eq(kind)))
            .fetch();
    }

    @Override
    public Account findByUuid(String uuid) {
        return create()
                .selectFrom(ACCOUNT)
                .where(ACCOUNT.UUID.eq(uuid))
                .fetchOne();
    }

    @Override
    public void deleteProjectMemberEntries(Account account) {
        if (!ProjectConstants.TYPE.equalsIgnoreCase(account.getKind())
                && StringUtils.isNotBlank(account.getExternalId())
                && StringUtils.isNotBlank(account.getExternalIdType())){
            create().delete(PROJECT_MEMBER)
                    .where(PROJECT_MEMBER.EXTERNAL_ID.eq(account.getExternalId())
                            .and(PROJECT_MEMBER.EXTERNAL_ID_TYPE.eq(account.getExternalIdType())))
                    .execute();
        }
        create().delete(PROJECT_MEMBER)
            .where(PROJECT_MEMBER.PROJECT_ID.eq(account.getId()))
            .execute();
    }

    @Override
    public Account getAdminAccountExclude(long accountId) {
        return create()
                .selectFrom(ACCOUNT)
                .where(ACCOUNT.STATE.in(getAccountActiveStates())
                        .and(ACCOUNT.KIND.eq(AccountConstants.ADMIN_KIND))
                        .and(ACCOUNT.ID.ne(accountId)))
                .orderBy(ACCOUNT.ID.asc()).limit(1).fetchOne();
    }


    @Override
    public Account getAccountById(Long id) {
        return create()
                .selectFrom(ACCOUNT)
                .where(
                        ACCOUNT.ID.eq(id)
                                .and(ACCOUNT.STATE.ne(AccountConstants.STATE_PURGED))
                                .and(ACCOUNT.REMOVED.isNull())
                ).fetchOne();
    }

    @Override
    public boolean isActiveAccount(Account account) {
        return GOOD_STATES.contains(account.getState());
    }

    @Override
    public List<String> getAccountActiveStates() {
        return Arrays.asList(CommonStatesConstants.ACTIVE, ServiceConstants.STATE_UPGRADING);
    }

    @Override
    public void generateAccountLinks(Account account, List<? extends Long> links) {
        createNewAccountLinks(account, links);
        deleteOldAccountLinks(account, links);
    }

    protected void createNewAccountLinks(Account account, List<? extends Long> newAccountIds) {
        for (Long accountId : newAccountIds) {
            AccountLink link = objectManager.findAny(AccountLink.class, ACCOUNT_LINK.ACCOUNT_ID,
                    account.getId(),
                    ACCOUNT_LINK.LINKED_ACCOUNT_ID, accountId,
                    ACCOUNT_LINK.REMOVED, null);
            if (link == null) {
                link = objectManager.create(AccountLink.class, ACCOUNT_LINK.ACCOUNT_ID,
                        account.getId(), ACCOUNT_LINK.LINKED_ACCOUNT_ID, accountId);
            }
            if (link.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
                objectProcessManager.executeStandardProcess(StandardProcess.CREATE, link, null);
            }
        }
    }

    protected void deleteOldAccountLinks(Account account, List<? extends Long> newAccountIds) {
        List<? extends AccountLink> allLinks = objectManager.find(AccountLink.class,
                ACCOUNT_LINK.ACCOUNT_ID, account.getId(),
                ACCOUNT_LINK.REMOVED, null);
        for (AccountLink link : allLinks) {
            if (!newAccountIds.contains(link.getLinkedAccountId())) {
                objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, link, null);
            }
        }
    }

    @Override
    public List<Long> getLinkedAccounts(long accountId) {
        List<Long> accountIds = new ArrayList<>();
        List<Long> linkedToAccounts = Arrays.asList(create().select(ACCOUNT_LINK.LINKED_ACCOUNT_ID)
                .from(ACCOUNT_LINK)
                .where(ACCOUNT_LINK.ACCOUNT_ID.eq(accountId)
                        .and(ACCOUNT_LINK.REMOVED.isNull()))
                .fetch().intoArray(ACCOUNT_LINK.LINKED_ACCOUNT_ID));

        List<Long> linkedFromAccounts = Arrays.asList(create().select(ACCOUNT_LINK.ACCOUNT_ID)
                .from(ACCOUNT_LINK)
                .where(ACCOUNT_LINK.LINKED_ACCOUNT_ID.eq(accountId)
                        .and(ACCOUNT_LINK.REMOVED.isNull()))
                .fetch().intoArray(ACCOUNT_LINK.ACCOUNT_ID));

        accountIds.addAll(linkedToAccounts);
        accountIds.addAll(linkedFromAccounts);
        return accountIds;
    }

}
