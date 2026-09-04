package org.example.temporal;

/** duplicateSuppressed = the gateway recognised the idempotency key and did not charge again. */
public record ChargeResult(String authId, boolean duplicateSuppressed, long amountCents) {}
