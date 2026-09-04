package org.example.temporal;

import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Runs the real workflow against Temporal's in-memory time-skipping test server. No server to
 * install, no docker, no ports: TestWorkflowEnvironment is a genuine Temporal service with the
 * same history, replay, retry and timer semantics, plus a virtual clock. A 72-hour approval
 * deadline and a 2-day settlement window elapse in microseconds.
 *
 *   mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.TemporalDemo
 *   mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.TemporalDemo -Dexec.args=lost-ack
 *
 * Use InvoiceWorker + InvoiceCli against `temporal server start-dev` when you want the web UI
 * and a real crash (kill -9 on the worker). This class is for seeing the semantics quickly.
 */
public final class TemporalDemo {

    record Scenario(String name, String title, String setup, long amountCents,
                    Duration signalAfter, Boolean approve, Map<String, String> props,
                    List<String> watch) {}

    static final long SMALL = 250_000L;      // $2,500  - under the approval threshold
    static final long LARGE = 1_250_000L;    // $12,500 - needs a human

    static final List<Scenario> SCENARIOS = List.of(
            new Scenario("happy", "HAPPY PATH",
                    "$2,500, under the threshold, nothing fails.",
                    SMALL, null, null, Map.of(),
                    List.of("one charge, one journal entry, a 2-day durable timer in between",
                            "the 2-day wait costs one timer row - watch the wall clock")),

            new Scenario("retry", "RETRIES ARE THE ENGINE'S JOB",
                    "The ERP times out on the first 2 attempts of matchPurchaseOrder.",
                    SMALL, null, null, Map.of("fail.matchPurchaseOrder", "2"),
                    List.of("attempt=1,2 throw; attempt=3 succeeds",
                            "the workflow code contains no retry loop - the policy is on the stub",
                            "history records one ActivityTaskCompleted, not three")),

            new Scenario("lost-ack", "AT-LEAST-ONCE, SAVED BY THE IDEMPOTENCY KEY",
                    "The gateway charges, then the ack is lost. Temporal cannot tell that apart "
                            + "from 'never ran', so it retries.",
                    SMALL, null, null, Map.of("fail.afterCharge", "1"),
                    List.of("chargePayment runs TWICE",
                            "the key came from Workflow.randomUUID(), so it is identical both times",
                            "the gateway suppresses the duplicate: ONE real charge",
                            "this is what 'at-least-once execution, exactly-once effect' means")),

            new Scenario("approve", "HUMAN IN THE LOOP - approved",
                    "$12,500, so the workflow blocks on Workflow.await for up to 72h. "
                            + "The controller answers after 1h.",
                    LARGE, Duration.ofHours(1), true, Map.of(),
                    List.of("the signal is durable: it is in history before the handler runs",
                            "no thread is held, no polling job, no pending_approvals table")),

            new Scenario("reject", "HUMAN IN THE LOOP - rejected",
                    "Same invoice, controller rejects it after 2h.",
                    LARGE, Duration.ofHours(2), false, Map.of(),
                    List.of("voidInvoice is just the next line of ordinary code",
                            "chargePayment is never reached: no money moves")),

            new Scenario("escalate", "TIMEOUT, ESCALATION, THEN APPROVAL",
                    "Nobody answers in 72h. Escalate, re-arm a 24h deadline, approval lands at 80h.",
                    LARGE, Duration.ofHours(80), true, Map.of(),
                    List.of("a deadline is an ordinary false return from Workflow.await",
                            "the escalation and the eventual approval are both on the record")),

            new Scenario("parked", "NOBODY EVER ANSWERS",
                    "72h, escalation, 24h, still silence.",
                    LARGE, null, null, Map.of(),
                    List.of("the workflow ends deliberately in PARKED - not stuck, not crashed",
                            "four days of waiting; note the wall clock")),

            new Scenario("compensate", "SAGA COMPENSATION",
                    "The charge succeeds, then the ledger period is closed and postToLedger "
                            + "fails non-retryably.",
                    SMALL, null, null, Map.of("fail.ledger", "true"),
                    List.of("the Saga unwinds: refundPayment runs",
                            "the workflow then fails on purpose - a failed workflow is a real outcome",
                            "compensation is a stack of undo calls, not a compensation table")));

    public static void main(String[] args) {
        System.setProperty("gateway.ledger", "target/demo-gateway-ledger.txt");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        // The SDK logs every retryable activity failure at WARN with a full stack trace, which
        // buries the demo. Raise it back with -Dorg.slf4j.simpleLogger.log.io.temporal=info
        // when you want to see what the SDK itself is doing.
        System.setProperty("org.slf4j.simpleLogger.log.io.temporal", "error");

        List<Scenario> toRun = args.length == 0 || args[0].equals("all")
                ? SCENARIOS
                : SCENARIOS.stream().filter(s -> s.name().equals(args[0])).toList();

        if (toRun.isEmpty()) {
            System.out.println("unknown scenario. try: all, "
                    + SCENARIOS.stream().map(Scenario::name).toList());
            return;
        }
        for (Scenario s : toRun) run(s);
        System.exit(0);
    }

    static void run(Scenario s) {
        line("=");
        System.out.println("  SCENARIO: " + s.name() + "  -  " + s.title());
        System.out.println("  " + s.setup());
        line("=");
        for (String w : s.watch()) System.out.println("  watch for: " + w);
        System.out.println();

        s.props().forEach(System::setProperty);
        PaymentGateway.reset();
        PaymentGateway gateway = new PaymentGateway();

        TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance();
        try {
            Worker worker = env.newWorker(InvoiceWorkflow.TASK_QUEUE);
            worker.registerWorkflowImplementationTypes(InvoiceWorkflowImpl.class);
            worker.registerActivitiesImplementations(new InvoiceActivitiesImpl(gateway));
            env.start();

            WorkflowClient client = env.getWorkflowClient();
            String workflowId = "INV-" + s.name();
            Invoice invoice = new Invoice(workflowId, "Acme Cloud Ltd", s.amountCents(), "USD");

            InvoiceWorkflow workflow = client.newWorkflowStub(
                    InvoiceWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setTaskQueue(InvoiceWorkflow.TASK_QUEUE)
                            .setWorkflowId(workflowId)
                            .setWorkflowExecutionTimeout(Duration.ofDays(30))
                            .build());

            if (s.signalAfter() != null) {
                // Fires at a point on the VIRTUAL clock, so "the human replies 80 hours later"
                // is expressible without waiting 80 hours.
                env.registerDelayedCallback(s.signalAfter(), () -> {
                    System.out.println("  [human] answering after " + s.signalAfter().toHours()
                            + "h: " + (s.approve() ? "APPROVE" : "REJECT"));
                    client.newWorkflowStub(InvoiceWorkflow.class, workflowId)
                            .decide(new ApprovalDecision(s.approve(), "controller@acme.test",
                                    s.approve() ? "within budget" : "PO amount mismatch"));
                });
            }

            long wall = System.currentTimeMillis();
            WorkflowClient.start(workflow::process, invoice);

            String result;
            try {
                result = WorkflowStub.fromTyped(workflow).getResult(String.class);
            } catch (WorkflowFailedException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                result = "FAILED: " + cause.getMessage();
            }
            wall = System.currentTimeMillis() - wall;

            Duration virtual = printHistory(env, WorkflowStub.fromTyped(workflow).getExecution());

            System.out.println();
            System.out.println("  result                 " + result);
            System.out.println("  virtual time elapsed   " + human(virtual)
                    + "   (server-side timers: the workflow really would have waited this long)");
            System.out.println("  wall clock             " + wall + "ms");
            System.out.println("  gateway real charges   " + gateway.distinctCharges()
                    + (gateway.distinctCharges() > 1 ? "   <-- CHARGED TWICE"
                       : gateway.distinctCharges() == 1 ? "   <-- exactly once"
                       : "   (no money moved)"));
        } finally {
            s.props().keySet().forEach(System::clearProperty);
            env.close();
        }
        System.out.println();
    }

    /**
     * The event history, compacted to the columns that matter. This IS the execution: it is
     * what a replay reads, what an auditor reads, and what `temporal workflow show` prints.
     * Returns how much time the execution itself spanned, taken from the event timestamps.
     */
    static Duration printHistory(TestWorkflowEnvironment env,
                                 io.temporal.api.common.v1.WorkflowExecution execution) {
        var events = env.getWorkflowExecutionHistory(execution).getEvents();
        System.out.println();
        System.out.println("  event history (" + events.size() + " events)");
        for (HistoryEvent e : events) {
            String type = e.getEventType().name().replace("EVENT_TYPE_", "");
            System.out.printf("    %3d  %-34s %s%n", e.getEventId(), type, detail(e));
        }
        if (events.isEmpty()) return Duration.ZERO;
        long from = events.get(0).getEventTime().getSeconds();
        long to = events.get(events.size() - 1).getEventTime().getSeconds();
        return Duration.ofSeconds(to - from);
    }

    static String detail(HistoryEvent e) {
        return switch (e.getEventType()) {
            case EVENT_TYPE_ACTIVITY_TASK_SCHEDULED ->
                    e.getActivityTaskScheduledEventAttributes().getActivityType().getName();
            case EVENT_TYPE_ACTIVITY_TASK_STARTED ->
                    "attempt " + e.getActivityTaskStartedEventAttributes().getAttempt();
            case EVENT_TYPE_ACTIVITY_TASK_FAILED ->
                    "attempt " + e.getActivityTaskFailedEventAttributes().getFailure().getMessage();
            case EVENT_TYPE_TIMER_STARTED -> "fires in "
                    + human(Duration.ofSeconds(
                            e.getTimerStartedEventAttributes().getStartToFireTimeout().getSeconds()));
            case EVENT_TYPE_WORKFLOW_EXECUTION_SIGNALED ->
                    "signal '" + e.getWorkflowExecutionSignaledEventAttributes().getSignalName() + "'";
            case EVENT_TYPE_MARKER_RECORDED ->
                    "marker '" + e.getMarkerRecordedEventAttributes().getMarkerName()
                            + "'   <- the versioning / side-effect record";
            case EVENT_TYPE_WORKFLOW_EXECUTION_FAILED ->
                    e.getWorkflowExecutionFailedEventAttributes().getFailure().getMessage();
            case EVENT_TYPE_WORKFLOW_EXECUTION_COMPLETED -> "done";
            default -> "";
        };
    }

    static String human(Duration d) {
        if (d.toDays() > 0) return d.toDays() + "d" + (d.toHoursPart() > 0 ? " " + d.toHoursPart() + "h" : "");
        if (d.toHours() > 0) return d.toHours() + "h";
        if (d.toMinutes() > 0) return d.toMinutes() + "m";
        return d.toSeconds() + "s";
    }

    static String indent(String s) {
        StringBuilder sb = new StringBuilder();
        for (String l : s.split("\n")) sb.append("    ").append(l).append('\n');
        return sb.toString();
    }

    static void line(String c) {
        System.out.println(c.repeat(78));
    }
}
