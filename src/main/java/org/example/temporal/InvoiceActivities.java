package org.example.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Activities are the ONLY code allowed to touch the outside world: HTTP, DB, files, clocks,
 * randomness, an LLM call. They may run more than once - the engine guarantees at-least-once
 * execution, never exactly-once - so every one of these that has a side effect must be
 * idempotent, which is why chargePayment takes a key rather than inventing one.
 */
@ActivityInterface
public interface InvoiceActivities {

    @ActivityMethod
    ValidationResult validateInvoice(Invoice invoice);

    /** Added in v1 of the "add-fraud-check" patch - see InvoiceWorkflowImpl. */
    @ActivityMethod
    String fraudCheck(Invoice invoice);

    /** Talks to the ERP. Flaky under load; the retry policy is on the stub, not in here. */
    @ActivityMethod
    String matchPurchaseOrder(Invoice invoice);

    /**
     * The money mover. idempotencyKey comes from Workflow.randomUUID(), so it is recorded in
     * history and is therefore identical across every retry AND every replay.
     */
    @ActivityMethod
    ChargeResult chargePayment(String idempotencyKey, Invoice invoice);

    /** Compensation for chargePayment, invoked by the Saga on a downstream failure. */
    @ActivityMethod
    void refundPayment(String authId, Invoice invoice);

    @ActivityMethod
    String postToLedger(String authId, String purchaseOrder, Invoice invoice);

    @ActivityMethod
    void reverseJournal(String journalId);

    @ActivityMethod
    void notifyVendor(Invoice invoice, String outcome);

    @ActivityMethod
    void escalateToController(Invoice invoice, String waited);

    @ActivityMethod
    void voidInvoice(Invoice invoice, String reason);
}
