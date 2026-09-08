package org.example.temporal;

/**
 * duplicateSuppressed is the whole point of this record: it is the gateway telling us that the
 * activity ran more than once and the money still only moved once.
 */
public record PaymentAuth(String authId, boolean duplicateSuppressed, long amountCents) {}
