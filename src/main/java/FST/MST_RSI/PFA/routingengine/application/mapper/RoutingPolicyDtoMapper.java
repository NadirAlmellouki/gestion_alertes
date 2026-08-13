package FST.MST_RSI.PFA.routingengine.application.mapper;

import FST.MST_RSI.PFA.routingengine.application.dto.RoutingPolicyDto;
import FST.MST_RSI.PFA.routingengine.domain.model.PolicyOrigin;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingPolicy;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingStepDefinition;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RoutingPolicyDtoMapper {

    public RoutingPolicyDto toDto(RoutingPolicy policy) {
        return new RoutingPolicyDto(
                policy.id(),
                policy.code(),
                policy.name(),
                policy.description(),
                policy.enabled(),
                policy.priority(),
                policy.origin(),
                policy.steps().stream().map(this::toStepDto).toList()
        );
    }

    public RoutingPolicy toDomain(RoutingPolicyDto dto) {
        return new RoutingPolicy(
                dto.id() != null ? dto.id() : UUID.randomUUID(),
                dto.code(),
                dto.name(),
                dto.description(),
                dto.enabled(),
                dto.priority(),
                dto.origin() != null ? dto.origin() : PolicyOrigin.CONFIGURED,
                dto.steps().stream().map(this::toStepDomain).toList()
        );
    }

    private RoutingPolicyDto.RoutingStepDto toStepDto(RoutingStepDefinition step) {
        return new RoutingPolicyDto.RoutingStepDto(
                step.id(),
                step.stepOrder(),
                step.actionType(),
                step.targetRole(),
                step.targetUnitType(),
                step.channel(),
                step.delayAfterSeconds()
        );
    }

    private RoutingStepDefinition toStepDomain(RoutingPolicyDto.RoutingStepDto dto) {
        return new RoutingStepDefinition(
                dto.id(),
                dto.stepOrder(),
                dto.actionType(),
                dto.targetRole(),
                dto.targetUnitType(),
                dto.channel(),
                dto.delayAfterSeconds()
        );
    }
}
