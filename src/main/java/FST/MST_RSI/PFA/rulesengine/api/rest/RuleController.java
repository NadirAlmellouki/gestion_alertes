package FST.MST_RSI.PFA.rulesengine.api.rest;

import FST.MST_RSI.PFA.common.application.dto.EnabledRequest;
import FST.MST_RSI.PFA.rulesengine.application.dto.RuleDto;
import FST.MST_RSI.PFA.rulesengine.application.usecase.CreateRuleUseCase;
import FST.MST_RSI.PFA.rulesengine.application.usecase.DeleteBusinessRuleUseCase;
import FST.MST_RSI.PFA.rulesengine.application.usecase.GetBusinessRuleUseCase;
import FST.MST_RSI.PFA.rulesengine.application.usecase.ListBusinessRulesUseCase;
import FST.MST_RSI.PFA.rulesengine.application.usecase.SetBusinessRuleEnabledUseCase;
import FST.MST_RSI.PFA.rulesengine.application.usecase.UpdateBusinessRuleUseCase;
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
@RequestMapping("/api/v1/business-rules")
public class RuleController {

    private final ListBusinessRulesUseCase listBusinessRulesUseCase;
    private final GetBusinessRuleUseCase getBusinessRuleUseCase;
    private final CreateRuleUseCase createRuleUseCase;
    private final UpdateBusinessRuleUseCase updateBusinessRuleUseCase;
    private final DeleteBusinessRuleUseCase deleteBusinessRuleUseCase;
    private final SetBusinessRuleEnabledUseCase setBusinessRuleEnabledUseCase;

    public RuleController(
            ListBusinessRulesUseCase listBusinessRulesUseCase,
            GetBusinessRuleUseCase getBusinessRuleUseCase,
            CreateRuleUseCase createRuleUseCase,
            UpdateBusinessRuleUseCase updateBusinessRuleUseCase,
            DeleteBusinessRuleUseCase deleteBusinessRuleUseCase,
            SetBusinessRuleEnabledUseCase setBusinessRuleEnabledUseCase
    ) {
        this.listBusinessRulesUseCase = listBusinessRulesUseCase;
        this.getBusinessRuleUseCase = getBusinessRuleUseCase;
        this.createRuleUseCase = createRuleUseCase;
        this.updateBusinessRuleUseCase = updateBusinessRuleUseCase;
        this.deleteBusinessRuleUseCase = deleteBusinessRuleUseCase;
        this.setBusinessRuleEnabledUseCase = setBusinessRuleEnabledUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPS','SUPERVISOR')")
    public List<RuleDto> list() {
        return listBusinessRulesUseCase.execute();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPS','SUPERVISOR')")
    public RuleDto get(@PathVariable UUID id) {
        return getBusinessRuleUseCase.execute(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OPS')")
    public RuleDto create(@Valid @RequestBody RuleDto request) {
        return createRuleUseCase.execute(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPS')")
    public RuleDto update(@PathVariable UUID id, @Valid @RequestBody RuleDto request) {
        return updateBusinessRuleUseCase.execute(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OPS')")
    public void delete(@PathVariable UUID id) {
        deleteBusinessRuleUseCase.execute(id);
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('OPS')")
    public RuleDto setEnabled(@PathVariable UUID id, @Valid @RequestBody EnabledRequest request) {
        return setBusinessRuleEnabledUseCase.execute(id, request.enabled());
    }
}
