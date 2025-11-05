package com.project.edusync.finance.service.implementation;



import com.project.edusync.common.exception.finance.FeeTypeNotFoundException;
import com.project.edusync.finance.dto.feetype.FeeTypeCreateUpdateDTO;
import com.project.edusync.finance.dto.feetype.FeeTypeResponseDTO;

import com.project.edusync.finance.model.entity.FeeType;
import com.project.edusync.finance.repository.FeeTypeRepository;
import com.project.edusync.finance.service.FeeTypeService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeeTypeServiceImpl implements FeeTypeService {

    private final FeeTypeRepository feeTypeRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public FeeTypeResponseDTO createFeeType(FeeTypeCreateUpdateDTO createDTO) {
        FeeType feeType = modelMapper.map(createDTO, FeeType.class);
        // The isActive flag will be set to 'true' by default from the entity.
        FeeType savedFeeType = feeTypeRepository.save(feeType);
        return modelMapper.map(savedFeeType, FeeTypeResponseDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public FeeTypeResponseDTO getFeeTypeById(Long id) {
        FeeType feeType = findDeletedFeeTypeById(id);

        // --- SOFT DELETE CHECK ---
        // not returning inactive fee types by their ID directly.
        if (!feeType.getIsActive()) {
            throw new FeeTypeNotFoundException("Either Fee Type does not exist or deleted; Fee type Id: " + id);
        }

        return modelMapper.map(feeType, FeeTypeResponseDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeeTypeResponseDTO> getAllFeeTypes() {
        // Only find fee types that are active.
        return feeTypeRepository.findByIsActive(true).stream()
                .map(feeType -> modelMapper.map(feeType, FeeTypeResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FeeTypeResponseDTO updateFeeType(Long id, FeeTypeCreateUpdateDTO updateDTO) {
        // We can update an inactive type, so we find it regardless of status
        FeeType existingFeeType = findDeletedFeeTypeById(id);

        modelMapper.map(updateDTO, existingFeeType);

        FeeType updatedFeeType = feeTypeRepository.save(existingFeeType);
        return modelMapper.map(updatedFeeType, FeeTypeResponseDTO.class);
    }

    @Override
    @Transactional
    public void deleteFeeType(Long id) {
        FeeType feeType = findDeletedFeeTypeById(id);

        // --- SOFT DELETE CHANGE ---
        // Instead of deleting, setting the flag and save.
        feeType.setIsActive(false);
        feeTypeRepository.save(feeType);
    }

    // Helper method now finds the entity even if it's "inactive"
    // This allows us to update or reactivate it if needed.
    private FeeType findDeletedFeeTypeById(Long id) {
        return feeTypeRepository.findById(id)
                .orElseThrow(() -> new FeeTypeNotFoundException("Fee type not found for Id: " + id));
    }
}
