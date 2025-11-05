package com.project.edusync.finance.service.implementation; // Assuming 'implementation' package

import com.project.edusync.common.exception.finance.FeeStructureNotFoundException;
import com.project.edusync.common.exception.finance.FeeTypeNotFoundException;
import com.project.edusync.finance.dto.feestructure.FeeParticularCreateDTO;
import com.project.edusync.finance.dto.feestructure.FeeStructureCreateDTO;
import com.project.edusync.finance.dto.feestructure.FeeStructureResponseDTO;

import com.project.edusync.finance.dto.feestructure.FeeStructureUpdateDTO;
import com.project.edusync.finance.mapper.FeeStructureMapper;
import com.project.edusync.finance.model.entity.FeeParticular;
import com.project.edusync.finance.model.entity.FeeStructure;
import com.project.edusync.finance.model.entity.FeeType;
import com.project.edusync.finance.repository.FeeParticularRepository;
import com.project.edusync.finance.repository.FeeStructureRepository;
import com.project.edusync.finance.repository.FeeTypeRepository;
import com.project.edusync.finance.service.FeeStructureService; // Import the interface
import lombok.RequiredArgsConstructor;
// We are no longer using ModelMapper in this method, so the import is not needed here.
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeeStructureServiceImpl implements FeeStructureService {

    private final FeeStructureRepository feeStructureRepository;
    private final FeeParticularRepository feeParticularRepository;
    private final FeeTypeRepository feeTypeRepository;
    private final FeeStructureMapper feeStructureMapper;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public FeeStructureResponseDTO createFeeStructure(FeeStructureCreateDTO createDTO) {

        // --- FIX: Reverted to manual mapping for entity creation ---

        // 1. Manually create and set fields for the parent FeeStructure
        FeeStructure feeStructure = new FeeStructure();
        feeStructure.setName(createDTO.getName());
        feeStructure.setAcademicYear(createDTO.getAcademicYear());
        feeStructure.setDescription(createDTO.getDescription());
        feeStructure.setActive(createDTO.isActive());
        // Note: 'isActive' in the DTO should be a boolean, not Boolean
        // or ensure your entity's 'isActive' field is initialized to 'false' if DTO is null.
        // Let's assume 'isActive' in FeeStructure entity is initialized.

        FeeStructure savedStructure = feeStructureRepository.save(feeStructure);

        List<FeeParticular> savedParticulars = new ArrayList<>();

        if (createDTO.getParticulars() != null && !createDTO.getParticulars().isEmpty()) {
            for (FeeParticularCreateDTO particularDTO : createDTO.getParticulars()) {

                FeeType feeType = feeTypeRepository.findById(particularDTO.getFeeTypeId())
                        .orElseThrow(() -> new FeeTypeNotFoundException("Particular with particular Id: " + particularDTO.getFeeTypeId()));

                // 2. Manually create and set fields for the child FeeParticular
                FeeParticular particular = new FeeParticular();
                particular.setName(particularDTO.getName());
                particular.setAmount(particularDTO.getAmount());
                particular.setFrequency(particularDTO.getFrequency());

                // 3. Set relationships
                particular.setFeeType(feeType);
                particular.setFeeStructure(savedStructure);

                // 4. Save the new, guaranteed-transient entity
                savedParticulars.add(feeParticularRepository.save(particular));
            }
        }

        // 5. Use the mapper for the *response* (which is safe)
        return feeStructureMapper.toDto(savedStructure, savedParticulars);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeeStructureResponseDTO> getAllFeeStructures() {

        List<FeeStructure> structures = feeStructureRepository.findAll();

        return structures.stream()
                .map(structure -> {
                    List<FeeParticular> particulars = feeParticularRepository.findByFeeStructure_Id(structure.getId());
                    return feeStructureMapper.toDto(structure, particulars);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FeeStructureResponseDTO getFeeStructureById(Long structureId) {
        // 1. Find the parent structure by ID
        FeeStructure structure = findStructureById(structureId);

        // 2. Find its child particulars
        List<FeeParticular> particulars = feeParticularRepository.findByFeeStructure_Id(structure.getId());

        // 3. Map both to the response DTO
        return feeStructureMapper.toDto(structure, particulars);
    }

    @Override
    @Transactional
    public FeeStructureResponseDTO updateFeeStructure(Long structureId, FeeStructureUpdateDTO updateDTO) {
        // 1. Find the existing entity
        FeeStructure existingStructure = findStructureById(structureId);

        // 2. Use ModelMapper to map new data from DTO to the existing entity
        modelMapper.map(updateDTO, existingStructure);

        // 3. Save the updated entity
        FeeStructure updatedStructure = feeStructureRepository.save(existingStructure);

        // 4. Reload particulars (as they weren't part of this update)
        //    to return a complete response object.
        List<FeeParticular> particulars = feeParticularRepository.findByFeeStructure_Id(updatedStructure.getId());

        return feeStructureMapper.toDto(updatedStructure, particulars);
    }


    @Override
    @Transactional
    public void deleteFeeStructure(Long structureId) {
        FeeStructure structure = findStructureById(structureId);

        // TODO: Add logic to check if this structure is linked to any
        // non-DRAFT invoices before allowing deletion.

        // Soft-delete the structure by setting it inactive
        // This is based on the 'isActive' field in your FeeStructure entity
        structure.setActive(false);
        feeStructureRepository.save(structure);
    }


    private FeeStructure findStructureById(Long structureId) {
        return feeStructureRepository.findById(structureId)
                .orElseThrow(() -> new FeeStructureNotFoundException("Fee Structure not found with Fee structure Id: " + structureId));
    }
}