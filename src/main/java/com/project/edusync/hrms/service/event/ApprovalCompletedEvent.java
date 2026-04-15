package com.project.edusync.hrms.service.event;

import com.project.edusync.hrms.model.enums.ApprovalActionType;

import java.util.UUID;

public record ApprovalCompletedEvent(ApprovalActionType actionType, UUID entityRef) {}

