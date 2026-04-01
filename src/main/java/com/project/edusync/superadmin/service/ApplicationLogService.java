package com.project.edusync.superadmin.service;

import com.project.edusync.superadmin.model.dto.LogTailResponseDto;

public interface ApplicationLogService {

    LogTailResponseDto tailLogs(Integer lines, String level);
}

