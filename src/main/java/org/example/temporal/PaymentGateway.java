package org.example.temporal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A stand-in for a real PSP - Stripe, Adyen, a card rail. Two properties are deliberate:
 *
 *  1. It HONOURS AN IDEMPOTENCY KEY, because every real payment API does. Same key twice ->
 *     the same auth id, no second hold. That is the other half of the at-least-once contract:
 *     Temporal promises to run the activity at least once, and this promises that running it
 *     twice with the same key costs the rider nothing.
 *
 *  2. It is FILE-BACKED, so it survives the worker process being killed. A crash in your
 *     process does not un-hold a card, and pretending otherwise would make the crash demo a
 *     lie. This is the thing outside your control that the whole design exists to be careful
 *     about.
 *
 * Delete the ledger file to reset it.
 */
public final class PaymentGateway {

    private static final Path LEDGER =
            Paths.get(System.getProperty("gateway.ledger", "gateway-ledger.txt"));

    private final Map<String, String> byKey = new LinkedHashMap<>();
    private int realMovements;

    public static void reset() {
        try {
            Files.deleteIfExists(LEDGER);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public PaymentGateway() {
        if (!Files.exists(LEDGER)) return;
        try {
            for (String line : Files.readAllLines(LEDGER, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                String[] f = line.split(" ", 2);
                byKey.put(f[0], f[1]);
                realMovements++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Put a hold on the card. Idempotent on key. */
    public synchronized PaymentAuth authorize(String key, long amountCents) {
        String existing = byKey.get(key);
        if (existing != null) {
            System.out.println("      [psp] key " + key + " already seen -> " + existing
                    + ", NO second hold");
            return new PaymentAuth(existing, true, amountCents);
        }
        String authId = "auth_" + Integer.toHexString((key + amountCents).hashCode());
        record(key, authId);
        System.out.println("      [psp] HELD " + money(amountCents) + " key=" + key
                + " -> " + authId + "   (real money movements so far: " + realMovements + ")");
        return new PaymentAuth(authId, false, amountCents);
    }

    /** Settle a hold. A DIFFERENT key from the auth, because it is a different effect. */
    public synchronized String capture(String key, String authId, long amountCents) {
        String existing = byKey.get(key);
        if (existing != null) {
            System.out.println("      [psp] key " + key + " already seen -> " + existing
                    + ", NO second capture");
            return existing;
        }
        String captureId = "cap_" + Integer.toHexString((key + authId).hashCode());
        record(key, captureId);
        System.out.println("      [psp] CAPTURED " + money(amountCents) + " on " + authId
                + " -> " + captureId);
        return captureId;
    }

    public synchronized String charge(String key, long amountCents, String what) {
        String existing = byKey.get(key);
        if (existing != null) {
            System.out.println("      [psp] key " + key + " already seen -> " + existing
                    + ", NO second " + what);
            return existing;
        }
        String id = "chg_" + Integer.toHexString(key.hashCode());
        record(key, id);
        System.out.println("      [psp] CHARGED " + money(amountCents) + " (" + what + ") -> " + id);
        return id;
    }

    public synchronized void voidAuth(String authId) {
        append("VOID " + authId);
        System.out.println("      [psp] VOIDED " + authId);
    }

    /** How many times real money actually moved, regardless of how often activities ran. */
    public synchronized int realMovements() {
        return realMovements;
    }

    private void record(String key, String id) {
        byKey.put(key, id);
        realMovements++;
        append(key + " " + id);
    }

    private void append(String line) {
        try {
            Files.writeString(LEDGER, line + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String money(long cents) {
        return "$" + (cents / 100) + "." + (cents % 100 < 10 ? "0" : "") + (cents % 100);
    }
}
