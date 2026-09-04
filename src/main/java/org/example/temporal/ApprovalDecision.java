package org.example.temporal;

/** Carried by the @SignalMethod. Recorded in history, so the audit trail names the approver. */
public record ApprovalDecision(boolean approved, String by, String reason) {}
