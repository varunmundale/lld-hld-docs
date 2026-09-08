package org.example.temporal;

import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Every failure mode, run against Temporal's in-memory TIME-SKIPPING test server.
 *
 * No docker, no ports, no install. TestWorkflowEnvironment is a genuine Temporal service -
 * same history, same replay, same retries, same timers - with a virtual clock. A 15-second
 * offer TTL and a 5-minute dispatch deadline elapse in microseconds, so you can watch a ride
 * that would take four minutes of real dispatching finish in 30ms.
 *
 *   mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.RideDemo
 *   mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.RideDemo -Dexec.args=race
 *
 * Use RideWorker + RideCli against `temporal server start-dev` when you want the web UI and a
 * real kill -9.
 */
public final class RideDemo {

    /** A scripted tap on a phone, at a point on the VIRTUAL clock. */
    record Act(Duration at, String kind, String driver, String arg) {
        static Act at(long sec, String kind, String driver) {
            return new Act(Duration.ofSeconds(sec), kind, driver, null);
        }
        static Act at(long sec, String kind, String driver, String arg) {
            return new Act(Duration.ofSeconds(sec), kind, driver, arg);
        }
    }

    record Scenario(String name, String title, String setup,
                    Map<String, String> props, List<Act> script, List<String> watch,
                    boolean farPickup, boolean noPushWorker) {
        Scenario(String name, String title, String setup, Map<String, String> props,
                 List<Act> script, List<String> watch) {
            this(name, title, setup, props, script, watch, false, false);
        }
    }

    static final List<Scenario> SCENARIOS = List.of(

            new Scenario("happy", "HAPPY PATH",
                    "Three drivers get the offer, D-101 accepts after 6s, trip runs.",
                    Map.of(),
                    List.of(Act.at(6, "accept", "D-101"),
                            Act.at(90, "arrive", "D-101"),
                            Act.at(120, "start", "D-101"),
                            Act.at(900, "end", "D-101", "4580")),
                    List.of("one broadcast round: three pushOffer activities, fired in parallel",
                            "the two losing cards are WITHDRAWN the moment D-101 wins",
                            "watch the task queue column: dispatch / notifications / payments")),

            new Scenario("decline", "DRIVERS DECLINE",
                    "D-101 and D-102 swipe the card away; D-103 takes it.",
                    Map.of(),
                    List.of(Act.at(3, "decline", "D-101"),
                            Act.at(4, "decline", "D-102"),
                            Act.at(6, "accept", "D-103"),
                            Act.at(90, "arrive", "D-103"),
                            Act.at(120, "start", "D-103"),
                            Act.at(700, "end", "D-103", "4310")),
                    List.of("declines are SIGNALS - fire-and-forget, nobody waits on a value",
                            "the round ends early once all three offers are answered")),

            new Scenario("silent", "NOBODY ANSWERS ROUND 1",
                    "All three phones stay dark. The 15s offer TTL expires and the ride is "
                            + "re-broadcast to the next three drivers.",
                    Map.of(),
                    List.of(Act.at(20, "accept", "D-104"),
                            Act.at(120, "arrive", "D-104"),
                            Act.at(150, "start", "D-104"),
                            Act.at(800, "end", "D-104", "4650")),
                    List.of("the TTL is a durable timer, not a scheduled job scanning a table",
                            "withdrawOffer runs for all three stale cards",
                            "round 2 goes to different drivers - alreadyTried is passed down")),

            new Scenario("race", "TWO DRIVERS ACCEPT THE SAME RIDE",
                    "D-101 and D-102 both tap ACCEPT in the same instant.",
                    Map.of(),
                    List.of(Act.at(6, "race", "D-101", "D-102"),
                            Act.at(90, "arrive", "D-101"),
                            Act.at(120, "start", "D-101"),
                            Act.at(600, "end", "D-101", "4390")),
                    List.of("one WON, one TOO_LATE - and there is NO LOCK anywhere in this repo",
                            "both updates are in history; the workflow thread ran them in order",
                            "the loser gets a real answer on its own call, not a push 400ms later")),

            new Scenario("stale-accept", "ACCEPTING AN EXPIRED OFFER",
                    "D-101 finally taps ACCEPT at 20s - after their card expired and the ride "
                            + "was re-broadcast to a different three drivers.",
                    Map.of(),
                    List.of(Act.at(20, "accept-stale", "D-101"),
                            Act.at(22, "accept", "D-104"),
                            Act.at(120, "arrive", "D-104"),
                            Act.at(150, "start", "D-104"),
                            Act.at(700, "end", "D-104", "4510")),
                    List.of("EXPIRED, not a second assignment: liveOffers was cleared at the TTL",
                            "the accept is still recorded - a real driver really did tap it")),

            new Scenario("at-least-once", "AT-LEAST-ONCE PUSH, DEDUPED ON THE OFFER ID",
                    "D-101's push lands, then the ack is lost. Temporal cannot tell that apart "
                            + "from 'never sent', so it sends again.",
                    Map.of("fail.after.pushOffer", "1", "fail.only.driver", "D-101"),
                    List.of(Act.at(6, "accept", "D-102"),
                            Act.at(90, "arrive", "D-102"),
                            Act.at(120, "start", "D-102"),
                            Act.at(600, "end", "D-102", "4405")),
                    List.of("pushOffer runs TWICE for D-101 - attempt=1 and attempt=2",
                            "the offerId came from Workflow.randomUUID(), so it is the SAME id",
                            "the app drops the duplicate: ONE card on the screen",
                            "push attempts > cards shown is the whole lesson in one line")),

            new Scenario("lost-ack", "AT-LEAST-ONCE ON MONEY",
                    "The PSP puts the hold on the card, then the ack is lost.",
                    Map.of("fail.after.authorizeFare", "1"),
                    List.of(Act.at(6, "accept", "D-101"),
                            Act.at(90, "arrive", "D-101"),
                            Act.at(120, "start", "D-101"),
                            Act.at(700, "end", "D-101", "4720")),
                    List.of("authorizeFare runs TWICE, with the SAME key, and holds ONE card",
                            "the key was minted before dispatch even started, and is in history",
                            "'at-least-once execution, exactly-once effect' - that is the deal")),

            new Scenario("cross-ride", "THE RACE TEMPORAL DOES NOT SOLVE FOR YOU",
                    "D-101 accepts, but a DIFFERENT ride's workflow reserved them one second "
                            + "earlier. Two workflows cannot be serialized against each other.",
                    Map.of(),
                    List.of(Act.at(5, "steal", "D-101", "RIDE-other"),
                            Act.at(6, "accept", "D-101"),
                            Act.at(20, "accept", "D-104"),
                            Act.at(120, "arrive", "D-104"),
                            Act.at(150, "start", "D-104"),
                            Act.at(800, "end", "D-104", "4480")),
                    List.of("acceptOffer returns WON - inside THIS workflow, D-101 did win",
                            "reserveDriver returns false: the fleet service is the arbiter",
                            "the loop just carries on dispatching; the rider never knows")),

            new Scenario("cancel", "RIDER CANCELS AFTER A DRIVER IS ASSIGNED",
                    "D-101 is en route when the rider cancels at 60s.",
                    Map.of(),
                    List.of(Act.at(6, "accept", "D-101"),
                            Act.at(60, "cancel", null, "changed my mind")),
                    List.of("the Saga unwinds: voidAuth, then releaseDriver, in reverse order",
                            "the cancellation fee has its OWN key - never the fare's",
                            "compensation is a stack of undo calls, not a compensation table")),

            new Scenario("cancel-early", "RIDER CANCELS WHILE STILL SEARCHING",
                    "Cancelled at 8s, before anybody accepted.",
                    Map.of(),
                    List.of(Act.at(8, "cancel", null, "took the bus")),
                    List.of("the await in the dispatch loop wakes on the cancel, not on the TTL",
                            "nothing was reserved and no money moved, so there is nothing to undo",
                            "cancelling while searching is free - and that falls out of the code")),

            new Scenario("declined", "CARD DECLINED AFTER THE DRIVER IS RESERVED",
                    "The driver is committed, then the PSP declines non-retryably.",
                    Map.of("fail.declined", "true"),
                    List.of(Act.at(6, "accept", "D-101")),
                    List.of("CardDeclined is in doNotRetry - a decline is not a blip",
                            "the Saga releases the driver back into the pool",
                            "the workflow FAILS on purpose: a failed workflow is a real outcome")),

            new Scenario("no-drivers", "NO SUPPLY",
                    "Pickup is out in the sticks. The radius doubles 2 -> 4 -> 8 -> 16km and "
                            + "still finds nobody, for five minutes.",
                    Map.of(), List.of(),
                    List.of("five minutes of dispatching, in a few ms of wall clock",
                            "ends deliberately in NO_DRIVERS - not stuck, not crashed"),
                    true, false),

            new Scenario("queues", "A TASK QUEUE WITH NOBODY POLLING IT",
                    "The ride-notifications pool is down - zero workers. Everything else is "
                            + "healthy.",
                    Map.of(), List.of(),
                    List.of("dispatch keeps working: validate, quote and match all run fine",
                            "pushOffer is SCHEDULED and then just sits there - nothing is dropped, "
                                    + "nothing is retried into a dead dependency",
                            "at 5s the scheduleToStartTimeout fires: TIMEOUT_TYPE_SCHEDULE_TO_START",
                            "that timeout means 'not enough workers' and NOTHING ELSE - it is the "
                                    + "one you page on",
                            "workers pull, so 'the pool is down' just means 'nobody is polling'"),
                    false, true));

    // ============================================================================================

    public static void main(String[] args) {
        System.setProperty("gateway.ledger", "target/demo-gateway-ledger.txt");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        // The SDK logs every retryable activity failure at WARN with a full stack trace, which
        // buries the trace. Raise it with -Dorg.slf4j.simpleLogger.log.io.temporal=info.
        System.setProperty("org.slf4j.simpleLogger.log.io.temporal", "error");

        List<Scenario> toRun = args.length == 0 || args[0].equals("all")
                ? SCENARIOS
                : SCENARIOS.stream().filter(s -> s.name().equals(args[0])).toList();

        if (toRun.isEmpty()) {
            System.out.println("unknown scenario. try: all, "
                    + SCENARIOS.stream().map(Scenario::name).toList());
            System.exit(1);
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
        PaymentGateway psp = new PaymentGateway();
        DriverFleet fleet = new DriverFleet();

        TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance();
        try {
            String city = "sfo";
            String dispatchQueue = TaskQueues.dispatch(city);

            // THREE worker pools, three task queues, one process here only because it is a demo.
            // In production these are three deployments with three scaling policies.
            Worker dispatchWorker = env.newWorker(dispatchQueue);
            dispatchWorker.registerWorkflowImplementationTypes(RideWorkflowImpl.class);
            dispatchWorker.registerActivitiesImplementations(new RideActivitiesImpl(fleet, psp));

            if (!s.noPushWorker()) {
                Worker notifyWorker = env.newWorker(TaskQueues.NOTIFICATIONS);
                notifyWorker.registerActivitiesImplementations(new RideActivitiesImpl(fleet, psp));
            }

            Worker paymentsWorker = env.newWorker(TaskQueues.PAYMENTS);
            paymentsWorker.registerActivitiesImplementations(new RideActivitiesImpl(fleet, psp));

            env.start();

            if (s.noPushWorker()) {
                System.out.println("  [ops] ZERO workers are polling '" + TaskQueues.NOTIFICATIONS
                        + "'\n");
            }

            WorkflowClient client = env.getWorkflowClient();
            String rideId = "RIDE-" + s.name().toUpperCase();
            RideRequest request = new RideRequest(
                    rideId, "R-4471", city,
                    s.farPickup()
                            ? new Location("Half Moon Bay", 37.4636, -122.4286)
                            : new Location("Ferry Building", 37.7955, -122.3937),
                    new Location("SFO Terminal 2", 37.6213, -122.3790),
                    "UberX");

            RideWorkflow workflow = client.newWorkflowStub(
                    RideWorkflow.class,
                    WorkflowOptions.newBuilder()
                            // THE routing decision, made by the caller, once, at start time.
                            .setTaskQueue(dispatchQueue)
                            // workflowId == rideId: the server itself dedupes a double tap.
                            .setWorkflowId(rideId)
                            .setWorkflowExecutionTimeout(Duration.ofHours(6))
                            .build());

            for (Act a : s.script()) scheduleAct(env, client, fleet, rideId, a);

            long wall = System.currentTimeMillis();
            WorkflowClient.start(workflow::requestRide, request);

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
            System.out.println("  result                  " + result);
            System.out.println("  virtual time elapsed    " + human(virtual)
                    + "   (real server-side timers: the ride really would have taken this long)");
            System.out.println("  wall clock              " + wall + "ms");
            System.out.println("  push attempts           " + fleet.pushAttempts()
                    + "   (activity executions)");
            System.out.println("  offer cards shown       " + fleet.cardsShown()
                    + (fleet.duplicatePushesSuppressed() > 0
                        ? "   <-- " + fleet.duplicatePushesSuppressed()
                          + " duplicate push(es) suppressed by offerId"
                        : ""));
            System.out.println("  real money movements    " + psp.realMovements()
                    + "   (holds + captures + fees that actually happened)");
        } finally {
            s.props().keySet().forEach(System::clearProperty);
            env.close();
        }
        System.out.println();
    }

    /**
     * Turns a scripted tap into a callback on the virtual clock. registerDelayedCallback runs
     * on the test service's own thread while time skipping is held, which is why these are
     * plain synchronous calls and not threads - the virtual clock cannot run away mid-tap.
     */
    static void scheduleAct(TestWorkflowEnvironment env, WorkflowClient client,
                            DriverFleet fleet, String rideId, Act a) {
        env.registerDelayedCallback(a.at(), () -> {
            RideWorkflow stub = client.newWorkflowStub(RideWorkflow.class, rideId);
            long t = a.at().getSeconds();
            try {
                act(stub, fleet, a, t);
            } catch (io.temporal.client.WorkflowNotFoundException
                     | io.temporal.client.WorkflowUpdateException e) {
                // The ride already ended before this scripted tap. Real drivers do this too.
                System.out.println("  [t+" + t + "s] " + a.kind() + " ignored: ride already closed");
            }
        });
    }

    static void act(RideWorkflow stub, DriverFleet fleet, Act a, long t) {
            switch (a.kind()) {
                case "accept" -> {
                    String offerId = fleet.openOfferFor(a.driver());
                    if (offerId == null) {
                        System.out.println("  [t+" + t + "s] " + a.driver()
                                + " has no open offer to accept");
                        return;
                    }
                    AcceptResult res = stub.acceptOffer(DriverResponse.of(offerId, a.driver()));
                    System.out.println("  [t+" + t + "s] " + a.driver() + " taps ACCEPT -> "
                            + res.outcome() + "  (" + res.note() + ")");
                }
                case "accept-stale" -> {
                    String offerId = fleet.lastOfferTo(a.driver());
                    AcceptResult res = stub.acceptOffer(DriverResponse.of(offerId, a.driver()));
                    System.out.println("  [t+" + t + "s] " + a.driver()
                            + " taps ACCEPT on an old card -> " + res.outcome()
                            + "  (" + res.note() + ")");
                }
                case "race" -> {
                    String o1 = fleet.openOfferFor(a.driver());
                    String o2 = fleet.openOfferFor(a.arg());
                    System.out.println("  [t+" + t + "s] " + a.driver() + " and " + a.arg()
                            + " both tap ACCEPT");
                    AcceptResult r1 = stub.acceptOffer(DriverResponse.of(o1, a.driver()));
                    AcceptResult r2 = stub.acceptOffer(DriverResponse.of(o2, a.arg()));
                    System.out.println("            " + a.driver() + " -> " + r1.outcome()
                            + "  (" + r1.note() + ")");
                    System.out.println("            " + a.arg() + " -> " + r2.outcome()
                            + "  (" + r2.note() + ")");
                }
                case "decline" -> {
                    String offerId = fleet.openOfferFor(a.driver());
                    stub.declineOffer(new DriverResponse(offerId, a.driver(), "too far"));
                    System.out.println("  [t+" + t + "s] " + a.driver() + " taps DECLINE");
                }
                case "steal" -> fleet.stealDriver(a.driver(), a.arg());
                case "cancel" -> {
                    stub.riderCancel(a.arg());
                    System.out.println("  [t+" + t + "s] rider CANCELS: " + a.arg());
                }
                case "arrive" -> {
                    stub.driverArrived();
                    System.out.println("  [t+" + t + "s] " + a.driver() + " arrives at pickup");
                }
                case "start" -> {
                    stub.tripStarted();
                    System.out.println("  [t+" + t + "s] trip starts");
                }
                case "end" -> {
                    stub.tripEnded(Long.parseLong(a.arg()));
                    System.out.println("  [t+" + t + "s] trip ends, metered fare "
                            + PaymentGateway.money(Long.parseLong(a.arg())));
                }
                default -> throw new IllegalStateException("unknown act " + a.kind());
            }
    }

    /**
     * The event history, compacted. This IS the execution: it is what a replay reads, what an
     * auditor reads, and what `temporal workflow show` prints. Returns the span of the
     * execution taken from the event timestamps.
     */
    static Duration printHistory(TestWorkflowEnvironment env,
                                 io.temporal.api.common.v1.WorkflowExecution execution) {
        var events = env.getWorkflowExecutionHistory(execution).getEvents();
        System.out.println();
        System.out.println("  event history (" + events.size() + " events)");
        List<HistoryEvent> shown = new ArrayList<>();
        for (HistoryEvent e : events) {
            String type = e.getEventType().name().replace("EVENT_TYPE_", "");
            // Workflow task scheduled/started/completed triples are 60% of any history and say
            // nothing about the business process. Fold them away.
            if (type.startsWith("WORKFLOW_TASK_")) continue;
            shown.add(e);
        }
        for (HistoryEvent e : shown) {
            System.out.printf("    %3d  %-36s %s%n", e.getEventId(),
                    e.getEventType().name().replace("EVENT_TYPE_", ""), detail(e));
        }
        System.out.println("    (" + (events.size() - shown.size())
                + " WORKFLOW_TASK_* bookkeeping events hidden)");
        if (events.isEmpty()) return Duration.ZERO;
        long from = events.get(0).getEventTime().getSeconds();
        long to = events.get(events.size() - 1).getEventTime().getSeconds();
        return Duration.ofSeconds(to - from);
    }

    static String detail(HistoryEvent e) {
        return switch (e.getEventType()) {
            case EVENT_TYPE_ACTIVITY_TASK_SCHEDULED -> {
                var a = e.getActivityTaskScheduledEventAttributes();
                yield a.getActivityType().getName() + "  on queue '" + a.getTaskQueue().getName() + "'";
            }
            case EVENT_TYPE_ACTIVITY_TASK_STARTED ->
                    "attempt " + e.getActivityTaskStartedEventAttributes().getAttempt();
            case EVENT_TYPE_ACTIVITY_TASK_FAILED ->
                    e.getActivityTaskFailedEventAttributes().getFailure().getMessage();
            case EVENT_TYPE_ACTIVITY_TASK_TIMED_OUT -> {
                var f = e.getActivityTaskTimedOutEventAttributes().getFailure();
                yield f.getTimeoutFailureInfo().getTimeoutType()
                        + "   <- nobody was polling that queue";
            }
            case EVENT_TYPE_TIMER_STARTED -> "fires in " + human(Duration.ofSeconds(
                    e.getTimerStartedEventAttributes().getStartToFireTimeout().getSeconds()));
            case EVENT_TYPE_WORKFLOW_EXECUTION_SIGNALED ->
                    "signal '" + e.getWorkflowExecutionSignaledEventAttributes().getSignalName() + "'";
            case EVENT_TYPE_WORKFLOW_EXECUTION_UPDATE_ACCEPTED ->
                    "update accepted   <- durable BEFORE the handler ran";
            case EVENT_TYPE_WORKFLOW_EXECUTION_UPDATE_COMPLETED -> "update answered";
            case EVENT_TYPE_MARKER_RECORDED ->
                    "marker '" + e.getMarkerRecordedEventAttributes().getMarkerName()
                            + "'   <- the getVersion record";
            case EVENT_TYPE_WORKFLOW_EXECUTION_FAILED ->
                    e.getWorkflowExecutionFailedEventAttributes().getFailure().getMessage();
            case EVENT_TYPE_WORKFLOW_EXECUTION_COMPLETED -> "done";
            default -> "";
        };
    }

    static String human(Duration d) {
        if (d.toDays() > 0) return d.toDays() + "d " + d.toHoursPart() + "h";
        if (d.toHours() > 0) return d.toHours() + "h " + d.toMinutesPart() + "m";
        if (d.toMinutes() > 0) return d.toMinutes() + "m " + d.toSecondsPart() + "s";
        return d.toSeconds() + "s";
    }

    static void line(String c) {
        System.out.println(c.repeat(84));
    }
}
