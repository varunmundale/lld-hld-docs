# LeetCode

Everything solved on LeetCode (account `mundale`), pulled 2026-09-22, with the crux and pitfalls of each problem.

| File | What |
|---|---|
| [index.md](index.md) | Quick index — all 198 solved problems by pattern, with attempts (AC/WA/TLE), last AC date, one-line crux, ⚠️ re-drill flags, and the coverage table |
| [notes.md](notes.md) | Quick notes — per problem: crux, pitfalls, traps, edge cases; incorporates the handwritten notebooks (✍️) and what the submission history says you got wrong (🔁). Starts with the cross-cutting "recurring traps" table |
| [data/submissions.tsv](data/submissions.tsv) | Per-problem stats: tags, submission counts by verdict, first/last dates, runtime |
| [data/leetcode_export.json](data/leetcode_export.json) | Raw export — solved list with tags, all 581 submission records, and the latest accepted code for every problem |

## Numbers

- 198 solved: 54 easy · 128 medium · 16 hard. 187 in Java, 11 in C++ (2017-era).
- 581 submissions: 348 AC · 165 WA · 33 RE · 22 TLE · 13 other. 33 problems flagged ⚠️ (≥3 WA, ≥2 TLE, or ≥6 attempts).
- Activity: a 2017 burst (C++), then Apr 2025 → Sep 2025 (LeetCode 75), May 2026 → Aug 2026 (LeetCode 150 + NeetCode 150), last solves 2026-09-22 (Edit Distance, Non-overlapping Intervals).

## How to use

1. Before an interview: read the **Recurring traps** table at the top of `notes.md`.
2. Drill the **⚠️ Re-drill list** at the bottom of `index.md` (sorted by pain) — 2542, 649, 437, 452, 334, 790, 1497, 153, 162, 130 are the top ten.
3. Pattern refresh: pick a section in `index.md`, cover the crux column, recall it, then open the notes entry.

## Sources

- LeetCode GraphQL (`questionList` filtered by AC, `submissionList`, `submissionDetails`) via the browser session.
- Handwritten notebooks: `~/Desktop/leetcode_75.xopp` (16 pp, problems 16–75 of LeetCode 75), `~/Desktop/leetcode_150.xopp` (11 pp, 25 problems), `~/Desktop/neetcode 150.xopp` (20 pp, 36 problems). `~/Desktop/leetcode_notes.xopp` is identical to `leetcode_75.xopp`.

Regenerate: re-run the GraphQL pull in the browser, drop the JSON into `data/`, and rebuild the index from `data/submissions.tsv`.
