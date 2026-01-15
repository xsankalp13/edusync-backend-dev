package com.project.edusync.iam.model.dto;

import com.project.edusync.uis.model.enums.SchoolLevel;
import com.project.edusync.uis.model.enums.StaffType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreatePrincipalRequestDTO extends BaseStaffRequestDTO {

    private List<String> administrativeCertifications;
    private SchoolLevel schoolLevelManaged;

    // Does not exist in Entity yet based on files, but assuming exists based on requirements
    // private BigDecimal budgetApprovalLimit;

    @Override
    public StaffType getStaffType() {
        return StaffType.PRINCIPAL;
    }
}