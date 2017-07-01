package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.NetworkDriverTable.*;
import static io.cattle.platform.core.model.tables.NetworkTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.SubnetConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.tables.records.NetworkRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.net.NetUtils;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Configuration;

import com.netflix.config.DynamicStringListProperty;

public class NetworkDaoImpl extends AbstractJooqDao implements NetworkDao {
    DynamicStringListProperty DOCKER_VIP_SUBNET_CIDR = ArchaiusUtil.getList("docker.vip.subnet.cidr");

    ObjectManager objectManager;
    GenericResourceDao resourceDao;
    LockManager lockManager;
    TransactionDelegate transactionDelegate;

    public NetworkDaoImpl(Configuration configuration, ObjectManager objectManager, GenericResourceDao resourceDao, LockManager lockManager,
            TransactionDelegate transactionDelegate) {
        super(configuration);
        this.objectManager = objectManager;
        this.resourceDao = resourceDao;
        this.lockManager = lockManager;
        this.transactionDelegate = transactionDelegate;
    }

    @Override
    public Network getNetworkByKind(long accountId, String kind) {
        return objectManager.findAny(Network.class,
                NETWORK.KIND, kind,
                NETWORK.ACCOUNT_ID, accountId,
                NETWORK.REMOVED, null);
    }

    @Override
    public Network getNetworkByName(long accountId, String name) {
        return objectManager.findAny(Network.class,
                NETWORK.NAME, name,
                NETWORK.ACCOUNT_ID, accountId,
                NETWORK.REMOVED, null);
    }

    @Override
    public Subnet addVIPSubnet(final long accountId) {
        return lockManager.lock(new SubnetCreateLock(accountId), new LockCallback<Subnet>() {
            @Override
            public Subnet doWithLock() {
                List<Subnet> subnets = objectManager.find(Subnet.class, SUBNET.ACCOUNT_ID, accountId, SUBNET.KIND,
                        SubnetConstants.KIND_VIP_SUBNET);
                if (subnets.size() > 0) {
                    return subnets.get(0);
                }

                Pair<String, Integer> cidr = NetUtils.getCidrAndSize(DOCKER_VIP_SUBNET_CIDR.get().get(0));

                return resourceDao.createAndSchedule(Subnet.class,
                        SUBNET.ACCOUNT_ID, accountId,
                        SUBNET.CIDR_SIZE, cidr.getRight(),
                        SUBNET.NETWORK_ADDRESS, cidr.getLeft(),
                        SUBNET.KIND, SubnetConstants.KIND_VIP_SUBNET);
            }
        });
    }


    @Override
    public Network getDefaultNetwork(Long accountId) {
        Account account = objectManager.loadResource(Account.class, accountId);
        if (account == null) {
            return null;
        }
        return objectManager.loadResource(Network.class, account.getDefaultNetworkId());
    }

    @Override
    public List<Long> findInstancesInUseByServiceDriver(Long serviceId) {
        Long[] ignore = create()
            .select(SERVICE_EXPOSE_MAP.INSTANCE_ID)
            .from(SERVICE_EXPOSE_MAP)
            .where(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(serviceId)
                    .and(SERVICE_EXPOSE_MAP.REMOVED.isNull()))
            .fetch().intoArray(SERVICE_EXPOSE_MAP.INSTANCE_ID);

        return create().select(INSTANCE.ID)
            .from(INSTANCE)
            .join(NETWORK)
                .on(INSTANCE.NETWORK_ID.eq(NETWORK.ID))
            .join(NETWORK_DRIVER)
                .on(NETWORK_DRIVER.ID.eq(NETWORK.NETWORK_DRIVER_ID))
            .where(NETWORK_DRIVER.SERVICE_ID.eq(serviceId)
                    .and(INSTANCE.REMOVED.isNull())
                    .and(INSTANCE.ID.notIn(ignore)))
            .fetchInto(Long.class);
    }

    @Override
    public List<Subnet> getSubnets(Network network) {
        return objectManager.find(Subnet.class,
                SUBNET.NETWORK_ID, network.getId(),
                SUBNET.STATE, CommonStatesConstants.ACTIVE);
    }

    @Override
    public List<? extends Network> getActiveNetworks(Long accountId) {
        return create().select(NETWORK.fields())
                .from(NETWORK)
                .where(NETWORK.ACCOUNT_ID.eq(accountId)
                    .and(NETWORK.STATE.in(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE)))
                .fetchInto(NetworkRecord.class);
    }

}
