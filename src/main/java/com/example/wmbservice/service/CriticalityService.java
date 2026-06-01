package com.example.wmbservice.service;

import com.example.wmbservice.model.BudgetTransaction;
import com.example.wmbservice.model.Criticality;
import com.example.wmbservice.model.ProjectedTransaction;
import com.example.wmbservice.repository.CriticalityRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class CriticalityService {

    public static final long ESSENTIAL_ID = 1L;
    public static final long NONESSENTIAL_ID = 2L;
    public static final long PLANNED_ID = 3L;

    private static final Logger logger = LoggerFactory.getLogger(CriticalityService.class);

    private final CriticalityRepository criticalityRepository;

    public CriticalityService(CriticalityRepository criticalityRepository) {
        this.criticalityRepository = criticalityRepository;
    }

    @PostConstruct
    void seedDefaults() {
        saveIfMissing(ESSENTIAL_ID, "Essential");
        saveIfMissing(NONESSENTIAL_ID, "Nonessential");
        saveIfMissing(PLANNED_ID, "Planned");
    }

    public void normalize(BudgetTransaction transaction) {
        ResolvedCriticality resolved = resolve(transaction.getCriticality(), transaction.getCriticalityId());
        transaction.setCriticality(resolved.name());
        transaction.setCriticalityId(resolved.id());
    }

    public void normalize(ProjectedTransaction transaction) {
        ResolvedCriticality resolved = resolve(transaction.getCriticality(), transaction.getCriticalityId());
        transaction.setCriticality(resolved.name());
        transaction.setCriticalityId(resolved.id());
    }

    public Long findIdByName(String criticality) {
        if (criticality == null || criticality.isBlank()) {
            return null;
        }
        return criticalityRepository.findByNameIgnoreCase(criticality.trim())
                .map(Criticality::getId)
                .orElse(null);
    }

    private ResolvedCriticality resolve(String criticalityName, Long criticalityId) {
        boolean hasName = criticalityName != null && !criticalityName.isBlank();
        boolean hasId = criticalityId != null;

        if (!hasName && !hasId) {
            throw new IllegalArgumentException("Either criticality or criticality_id is required");
        }

        Optional<Criticality> byId = hasId ? criticalityRepository.findById(criticalityId) : Optional.empty();
        Optional<Criticality> byName = hasName ? criticalityRepository.findByNameIgnoreCase(criticalityName.trim()) : Optional.empty();

        if (hasId && byId.isEmpty()) {
            throw new IllegalArgumentException("Unknown criticality_id: " + criticalityId);
        }

        if (hasName && byName.isEmpty()) {
            throw new IllegalArgumentException("Unknown criticality: " + criticalityName);
        }

        if (byId.isPresent() && byName.isPresent() && !Objects.equals(byId.get().getId(), byName.get().getId())) {
            throw new IllegalArgumentException("criticality and criticality_id do not match");
        }

        Criticality resolved = byId.or(() -> byName)
                .orElseThrow(() -> new IllegalArgumentException("Unable to resolve criticality"));
        return new ResolvedCriticality(resolved.getId(), resolved.getName());
    }

    private void saveIfMissing(long id, String name) {
        if (criticalityRepository.findById(id).isPresent()) {
            return;
        }
        criticalityRepository.findByNameIgnoreCase(name)
                .ifPresentOrElse(existing -> {
                    if (!Objects.equals(existing.getId(), id)) {
                        logger.warn("Criticality {} exists with unexpected id={}", name, existing.getId());
                    }
                }, () -> criticalityRepository.save(new Criticality(id, name)));
    }

    private record ResolvedCriticality(Long id, String name) {}
}
