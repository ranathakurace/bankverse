package com.bank.api.dto;
import lombok.Data;
@Data public class ControlCaseResponse { private String caseReference; private String assessmentReference; private String status; private String decision; private String resolvedBy; private String resolutionNotes; }
