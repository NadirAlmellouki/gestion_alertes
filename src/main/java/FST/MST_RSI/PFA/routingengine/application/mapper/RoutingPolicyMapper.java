package FST.MST_RSI.PFA.routingengine.application.mapper;

import FST.MST_RSI.PFA.routingengine.domain.model.RoutingPolicy;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingStepDefinition;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingPolicyEntity;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingStepEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoutingPolicyMapper {

    public RoutingPolicy toDomain(RoutingPolicyEntity entity) {
        List<RoutingStepDefinition> steps = entity.getSteps().stream()
                .map(s -> new RoutingStepDefinition(
                        s.getId(),
                        s.getStepOrder(),
                        s.getActionType(),
                        s.getTargetRole(),
                        s.getTargetUnitType(),
                        s.getChannel(),
                        s.getDelayAfterSeconds()
                ))
                .toList();
        return new RoutingPolicy(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.isEnabled(),
                entity.getPriority(),
                entity.getPolicyOrigin(),
                steps
        );
    }

    public void mapToEntity(RoutingPolicy policy, RoutingPolicyEntity entity) {
        entity.setId(policy.id());
        entity.setCode(policy.code());
        entity.setName(policy.name());
        entity.setDescription(policy.description());
        entity.setEnabled(policy.enabled());
        entity.setPriority(policy.priority());
        entity.setPolicyOrigin(policy.origin());
        entity.getSteps().clear();
        for (RoutingStepDefinition step : policy.steps()) {
            RoutingStepEntity stepEntity = new RoutingStepEntity();
            stepEntity.setId(step.id() != null ? step.id() : java.util.UUID.randomUUID());
            stepEntity.setPolicy(entity);
            stepEntity.setStepOrder(step.stepOrder());
            stepEntity.setActionType(step.actionType());
            stepEntity.setTargetRole(step.targetRole());
            stepEntity.setTargetUnitType(step.targetUnitType());
            stepEntity.setChannel(step.channel());
            stepEntity.setDelayAfterSeconds(step.delayAfterSeconds());
            entity.getSteps().add(stepEntity);
        }
    }
}
