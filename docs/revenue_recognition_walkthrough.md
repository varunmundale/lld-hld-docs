# Revenue Recognition — worked example

Companion to [Revenue Recognition — End to End](revenue_recognition_pipeline.md). **One invoice**,
traced through three artefacts, so the shape of each table is concrete before the Kafka/Iceberg/Pinot
argument starts.

- **Flow 1 — the clean subscription.** One invoice, three obligations, deferred and recognised.
- **Flow 2 — the mutation.** *(next)* Void and refund, including one landing on a closed period.

| Stage | Audience | Grain | What it is |
|---|---|---|---|
| **1. Journal** | internal | one row per object **version**, plus the entries it generates | CDC log off the oplog — internal account names, online stream + hourly offline partitions |
| **2. Ledger** | **user-facing** | one row per debit/credit **pair** | the [debits and credits report](https://docs.stripe.com/revenue-recognition/reports/debits-and-credits) — mapped to the customer's GL codes, exportable to their ERP |
| **3. Revenue Recognition Entries** | internal serving | one row per **(invoice, period)** | denormalized *and aggregated* — movements, not balances. The user-facing [period summary](https://docs.stripe.com/revenue-recognition/reports/period-summary) is this table `GROUP BY` period, nothing more |

Journal and ledger carry the *same* debits and credits. The journal keeps `version`, the CDC `op` and
internal object ids, and is what you replay to rebuild everything else. The ledger drops all of that
and renames the accounts, which is what makes it importable into NetSuite, SAP or QuickBooks.

---

# Flow 1 — one Netflix invoice

Subscription `sub_4471` for customer `cus_88012`. Invoice `in_9001` finalises **2026-01-01** with
three line items:

| Line item | Amount | Service period | Recognition |
|---|---|---|---|
| `il_1` Netflix Premium — Annual *(list 150.00 less `ANNUAL20` 30.00)* | 120.00 | 2026-01-01 → 2026-12-31 | 10.00 × 12 months |
| `il_2` Extra Screens add-on | 36.00 | 2026-01-01 → 2026-06-30 | 6.00 × 6 months |
| `il_3` Activation fee | 24.00 | *none* | 24.00 in full, at finalisation |
| **Subtotal** | **180.00** | | |
| Exclusive sales tax @ 10% | 18.00 | | |
| **Invoice total = accounts receivable** | **198.00** | | |

**Each line item is its own performance obligation** — *"Revenue Recognition treats each invoice line
item as its own performance obligation"* — with its own service period and its own schedule. Three
different recognition patterns on one invoice is what gives the aggregate in stage 3 real work to do.
`il_3` has no service period, so *"Revenue Recognition recognises the full amount on the invoice line
item when the invoice is finalised."*

Only the **180.00** of line-item subtotals ever becomes revenue. Tax is a liability owed to a tax
authority: *"an invoice for 50 USD with an exclusive tax of 5 USD has 50 USD in recognisable revenue
and 5 USD of tax liability."*

## The pools

Every dollar of the invoice sits in exactly one pool, and the model is two flows between them:

```
   Future scheduled billings  ──BOOKED──▶  Deferred revenue  ──RECOGNISED──▶  Revenue
   contracted, not yet billed              billed, not yet earned             earned

              0.00                              0.00                        0.00    before finalisation
              0.00                            140.00                       40.00    end of 2026-01
              0.00                             60.00                      120.00    end of 2026-06
              0.00                              0.00                      180.00    end of 2026
```

**Booked** is the flow into deferred; **recognised** is the flow out of it. Nothing skips a pool, and
the pools always sum to 180.00. Future scheduled billings stays at zero here because the whole
commitment was billed on one invoice — that pool is non-zero only with [revenue
contracts](#which-accounts-back-each-pool), where the commitment is signed before it is billed, which
is why every example in Stripe's own docs shows it as 0.00 too.

---

## Stage 1 — Journal

**Input:** one CDC event.

| ts | journal | v | op | object | payload |
|---|---|---|---|---|---|
| 2026-01-01T00:04:12Z | `J-1001` | 1 | `CREATE` | invoice `in_9001` | finalized · 3 line items · subtotal 180.00 · tax 18.00 · total 198.00 · sub `sub_4471` |

**Output: 23 journal entries**, each a debit account, a credit account and an amount. Seven of them
land in January:

| # | date | trigger | line | debit | credit | amount |
|---|---|---|---|---|---|---|
| 1 | 2026-01-01 | invoice finalised | `il_1` | `AccountsReceivable` | `DeferredRevenue` | 120.00 |
| 2 | 2026-01-01 | invoice finalised | `il_2` | `AccountsReceivable` | `DeferredRevenue` | 36.00 |
| 3 | 2026-01-01 | invoice finalised | `il_3` | `AccountsReceivable` | `DeferredRevenue` | 24.00 |
| 4 | 2026-01-01 | invoice finalised | — | `AccountsReceivable` | `TaxLiability` | 18.00 |
| 5 | 2026-01-01 | no service period → recognise in full | `il_3` | `DeferredRevenue` | `Revenue` | 24.00 |
| 6 | 2026-01-31 | month-end close | `il_1` | `DeferredRevenue` | `Revenue` | 10.00 |
| 7 | 2026-01-31 | month-end close | `il_2` | `DeferredRevenue` | `Revenue` | 6.00 |

The remaining sixteen follow the two schedules:

| period | entries | detail |
|---|---|---|
| 2026-02 … 2026-06 | 2 each | `il_1` 10.00 + `il_2` 6.00 |
| 2026-07 … 2026-12 | 1 each | `il_1` 10.00 — the add-on's service period has ended |

Three facts about this table do the work later:

- **Entries 1–5 come from the event; 6–7 come from the close.** No upstream message says *"recognise
  10.00 in March"* — the schedule is derived from each line item's service window and re-derived on
  every replay. That is what makes every downstream report a pure function of the journal.
- **Finalisation is four entries, not one.** One per line item, because each is a separate
  performance obligation, plus one for tax — the receivable is the full 198.00 billed, but only
  180.00 of it is recognisable.
- **The same account is hit repeatedly inside one period.** `DeferredRevenue` appears in six of
  January's seven entries — credited three times, debited three times. Nothing folds them here; the
  journal keeps every entry separate because each traces to a different line item.

> Stripe amortises **by the second** (its own examples use whole days — a 3-month US$90 subscription
> recognises 31 / 28 / 31). Equal months are used here for legibility; on a real 365-day service
> January would be 120 × 31 ÷ 365 = **10.19**.

## Stage 2 — Ledger

**Input:** the 23 journal entries. **Output:** the same 23 rows, with internal account names replaced
by the customer's GL accounts and an accounting period stamped on each.

The mapping is configured once:

| internal account | their GL | their account name |
|---|---|---|
| `AccountsReceivable` | `1200` | Accounts Receivable — Trade |
| `DeferredRevenue` | `2400` | Deferred Revenue — Subscriptions |
| `TaxLiability` | `2200` | Sales Tax Payable |
| `Revenue` | `4000` | Subscription Revenue |

January 2026 in full — seven rows, one per entry, still unfolded:

| Accounting period | Open accounting period | Currency | Debit | Credit | Amount | Debit GL | Credit GL |
|---|---|---|---|---|---|---|---|
| 2026-01 | *null* | usd | Accounts Receivable — Trade | Deferred Revenue — Subscriptions | 120.00 | `1200` | `2400` |
| 2026-01 | *null* | usd | Accounts Receivable — Trade | Deferred Revenue — Subscriptions | 36.00 | `1200` | `2400` |
| 2026-01 | *null* | usd | Accounts Receivable — Trade | Deferred Revenue — Subscriptions | 24.00 | `1200` | `2400` |
| 2026-01 | *null* | usd | Accounts Receivable — Trade | Sales Tax Payable | 18.00 | `1200` | `2200` |
| 2026-01 | *null* | usd | Deferred Revenue — Subscriptions | Subscription Revenue | 24.00 | `2400` | `4000` |
| 2026-01 | *null* | usd | Deferred Revenue — Subscriptions | Subscription Revenue | 10.00 | `2400` | `4000` |
| 2026-01 | *null* | usd | Deferred Revenue — Subscriptions | Subscription Revenue | 6.00 | `2400` | `4000` |

Rows 1–3 are the thing to look at: **identical account pair, identical period, three different
amounts.** They are not duplicates and must not be deduplicated — they are three separate performance
obligations that happen to post to the same two accounts. Only the line item tells them apart, which
is why `line_item_id` has to survive into this layer even though no published report displays it.

Later periods are one or two rows each, until the books close:

| Accounting period | Open accounting period | Currency | Debit | Credit | Amount | Debit GL | Credit GL |
|---|---|---|---|---|---|---|---|
| 2026-07 | *null* | usd | Deferred Revenue — Subscriptions | Subscription Revenue | 10.00 | `2400` | `4000` |
| *null* | **2026-08** | usd | Deferred Revenue — Subscriptions | Subscription Revenue | 10.00 | `2400` | `4000` |

The two period columns are mutually exclusive, and they are the ledger's own statement of what is
closed: **`Accounting period` is populated only when that period is closed; `Open accounting period`
only while it is still open for edits.** With books closed through July, the August row is the only
one still mutable — the entire subject of flow 2.

### The first fold — into one bucket per account

The customer's ERP does not want seven rows. The export nets every entry in the period **down to one
line per GL account**, and this is where entries from the same invoice first land in the same bucket:

| Journal No. | Journal Date | Account Name | Journal/Description | Debits | Credits | folded from |
|---|---|---|---|---|---|---|
| `SJE-20260131-USD-01` | 01/31/2026 | Accounts Receivable — Trade | Summarised Journal Entry for 01/2026. | 198.00 | | 4 entries |
| `SJE-20260131-USD-01` | 01/31/2026 | Deferred Revenue — Subscriptions | Summarised Journal Entry for 01/2026. | 40.00 | 180.00 | **6 entries** |
| `SJE-20260131-USD-01` | 01/31/2026 | Sales Tax Payable | Summarised Journal Entry for 01/2026. | | 18.00 | 1 entry |
| `SJE-20260131-USD-01` | 01/31/2026 | Subscription Revenue | Summarised Journal Entry for 01/2026. | | 40.00 | 3 entries |
| | | | | **238.00** | **238.00** | 7 entries → 4 lines |

`DeferredRevenue` is the bucket that does the most work: three credits (120 + 36 + 24 = **180.00
booked**) and three debits (24 + 10 + 6 = **40.00 recognised**) land in one account line carrying both
a debit and a credit column — a net movement of **140.00 credit**, from six entries across three
different line items and two different dates.

## Stage 3 — Revenue Recognition Entries

**Input:** the ledger rows. **Output:** one row per `(invoice, accounting period)`. This is the second
fold: January's seven ledger rows — three line items, two triggers, two dates — become **one row**,
whose columns are **movements**, not balances.

| period | invoice | **booked** | **− recognised** | − credits issued | **Δ deferred** | contra | **revenue net** | tax | *entries folded* |
|---|---|---|---|---|---|---|---|---|---|
| 2026-01 | `in_9001` | 180.00 | 40.00 | — | **+140.00** | — | **40.00** | 18.00 | *7* |
| 2026-02 | `in_9001` | — | 16.00 | — | **−16.00** | — | **16.00** | — | *2* |
| … through 2026-06 | `in_9001` | — | 16.00 | — | **−16.00** | — | **16.00** | — | *2* |
| 2026-07 | `in_9001` | — | 10.00 | — | **−10.00** | — | **10.00** | — | *1* |
| … through 2026-12 | `in_9001` | — | 10.00 | — | **−10.00** | — | **10.00** | — | *1* |
| **total** | | **180.00** | **180.00** | **—** | **0.00** | **—** | **180.00** | **18.00** | *23* |

Read January across: **180.00 booked** is entries 1–3 summed into the deferred bucket, **40.00
recognised** is entries 5–7 summed out of it, and **18.00 tax** is entry 4 — three line items with
three unrelated schedules, reduced to one row. February through June recognise 16.00 (`il_1` 10.00 +
`il_2` 6.00); July onward recognise 10.00, because the add-on's service period ended in June. The
measure never changes shape — only the number of entries behind it does.

**What is lost and what is kept.** The line item disappears as a *row*, but nothing is lost as a
*number* — the fold is a sum, not a filter. Kept alongside as **attributes**: `customer_id`,
`subscription_id`, `product`, `plan_interval`, `invoice_status`, `invoice_finalised_period`,
`invoice_total`, `list_amount`, `discount_code`, `discount_amount`, `tax_rate`, `currency`,
`service_start`, `service_end`, `gl_code`. That split is the reason the table exists — `revenue_net`
for any cut is a `SUM` with a `WHERE` and no joins, which is the only shape Pinot serves well.

> **Why the invoice, and not the line item, is the grain.** The line item is where the *schedule*
> lives, so the ledger must keep it. But nothing downstream slices by it: the reports cut by customer,
> product, period and subscription, all of which are invoice-level or coarser. Choosing invoice grain
> collapses 23 ledger rows to 12 even in this tiny example, and roughly an order of magnitude on a
> real merchant — which is what keeps the Pinot segment count sane. Go finer and the aggregate stops
> earning its place; go coarser, to `(customer, period)`, and you can no longer answer *which invoice*
> without going back to the ledger.

**No balance is ever stored.** A balance is a `SUM` over movements up to a period, which is what
makes every report rebuildable from the journal, and what lets a correction be appended to an open
period without rewriting a single closed row. Store balances instead and every late-arriving event
becomes a read-modify-write across history — the thing
[Pinot cannot do](revenue_recognition_pipeline.md#why-not-serve-straight-out-of-pinot).

### The Period Summary is this table, grouped by period

There is no fourth artefact and no further logic. Drop the invoice from the key, sum the movements,
and run the balances as a window over the result:

```sql
SELECT accounting_period,
       SUM(booked)          AS deferred_change_from_new_billings,
       SUM(recognised)      AS less_recognised_revenue,
       SUM(credits_issued)  AS less_credits_issued,
       SUM(revenue_net)     AS net_revenue,
       SUM(SUM(delta_deferred)) OVER (ORDER BY accounting_period) AS deferred_ending
FROM   revenue_recognition_entries
GROUP  BY accounting_period
```

Every line of the published report is one of those columns, relabelled:

| Report section | Report line | Comes from |
|---|---|---|
| Recognised revenue | Revenue from billings this month | `SUM(recognised)` where `invoice_finalised_period = accounting_period` |
| | Recognised revenue previously deferred | `SUM(recognised)` where `invoice_finalised_period < accounting_period` |
| | Discounts, refunds, voids … | `SUM(contra)`, split by contra account |
| | **Net revenue** | `SUM(revenue_net)` |
| Deferred revenue | Starting balance | running `Σ Δ deferred` **before** this period |
| | Deferred change from new billings | `SUM(booked)` |
| | Less recognised revenue | `SUM(recognised)` |
| | Less credits issued | `SUM(credits_issued)` |
| | **Ending balance** | starting + `SUM(Δ deferred)` |
| Future scheduled billings | Starting / **Ending balance** | running `Σ Δ future billings` |

**January 2026** — invoice billed, three obligations start earning:

| Recognised revenue | | Deferred revenue | |
|---|---|---|---|
| Revenue from billings this month | 40.00 | Starting balance | 0.00 |
| Recognised revenue previously deferred | — | Deferred change from new billings | 180.00 |
| Discounts *(contra)* | — | Less recognised revenue | (40.00) |
| | | Less credits issued | — |
| **Net revenue** | **40.00** | **Ending balance** | **140.00** |

**February 2026** — nothing is billed, two obligations keep earning:

| Recognised revenue | | Deferred revenue | |
|---|---|---|---|
| Revenue from billings this month | — | Starting balance | 140.00 |
| **Recognised revenue previously deferred** | **16.00** | Less recognised revenue | (16.00) |
| **Net revenue** | **16.00** | **Ending balance** | **124.00** |

January's revenue is *from billings this month* because the invoice finalised in January; every month
after, it is *previously deferred*. The two lines are one `SUM(recognised)` split by a predicate on
`invoice_finalised_period`, and that is how a reader separates new business from the tail of old
business.

---

## One January, three folds

The same money at every stage, re-grained. Every arrow is an aggregation, never a re-derivation:

| Stage | Rows for 2026-01 | What one row is | Folded by |
|---|---|---|---|
| **Journal** | 1 event + 7 entries | a CDC version, and a debit/credit/amount triple | — |
| **Ledger** | 7 rows | one debit/credit pair, GL-mapped and period-stamped | — |
| *— ERP export* | *4 lines* | *one GL account's net movement for the period* | *account* |
| **Revenue Recognition Entries** | 1 row | one `(invoice, period)`, all movements on it | *invoice × period* |
| *— period summary* | *1 row, 2 sections* | *the above, `GROUP BY` period — a query, not a table* | *period* |

Nothing downstream knows anything the journal did not already say, which is the property that makes
recovery a replay rather than a repair.

## The formulas

Three movements, one per pool — [as Stripe defines them](https://docs.stripe.com/revenue-recognition/reports/period-summary):

```
Δ FutureScheduledBillings  =  NewContractValue  −  BookedRevenue      −  CancelledSchedules

Δ DeferredRevenue          =  BookedRevenue     −  RecognisedRevenue  −  CreditsIssued

NetRevenue                 =  RecognisedRevenue (+ other revenue items)  −  ContraRevenue
```

**`BookedRevenue`** — *"deferred change from new billings this month"* — is the hinge: subtracted
from one pool and added to the next in the same movement. **`RecognisedRevenue`** is the second
hinge. Rearranged:

```
RecognisedRevenue  =  BookedRevenue  −  CreditsIssued  −  Δ DeferredRevenue
```

Because the pools are exhaustive, the movements integrate into a conservation law — with future
billings at zero here, since this invoice *is* the whole commitment:

```
BookedRevenue  =  DeferredRevenue  +  RevenueToDate  +  CreditsIssued

    180.00     =      140.00       +      40.00      +      0.00        end of Jan 2026
    180.00     =       60.00       +     120.00      +      0.00        end of Jun 2026
    180.00     =        0.00       +     180.00      +      0.00        end of Dec 2026
```

Balances are never stored — they are the running sum of the movements:

```
Balance(pool, P)  =  Σ  Δ pool(p)   for all p ≤ P
```

**Where `recognised = booked − discounts − deferred` goes wrong.** The `− deferred` term is right
once read as **Δ deferred** rather than the balance. Discounts are not in this equation at all — they
are one of ten **contra revenue** items in the third formula, so they reduce net revenue without
moving the deferred balance. The term genuinely missing is **credits issued** — *"remaining deferred
revenue removed due to refunds, disputes, voids, uncollectible invoices and credit notes"* — the only
way deferred can leave without being earned. Both are zero in flow 1, which is why it is the control
case.

### Which accounts back each pool

| Pool | Report line | Backing accounts |
|---|---|---|
| Future scheduled billings | starting / ending balance | `UnbilledDeferredRevenue` (liability), `ContractAsset` (asset) — *only used when working with revenue contracts* |
| Deferred revenue | the roll-forward | `DeferredRevenue`, plus `LongTermDeferredRevenue` for service beyond 12 periods |
| Revenue | net revenue | `Revenue`, less contra accounts `Refunds`, `Disputes`, `CreditNotes`, `BadDebt`, `Voids`, `Discounts`, … |

> **Where the discount went, exactly.** It depends on one setting. By default `ANNUAL20` is applied
> before finalisation and simply reduces `il_1`'s transaction price — booked is 120.00 for that line,
> not the 150.00 list, and no discount account is touched. Turn on **Record discounts as contra
> revenue** and Stripe books the gross 150.00 against a `DeferredDiscounts` liability, then amortises
> 2.50/month out of it into the `Discounts` contra-revenue account: *"this account is only used when
> you enable Record discounts as contra revenue in your settings."* Net revenue is unchanged either
> way; what changes is whether the report can show gross revenue and discounts separately.

Flow 2 takes this invoice and breaks it, which is what puts contra revenue and `credits issued` to
work.
