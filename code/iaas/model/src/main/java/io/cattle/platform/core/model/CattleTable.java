/**
 * This class is generated by jOOQ
 */
package io.cattle.platform.core.model;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(value    = { "http://www.jooq.org", "3.3.0" },
                            comments = "This class is generated by jOOQ")
@java.lang.SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class CattleTable extends org.jooq.impl.SchemaImpl {

	private static final long serialVersionUID = 207728954;

	/**
	 * The singleton instance of <code>cattle</code>
	 */
	public static final CattleTable CATTLE = new CattleTable();

	/**
	 * No further instances allowed
	 */
	private CattleTable() {
		super("cattle");
	}

	@Override
	public final java.util.List<org.jooq.Table<?>> getTables() {
		java.util.List result = new java.util.ArrayList();
		result.addAll(getTables0());
		return result;
	}

	private final java.util.List<org.jooq.Table<?>> getTables0() {
		return java.util.Arrays.<org.jooq.Table<?>>asList(
			io.cattle.platform.core.model.tables.AccountTable.ACCOUNT,
			io.cattle.platform.core.model.tables.AgentTable.AGENT,
			io.cattle.platform.core.model.tables.AgentGroupTable.AGENT_GROUP,
			io.cattle.platform.core.model.tables.AuditLogTable.AUDIT_LOG,
			io.cattle.platform.core.model.tables.AuthTokenTable.AUTH_TOKEN,
			io.cattle.platform.core.model.tables.BackupTable.BACKUP,
			io.cattle.platform.core.model.tables.BackupTargetTable.BACKUP_TARGET,
			io.cattle.platform.core.model.tables.CertificateTable.CERTIFICATE,
			io.cattle.platform.core.model.tables.ClusterHostMapTable.CLUSTER_HOST_MAP,
			io.cattle.platform.core.model.tables.ConfigItemTable.CONFIG_ITEM,
			io.cattle.platform.core.model.tables.ConfigItemStatusTable.CONFIG_ITEM_STATUS,
			io.cattle.platform.core.model.tables.ContainerEventTable.CONTAINER_EVENT,
			io.cattle.platform.core.model.tables.CredentialTable.CREDENTIAL,
			io.cattle.platform.core.model.tables.CredentialInstanceMapTable.CREDENTIAL_INSTANCE_MAP,
			io.cattle.platform.core.model.tables.DataTable.DATA,
			io.cattle.platform.core.model.tables.DatabasechangelogTable.DATABASECHANGELOG,
			io.cattle.platform.core.model.tables.DatabasechangeloglockTable.DATABASECHANGELOGLOCK,
			io.cattle.platform.core.model.tables.DeploymentUnitTable.DEPLOYMENT_UNIT,
			io.cattle.platform.core.model.tables.DynamicSchemaTable.DYNAMIC_SCHEMA,
			io.cattle.platform.core.model.tables.DynamicSchemaRoleTable.DYNAMIC_SCHEMA_ROLE,
			io.cattle.platform.core.model.tables.StackTable.STACK,
			io.cattle.platform.core.model.tables.ExternalEventTable.EXTERNAL_EVENT,
			io.cattle.platform.core.model.tables.ExternalHandlerTable.EXTERNAL_HANDLER,
			io.cattle.platform.core.model.tables.ExternalHandlerExternalHandlerProcessMapTable.EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP,
			io.cattle.platform.core.model.tables.ExternalHandlerProcessTable.EXTERNAL_HANDLER_PROCESS,
			io.cattle.platform.core.model.tables.GenericObjectTable.GENERIC_OBJECT,
			io.cattle.platform.core.model.tables.HealthcheckInstanceTable.HEALTHCHECK_INSTANCE,
			io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable.HEALTHCHECK_INSTANCE_HOST_MAP,
			io.cattle.platform.core.model.tables.HostTable.HOST,
			io.cattle.platform.core.model.tables.HostIpAddressMapTable.HOST_IP_ADDRESS_MAP,
			io.cattle.platform.core.model.tables.HostLabelMapTable.HOST_LABEL_MAP,
			io.cattle.platform.core.model.tables.HostVnetMapTable.HOST_VNET_MAP,
			io.cattle.platform.core.model.tables.ImageTable.IMAGE,
			io.cattle.platform.core.model.tables.ImageStoragePoolMapTable.IMAGE_STORAGE_POOL_MAP,
			io.cattle.platform.core.model.tables.InstanceTable.INSTANCE,
			io.cattle.platform.core.model.tables.InstanceHostMapTable.INSTANCE_HOST_MAP,
			io.cattle.platform.core.model.tables.InstanceLabelMapTable.INSTANCE_LABEL_MAP,
			io.cattle.platform.core.model.tables.InstanceLinkTable.INSTANCE_LINK,
			io.cattle.platform.core.model.tables.IpAddressTable.IP_ADDRESS,
			io.cattle.platform.core.model.tables.IpAddressNicMapTable.IP_ADDRESS_NIC_MAP,
			io.cattle.platform.core.model.tables.IpAssociationTable.IP_ASSOCIATION,
			io.cattle.platform.core.model.tables.IpPoolTable.IP_POOL,
			io.cattle.platform.core.model.tables.LabelTable.LABEL,
			io.cattle.platform.core.model.tables.MachineDriverTable.MACHINE_DRIVER,
			io.cattle.platform.core.model.tables.MountTable.MOUNT,
			io.cattle.platform.core.model.tables.NetworkTable.NETWORK,
			io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE,
			io.cattle.platform.core.model.tables.NetworkServiceProviderTable.NETWORK_SERVICE_PROVIDER,
			io.cattle.platform.core.model.tables.NetworkServiceProviderInstanceMapTable.NETWORK_SERVICE_PROVIDER_INSTANCE_MAP,
			io.cattle.platform.core.model.tables.NicTable.NIC,
			io.cattle.platform.core.model.tables.OfferingTable.OFFERING,
			io.cattle.platform.core.model.tables.PhysicalHostTable.PHYSICAL_HOST,
			io.cattle.platform.core.model.tables.PortTable.PORT,
			io.cattle.platform.core.model.tables.ProcessExecutionTable.PROCESS_EXECUTION,
			io.cattle.platform.core.model.tables.ProcessInstanceTable.PROCESS_INSTANCE,
			io.cattle.platform.core.model.tables.ProjectMemberTable.PROJECT_MEMBER,
			io.cattle.platform.core.model.tables.ResourcePoolTable.RESOURCE_POOL,
			io.cattle.platform.core.model.tables.ServiceTable.SERVICE,
			io.cattle.platform.core.model.tables.ServiceConsumeMapTable.SERVICE_CONSUME_MAP,
			io.cattle.platform.core.model.tables.ServiceEventTable.SERVICE_EVENT,
			io.cattle.platform.core.model.tables.ServiceExposeMapTable.SERVICE_EXPOSE_MAP,
			io.cattle.platform.core.model.tables.ServiceIndexTable.SERVICE_INDEX,
			io.cattle.platform.core.model.tables.ServiceLogTable.SERVICE_LOG,
			io.cattle.platform.core.model.tables.SettingTable.SETTING,
			io.cattle.platform.core.model.tables.SnapshotTable.SNAPSHOT,
			io.cattle.platform.core.model.tables.StoragePoolTable.STORAGE_POOL,
			io.cattle.platform.core.model.tables.StoragePoolHostMapTable.STORAGE_POOL_HOST_MAP,
			io.cattle.platform.core.model.tables.SubnetTable.SUBNET,
			io.cattle.platform.core.model.tables.SubnetVnetMapTable.SUBNET_VNET_MAP,
			io.cattle.platform.core.model.tables.TaskTable.TASK,
			io.cattle.platform.core.model.tables.TaskInstanceTable.TASK_INSTANCE,
			io.cattle.platform.core.model.tables.UserPreferenceTable.USER_PREFERENCE,
			io.cattle.platform.core.model.tables.VnetTable.VNET,
			io.cattle.platform.core.model.tables.VolumeTable.VOLUME,
			io.cattle.platform.core.model.tables.VolumeStoragePoolMapTable.VOLUME_STORAGE_POOL_MAP,
			io.cattle.platform.core.model.tables.VolumeTemplateTable.VOLUME_TEMPLATE,
			io.cattle.platform.core.model.tables.ZoneTable.ZONE);
	}
}
