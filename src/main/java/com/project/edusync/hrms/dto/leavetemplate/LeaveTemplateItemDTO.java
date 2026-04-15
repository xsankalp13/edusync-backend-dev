package com.project.edusync.hrms.dto.leavetemplate;

public record LeaveTemplateItemDTO(
        Long itemId,
        String uuid,
        Long leaveTypeId,
        String leaveTypeCode,
        String leaveTypeName,
        Integer defaultQuota,
        Integer customQuota
) {
}
