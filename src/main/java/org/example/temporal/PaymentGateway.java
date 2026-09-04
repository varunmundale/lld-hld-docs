package org.example.temporal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A stand-in for a real PSP (Stripe, Adyen, a bank rail). Two properties are deliberate:
 *
 *  1. It honours an idempotency key, like every real payment API does. Same key twice ->
 *     the same auth id and no second charge.
 *  2. It is FILE-BACKED, so it survives the worker process being killed. A crash in your
 *     process does not un-charge a card, and pretending otherwise would make the crash
 *     demo a lie.
 *
 * Delete gateway-ledger.txt to reset it.
 */
public final class PaymentGateway {

    private static final Path LEDGER =
            Paths.get(System.getProperty("gateway.ledger", "gateway-ledger.txt"));
    private final Map<String, String> charges = new LinkedHashMap<>();

    /** Wipe the PSP's memory. Only for demos - a real gateway has no such method. */
    public static void reset() {
        try { Files.deleteIfExists(LEDGER); } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public PaymentGateway() {
        if (Files.exists(LEDGER)) {
            try {
                for (String line : Files.readAllLines(LEDGER, StandardCharsets.UTF_8)) {
                    if (line.isBlank()) continue;
                    String[] f = line.split(" ", 2);
                    charges.put(f[0], f[1]);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /** Returns the auth id, and whether this call was a suppressed duplicate. */
    public synchronized ChargeResult charge(String idempotencyKey, long amountCents) {
        String existing = charges.get(idempotencyKey);
        if (existing != null) {
            System.out.println("  [gateway] key " + idempotencyKey
                    + " already seen -> returning " + existing + ", NO second charge");
            return new ChargeResult(existing, true, amountCents);
        }

        String auth = "auth_" + Integer.toHexString((idempotencyKey + amountCents).hashCode());
        charges.put(idempotencyKey, auth);
        append(idempotencyKey + " " + auth);
        System.out.println("  [gateway] CHARGED " + (amountCents / 100.0)
                + " key=" + idempotencyKey + " -> " + auth
                + "   (distinct charges so far: " + charges.size() + ")");
        return new ChargeResult(auth, false, amountCents);
    }

    public synchronized void refund(String authId) {
        append("REFUND " + authId);
        System.out.println("  [gateway] REFUNDED " + authId);
    }

    public synchronized int distinctCharges() {
        return charges.size();
    }

    private void append(String line) {
        try {
            Files.writeString(LEDGER, line + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
