package com.project.edusync.finance.service;

import com.project.edusync.finance.dto.configuration.LateFeeRuleCreateDTO;
import com.project.edusync.finance.dto.configuration.LateFeeRuleResponseDTO;

import java.util.List;

/**
 * Service interface for managing Late Fee Rules.
 */
public interface LateFeeRuleService {

    /**
     * Creates a new late fee rule.
     *
     * @param createDTO The DTO containing the rule details.
     * @return The response DTO of the newly created rule.
     */
    LateFeeRuleResponseDTO createLateFeeRule(LateFeeRuleCreateDTO createDTO);

    /**
     * Retrieves all active late fee rules.
     *
     * @return A list of active late fee rule DTOs.
     */
    List<LateFeeRuleResponseDTO> getAllActiveLateFeeRules();

    /**
     * Updates an existing late fee rule.
     *
     * @param ruleId    The ID of the rule to update.
     * @param updateDTO The DTO with new data.
     * @return The response DTO of the updated rule.
     */
    LateFeeRuleResponseDTO updateLateFeeRule(Long ruleId, LateFeeRuleCreateDTO updateDTO);
}