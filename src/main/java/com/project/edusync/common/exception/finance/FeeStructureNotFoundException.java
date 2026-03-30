package com.project.edusync.common.exception.finance;

import com.project.edusync.common.exception.EdusyncException;
import org.springframework.http.HttpStatus;

public class FeeStructureNotFoundException extends EdusyncException {
  public FeeStructureNotFoundException(String message) {
    super(message, HttpStatus.NOT_FOUND);
  }
}
