package com.project.edusync.iam.model.dto;

import com.project.edusync.uis.model.enums.StaffType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateAuditorRequestDTO extends BaseStaffRequestDTO {

    @Override
    public StaffType getStaffType() {
        return StaffType.AUDITOR;
    }
}
