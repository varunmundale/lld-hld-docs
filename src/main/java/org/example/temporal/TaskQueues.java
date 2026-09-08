package org.example.temporal;

import java.util.List;

/**
 * TASK QUEUES.
 *
 * A task queue is just a NAME. It is not a topic you create, not a Kafka partition you size,
 * not a resource you provision. You invent the string, a worker long-polls it, and the server
 * starts handing that worker tasks whose queue name matches. Nothing else exists.
 *
 * What actually happens on a poll:
 *
 *   worker  --PollWorkflowTaskQueue(name="ride-dispatch-sfo")-->  server
 *                                                                 (holds the request open,
 *                                                                  up to ~60s, no task yet)
 *   worker  <----------------- WorkflowTask ----------------      server
 *
 * Three consequences fall out of that arrow pointing the wrong way from what you would expect:
 *
 *  1. NO ADDRESSING. The server never dials a worker. It has no hostname for one, no port, no
 *     health check. So workers can sit behind NAT, in a laptop, in a different cloud, in a
 *     different language - and a deploy is just "stop polling, start polling".
 *
 *  2. BACKPRESSURE IS FREE AND CORRECT. If every worker on a queue is busy or dead, nobody
 *     polls, and tasks simply accumulate server-side. Nothing is dropped and nothing is
 *     retried into a downstream that is already on fire. The queue depth IS your saturation
 *     metric, and scheduleToStartTimeout is the alarm on it: it fires when a task sat in the
 *     queue with nobody to take it, which is the one timeout that always means "capacity
 *     problem", never "the code is slow".
 *
 *  3. THE QUEUE NAME IS YOUR ROUTING AND ISOLATION UNIT. It is the only knob. Two different
 *     names = two independent pools that cannot starve each other, cannot be deployed at the
 *     same time, and cannot share a bad dependency.
 *
 * This example uses that knob three ways:
 *
 *   ride-dispatch-<city>   One workflow queue PER CITY. A ride's workflow lives on the queue
 *                          for its city forever. Matching is latency-critical and regional,
 *                          the fleet cache is regional, and a bad deploy or a supply-service
 *                          outage in SFO leaves NYC completely untouched. Cardinality stays
 *                          in the hundreds, which is fine - task queues are cheap names, not
 *                          heavyweight objects. (Per-RIDER queues would not be: you would get
 *                          millions of mostly-idle queues and pay for the polls.)
 *
 *   ride-payments          Money movement. Separate pool because the PSP is slow (seconds,
 *                          not milliseconds), because the blast radius of a bad payments
 *                          deploy must not include dispatch, and because these workers hold
 *                          credentials that a dispatch worker has no business having.
 *
 *   ride-notifications     Push/SMS fan-out. Enormous volume, trivially cheap per task, high
 *                          failure rate. On its own queue it can be scaled to hundreds of
 *                          workers, rate-limited per worker, and allowed to fall behind
 *                          without a single ride being delayed.
 *
 * The routing is expressed in exactly two places, and nowhere else:
 *
 *   - the CLIENT picks the workflow's queue at start time: WorkflowOptions.setTaskQueue(...)
 *   - the WORKFLOW picks each activity's queue: ActivityOptions.setTaskQueue(...)
 *
 * An activity stub with no task queue set inherits the workflow's queue. That default is the
 * right one most of the time, and RideWorkflowImpl leans on it for the dispatch activities.
 */
public final class TaskQueues {

    /**
     * Per-city workflow queue. The workflow itself cannot choose this - a workflow's queue is
     * fixed by whoever started it - so the derivation lives here and is called by the client.
     */
    public static String dispatch(String city) {
        return "ride-dispatch-" + city.toLowerCase();
    }

    public static final String PAYMENTS = "ride-payments";

    public static final String NOTIFICATIONS = "ride-notifications";

    /** The cities this deployment has dispatch workers for. */
    public static final List<String> CITIES = List.of("sfo", "nyc");

    private TaskQueues() {}
}
