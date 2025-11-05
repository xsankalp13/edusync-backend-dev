package com.project.edusync.finance.service.implementation;

import com.project.edusync.finance.dto.feestructure.FeeParticularCreateDTO;
import com.project.edusync.finance.dto.feestructure.FeeStructureCreateDTO;
import com.project.edusync.finance.dto.feestructure.FeeStructureResponseDTO;
import com.project.edusync.common.exception.finance.FeeTypeNotFoundException;
import com.project.edusync.finance.mapper.FeeStructureMapper;
import com.project.edusync.finance.model.entity.FeeParticular;
import com.project.edusync.finance.model.entity.FeeStructure;
import com.project.edusync.finance.model.entity.FeeType;
import com.project.edusync.finance.repository.FeeParticularRepository;
import com.project.edusync.finance.repository.FeeStructureRepository;
import com.project.edusync.finance.repository.FeeTypeRepository;
import com.project.edusync.finance.service.FeeStructureService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeeStructureServiceImpl implements FeeStructureService {

    // Inject all required repositories and the mapper
    private final FeeStructureRepository feeStructureRepository;
    private final FeeParticularRepository feeParticularRepository;
    private final FeeTypeRepository feeTypeRepository;
    private final FeeStructureMapper feeStructureMapper;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public FeeStructureResponseDTO createFeeStructure(FeeStructureCreateDTO createDTO) {

        // 1. Use ModelMapper to map DTO to entity
        // This replaces 4 manual 'set' calls
        FeeStructure feeStructure = modelMapper.map(createDTO, FeeStructure.class);

        FeeStructure savedStructure = feeStructureRepository.save(feeStructure);

        List<FeeParticular> savedParticulars = new ArrayList<>();

        if (createDTO.getParticulars() != null && !createDTO.getParticulars().isEmpty()) {
            for (FeeParticularCreateDTO particularDTO : createDTO.getParticulars()) {

                Long particularId = particularDTO.getFeeTypeId();
                FeeType feeType = feeTypeRepository.findById(particularDTO.getFeeTypeId())
                        .orElseThrow(() -> new FeeTypeNotFoundException("particular not for the Id: " + particularId));

                // 2. Use ModelMapper to map the nested DTO
                FeeParticular particular = modelMapper.map(particularDTO, FeeParticular.class);

                // 3. Manually set the complex relationships
                particular.setFeeType(feeType);
                particular.setFeeStructure(savedStructure);

                savedParticulars.add(feeParticularRepository.save(particular));
            }
        }

        // 3. Use our dedicated mapper for the response
        return feeStructureMapper.toDto(savedStructure, savedParticulars);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeeStructureResponseDTO> getAllFeeStructures() {

        List<FeeStructure> structures = feeStructureRepository.findAll();

        return structures.stream()
                .map(structure -> {
                    // For each structure, find its particulars
                    List<FeeParticular> particulars = feeParticularRepository.findByFeeStructure_Id(structure.getId());
                    // Map to the response DTO
                    return feeStructureMapper.toDto(structure, particulars);
                })
                .collect(Collectors.toList());
    }
}