package com.project.edusync.common.exception.finance;

import com.project.edusync.common.exception.EdusyncException;

public class LateFeeRuleNotFoundException extends EdusyncException {
    public LateFeeRuleNotFoundException(String message) {
        super(message);
    }
}
