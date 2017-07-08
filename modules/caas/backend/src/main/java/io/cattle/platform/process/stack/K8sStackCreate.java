package io.cattle.platform.process.stack;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializerFactory;

public class K8sStackCreate extends StackAgentHandler {

    public K8sStackCreate(AgentLocator agentLocator, ObjectSerializerFactory factory, ObjectManager objectManager, ObjectProcessManager processManager) {
        super(agentLocator, factory, objectManager, processManager);
        commandName = "kubernetesStack.create";
        dataTypeClass = Stack.class;
        stackKind = "kubernetesStack";
        agentService = "io.rancher.container.agent_service.kubernetes_stack";
        errorChainProcess = "stack.error";
    }

}