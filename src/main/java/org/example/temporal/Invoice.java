package org.example.temporal;

/**
 * Payloads crossing the workflow/activity boundary are serialised into the event history by
 * the SDK's Jackson converter. Two consequences worth remembering:
 *
 *  - they are stored, so keep them small (~2 MB payload cap; pass references, not blobs)
 *  - they are stored for the whole retention period, so schema changes must stay backward
 *    compatible: a replay in six months deserialises today's JSON with tomorrow's class
 */
public record Invoice(String invoiceId, String vendor, long amountCents, String currency) {
    public String display() {
        return invoiceId + " " + vendor + " " + (amountCents / 100.0) + " " + currency;
    }
}
