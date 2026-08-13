package FST.MST_RSI.PFA.routingengine.api.rest;

import FST.MST_RSI.PFA.common.application.dto.EnabledRequest;
import FST.MST_RSI.PFA.routingengine.application.dto.RoutingPolicyDto;
import FST.MST_RSI.PFA.routingengine.application.usecase.CreateRoutingPolicyUseCase;
import FST.MST_RSI.PFA.routingengine.application.usecase.DeleteRoutingPolicyUseCase;
import FST.MST_RSI.PFA.routingengine.application.usecase.GetRoutingPolicyUseCase;
import FST.MST_RSI.PFA.routingengine.application.usecase.ListRoutingPoliciesUseCase;
import FST.MST_RSI.PFA.routingengine.application.usecase.SetRoutingPolicyEnabledUseCase;
import FST.MST_RSI.PFA.routingengine.application.usecase.UpdateRoutingPolicyUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/routing-policies")
public class RoutingPolicyController {

    private final ListRoutingPoliciesUseCase listRoutingPoliciesUseCase;
    private final GetRoutingPolicyUseCase getRoutingPolicyUseCase;
    private final CreateRoutingPolicyUseCase createRoutingPolicyUseCase;
    private final UpdateRoutingPolicyUseCase updateRoutingPolicyUseCase;
    private final DeleteRoutingPolicyUseCase deleteRoutingPolicyUseCase;
    private final SetRoutingPolicyEnabledUseCase setRoutingPolicyEnabledUseCase;

    public RoutingPolicyController(
            ListRoutingPoliciesUseCase listRoutingPoliciesUseCase,
            GetRoutingPolicyUseCase getRoutingPolicyUseCase,
            CreateRoutingPolicyUseCase createRoutingPolicyUseCase,
            UpdateRoutingPolicyUseCase updateRoutingPolicyUseCase,
            DeleteRoutingPolicyUseCase deleteRoutingPolicyUseCase,
            SetRoutingPolicyEnabledUseCase setRoutingPolicyEnabledUseCase
    ) {
        this.listRoutingPoliciesUseCase = listRoutingPoliciesUseCase;
        this.getRoutingPolicyUseCase = getRoutingPolicyUseCase;
        this.createRoutingPolicyUseCase = createRoutingPolicyUseCase;
        this.updateRoutingPolicyUseCase = updateRoutingPolicyUseCase;
        this.deleteRoutingPolicyUseCase = deleteRoutingPolicyUseCase;
        this.setRoutingPolicyEnabledUseCase = setRoutingPolicyEnabledUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPS','SUPERVISOR')")
    public List<RoutingPolicyDto> list() {
        return listRoutingPoliciesUseCase.execute();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPS','SUPERVISOR')")
    public RoutingPolicyDto get(@PathVariable UUID id) {
        return getRoutingPolicyUseCase.execute(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OPS')")
    public RoutingPolicyDto create(@Valid @RequestBody RoutingPolicyDto request) {
        return createRoutingPolicyUseCase.execute(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPS')")
    public RoutingPolicyDto update(@PathVariable UUID id, @Valid @RequestBody RoutingPolicyDto request) {
        return updateRoutingPolicyUseCase.execute(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OPS')")
    public void delete(@PathVariable UUID id) {
        deleteRoutingPolicyUseCase.execute(id);
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('OPS')")
    public RoutingPolicyDto setEnabled(@PathVariable UUID id, @Valid @RequestBody EnabledRequest request) {
        return setRoutingPolicyEnabledUseCase.execute(id, request.enabled());
    }
}
