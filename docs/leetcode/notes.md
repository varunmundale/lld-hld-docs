# LeetCode — Quick Notes: crux · pitfalls · traps · edge cases

Companion to [index.md](index.md). One entry per solved problem (198), grouped by pattern in the same order as the index. Each entry = the crux in one or two lines, then the pitfalls/edge cases. Sources:

- your latest accepted submission for every problem (what you actually wrote), plus the submission history (WA/TLE/RE counts — `🔁` marks where you burned attempts and why),
- your handwritten notebooks (`✍️` = lifted from `leetcode_75.xopp`, `leetcode_150.xopp`, `neetcode 150.xopp` on the Desktop; `leetcode_notes.xopp` is a byte-identical copy of `leetcode_75.xopp`).

## Recurring traps (from your own WA/TLE history)

These are the mistakes that show up across many problems. Check this list before hitting submit.

| Trap | Where it bit you | Habit |
|---|---|---|
| **int overflow** | 7, 452, 643, 2300, 637, 1894, 437, 374 (mid), 790 (mod) | products/sums of two ints → `long`; `mid = s + (e-s)/2`; comparators use `Integer.compare`/`Long.compare`, never `x - y` |
| **Boxed `Integer` compared with `==`** | 872 | `.equals()` / `.intValue()` on `List<Integer>`/`Map` values |
| **Dangling tail on relinked lists** (cycle or extra nodes) | 82, 86, 206, 328, 143 | after relinking, explicitly `tail.next = null`; when reversing, `head.next = null` first |
| **Visited on dequeue instead of enqueue** → exponential BFS | 1926, 127, 433, 994 | mark visited when you *push* to the queue |
| **Recursion without memo** → TLE | 678, 1143, 1372, 120, 198, 55/45 (O(n²) DP where greedy is O(n)) | write the recurrence, then immediately add the memo table; ask "is there a greedy?" |
| **Wrong greedy sort key on intervals** | 435, 452 | for "max non-overlapping"/"min arrows" sort by **end** |
| **Last group/level not flushed** | 102, 199, 443, 228, 151 | after the loop, push the pending state |
| **All-negative input** | 53, 643 | initialise `best` to the first element/window, not 0 |
| **Boundary cells / out-of-bounds neighbours** | 162, 605, 1926, 130 | write `get(i)` returning a sentinel, or `isValid(x,y)` once and reuse |
| **Edge cases: empty / size 1 / k ≥ n** | 17, 392, 228, 61, 189, 2095, 1493 | `k %= n`, guard `n==1`, empty input returns `[]` not `[""]` |
| **Debug `println` left in** | 128, 135, 143, 1657, 3076, 22, 17 | strip before submitting — cost you 300–800ms on several |
| **Java `%` on negatives** | 1497 | `((x % k) + k) % k` |
| **`>>` vs `>>>`** | 190, 191 | logical shift for bit loops |

## Approaches you noted but haven't submitted yet

- 42 Trapping Rain Water — two-pointer O(1) space.
- 437 Path Sum III — prefix-sum hashmap O(n) (✍️ "todo: optimal approach").
- 55 / 45 Jump Game I/II — greedy O(n) (your DP is O(n²), 1.5 s on 55).
- 239 Sliding Window Maximum — monotonic deque.
- 300 LIS — patience sorting O(n log n).
- 173 BST Iterator — stack of left spine.
- 450 Delete Node in BST — recursive-return version (no parent tracking).
- 130 Surrounded Regions — border-first marking (✍️ "Better approach").
- 4 Median of Two Sorted Arrays, 10 Regex Matching, 149 Max Points on a Line, 567 Permutation in String, 332 Reconstruct Itinerary, 853 Car Fleet, 40 Combination Sum II — in the notebooks (✍️) but **not solved** on LeetCode.


## Arrays & Hashing

### 1. Two Sum

*E · Array, Hash Table · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2017-08-08 · [LC](https://leetcode.com/problems/two-sum/)*

**Crux:** one pass with `map<value,index>`; for each x check `target-x` in map *before* inserting x.
- Your 2017 C++ is O(n²) and has no return on the no-answer path (UB). Re-solve in Java with the hashmap.
- Same element can't be used twice → check map before insert.

### 36. Valid Sudoku

*M · Array, Hash Table, Matrix · 4 sub · 2 AC / 2 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/valid-sudoku/)*

**Crux:** 3 independent checks: each row, each column, each 3×3 box; skip `'.'`.
- 🔁 2 WA: box index math — box `b` starts at `(b/3*3, b%3*3)`; cell `(r,c)` is in box `r/3*3 + c/3`.
- Don't put `'.'` into the set (or you'll false-positive on the second `'.'`) — your `isInt(c) && set.contains(c)` guard does this; cleaner to `continue` on `'.'`.

### 49. Group Anagrams

*M · Array, Hash Table, String, Sorting · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-05-24 · [LC](https://leetcode.com/problems/group-anagrams/)*

**Crux:** canonical key per word (sorted string, or 26-count encoded) → `map<key, list>`.
- Count-encoding must be unambiguous: your `encoding.append((char)i+'a')` actually appends an *int* (e.g. `97`), so key looks like `970971...`. It works because it's deterministic, but always put a separator between counts (`#1#0#2`) — `1,12` vs `11,2` collide otherwise.
- Sorting each word is O(k log k) — fine for interview; counting is O(k).

### 128. Longest Consecutive Sequence

*M · Array, Hash Table, Union-Find · 3 sub · 3 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/longest-consecutive-sequence/)*

**Crux:** put all in a `HashSet`; for each number, expand +1/−1 while present, *removing* as you go (so each element is touched once → O(n)).
- ✍️ *Question poorly worded*: `0 1 1 2` → answer 3 (duplicates collapse). Set handles it.
- ✍️ Sorting is O(n log n) — the interviewer wants O(n). Standard trick: only start from `x` when `x-1` is absent.

### 169. Majority Element

*E · Array, Hash Table, Divide and Conquer, Sorting, Counting, Boyer–Moore Majority Vote Algorithm · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-25 · [LC](https://leetcode.com/problems/majority-element/)*

**Crux:** guaranteed majority → sorted middle element, or Boyer-Moore vote (O(1) space).
- Boyer-Moore: `count==0 → candidate=x`; `count += (x==candidate ? 1 : -1)`.

### 189. Rotate Array

*M · Array, Math, Two Pointers · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-07-20 · [LC](https://leetcode.com/problems/rotate-array/)*

**Crux:** `k %= n`; reverse whole, reverse `[0,k)`, reverse `[k,n)`.
- ✍️ `k` can exceed `n` — normalise first. `k==0` after mod → the reverses are no-ops (`reverse(0,-1)` must be safe: your `while(i<j)` handles it).

### 205. Isomorphic Strings

*E · Hash Table, String · 2 sub · 1 AC / 1 WA / 0 TLE · last AC 2025-09-07 · [LC](https://leetcode.com/problems/isomorphic-strings/)*

**Crux:** need a *bijection*: map s→t **and** t→s. 🔁 1 WA — the one-direction map passes `"ab"→"aa"` incorrectly.
- Check both `charMap.get(a)!=b` and that `b` isn't already mapped from another char.

### 219. Contains Duplicate II

*E · Array, Hash Table, Sliding Window · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-07-02 · [LC](https://leetcode.com/problems/contains-duplicate-ii/)*

**Crux:** `map<value, lastIndex>`; on repeat check `i - last ≤ k`, then overwrite with the newer index.
- ✍️ Always keep the *latest* index — the closest previous occurrence is the only one that can satisfy the bound.

### 228. Summary Ranges

*E · Array · 2 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-07-20 · [LC](https://leetcode.com/problems/summary-ranges/)*

**Crux:** single pass tracking `start`; when `nums[i] != prev+1` close the range.
- ✍️ *single element* → `"a"` not `"a->a"`. Always flush the last range after the loop. Empty input → `[]`.
- Watch `prev+1` overflow at `Integer.MAX_VALUE` (use long or compare `nums[i]-prev==1`).

### 242. Valid Anagram

*E · Hash Table, String, Sorting · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-27 · [LC](https://leetcode.com/problems/valid-anagram/)*

**Crux:** sort both / 26-count array.
- Length mismatch → false early.

### 383. Ransom Note

*E · Hash Table, String, Counting · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-25 · [LC](https://leetcode.com/problems/ransom-note/)*

**Crux:** 26-count of magazine, decrement per ransom char; negative → false.

### 628. Maximum Product of Three Numbers

*E · Array, Math, Sorting · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2025-04-26 · [LC](https://leetcode.com/problems/maximum-product-of-three-numbers/)*

**Crux:** sort; answer is `max(a[n-1]·a[n-2]·a[n-3], a[0]·a[1]·a[n-1])` — two large negatives × largest positive.
- Product of three values up to 1000 fits in int; if bounds are larger use long.

### 1207. Unique Number of Occurrences

*E · Array, Hash Table · 3 sub · 3 AC / 0 WA / 0 TLE · last AC 2026-05-27 · [LC](https://leetcode.com/problems/unique-number-of-occurrences/)*

**Crux:** freq map → put frequencies in a set; any collision → false.

### 1431. Kids With the Greatest Number of Candies

*E · Array · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-24 · [LC](https://leetcode.com/problems/kids-with-the-greatest-number-of-candies/)*

**Crux:** compute `max` once; answer `c + extra >= max` (≥, ties count).

### 1497. Check If Array Pairs Are Divisible by k

*M · Array, Hash Table, Counting · 6 sub · 1 AC / 5 WA / 0 TLE · last AC 2025-05-19 · [LC](https://leetcode.com/problems/check-if-array-pairs-are-divisible-by-k/)*

**Crux:** normalise remainders `r = ((x % k) + k) % k` (Java `%` is negative for negatives). Then `cnt[r]` must equal `cnt[k-r]`; `r==0` and `r==k/2` (k even) must have *even* counts.
- 🔁 5 WA — this is a trap problem: negative modulo, and the `2r==k` self-pair case.
- Use `Integer.equals` when comparing map values.

### 1657. Determine if Two Strings Are Close

*M · Hash Table, String, Sorting, Counting · 5 sub · 2 AC / 3 WA / 0 TLE · last AC 2026-05-28 · [LC](https://leetcode.com/problems/determine-if-two-strings-are-close/)*

**Crux:** two operations allow: (1) same set of characters and (2) same *multiset of frequencies*. Compare `set(chars)` and sorted frequency lists.
- 🔁 3 WA. Traps: `"a"` vs `"aa"` (freqs differ); `"uau"` vs `"ssx"` (char sets differ even though freq multisets match).
- ✍️ *how many characters occurred 1 time* — your notebook's `map<int,int>` of freq→count is exactly the right comparator.
- Compare `Integer` values with `.equals`/`intValue()`, not `==` (boxed).

### 2215. Find the Difference of Two Arrays

*E · Array, Hash Table · 3 sub · 3 AC / 0 WA / 0 TLE · last AC 2026-05-21 · [LC](https://leetcode.com/problems/find-the-difference-of-two-arrays/)*

**Crux:** two sets; elements in one not in the other; output distinct.

### 2352. Equal Row and Column Pairs

*M · Array, Hash Table, Matrix, Simulation · 3 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-21 · [LC](https://leetcode.com/problems/equal-row-and-column-pairs/)*

**Crux:** hash every row (`Arrays.toString` / string join) into `map<key,count>`, then for each column build the same key and add its count.
- Your sum-hash + verify works but is O(n³) worst case when many rows share a sum; string key is O(n²).

### 3046. Split the Array

*E · Array, Hash Table, Counting · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-05-22 · [LC](https://leetcode.com/problems/split-the-array/)*

**Crux:** possible iff no value appears >2 times (each half can hold one copy) and n is even.

### 3076. Shortest Uncommon Substring in an Array

*M · Array, Hash Table, String, Trie · 4 sub · 1 AC / 2 WA / 0 TLE · last AC 2025-02-09 · [LC](https://leetcode.com/problems/shortest-uncommon-substring-in-an-array/)*

**Crux:** enumerate all substrings of all strings → `map<substr, set<ownerIdx>>`; for each string pick the substring whose owner-set is `{i}` only, shortest first, lexicographically smallest on tie.
- Empty string if none.
- Constraints are tiny (n,len ≤ 20) so O(n·L²) substrings is fine; 873ms because of `println` and `HashSet` churn — remove debug prints before submitting.


## Prefix Sum

### 238. Product of Array Except Self

*M · Array, Prefix Sum · 3 sub · 3 AC / 0 WA / 0 TLE · last AC 2026-05-24 · [LC](https://leetcode.com/problems/product-of-array-except-self/)*

**Crux:** `prefix[i-1] * suffix[i+1]`; no division. ✍️ *keep prefix & suffix product*.
- O(1) extra space: write prefix into result, then sweep from right with a running suffix.
- Zeros are handled naturally (division approach breaks on them).

### 724. Find Pivot Index

*E · Array, Prefix Sum · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-24 · [LC](https://leetcode.com/problems/find-pivot-index/)*

**Crux:** total sum; running left sum; pivot when `left == total - left - nums[i]`.
- ✍️ *Σleft == Σright, keep 2 sum arrays* — your version; single-pass with total is O(1) space.
- Return the *leftmost* pivot; pivot at index 0 has left sum 0.

### 1732. Find the Highest Altitude

*E · Array, Prefix Sum · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-28 · [LC](https://leetcode.com/problems/find-the-highest-altitude/)*

**Crux:** running sum, track max — starting altitude 0 counts (`max` initialised to 0).

### 1894. Find the Student that Will Replace the Chalk

*M · Array, Binary Search, Simulation, Prefix Sum · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-05-19 · [LC](https://leetcode.com/problems/find-the-student-that-will-replace-the-chalk/)*

**Crux:** `k %= sum(chalk)` then walk the prefix until `sum > k`.
- Use **long** for the total (`1e5 × 1e5` overflows int). Your first loop returns early when `k < total`, which incidentally avoids overflow on the sum — but compute the total in long anyway.


## Two Pointers

### 11. Container With Most Water

*M · Array, Two Pointers, Greedy · 4 sub · 4 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/container-with-most-water/)*

**Crux:** two pointers at the ends; area bounded by the *shorter* line, so move the shorter pointer inward (moving the taller can never help).
- ✍️ *any subarray to the right of the short line is waste since height is capped* — that's the proof.
- Compute area before moving.

### 15. 3Sum

*M · Array, Two Pointers, Sorting · 3 sub · 2 AC / 1 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/3sum/)*

**Crux:** sort; fix `i`, two-pointer `j,k` for `-nums[i]`.
- Dedup properly: skip `i` if `nums[i]==nums[i-1]`; after a hit advance `j` while equal and `k` while equal. Your `stream().distinct()` on results works but costs 607ms — do it in the loop.
- Early exit when `nums[i] > 0`.
- 🔁 1 WA — duplicates.

### 26. Remove Duplicates from Sorted Array

*E · Array, Two Pointers · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-06-22 · [LC](https://leetcode.com/problems/remove-duplicates-from-sorted-array/)*

**Crux:** write-index `w`; `if nums[i] != nums[w-1] nums[w++]=nums[i]`.
- ✍️ *keep state, 2 indices; nums[distinctIdx++] = nums[i]*. Your sentinel `-101` relies on constraints (values ≥ −100) — compare to `nums[w-1]` instead to avoid a magic number.

### 42. Trapping Rain Water

*H · Array, Two Pointers, Dynamic Programming, Stack, Monotonic Stack · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/trapping-rain-water/)*

**Crux:** water at `i` = `min(maxLeft[i], maxRight[i]) - h[i]`, clamped at 0. Two prefix arrays, or the two-pointer version (move the side with the smaller max).
- ✍️ *be careful of −ve* — clamp. Prefix arrays exclude the current bar (yours do: maxLeft[i] is max of `[0,i)`), which is fine because `min(maxL,maxR) - h` then ≤ 0 for the tallest bar.
- ✍️ *Better approach: two pointers* — O(1) space; you noted it, haven't submitted it.

### 125. Valid Palindrome

*E · Two Pointers, String · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-06-24 · [LC](https://leetcode.com/problems/valid-palindrome/)*

**Crux:** two pointers skipping non-alphanumerics; lowercase first.
- ✍️ *skip non-alpha* — remember digits count as alphanumeric (`"0P"` → false).
- Use `Character.isLetterOrDigit`; your manual range check is fine after `toLowerCase`.

### 151. Reverse Words in a String

*M · Two Pointers, String · 4 sub · 4 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/reverse-words-in-a-string/)*

**Crux:** split on runs of whitespace, reverse, join with single space.
- Leading/trailing/multiple spaces must collapse — `s.trim().split("\\s+")` does it. Your hand tokenizer works but is the hard way.
- ✍️ *StringBuilder, iterate reverse* — avoid `result +=` in a loop.

### 283. Move Zeroes

*E · Array, Two Pointers · 3 sub · 2 AC / 1 WA / 0 TLE · last AC 2026-05-27 · [LC](https://leetcode.com/problems/move-zeroes/)*

**Crux:** write-index for non-zeros, then fill the rest with 0. 🔁 1 WA.
- ✍️ *count non-0s, 1st iteration compact, 2nd iteration fill* — two passes, in place, stable order.
- Swap version (`swap(nums[w++], nums[i])`) is one pass.

### 345. Reverse Vowels of a String

*E · Two Pointers, String · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-27 · [LC](https://leetcode.com/problems/reverse-vowels-of-a-string/)*

**Crux:** l/r pointers, swap when both are vowels, else advance the non-vowel side.
- Include **uppercase** vowels (`AEIOU`) — common WA.

### 392. Is Subsequence

*E · Two Pointers, String, Dynamic Programming · 4 sub · 3 AC / 1 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/is-subsequence/)*

**Crux:** pointer `j` into `s`; advance on match; done when `j==s.length()`.
- Empty `s` → true (guard before indexing `s.charAt(0)`). 🔁 1 WA.
- Follow-up (many `s` against one `t`): precompute next-occurrence table per char.

### 443. String Compression

*M · Two Pointers, String · 3 sub · 2 AC / 1 WA / 0 TLE · last AC 2026-05-28 · [LC](https://leetcode.com/problems/string-compression/)*

**Crux:** write-index; on state change write the char then the count's *digits* (count can be ≥10 → multiple chars); count of 1 writes nothing. Flush the last group after the loop.
- ✍️ *weird impl → better: keep idx, push compressed repr in the same array* — your final `encode` is this. The earlier "fill with blanks then compress" approach is the messy one (`a12` ambiguity).
- 🔁 1 WA — last group not flushed / multi-digit count.

### 844. Backspace String Compare

*E · Two Pointers, String, Stack, Simulation · 2 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-04-27 · [LC](https://leetcode.com/problems/backspace-string-compare/)*

**Crux:** build with a stack (pop on `#`), compare. O(1) space: walk both from the end counting skips.
- Your `getProcessed` pushes `#` then pops twice — works, but clearer: `if c=='#' { if(!empty) pop } else push`.
- `#` on empty stack must be a no-op.

### 1679. Max Number of K-Sum Pairs

*M · Array, Hash Table, Two Pointers, Sorting · 4 sub · 3 AC / 0 WA / 1 TLE · last AC 2026-05-21 · [LC](https://leetcode.com/problems/max-number-of-k-sum-pairs/)*

**Crux:** sort; l/r; `sum>k → r--`, `<k → l++`, `==k → pairs++, both move`.
- 🔁 1 TLE — the naive nested loop. Hashmap counting is the other O(n) approach (`cnt[k-x]>0 → pair`).

### 1768. Merge Strings Alternately

*E · Two Pointers, String · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-27 · [LC](https://leetcode.com/problems/merge-strings-alternately/)*

**Crux:** interleave to `min(m,n)`, append remainder of the longer one.
- Use `StringBuilder`; string `+=` in a loop is O(n²).


## Sliding Window

### 3. Longest Substring Without Repeating Characters

*M · Hash Table, String, Sliding Window · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-25 · [LC](https://leetcode.com/problems/longest-substring-without-repeating-characters/)*

**Crux:** set + left pointer; on duplicate shrink from the left *until* the duplicate is gone (`while`, not `if`).
- `map<char,lastIdx>` lets you jump `start = max(start, last+1)` in one step.

### 30. Substring with Concatenation of All Words

*H · Hash Table, String, Sliding Window · 4 sub · 1 AC / 2 WA / 0 TLE · last AC 2025-04-27 · [LC](https://leetcode.com/problems/substring-with-concatenation-of-all-words/)*

**Crux:** all words same length `L`. For each offset `0..L-1`, tokenize `s` into L-chunks and run a sliding window of `k` tokens with a count map; slide when a token exceeds its allowed count; reset on a non-dictionary token.
- 🔁 2 WA + 1 RE. Traps: duplicate words in `words`; the same start index found from different offsets (use a set); tokens beyond `s.length()`.
- Naive `k!` permutations is a dead end — you left that comment in the code.

### 76. Minimum Window Substring

*H · Hash Table, String, Sliding Window · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/minimum-window-substring/)*

**Crux:** `need[c]` counts from `t`; expand right; `matched++` only while `have[c] <= need[c]`; once `matched == t.length()` shrink from the left while still matched, recording the min.
- ✍️ *impl heavy* — the two subtle lines: increment `matched` only when the char is still needed; on shrink decrement `matched` only if `have[c] <= need[c]` **before** removal.
- ✍️ *store indices, not substrings* while scanning; build the answer once.
- Duplicates in `t` (`"AABC"`) are why counts, not a set.
- Empty answer sentinel: window `(0,n)` never beaten → return `""`.

### 209. Minimum Size Subarray Sum

*M · Array, Binary Search, Sliding Window, Prefix Sum · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-26 · [LC](https://leetcode.com/problems/minimum-size-subarray-sum/)*

**Crux:** grow `j` until `sum ≥ target`, then shrink `i` while still `≥ target`, tracking min length.
- Positive numbers only — that's what makes the window monotone. Return 0 if never reached.
- Your `while(true)` with two `break`s works but the standard `for j { sum+=; while(sum>=t){...; sum-=nums[i++]} }` shape is easier to get right.

### 643. Maximum Average Subarray I

*E · Array, Sliding Window · 7 sub · 3 AC / 4 WA / 0 TLE · last AC 2026-05-24 · [LC](https://leetcode.com/problems/maximum-average-subarray-i/)*

**Crux:** fixed window of `k`: seed with the first `k`, then `sum += nums[i] - nums[i-k]`.
- 🔁 4 WA. Traps: initialise `maxSum` to the **first window**, not 0 (all-negative arrays); use **long** for the sum; divide as double at the end (`maxSum/(double)k`), not per step.
- ✍️ *n==k → 1 subarray, else n-k+1 subarrays*.

### 1004. Max Consecutive Ones III

*M · Array, Binary Search, Sliding Window, Prefix Sum · 6 sub · 4 AC / 2 WA / 0 TLE · last AC 2026-05-24 · [LC](https://leetcode.com/problems/max-consecutive-ones-iii/)*

**Crux:** longest window with ≤ k zeros. Keep a queue of zero indices; when a (k+1)-th zero arrives, `left = q.poll()+1`.
- ✍️ *keep track in a queue of size k; if k reached pop front, update start = front+1*.
- Edge `k == 0` → your `q.size()==0` branch resets count; standard two-pointer `while(zeros>k) left++` handles it uniformly.
- 🔁 2 WA — the k=0 / reset path.

### 1456. Maximum Number of Vowels in a Substring of Given Length

*M · String, Sliding Window · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-27 · [LC](https://leetcode.com/problems/maximum-number-of-vowels-in-a-substring-of-given-length/)*

**Crux:** fixed window count; when `i-start+1 > k` drop `s[start]`.
- Lowercase only per constraints; if not, include uppercase.

### 1493. Longest Subarray of 1's After Deleting One Element

*M · Array, Dynamic Programming, Sliding Window · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-27 · [LC](https://leetcode.com/problems/longest-subarray-of-1s-after-deleting-one-element/)*

**Crux:** window with at most one zero; answer is `window length - 1` (you *must* delete one). All ones → `n-1`.
- ✍️ *keep track of previous zero; on a new zero start = prevZero+1*. Equivalent: `dp` of ones-left / ones-right around each zero.
- Edge: single element `[1]` → 0; `[0]` → 0.


## Stack

### 20. Valid Parentheses

*E · String, Stack, Bracket Sequences · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-27 · [LC](https://leetcode.com/problems/valid-parentheses/)*

**Crux:** push opens; on close, stack must be non-empty and top must match; end with empty stack.
- Trap: a close with empty stack → false (your `!stack.empty() &&` guard).

### 71. Simplify Path

*M · String, Stack · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-04-27 · [LC](https://leetcode.com/problems/simplify-path/)*

**Crux:** split on `/`; `..` pops (if non-empty), `.` and empty tokens ignored, else push. Join with `/` and prefix `/`.
- Root `..` is a no-op; result for `"/../"` is `"/"`. `"/a//b"` → `/a/b`. Names like `"..."` are valid dirs.

### 155. Min Stack

*M · Stack, Design · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-08-16 · [LC](https://leetcode.com/problems/min-stack/)*

**Crux:** each entry stores `(value, minSoFar)`; `getMin` = top's min.
- ✍️ *Approach 1 (map counts + heap) is over-engineered* — the pair-stack is O(1) everything.
- Alternative: second stack of mins, push only when `≤` current min (equal must be pushed too).

### 394. Decode String

*M · String, Stack, Recursion · 3 sub · 2 AC / 1 WA / 0 TLE · last AC 2026-05-28 · [LC](https://leetcode.com/problems/decode-string/)*

**Crux:** stack of chars; on `]` pop until `[`, then pop the *multi-digit* number, repeat, push back.
- ✍️ *good for string processing; process when ']'*. Multi-digit counts (`100[ab]`) — pop digits until non-digit. Nested brackets fall out naturally.
- 🔁 1 WA — reversed order when popping (you `Collections.reverse` the popped chars).
- Cleaner: two stacks (counts, strings) + a current `StringBuilder`.

### 735. Asteroid Collision

*M · Array, Stack, Simulation · 7 sub · 3 AC / 4 WA / 0 TLE · last AC 2026-05-28 · [LC](https://leetcode.com/problems/asteroid-collision/)*

**Crux:** stack; collision only when `top > 0 && cur < 0`. Loop: `|cur| > top` → pop and keep checking; `==` → pop, drop cur; `<` → drop cur. Otherwise push.
- 🔁 4 WA. Traps: cur survives multiple pops then must be pushed; equal sizes destroy *both*; `[-2,-1,1,2]` never collides (same direction / moving apart).
- Your `bothExplode` flag exists because the push-after-loop is awkward; use a boolean `alive` and push if still alive after the while.

### 2390. Removing Stars From a String

*M · String, Stack, Simulation · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-28 · [LC](https://leetcode.com/problems/removing-stars-from-a-string/)*

**Crux:** stack; `*` pops. Build result from the stack in order.
- Guaranteed valid input, so pop on empty won't occur — but say it out loud in an interview.


## Monotonic Stack

### 84. Largest Rectangle in Histogram

*H · Array, Stack, Monotonic Stack, Range Minimum/Maximum Query · 2 sub · 1 AC / 1 WA / 0 TLE · last AC 2026-06-19 · [LC](https://leetcode.com/problems/largest-rectangle-in-histogram/)*

**Crux:** for each bar, width = `nextSmallerRight - prevSmallerLeft - 1`; area = width × height. Two monotonic-stack passes (or one pass popping on `≤`).
- ✍️ *write a function returning −1 / n if no element exists; push indices in increasing order; larger elements to the left are useless.*
- ✍️ *edge case 2 1 2* — equal heights: use strict `<` for "smaller" so equal bars extend through each other (your pop condition `nums[top] < current → stop` keeps equals popped → correct).
- 🔁 1 WA — width formula off-by-one. Your `(i-left+1)h + (right-i+1)h - h` equals `(right-left+1)h`; just compute that.

### 239. Sliding Window Maximum

*H · Array, Queue, Sliding Window, Heap (Priority Queue), Monotonic Queue, Range Minimum/Maximum Query · 2 sub · 1 AC / 0 WA / 1 TLE · last AC 2025-04-26 · [LC](https://leetcode.com/problems/sliding-window-maximum/)*

**Crux:** monotonic deque of indices (decreasing values); pop front when out of window, pop back while `< current`; front is the max. O(n).
- Your `TreeSet<(val,idx)>` with remove is O(n log k) and passed (301ms); 🔁 1 TLE was a heap without removal. Comparator must include index so equal values are distinct entries.

### 739. Daily Temperatures

*M · Array, Stack, Monotonic Stack · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-24 · [LC](https://leetcode.com/problems/daily-temperatures/)*

**Crux:** iterate from the right with a stack of indices; pop while `temp[top] <= temp[i]`; answer = `top - i` or 0. Push `i`.
- ✍️ *all smaller elements to the right are useless* — that's why popping is safe (invariant: stack strictly decreasing from bottom).
- Sentinel `(MAX_VALUE, n)` avoids the empty check but must map to 0.
- Left-to-right variant: pop while `temp[i] > temp[top]` and set `ans[top] = i - top`.

### 901. Online Stock Span

*M · Stack, Design, Monotonic Stack, Data Stream · 5 sub · 2 AC / 3 WA / 0 TLE · last AC 2026-05-24 · [LC](https://leetcode.com/problems/online-stock-span/)*

**Crux:** monotonic stack of `(price, idx)`; pop while `top.price <= price`; span = `idx - top.idx`. Sentinel `(∞, -1)`.
- ✍️ *find first index going left which is > current; all smaller to the left are useless*.
- 🔁 3 WA — `<=` vs `<` (equal prices count in the span) and the sentinel index. Store the running index yourself; the API only gives `price`.


## Binary Search

### 33. Search in Rotated Sorted Array

*M · Array, Binary Search · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-07-06 · [LC](https://leetcode.com/problems/search-in-rotated-sorted-array/)*

**Crux:** at each `mid` one half is sorted. If `nums[start] <= nums[mid]` the left half is sorted → if `target` in `[nums[start], nums[mid]]` go left else right; symmetric for the right half.
- ✍️ *Approach 1 (find pivot, then offset search) is harder; Approach 2 "which part is good" is the one to remember.*
- The `<=` in `nums[start] <= nums[mid]` matters for 2-element arrays. Bounds check must be inclusive.

### 34. Find First and Last Position of Element in Sorted Array

*M · Array, Binary Search · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-06-30 · [LC](https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/)*

**Crux:** two binary searches: on equal, record candidate and keep going left (first) / right (last). ✍️ *keep the candidate and look for better*.
- Missing → `[-1,-1]`.

### 74. Search a 2D Matrix

*M · Array, Binary Search, Matrix · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/search-a-2d-matrix/)*

**Crux:** binary search rows for the *last* row with `row[0] <= target` (keep candidate, go right), then binary search that row. Or treat as one flat sorted array: `mid/cols, mid%cols`.
- ✍️ *first find highest row where element ≥ row[0]*. No candidate row → false.

### 153. Find Minimum in Rotated Sorted Array

*M · Array, Binary Search · 9 sub · 5 AC / 4 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/)*

**Crux:** if `nums[mid] <= nums[end]` the min is at or left of mid → `end = mid-1` (keeping mid as candidate); else `start = mid+1`.
- 🔁 **9 submissions, 4 WA** — the neighbour-based `[mid-1] > [mid] < [mid+1]` approach (✍️ notebook) is fragile at the edges (mid=0, not rotated). Use the `compare to end` invariant instead (✍️ *Better approach*).
- Track `result = min(result, nums[mid])` rather than trying to return at a specific mid.
- Not rotated at all → first element; single element.

### 162. Find Peak Element

*M · Array, Binary Search · 10 sub · 3 AC / 3 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/find-peak-element/)*

**Crux:** compare `nums[mid]` with neighbours treating out-of-bounds as −∞; if left neighbour is bigger go left, else right — you always move toward an incline, so a peak exists there.
- 🔁 **10 submissions, 3 WA, 4 RE** — out-of-bounds at `mid±1`. Your `get(i)` returning `Long.MIN_VALUE` is the fix; simpler: `while(l<r) if(nums[mid] < nums[mid+1]) l=mid+1 else r=mid`.
- ✍️ *find the direction of incline, move accordingly*.

### 374. Guess Number Higher or Lower

*E · Binary Search, Interactive · 4 sub · 2 AC / 0 WA / 2 TLE · last AC 2026-05-27 · [LC](https://leetcode.com/problems/guess-number-higher-or-lower/)*

**Crux:** binary search over `[1,n]`; `guess(mid)` returns −1 when your pick is **higher** than the target (go left).
- 🔁 2 TLE — `(start+end)/2` overflows at `n = 2^31-1`. Use `start + (end-start)/2` (you do now).

### 704. Binary Search

*E · Array, Binary Search · 2 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-08-12 · [LC](https://leetcode.com/problems/binary-search/)*

**Crux:** `while(start<=end)`; `mid=(s+e)/2` is safe up to ~1e9 indices; use `s+(e-s)/2` habitually.

### 875. Koko Eating Bananas

*M · Array, Binary Search · 3 sub · 2 AC / 1 WA / 0 TLE · last AC 2026-05-29 · [LC](https://leetcode.com/problems/koko-eating-bananas/)*

**Crux:** binary search on speed `k ∈ [1, max(piles)]`; feasible if `Σ ceil(p/k) ≤ h`; take the smallest feasible.
- ✍️ *piles[i]/k if divisible else piles[i]/k + 1* — or `(p + k - 1)/k`. Sum in **long**.
- 🔁 1 WA — `h` compare / lower bound of 1.

### 2300. Successful Pairs of Spells and Potions

*M · Array, Two Pointers, Binary Search, Sorting · 2 sub · 1 AC / 1 WA / 0 TLE · last AC 2025-06-24 · [LC](https://leetcode.com/problems/successful-pairs-of-spells-and-potions/)*

**Crux:** sort potions; for each spell binary-search the first potion with `potion * spell >= success`; answer `m - idx`.
- ✍️ *Note s == e is possible* → use `>=`. **Product overflows int** — cast to long (🔁 1 WA).
- Equivalent: find the smallest potion `>= ceil(success/spell)`.


## Linked List

### 2. Add Two Numbers

*M · Linked List, Math, Recursion · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/add-two-numbers/)*

**Crux:** dummy head; loop `while (l1 || l2 || carry)`; treat missing node as 0.
- ✍️ *corner cases: add node if needed (carry at the end), stop when all lists exhausted, keep result in a 3rd list*. Lengths differ.

### 19. Remove Nth Node From End of List

*M · Linked List, Two Pointers · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-27 · [LC](https://leetcode.com/problems/remove-nth-node-from-end-of-list/)*

**Crux:** dummy head; two pointers with gap `n` (fast moves n first) → slow ends at the node before the target. Or count then walk `size-n` from dummy.
- Removing the head is why the dummy exists. `n == size` → delete first.

### 21. Merge Two Sorted Lists

*E · Linked List, Recursion · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-25 · [LC](https://leetcode.com/problems/merge-two-sorted-lists/)*

**Crux:** dummy head; splice the smaller node; append the leftover list.
- You allocate new nodes — fine, but relinking existing nodes (`iter.next = p; p = p.next`) is O(1) space. Use `<=` to keep stability.

### 25. Reverse Nodes in k-Group

*H · Linked List, Recursion · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/reverse-nodes-in-k-group/)*

**Crux:** from `prevTail`, find the k-th node (`tail`); if fewer than k remain, stop (leave as is). Reverse `[head..tail]`, then `prevTail.next = tail`, `head.next = tailNext`, `prevTail = head`, continue from `tailNext`.
- ✍️ *get first k block, reverse, return head & tail, attach head & tail, repeat — draw diagrams*. Save `tailNext` **before** reversing.
- Your `reverseList(head, tail)` stops at `tailNext`, so it doesn't need to null-terminate — good.

### 61. Rotate List

*M · Linked List, Two Pointers · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-25 · [LC](https://leetcode.com/problems/rotate-list/)*

**Crux:** count `n`, link tail→head to make a ring, walk `n - k%n` steps from the tail, new head is `q.next`, cut `q.next = null`.
- `k % n` (k can be huge); `k%n == 0` → unchanged (your loop walks `n` steps → same head). Empty list guard.

### 82. Remove Duplicates from Sorted List II

*M · Linked List, Two Pointers · 3 sub · 2 AC / 1 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/remove-duplicates-from-sorted-list-ii/)*

**Crux:** keep a node only if `val != prev.val && val != next.val`. Dummy pre-head; append kept nodes; **null-terminate the tail** every time (`r.next = null`).
- ✍️ *isLast = q.val != q.next.val (transition); isDuplicate = q.val == ongoingState; ★ r.next = null (imp to mark last node)*. 🔁 1 WA — the dangling tail.
- Your `prevState`/`ongoingState` sentinel uses `-101` from constraints; the `prev/next` compare needs no sentinel.

### 86. Partition List

*M · Linked List, Two Pointers · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-07-02 · [LC](https://leetcode.com/problems/partition-list/)*

**Crux:** two dummy heads `small`/`large`; append each node to one; `small.tail.next = large.head; large.tail.next = null`.
- ✍️ *make sure lq.next = null else cycle — lq.next keeps pointing to the original list*. Stable order is required and preserved.

### 141. Linked List Cycle

*E · Hash Table, Linked List, Two Pointers, Floyd's Cycle Finding Algorithm · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-07-06 · [LC](https://leetcode.com/problems/linked-list-cycle/)*

**Crux:** slow/fast; meet → cycle; fast or fast.next null → none.
- ✍️ *just handle all null pointers*. Start `fast = head.next` or both at head with `do/while` — either works if the null checks are before the move.

### 143. Reorder List

*M · Linked List, Two Pointers, Stack, Recursion · 2 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-06-20 · [LC](https://leetcode.com/problems/reorder-list/)*

**Crux:** count → find the node *before* the middle → reverse the second half → merge alternately using a third pointer `m` (`m.next = p; p.next = q; m = q`).
- ✍️ *for merging preferably use a 3rd pointer m*. Cut the first half (`preMid.next = null`) so the merge terminates; your `reverse()` sets `p.next = null` which does it.
- Odd length: first half is one longer; stop when `q` runs out.
- Debug `println` left in (`premid:`) — remove before submitting.

### 206. Reverse Linked List

*E · Linked List, Recursion · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-29 · [LC](https://leetcode.com/problems/reverse-linked-list/)*

**Crux:** `prev=null, cur=head; while cur { next=cur.next; cur.next=prev; prev=cur; cur=next }; return prev`.
- ✍️ *corner case: who modifies head.next* — your `p.next = null; // very important!` is that. The `prev=null` formulation avoids the special case entirely. Empty list.

### 328. Odd Even Linked List

*M · Linked List · 7 sub · 3 AC / 1 WA / 0 TLE · last AC 2026-05-29 · [LC](https://leetcode.com/problems/odd-even-linked-list/)*

**Crux:** `odd = head, even = head.next, evenHead = even; while (even && even.next) { odd.next = even.next; odd = odd.next; even.next = odd.next; even = even.next }; odd.next = evenHead`.
- 🔁 7 submissions — tracking `lastOddNode` separately (your version) is where it went wrong; in the canonical loop `odd` *is* the last odd node when it exits. ✍️ *if odd 1 3 5 | 2 4; if even 1 3 | 2 4*.
- Length 0/1/2 edges.

### 2095. Delete the Middle Node of a Linked List

*M · Linked List, Two Pointers · 4 sub · 2 AC / 1 WA / 0 TLE · last AC 2026-05-29 · [LC](https://leetcode.com/problems/delete-the-middle-node-of-a-linked-list/)*

**Crux:** count `n`, delete node at index `n/2` (0-based) by walking to `n/2 - 1`. `n == 1` → null. Or slow/fast with `prev`.
- 🔁 1 WA + 1 RE — `n==1` and the off-by-one on where to stop (`middle-1` steps from head).

### 2130. Maximum Twin Sum of a Linked List

*M · Linked List, Two Pointers, Stack · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-29 · [LC](https://leetcode.com/problems/maximum-twin-sum-of-a-linked-list/)*

**Crux:** find middle (n even) → reverse second half → walk both halves summing pairs.
- ✍️ *get count, traverse to middle, reverse, traverse till middle*. Or push first half on a stack while slow/fast runs.


## Trees

### 102. Binary Tree Level Order Traversal

*M · Tree, Breadth-First Search, Binary Tree · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-09-02 · [LC](https://leetcode.com/problems/binary-tree-level-order-traversal/)*

**Crux:** BFS; either process the queue by `size()` per level, or tag nodes with level and flush when it changes — **flush the last level after the loop**.
- Your level-tag version needs the final `result.add(ongoingLevelList)`; the `size()` loop avoids that.

### 103. Binary Tree Zigzag Level Order Traversal

*M · Tree, Breadth-First Search, Binary Tree · 2 sub · 1 AC / 0 WA / 0 TLE · last AC 2017-11-28 · [LC](https://leetcode.com/problems/binary-tree-zigzag-level-order-traversal/)*

**Crux:** level order, then reverse odd levels (or a deque adding at front/back alternately).

### 104. Maximum Depth of Binary Tree

*E · Tree, Depth-First Search, Breadth-First Search, Binary Tree · 3 sub · 3 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/maximum-depth-of-binary-tree/)*

**Crux:** `1 + max(depth(l), depth(r))`; null → 0. ✍️ *global variable for max depth* also fine.

### 105. Construct Binary Tree from Preorder and Inorder Traversal

*M · Array, Hash Table, Divide and Conquer, Tree, Binary Tree · 2 sub · 1 AC / 1 WA / 0 TLE · last AC 2025-08-12 · [LC](https://leetcode.com/problems/construct-binary-tree-from-preorder-and-inorder-traversal/)*

**Crux:** `preorder[0]` is root; find it in inorder (hash map); left subtree size = `idx - inStart`; recurse on the two index ranges.
- 🔁 1 WA — range arithmetic. Right preorder range starts at `preStart + 1 + leftSize`. Base `s > e → null`.
- Values are unique (that's why the map works).

### 110. Balanced Binary Tree

*E · Tree, Depth-First Search, Binary Tree · 5 sub · 1 AC / 3 WA / 0 TLE · last AC 2024-08-04 · [LC](https://leetcode.com/problems/balanced-binary-tree/)*

**Crux:** one DFS returning height, or −1 if any subtree is unbalanced (`|hl - hr| > 1`). O(n).
- 🔁 5 submissions, 3 WA — your accepted version recomputes `height()` inside `isBalanced()` → O(n²) and easy to get wrong. Use the −1 sentinel.

### 112. Path Sum

*E · Tree, Depth-First Search, Breadth-First Search, Binary Tree · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2017-10-05 · [LC](https://leetcode.com/problems/path-sum/)*

**Crux:** DFS with running sum; test **only at a leaf** (`left == null && right == null`).
- Empty tree → false (not "sum == 0").

### 113. Path Sum II

*M · Backtracking, Tree, Depth-First Search, Binary Tree · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2017-10-05 · [LC](https://leetcode.com/problems/path-sum-ii/)*

**Crux:** DFS carrying the path; at a leaf with matching sum, add a *copy*. Backtrack (`removeLast`) if using a shared list.
- Your C++ passes the vector by value (implicit copy per call) — correct but O(n·h) copies.

### 114. Flatten Binary Tree to Linked List

*M · Linked List, Stack, Tree, Depth-First Search, Binary Tree · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-27 · [LC](https://leetcode.com/problems/flatten-binary-tree-to-linked-list/)*

**Crux:** recursive helper returns the tail of the flattened subtree: flatten left & right; if left exists, `leftTail.right = node.right; node.right = node.left; node.left = null`; return `rightTail ?? leftTail`.
- Must be in place, `left` must end up null. O(1)-space Morris-style variant: for each node with a left child, find the left subtree's rightmost node and hang the right subtree there.

### 124. Binary Tree Maximum Path Sum

*H · Dynamic Programming, Tree, Depth-First Search, Binary Tree, DP on Trees · 6 sub · 2 AC / 4 WA / 0 TLE · last AC 2017-11-26 · [LC](https://leetcode.com/problems/binary-tree-maximum-path-sum/)*

**Crux:** DFS returns best *downward* branch from the node, clamped ≥ 0 (`max(0, l)`); global = `max(global, l + node + r)`.
- 🔁 6 submissions, 4 WA (2017): negatives. Your C++ handles it by taking `max3(l+curr, r+curr, curr)` and checking all 4 combos globally; the clamp-at-0 form is shorter and less error-prone. Initialise global to `INT_MIN`, not 0 (all-negative tree).

### 199. Binary Tree Right Side View

*M · Tree, Depth-First Search, Breadth-First Search, Binary Tree · 3 sub · 3 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/binary-tree-right-side-view/)*

**Crux:** BFS; last node of each level. ✍️ *keep a variable per level, keep overwriting*; or DFS right-first, record first node seen at each depth.
- Flush the last level after the loop (your `result.add(rightMostElement)`).

### 226. Invert Binary Tree

*E · Tree, Depth-First Search, Breadth-First Search, Binary Tree · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-08-12 · [LC](https://leetcode.com/problems/invert-binary-tree/)*

**Crux:** swap children, recurse.

### 236. Lowest Common Ancestor of a Binary Tree

*M · Tree, Depth-First Search, Binary Tree, Binary Lifting, Lowest Common Ancestor · 3 sub · 3 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree/)*

**Crux (recursive):** `lca(node)`: if node is null/p/q return it; `l = lca(left), r = lca(right)`; both non-null → node; else the non-null one.
- ✍️ *(i) p & q in different subtrees → found, stop; same subtree → keep iterating on that subtree. (ii) store root-to-node path in stacks, return first matching node* — (ii) is your submitted O(n) two-path version; (i) is the one-pass recursion, know both.
- One of p/q may be the ancestor of the other.

### 437. Path Sum III

*M · Tree, Depth-First Search, Binary Tree · 11 sub · 4 AC / 5 WA / 0 TLE · last AC 2026-05-25 · [LC](https://leetcode.com/problems/path-sum-iii/)*

**Crux (optimal):** prefix-sum along the root path: `map<prefixSum, count>`; at each node `count += map[cur - target]`; increment `map[cur]`, recurse, decrement (backtrack). O(n).
- 🔁 **11 submissions, 5 WA, 2 RE** — the naive "restart from every node" double-counts if you restart *inside* the same DFS (✍️ *3 is traversed from 1 & 2*). Your accepted version collects all nodes first then runs a fresh `traverse(node, 0)` from each → O(n²) but correct.
- ✍️ *todo: optimal approach = prefix sum* — do it. Sums in **long** (values ±1e9). Path must go downward only. Don't forget `map[0] = 1` (path starting at root).

### 543. Diameter of Binary Tree

*E · Tree, Depth-First Search, Binary Tree, DP on Trees · 2 sub · 1 AC / 1 WA / 0 TLE · last AC 2017-11-28 · [LC](https://leetcode.com/problems/diameter-of-binary-tree/)*

**Crux:** DFS returns depth; at each node `global = max(global, l + r)` (edges). Return `1 + max(l,r)`.
- Your C++ counts nodes then `-1`; equivalent. Single node → 0.

### 637. Average of Levels in Binary Tree

*E · Tree, Depth-First Search, Breadth-First Search, Binary Tree · 2 sub · 1 AC / 1 WA / 0 TLE · last AC 2025-07-25 · [LC](https://leetcode.com/problems/average-of-levels-in-binary-tree/)*

**Crux:** per-level `(sum, count)`; sum in **long** (values up to 2^31−1 × many). 🔁 1 WA — overflow.
- DFS with a level map works; BFS by level is simpler.

### 687. Longest Univalue Path

*M · Tree, Depth-First Search, Binary Tree, DP on Trees · 3 sub · 2 AC / 1 WA / 0 TLE · last AC 2017-10-05 · [LC](https://leetcode.com/problems/longest-univalue-path/)*

**Crux:** DFS returns longest same-value arm from the node: `incl = (left && left.val == val) ? 1 + l : 0`; global = `incl + incr`; return `max(incl, incr)`.
- Answer is in edges; empty tree → 0.

### 872. Leaf-Similar Trees

*E · Tree, Depth-First Search, Binary Tree · 6 sub · 4 AC / 2 WA / 0 TLE · last AC 2026-05-25 · [LC](https://leetcode.com/problems/leaf-similar-trees/)*

**Crux:** collect leaf values in DFS order for both, compare lists.
- 🔁 2 WA — comparing `List<Integer>` elements with `==` (boxed identity, breaks beyond 127). Use `.equals` / `list1.equals(list2)`.

### 1161. Maximum Level Sum of a Binary Tree

*M · Tree, Depth-First Search, Breadth-First Search, Binary Tree · 3 sub · 2 AC / 1 WA / 0 TLE · last AC 2026-05-29 · [LC](https://leetcode.com/problems/maximum-level-sum-of-a-binary-tree/)*

**Crux:** level sums; return the **smallest** level index with the max sum (1-indexed). 🔁 1 WA — the tie rule (`>` not `>=`).
- ✍️ *levels ArrayList result; extend if not exists; keep adding*.

### 1372. Longest ZigZag Path in a Binary Tree

*M · Dynamic Programming, Tree, Depth-First Search, Binary Tree, DP on Trees · 4 sub · 2 AC / 0 WA / 2 TLE · last AC 2026-05-24 · [LC](https://leetcode.com/problems/longest-zigzag-path-in-a-binary-tree/)*

**Crux:** one DFS carrying `(lengthIfCameFromLeft, lengthIfCameFromRight)`: going to `left` child gives `(right+1, 0)`... i.e. `dfs(node.left, goLeft=false, len+1)` / `dfs(node.right, goLeft=true, len+1)` and restart at 0 in the other direction. Global max. O(n).
- 🔁 2 TLE — BFS from every node without memo (✍️ *do this for all nodes, save state → optimize using DP*). Your memo on `(node, direction)` fixed it; the single-DFS version needs no memo.
- Answer is edges: `maxPath - 1` in your node-count version; single node → 0.

### 1448. Count Good Nodes in Binary Tree

*M · Tree, Depth-First Search, Breadth-First Search, Binary Tree · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-21 · [LC](https://leetcode.com/problems/count-good-nodes-in-binary-tree/)*

**Crux:** carry `maxSoFar` down; node is good if `val >= maxSoFar` (**≥**, equal counts). Root always good (start with `MIN_VALUE`).


## BST

### 98. Validate Binary Search Tree

*M · Tree, Depth-First Search, Binary Search Tree, Binary Tree · 2 sub · 1 AC / 1 WA / 0 TLE · last AC 2025-07-19 · [LC](https://leetcode.com/problems/validate-binary-search-tree/)*

**Crux:** pass bounds down: `valid(node, lo, hi)` with `lo < val < hi`, using **long** (or null) bounds. Or inorder must be strictly increasing.
- 🔁 1 WA — equal values are invalid (strict `<`), and `Integer.MIN/MAX` values as node values break int bounds. Your (min,max,valid)-per-subtree version avoids the bounds issue.

### 108. Convert Sorted Array to Binary Search Tree

*E · Array, Divide and Conquer, Tree, Binary Search Tree, Binary Tree · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-25 · [LC](https://leetcode.com/problems/convert-sorted-array-to-binary-search-tree/)*

**Crux:** middle element as root, recurse on halves; `s > e → null`.

### 173. Binary Search Tree Iterator

*M · Stack, Tree, Design, Binary Search Tree, Binary Tree, Iterator · 4 sub · 3 AC / 1 WA / 0 TLE · last AC 2025-09-07 · [LC](https://leetcode.com/problems/binary-search-tree-iterator/)*

**Crux:** stack of the left spine: constructor pushes leftmost path; `next()` pops, pushes the popped node's right child's left spine. O(h) memory, amortised O(1).
- 🔁 1 WA — your parent-pointer rebuild works but is far more code (climb while `isRight`). Know the stack version.

### 230. Kth Smallest Element in a BST

*M · Tree, Depth-First Search, Binary Search Tree, Binary Tree · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-06-30 · [LC](https://leetcode.com/problems/kth-smallest-element-in-a-bst/)*

**Crux:** inorder traversal, stop at the k-th. O(h + k). Or precompute subtree sizes and descend (your submission; ✍️ *combine approaches 2 & 3: Map<Node,count>, go left/right adjusting k*).
- ✍️ Approach 1 (iterator with parent pointers) is heavy; Approach 2 (dump inorder to a list) is O(n) space.
- Follow-up "frequent inserts/deletes": store subtree counts in nodes → O(h) per query.

### 235. Lowest Common Ancestor of a Binary Search Tree

*M · Tree, Depth-First Search, Binary Search Tree, Binary Tree, Binary Lifting, Lowest Common Ancestor · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-08-16 · [LC](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-search-tree/)*

**Crux:** walk from root: both smaller → left; both larger → right; else current node is the LCA (split point or one equals node).

### 450. Delete Node in a BST

*M · Tree, Binary Search Tree, Binary Tree · 6 sub · 3 AC / 3 WA / 0 TLE · last AC 2026-05-27 · [LC](https://leetcode.com/problems/delete-node-in-a-bst/)*

**Crux:** recursive: `if key < val → left = delete(left)`, `> → right = delete(right)`; found: 0/1 child → return the other child; 2 children → copy the inorder successor's value (leftmost of right subtree) and delete it from the right subtree.
- 🔁 6 submissions, 3 WA. Your parent-finding version needs a sentinel root (deleting the actual root) and a recursive successor delete; the recursive-return version has none of that bookkeeping.
- ✍️ *replace node with successor, delete the successor; successor = leftmost in right subtree*.

### 700. Search in a Binary Search Tree

*E · Tree, Binary Search Tree, Binary Tree · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-21 · [LC](https://leetcode.com/problems/search-in-a-binary-search-tree/)*

**Crux:** descend by comparison.


## Trie

### 208. Implement Trie (Prefix Tree)

*M · Hash Table, String, Design, Trie · 3 sub · 3 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/implement-trie-prefix-tree/)*

**Crux:** node = `Map<Character, Node>` (or `Node[26]`) + `isEnd`. `search` requires `isEnd`; `startsWith` doesn't.
- ✍️ *insert: keep adding nodes, if last char isLeaf = true*. Empty string edge: `isEnd` on root.

### 212. Word Search II

*H · Array, String, Backtracking, Trie, Matrix · 5 sub · 1 AC / 1 WA / 0 TLE · last AC 2025-02-09 · [LC](https://leetcode.com/problems/word-search-ii/)*

**Crux:** trie of the dictionary; DFS from every cell carrying the current trie node; only descend into children that exist; mark cell visited, unmark on return.
- 🔁 5 submissions, 3 RE; 1200ms. Dedupe by clearing `isEnd` after a word is found (you dedupe with a set at the end — slower). Prune: remove trie leaves after use. Don't re-create `visited` per start cell.
- Words can share prefixes — that's the whole point of the trie over per-word search.

### 1268. Search Suggestions System

*M · Array, String, Binary Search, Trie, Sorting, Heap (Priority Queue) · 7 sub · 2 AC / 3 WA / 0 TLE · last AC 2026-05-22 · [LC](https://leetcode.com/problems/search-suggestions-system/)*

**Crux:** sort products; for each prefix, binary search the first product ≥ prefix and take up to 3 that start with it. Trie + lexicographic DFS limited to 3 (your version) also works.
- 🔁 7 submissions, 3 WA, 2 RE — once a prefix has no match, **all remaining prefixes are empty** (your `for` loop filling empties); the 3-limit must stop the DFS (`result.size()==3 → return`); children must be visited in sorted order (`HashMap` isn't).


## Heap

### 23. Merge k Sorted Lists

*H · Linked List, Divide and Conquer, Heap (Priority Queue), Merge Sort, Tournament Sort · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/merge-k-sorted-lists/)*

**Crux:** PQ of list heads by `val`; poll → append → push `polled.next` if non-null. O(N log k).
- Skip null lists when seeding. ✍️ *keep k pointers* is the O(Nk) naive version; divide-and-conquer pairwise merge is the other O(N log k).

### 215. Kth Largest Element in an Array

*M · Array, Divide and Conquer, Sorting, Heap (Priority Queue), Quickselect · 3 sub · 3 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/kth-largest-element-in-an-array/)*

**Crux:** min-heap of size `k`; push, pop when `> k`; top is the answer. O(n log k). Quickselect is O(n) average.

### 295. Find Median from Data Stream

*H · Two Pointers, Design, Sorting, Heap (Priority Queue), Data Stream · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-08-12 · [LC](https://leetcode.com/problems/find-median-from-data-stream/)*

**Crux:** max-heap `left` (lower half) and min-heap `right` (upper half); keep `|left| - |right| ∈ {0,1}`; median = `left.top` or avg of tops.
- ✍️ **Pitfall: ensure left has only the lower half, not a random half** — route every insert *through* the other heap: push into `right`, then move `right.poll()` to `left` (or vice versa). Your `addNum` does exactly this.
- ✍️ *PriorityQueue is a min-heap* → `Comparator.reverseOrder()` for the left.
- Average with `/2.0`.

### 347. Top K Frequent Elements

*M · Array, Hash Table, Divide and Conquer, Sorting, Heap (Priority Queue), Bucket Sort, Counting, Quickselect · 3 sub · 3 AC / 0 WA / 0 TLE · last AC 2026-08-15 · [LC](https://leetcode.com/problems/top-k-frequent-elements/)*

**Crux:** freq map → min-heap of `(num,freq)` capped at `k` → pop all. O(n log k). Bucket sort by frequency is O(n).
- ✍️ *Approach 1 TreeSet sorted by (freq,num) with delete/insert on update is heavier; build the frequency map first, then adjust the heap* (Approach 2 = what you submitted).
- Comparator must break ties (`num`) so the heap doesn't misbehave on equal freqs — yours does.

### 2336. Smallest Number in Infinite Set

*M · Hash Table, Design, Heap (Priority Queue), Ordered Set · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-21 · [LC](https://leetcode.com/problems/smallest-number-in-infinite-set/)*

**Crux:** `currentSmallest` counter + `TreeSet` of numbers added back that are `< currentSmallest`. `pop` takes the set's first if it's smaller, else the counter.
- `addBack(x)` with `x >= currentSmallest` is a no-op (never popped). TreeSet dedups repeated add-backs.

### 2462. Total Cost to Hire K Workers

*M · Array, Two Pointers, Heap (Priority Queue), Simulation · 5 sub · 2 AC / 3 WA / 0 TLE · last AC 2026-05-28 · [LC](https://leetcode.com/problems/total-cost-to-hire-k-workers/)*

**Crux:** two min-heaps fed from the left and right ends with index pointers `l`, `r`; pop the cheaper (tie → left), refill from that side **only if `l < r`** (no overlap).
- ✍️ *should not overlap; consider sentinel MAX values to avoid conditions*. 🔁 3 WA — overlap/double-add when `2*candidates >= n`, and the tail-loops when one heap runs dry.
- Simplification: when `l > r` just drain whichever heap is non-empty (your trailing `while` loops).
- Total in **long**.

### 2542. Maximum Subsequence Score

*M · Array, Greedy, Sorting, Heap (Priority Queue) · 13 sub · 1 AC / 10 WA / 0 TLE · last AC 2026-05-28 · [LC](https://leetcode.com/problems/maximum-subsequence-score/)*

**Crux:** score = `sum(k of nums1) × min(k of nums2)`. Sort indices by `nums2` **desc**; sweep — each element is the current min; keep the `k` largest `nums1` seen so far in a min-heap with a running sum; when heap size == k, `score = sum × nums2[i]`.
- 🔁 **13 submissions, 10 WA** — your worst. Traps: computing a score before the heap has `k` elements; replacing the heap min only if the new value is larger (you do, but a plain push-then-pop-if->k is simpler and correct); **long** for sum × multiplier; sort descending, not ascending.
- ✍️ *if we sort nums2 in descending order we can follow greedy; keep top-k in a min-heap*. Ignore the "try to ignore 0's" idea — not needed.


## Backtracking

### 17. Letter Combinations of a Phone Number

*M · Hash Table, String, Backtracking · 3 sub · 3 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/letter-combinations-of-a-phone-number/)*

**Crux:** backtrack over digits: for each letter `add → recurse → removeLast`.
- Edge: `digits == ""` must return `[]`, not `[""]` — guard before recursing (your base case alone would emit the empty string).

### 22. Generate Parentheses

*M · String, Dynamic Programming, Backtracking, Bracket Sequences · 3 sub · 1 AC / 2 WA / 0 TLE · last AC 2026-07-21 · [LC](https://leetcode.com/problems/generate-parentheses/)*

**Crux:** backtrack with `(open, close)`: add `(` if `open < n`; add `)` if `close < open`; emit when `open == close == n`.
- ✍️ *at any point diff cannot be −ve; terminating condition leftBrackets < n*. 🔁 2 WA — pruning order (your `diff<0 → return` before the emit check is right; make sure the `)` branch is also guarded by `close < open` instead of relying on the next call to prune).
- Remove the `println` — it's still in the accepted code.

### 39. Combination Sum

*M · Array, Backtracking · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/combination-sum/)*

**Crux:** backtrack from index `i` with remaining target; **reuse allowed** → recurse with the same `i`; move to `i+1` to avoid permutations of the same multiset.
- ✍️ *looks like coin change 2; take = combinations(target - cand[n], n), don't take = combinations(target, n-1); tricky list copies!* Your DP-of-lists version memoises `dp[x][target]` and must deep-copy lists on the way up — correct but the plain backtracking with one shared list + `removeLast` is what interviewers expect.
- Base: `target < 0 → nothing`, `target == 0 → one empty combo`.

### 46. Permutations

*M · Array, Backtracking · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-07-06 · [LC](https://leetcode.com/problems/permutations/)*

**Crux:** swap-based: for `i in fromIdx..n-1`: `swap(fromIdx,i) → permute(fromIdx+1) → swap back`; emit at `fromIdx == n-1` (or `n`).
- ✍️ *note: swap(i,i) with itself is a valid branch*. Copy the list when emitting.

### 51. N-Queens

*H · Array, Backtracking, Algorithm X · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-07-24 · [LC](https://leetcode.com/problems/n-queens/)*

**Crux:** place one queen per column; for row `i` check the row and both diagonals for existing queens; place → recurse col+1 → unplace; emit a **copy** of the board at `col == n`.
- ✍️ *isValidPlacement: check row, diag1, diag2 within bounds 0..n-1; update the global result by making a copy*. O(1) checks with three boolean arrays: `rows[i]`, `diag1[i+j]`, `diag2[i-j+n]`.
- Missing `return` after emitting at `col == n` is harmless here (loop checks `isValid` on a full column) but add it.

### 77. Combinations

*M · Backtracking · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-25 · [LC](https://leetcode.com/problems/combinations/)*

**Crux:** backtrack choosing next number `> last`; emit at size `k`.
- Prune: if remaining numbers can't fill `k`, stop (`i <= n - (k - size) + 1`).

### 78. Subsets

*M · Array, Backtracking, Bit Manipulation · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-08-16 · [LC](https://leetcode.com/problems/subsets/)*

**Crux:** take / don't take at each index; emit at `idx == n`. ✍️ *clear state between the two branches* (your `removeLast` before the don't-take call).
- 2^n subsets; bitmask enumeration is the alternative.

### 140. Word Break II

*H · Array, Hash Table, String, Dynamic Programming, Backtracking, Trie, Memoization · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-02-06 · [LC](https://leetcode.com/problems/word-break-ii/)*

**Crux:** recurse on every dictionary prefix, join with the sentences of the remainder. Memoise by start index if inputs are large.
- Your version has no memo (passed because n ≤ 20). Return `[]` (not `[""]`) when impossible; the `tmp.length()==n` branch handles the last word.

### 216. Combination Sum III

*M · Array, Backtracking · 7 sub · 7 AC / 0 WA / 0 TLE · last AC 2026-05-21 · [LC](https://leetcode.com/problems/combination-sum-iii/)*

**Crux:** backtrack over digits `startIndex..9`, `k` picks, remaining sum; prune when `sum < 0 || k < 0`.
- 7 submissions all AC — re-solved several times; no issues. ✍️ (Combination Sum II note) *sort candidates, skip duplicates at the same depth* — the `TLE` you noted there came from not sorting/pruning.

### 679. 24 Game

*H · Array, Math, Backtracking · 3 sub · 1 AC / 2 WA / 0 TLE · last AC 2025-04-25 · [LC](https://leetcode.com/problems/24-game/)*

**Crux:** pick any 2 numbers, apply `+ − × ÷` (both orders for − and ÷), replace with the result, recurse on the smaller list; success when one number ≈ 24.
- 🔁 2 WA — floating point: compare with `|x - 24| < 1e-6`; skip division by zero; use doubles throughout (`8/(3-8/3)` needs fractions).


## Graphs

### 127. Word Ladder

*H · Hash Table, String, Breadth-First Search, Bidirectional Search · 4 sub · 2 AC / 0 WA / 2 TLE · last AC 2026-08-13 · [LC](https://leetcode.com/problems/word-ladder/)*

**Crux:** BFS over words; neighbours = all one-letter mutations that exist in the dictionary set (`26 × L` per word), not pairwise comparison.
- 🔁 2 TLE — pairwise O(N²L) adjacency. Precomputing adjacency via mutation lookups (your `wordMutations` with `graph.containsKey`) is O(N·L·26).
- `endWord` must be in `wordList` (else 0). Count includes both ends (start at 1). Bidirectional BFS is the follow-up.
- Mark visited on enqueue rather than on poll (you poll-check; works, but enqueues duplicates).

### 130. Surrounded Regions

*M · Array, Depth-First Search, Breadth-First Search, Union-Find, Matrix · 5 sub · 1 AC / 4 WA / 0 TLE · last AC 2026-07-06 · [LC](https://leetcode.com/problems/surrounded-regions/)*

**Crux (✍️ Better approach):** DFS from every border `'O'` and mark reachable cells `#`; then flip all remaining `'O'` → `'X'` and `#` → `'O'`.
- 🔁 5 submissions, 4 WA — the "is this region surrounded?" DFS with a global flag (your accepted version) is fragile: when the DFS meets an already-visited cell of a *non*-surrounded region the flag isn't propagated. Border-first marking has no such state.
- Border cells themselves are never captured.

### 133. Clone Graph

*M · Hash Table, Depth-First Search, Breadth-First Search, Graph Theory · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-26 · [LC](https://leetcode.com/problems/clone-graph/)*

**Crux:** `map<original(or val), copy>`; DFS: create copy, register in map *before* recursing into neighbours (cycles), attach copies.
- Null input → null. Node values are unique 1..100 so keying by `val` works; keying by the node object is more general.

### 200. Number of Islands

*M · Array, Depth-First Search, Breadth-First Search, Union-Find, Matrix · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/number-of-islands/)*

**Crux:** for each `'1'` not visited: DFS/BFS flood fill marking visited, `count++`.
- Bounds check before indexing; 4-directional only. Modifying the grid in place (`'1'→'0'`) avoids the visited array.

### 207. Course Schedule

*M · Depth-First Search, Breadth-First Search, Graph Theory, Topological Sort, Directed Acyclic Graph · 2 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-08-16 · [LC](https://leetcode.com/problems/course-schedule/)*

**Crux:** cycle detection in a directed graph: DFS with `visited` + `onStack` (three colours). Cycle ⇒ impossible. Or Kahn's BFS: processed count == n.
- ✍️ *impossible only in case of cycle; return !isCycle*. Reset `onStack` on exit; keep `visited` to avoid re-exploring.

### 210. Course Schedule II

*M · Depth-First Search, Breadth-First Search, Graph Theory, Topological Sort · 3 sub · 2 AC / 1 WA / 0 TLE · last AC 2025-09-02 · [LC](https://leetcode.com/problems/course-schedule-ii/)*

**Crux:** DFS post-order append gives a valid order when edges point `course → prerequisite` (your build); with `prereq → course` edges you'd reverse. Detect cycle with a recursion stack → return `[]`.
- 🔁 1 WA — edge direction / reversing the result. Decide the direction before coding and say which way the list comes out.
- Kahn's algorithm (in-degree queue) is the safer interview version — order pops out directly.

### 399. Evaluate Division

*M · Array, String, Depth-First Search, Breadth-First Search, Union-Find, Graph Theory, Shortest Path, Bellman–Ford Algorithm, Floyd–Warshall Algorithm · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-27 · [LC](https://leetcode.com/problems/evaluate-division/)*

**Crux:** weighted graph: `a→b = v`, `b→a = 1/v`. Per query DFS/BFS multiplying edge weights; unknown variable or unreachable → −1.
- ✍️ *not commutative; populate both directions*. `a/a` with `a` present → 1.0; `x/x` with `x` unknown → −1.
- Reset `visited` per query (you do). Union-find with weights is the follow-up.

### 433. Minimum Genetic Mutation

*M · Hash Table, String, Breadth-First Search, Bidirectional Search · 4 sub · 3 AC / 0 WA / 0 TLE · last AC 2025-04-14 · [LC](https://leetcode.com/problems/minimum-genetic-mutation/)*

**Crux:** same as Word Ladder with alphabet `ACGT`; BFS, neighbours must be in the bank.
- Return −1 if unreachable; start == end → 0.

### 547. Number of Provinces

*M · Depth-First Search, Breadth-First Search, Union-Find, Graph Theory · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-21 · [LC](https://leetcode.com/problems/number-of-provinces/)*

**Crux:** adjacency matrix → count connected components with DFS/union-find.
- Every node must be in the graph even with no edges (diagonal `isConnected[i][i]==1` guarantees it in your map-based build).

### 684. Redundant Connection

*M · Depth-First Search, Breadth-First Search, Union-Find, Graph Theory · 2 sub · 1 AC / 1 WA / 0 TLE · last AC 2017-10-05 · [LC](https://leetcode.com/problems/redundant-connection/)*

**Crux:** union-find; the first edge whose endpoints already share a root is the answer (last such in input order per problem statement — first found *is* the last-in-input that closes a cycle for a tree+1 edge).
- Union by size + path compression; nodes are 1-indexed.

### 841. Keys and Rooms

*M · Depth-First Search, Breadth-First Search, Graph Theory · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-27 · [LC](https://leetcode.com/problems/keys-and-rooms/)*

**Crux:** DFS from room 0 with visited; all visited?
- ✍️ *keep visited to prevent cycle*.

### 994. Rotting Oranges

*M · Array, Breadth-First Search, Matrix · 5 sub · 2 AC / 3 WA / 0 TLE · last AC 2026-05-22 · [LC](https://leetcode.com/problems/rotting-oranges/)*

**Crux:** multi-source BFS: seed *all* rotten oranges at step 0; propagate to fresh neighbours; answer = max step; if any fresh remains → −1.
- 🔁 3 WA — no fresh oranges at all → 0 (not −1); mark visited/rotten on enqueue to avoid double counting; grid with no rotten and some fresh → −1.

### 1466. Reorder Routes to Make All Paths Lead to the City Zero

*M · Depth-First Search, Breadth-First Search, Graph Theory · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-28 · [LC](https://leetcode.com/problems/reorder-routes-to-make-all-paths-lead-to-the-city-zero/)*

**Crux:** store each edge twice: `a→b` weight 1 (real direction), `b→a` weight 0 (virtual). BFS/DFS from 0; every real edge you traverse *outward* must be flipped → count.
- ✍️ *push bidirectional edges with value −1 for the reverse; BFS for shortest*. It's a tree, so visited suffices.

### 1926. Nearest Exit from Entrance in Maze

*M · Array, Breadth-First Search, Matrix · 8 sub · 3 AC / 1 WA / 4 TLE · last AC 2026-05-27 · [LC](https://leetcode.com/problems/nearest-exit-from-entrance-in-maze/)*

**Crux:** BFS from entrance; exit = any *border* empty cell that is not the entrance; return its distance.
- 🔁 **8 submissions, 4 TLE** — marking visited on *dequeue* lets the same cell be enqueued many times → exponential queue. **Mark visited when enqueuing** (your `addedQueue` array is that fix; you don't need both arrays).
- Your `isExitReached` checks whether a neighbour is out of bounds — equivalent to "current is a border cell"; simpler to test `x==0||x==m-1||y==0||y==n-1` on the dequeued cell.
- Typo in offsets `yOffsets = {1,-1,0,-0}` (−0 == 0, harmless).


## 1-D DP

### 45. Jump Game II

*M · Array, Dynamic Programming, Greedy · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-07-24 · [LC](https://leetcode.com/problems/jump-game-ii/)*

**Crux:** greedy "BFS by levels": track `curEnd` and `farthest`; when `i == curEnd` → `jumps++`, `curEnd = farthest`. O(n).
- ✍️ *minSteps(x) = 1 + min over j of minSteps(x+j); base x == n-1 → 0* is the O(n²) DP (107ms). Don't jump from the last index (loop `i < n-1`).

### 53. Maximum Subarray

*M · Array, Divide and Conquer, Dynamic Programming · 3 sub · 1 AC / 2 WA / 0 TLE · last AC 2025-08-12 · [LC](https://leetcode.com/problems/maximum-subarray/)*

**Crux (Kadane):** `cur = max(x, cur + x); best = max(best, cur)`; start `best = nums[0]`.
- 🔁 2 WA — all-negative arrays. Your accepted code special-cases `allNegative`; the `max(x, cur+x)` form needs no special case because it never resets to 0.

### 55. Jump Game

*M · Array, Dynamic Programming, Greedy · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-07-24 · [LC](https://leetcode.com/problems/jump-game/)*

**Crux:** greedy `maxReach`; if `i > maxReach` → false; `maxReach = max(maxReach, i + nums[i])`. O(n).
- Your memo DP is O(n²) — 1492ms. ✍️ *isReachable(x) = OR over j of isReachable(x+j)* is the DP; know the greedy.

### 70. Climbing Stairs

*E · Math, Dynamic Programming, Memoization · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-29 · [LC](https://leetcode.com/problems/climbing-stairs/)*

**Crux:** `f(n) = f(n-1) + f(n-2)`; base `f(0)=1, f(1)=1`.

### 121. Best Time to Buy and Sell Stock

*E · Array, Dynamic Programming · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-29 · [LC](https://leetcode.com/problems/best-time-to-buy-and-sell-stock/)*

**Crux:** track min price so far; profit = `price - min`. One pass.

### 139. Word Break

*M · Array, Hash Table, String, Dynamic Programming, Trie, Memoization, Brute-Force Search · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-09-11 · [LC](https://leetcode.com/problems/word-break/)*

**Crux:** `dp[i]` = can `s[i:]` be segmented = any `j>i` with `s[i:j] ∈ dict && dp[j]`. Memoise. O(n²·L) with `substring`.
- Optimisation: only try lengths present in the dictionary (max word length bound).

### 198. House Robber

*M · Array, Dynamic Programming · 5 sub · 4 AC / 0 WA / 1 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/house-robber/)*

**Crux:** `rob(i) = max(nums[i] + rob(i-2), rob(i-1))`; base `rob(-1)=rob(-2)=0`.
- ✍️ *it is irrelevant if n-1 robs or not* — that's why a 1-state DP works. 🔁 1 TLE — plain recursion without memo.

### 213. House Robber II

*M · Array, Dynamic Programming · 4 sub · 1 AC / 1 WA / 1 TLE · last AC 2025-04-13 · [LC](https://leetcode.com/problems/house-robber-ii/)*

**Crux:** circular → `max(rob(nums[0..n-2]), rob(nums[1..n-1]))`. `n == 1` → `nums[0]`.
- 🔁 4 submissions — your 3-D DP `(till, isTaken, is0Included)` is correct but heavy; the two-call reduction is the expected answer.

### 300. Longest Increasing Subsequence

*M · Array, Binary Search, Dynamic Programming, Longest Increasing Subsequence · 2 sub · 1 AC / 1 WA / 0 TLE · last AC 2025-09-11 · [LC](https://leetcode.com/problems/longest-increasing-subsequence/)*

**Crux:** `lis[i] = 1 + max(lis[j])` for `j<i, nums[j] < nums[i]` → O(n²). Patience sorting with binary search (`tails` array, replace lower-bound) → O(n log n).
- 🔁 1 WA — answer is `max(lis[i])`, not `lis[n-1]` (you take the max over `dp`). Strictly increasing → `<`, not `<=`.

### 322. Coin Change

*M · Array, Dynamic Programming, Breadth-First Search, Knapsack Problem, Complete Knapsack · 4 sub · 2 AC / 0 WA / 0 TLE · last AC 2024-07-28 · [LC](https://leetcode.com/problems/coin-change/)*

**Crux:** `dp[a] = 1 + min(dp[a - c])` over coins; `dp[0]=0`; INF sentinel; answer `dp[amount]` or −1. Unbounded → coin loop can reuse.
- 🔁 2 non-WA errors — `Integer.MAX_VALUE + 1` overflow; check `left != MAX` before adding 1 (you do). Use `amount+1` as INF instead.
- Your 2-D `(sum, j)` memo is the knapsack form; 1-D over amount is enough.

### 678. Valid Parenthesis String

*M · String, Dynamic Programming, Stack, Greedy, Bracket Sequences · 2 sub · 1 AC / 0 WA / 1 TLE · last AC 2026-08-16 · [LC](https://leetcode.com/problems/valid-parenthesis-string/)*

**Crux (greedy):** track a range `[lo, hi]` of possible open counts: `(` → both +1; `)` → both −1; `*` → `lo-1, hi+1`; clamp `lo ≥ 0`; fail if `hi < 0`; end with `lo == 0`. O(n).
- ✍️ *scan prefix, should never go −ve; for * traverse all cases in DFS; use DP for overlapping subproblems* — that's your memo `(idx, open)` solution (🔁 1 TLE without memo). `dp` size must be `[n][n+1]` in worst case (open can reach n) — yours is `[n][n]`, passed by luck.

### 714. Best Time to Buy and Sell Stock with Transaction Fee

*M · Array, Dynamic Programming, Greedy · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-05-30 · [LC](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-with-transaction-fee/)*

**Crux:** state DP `(day, holding)`: not holding → `max(skip, -price + hold)`; holding → `max(skip, price - fee + notHold)`. Answer `f(0, false)`. Iterative: two variables `cash`, `hold`.
- ✍️ *Greedy might not work: 10 15 16 fee 2 → the solution is DP* — your notebook derivation is exactly the state machine. Charge the fee once (on sell).
- Your `HashMap<Boolean,Integer>[]` memo works; `int[n][2]` is the idiom.

### 746. Min Cost Climbing Stairs

*E · Array, Dynamic Programming · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-24 · [LC](https://leetcode.com/problems/min-cost-climbing-stairs/)*

**Crux:** `dp[i]` = min cost to *stand on* step i; `dp[0]=dp[1]=0`; `dp[i] = min(dp[i-1]+cost[i-1], dp[i-2]+cost[i-2])`; answer `dp[n]` (the top is *beyond* the last index).
- ✍️ *last floor = n; base if n==0 || n==1*. Off-by-one on whether cost is paid on leaving or arriving is the classic trap.

### 790. Domino and Tromino Tiling

*M · Dynamic Programming · 8 sub · 1 AC / 6 WA / 0 TLE · last AC 2026-05-29 · [LC](https://leetcode.com/problems/domino-and-tromino-tiling/)*

**Crux:** `dp[n] = 2·dp[n-1] + dp[n-3]`, `dp[0..3] = 1,1,2,5`, mod 1e9+7, **long** arithmetic.
- 🔁 **8 submissions, 6 WA** — the derivation. ✍️ notebook: `ways(n) = ways(n-1) + ways(n-2) + 2·(ways(n-3) + … + ways(0))`; subtract the `n-1` equation from the `n` equation to collapse the tail → `dp[n] − dp[n-1] = dp[n-1] + dp[n-3]`. Apply MOD at every step; cast to int only at the end.

### 1137. N-th Tribonacci Number

*E · Math, Dynamic Programming, Memoization · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-24 · [LC](https://leetcode.com/problems/n-th-tribonacci-number/)*

**Crux:** rolling three variables; base `T0=0, T1=T2=1`. Fits in int for n ≤ 37.


## 2-D DP

### 5. Longest Palindromic Substring

*M · Two Pointers, String, Dynamic Programming, Manacher · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-23 · [LC](https://leetcode.com/problems/longest-palindromic-substring/)*

**Crux:** expand around each centre, both odd (`i,i`) and even (`i,i+1`); keep the longest. O(n²), O(1) space.
- Return `substring(start, end+1)`. Single char is a palindrome. Manacher is O(n) but not expected.

### 62. Unique Paths

*M · Math, Dynamic Programming, Combinatorics · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-21 · [LC](https://leetcode.com/problems/unique-paths/)*

**Crux:** `paths(x,y) = paths(x+1,y) + paths(x,y+1)`, base at target → 1; memo. Or `C(m+n-2, m-1)`.
- Fits in int for the given constraints; combinatorics needs long.

### 63. Unique Paths II

*M · Array, Dynamic Programming, Matrix · 3 sub · 1 AC / 2 WA / 0 TLE · last AC 2026-07-20 · [LC](https://leetcode.com/problems/unique-paths-ii/)*

**Crux:** same recurrence, obstacle cell → 0. 🔁 2 WA — obstacle at the **start** or **end** cell must give 0; check validity *before* the `x==m-1 && y==n-1 → 1` base case (your `isValid` guard is first — correct).
- ✍️ *end state bottom-right; base when end-state reached return 1*.

### 64. Minimum Path Sum

*M · Array, Dynamic Programming, Matrix · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-19 · [LC](https://leetcode.com/problems/minimum-path-sum/)*

**Crux:** `dp[x][y] = grid + min(up, left)`; first row/col only one option; memo from bottom-right or iterate from top-left.
- Your `Integer.MAX_VALUE` sentinel then "if unchanged use grid only" is the base-case handling for `(0,0)`; explicit `x==0&&y==0 → grid` is clearer.

### 72. Edit Distance

*M · String, Dynamic Programming · 8 sub · 5 AC / 3 WA / 0 TLE · last AC 2026-09-22 · [LC](https://leetcode.com/problems/edit-distance/)*

**Crux:** `ed(x,y)`: chars equal → `ed(x-1,y-1)`; else `1 + min(ed(x-1,y-1) replace, ed(x-1,y) delete, ed(x,y-1) insert)`; base: one string empty → length of the other + 1 (with your −1 indexing: `x==-1 → y+1`).
- 🔁 8 submissions, 3 WA (solved 5 times over 18 months — keep re-drilling). ✍️ *base cases: if m==−1 || n==−1 return max(m,n)* — with 0-based-minus-one indices that's `y+1` / `x+1`, easy to get wrong; 1-indexed `dp[m+1][n+1]` with `dp[i][0]=i, dp[0][j]=j` is safer.

### 120. Triangle

*M · Array, Dynamic Programming · 2 sub · 1 AC / 0 WA / 1 TLE · last AC 2026-07-06 · [LC](https://leetcode.com/problems/triangle/)*

**Crux:** top-down memo `min(x+1,y) , (x+1,y+1)` + `tri[x][y]`; base row `n → 0`. Bottom-up in place is O(1) extra space.
- 🔁 1 TLE — no memo. ✍️ *isBottom → base case return val*.

### 518. Coin Change II

*M · Array, Dynamic Programming, Knapsack Problem, Complete Knapsack · 2 sub · 1 AC / 1 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/coin-change-ii/)*

**Crux:** count *combinations*: `ways(amount, i) = ways(amount - coins[i], i) + ways(amount, i-1)`; base `amount==0 → 1`, `amount<0 || i<0 → 0`. Iterative 1-D: **coins outer loop, amount inner** (inner-outer swapped counts permutations).
- ✍️ *2D dp, infinite coins; base if amount==0 return 1, if n==−1 return 0; if amount − coins[i] ≥ 0 then recurse*. 🔁 1 WA — base-case order (check `amount==0` before `i==-1`).
- Counts can exceed int in general — this problem guarantees fit.

### 1105. Filling Bookcase Shelves

*M · Array, Dynamic Programming · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-05-19 · [LC](https://leetcode.com/problems/filling-bookcase-shelves/)*

**Crux:** `dp[i]` = min height for books `0..i`; for each `i` try starting a new shelf at `j ≤ i` while width fits: `dp[i] = min(dp[j-1] + max(height[j..i]))`. O(n·maxBooksPerShelf).
- Your memo on `(x, remainingWidth)` while also passing `maxBookHeight` is subtly incomplete (the memo key ignores the running max) — it passed but the `dp[i]` + inner loop formulation is the correct one.

### 1143. Longest Common Subsequence

*M · String, Dynamic Programming, Longest Common Subsequence · 4 sub · 2 AC / 0 WA / 1 TLE · last AC 2026-05-27 · [LC](https://leetcode.com/problems/longest-common-subsequence/)*

**Crux:** match → `1 + lcs(x-1,y-1)`; else `max(lcs(x,y-1), lcs(x-1,y))`; base −1 → 0.
- 🔁 1 TLE — no memo (185ms with memo + `List` allocations per call; use plain `Math.max`). ✍️ *base case empty m==−1 || n==−1 : 0*.


## Greedy / Intervals

### 56. Merge Intervals

*M · Array, Sorting, Quicksort · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-06-06 · [LC](https://leetcode.com/problems/merge-intervals/)*

**Crux:** sort by start; if `last.end >= cur.start` extend `last.end = max(...)`, else push.
- Touching intervals `[1,4],[4,5]` merge (`>=`). Update with `max` — a later interval can be fully inside the previous.

### 135. Candy

*H · Array, Greedy · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-02-21 · [LC](https://leetcode.com/problems/candy/)*

**Crux:** two passes: L→R `if r[i] > r[i-1] c[i] = c[i-1]+1`; R→L `if r[i] > r[i+1] c[i] = max(c[i], c[i+1]+1)`. Sum.
- The `max` in the second pass is the whole trick. Debug `println`s are in the accepted code (347ms) — strip them.

### 334. Increasing Triplet Subsequence

*M · Array, Greedy, Longest Increasing Subsequence · 7 sub · 2 AC / 5 WA / 0 TLE · last AC 2026-05-27 · [LC](https://leetcode.com/problems/increasing-triplet-subsequence/)*

**Crux:** track `min1 ≤ min2` seen so far: `x <= min1 → min1 = x; else x <= min2 → min2 = x; else true`.
- 🔁 **7 submissions, 5 WA.** ✍️ *stack idea fails for 10 12 5 13; keep 2 min variables — be careful 5 1 6: min1's index may be after min2's, that's fine because min2 was set when a smaller-before existed*. Use `<=` so duplicates don't become a fake triplet (`1 1 1`).
- O(1) space required; LIS-of-length-3 is the general version.

### 435. Non-overlapping Intervals

*M · Array, Dynamic Programming, Greedy, Sorting · 5 sub · 3 AC / 2 WA / 0 TLE · last AC 2026-09-22 · [LC](https://leetcode.com/problems/non-overlapping-intervals/)*

**Crux:** sort by **end** (earliest-finishing first); keep an interval if `start >= prevEnd`; answer = `n - kept`.
- ✍️ *sort ascending by end; if ends equal, ascending start; overlap condition is s < e (touching is fine)*. Your notebook has the counter-example for sorting by start. 🔁 2 WA — sort key.
- Solved again today (2026-09-22) — this one and Edit Distance are your current re-drills.

### 452. Minimum Number of Arrows to Burst Balloons

*M · Array, Greedy, Sorting · 8 sub · 3 AC / 5 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/)*

**Crux:** sort by **end**; shoot at the first end; every balloon with `start <= arrowX` is burst; next balloon beyond starts a new arrow. Touching (`start == end`) counts as burst.
- 🔁 **8 submissions, 5 WA.** ✍️ *sort by start → staircase → must shrink boundary (curEnd = min(curEnd, node.end)) for the fully-covered case (1-4),(2-3); sort by end is much better (greedy end)*. Your accepted code is the sort-by-start + shrink version — know the sort-by-end one, it has no shrink step.
- Coordinates go to ±2^31 → use **long** for `currentEnd` init (`Long.MIN_VALUE`), and never do `x - y` in a comparator (use `Long.compare`).

### 605. Can Place Flowers

*E · Array, Greedy · 5 sub · 2 AC / 3 WA / 0 TLE · last AC 2026-05-25 · [LC](https://leetcode.com/problems/can-place-flowers/)*

**Crux:** greedy left→right: plant at `i` if `bed[i]==0` and both neighbours are 0 (out-of-bounds counts as 0); mutate the bed; count ≥ n.
- 🔁 5 submissions, 3 WA — boundary cells (`i==0`, `i==len-1`) and forgetting to mutate after planting (else adjacent zeros both count). ✍️ *write isFlower / canPlace helpers* — your bounds-safe `isFlower` is the clean way.
- Early return when `count >= n`.

### 649. Dota2 Senate

*M · String, Greedy, Queue · 10 sub · 1 AC / 9 WA / 0 TLE · last AC 2025-04-23 · [LC](https://leetcode.com/problems/dota2-senate/)*

**Crux:** two queues of indices (R, D); pop one from each; the smaller index bans the other and is re-queued with `index + n`; loop until one queue is empty.
- 🔁 **10 submissions, 9 WA.** ✍️ *delete one from the opposite party; if all from same party announce win; keep 2 queues, peek & compare which is smaller, pop, continue; +n to indicate next round — iterating 0..n-1 in rollover won't work*. The `+n` trick is the whole problem.
- Your `"Default"` return is unreachable — fine, but say so.


## Math & Bits

### 7. Reverse Integer

*M · Math · 3 sub · 1 AC / 2 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/reverse-integer/)*

**Crux:** pop digits with `%10`, push with `*10`; check overflow **before** pushing: `rev > MAX/10 || (rev == MAX/10 && digit > 7)` (and the MIN side), or just accumulate in `long` and range-check at the end.
- 🔁 2 WA — overflow. Your digit-by-digit comparison against the digits of `INT_MAX` works but is ✍️ *impl heavy*; the `long` version is 5 lines. Negative numbers: work on `|x|` and reapply the sign — careful `-MIN_VALUE` overflows int (long again).

### 9. Palindrome Number

*E · Math · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-26 · [LC](https://leetcode.com/problems/palindrome-number/)*

**Crux:** negatives → false; reverse the number and compare. Reverse only half to avoid overflow, or reverse into `long`.

### 12. Integer to Roman

*M · Hash Table, Math, String · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-07-02 · [LC](https://leetcode.com/problems/integer-to-roman/)*

**Crux:** greedy over the 13 values in descending order including the subtractive pairs (900, 400, 90, 40, 9, 4). ✍️ *good problem for syntax*.
- Keep the values in an ordered array; your `HashMap` + sort works but hides the order.

### 13. Roman to Integer

*E · Hash Table, Math, String · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/roman-to-integer/)*

**Crux:** scan right→left; if `val < prev` subtract else add. ✍️ *Better: traverse right → left, if [left] < [right] subtract — one-off state parsing (I/X/C cases) is the clumsy way*.

### 50. Pow(x, n)

*M · Math, Recursion · 2 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-07-20 · [LC](https://leetcode.com/problems/powx-n/)*

**Crux:** fast exponentiation: `half = pow(x, n/2)`; even → `half*half`, odd → `half*half*x`; negative `n` → `1/result`.
- ✍️ *n can be negative; base n==0 → 1*. `Integer.MIN_VALUE`: `-n` overflows int — use `long n` or recurse on `n/2` and handle the sign at each level (your `Math.abs(n/2)` works because `MIN/2` fits).

### 136. Single Number

*E · Array, Bit Manipulation · 3 sub · 3 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/single-number/)*

**Crux:** XOR all; pairs cancel.

### 172. Factorial Trailing Zeroes

*M · Math · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-07-04 · [LC](https://leetcode.com/problems/factorial-trailing-zeroes/)*

**Crux:** count factors of 5: `n/5 + n/25 + n/125 + …` (2s are always in excess). ✍️ *just count #5's; log(n)*.
- Loop `x *= 5` overflows int for large n — stop while `x <= n` with `x` as long, or divide `n` instead.

### 190. Reverse Bits

*E · Divide and Conquer, Bit Manipulation · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-08-16 · [LC](https://leetcode.com/problems/reverse-bits/)*

**Crux:** 32 iterations: `res = (res << 1) | (n & 1); n >>>= 1`. ✍️ *left shift result, extract & add LSB, right shift original*.
- Use `>>>` (logical) — `>>` on a negative int sign-extends (works here because you only read `n&1` 32 times, but `>>>` is the correct habit).

### 191. Number of 1 Bits

*E · Divide and Conquer, Bit Manipulation · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-25 · [LC](https://leetcode.com/problems/number-of-1-bits/)*

**Crux:** `while (n != 0) { cnt += n & 1; n >>>= 1 }` or `n &= n - 1` (clears lowest set bit; loop runs popcount times).
- Trap: with `>>` a negative `n` never reaches 0 (infinite loop). Current constraints make `n` positive, but use `>>>`. `Integer.bitCount` exists.

### 201. Bitwise AND of Numbers Range

*M · Bit Manipulation · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-07-02 · [LC](https://leetcode.com/problems/bitwise-and-of-numbers-range/)*

**Crux:** the AND of a range is the common binary prefix of `left` and `right`: shift both right until equal, counting shifts, then shift back. ✍️ *common prefix; shift right till equal*.
- `left == right` → itself. Alternative: `while (left < right) right &= right - 1`.

### 202. Happy Number

*E · Hash Table, Math, Two Pointers, Floyd's Cycle Finding Algorithm · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-08-12 · [LC](https://leetcode.com/problems/happy-number/)*

**Crux:** iterate digit-square-sum; detect a cycle with a set (or Floyd's slow/fast). Reach 1 → happy.
- Values shrink fast so int is fine; you used long — harmless.

### 338. Counting Bits

*E · Dynamic Programming, Bit Manipulation · 5 sub · 4 AC / 0 WA / 0 TLE · last AC 2026-05-26 · [LC](https://leetcode.com/problems/counting-bits/)*

**Crux:** `dp[i] = dp[i >> 1] + (i & 1)` (drop the LSB). ✍️ *find 1's in dp[i>>1] + lsb*.
- Alternative `dp[i] = dp[i & (i-1)] + 1`.

### 1071. Greatest Common Divisor of Strings

*E · Math, String, Euclidean Algorithm, Greatest Common Divisor · 3 sub · 2 AC / 1 WA / 0 TLE · last AC 2026-05-28 · [LC](https://leetcode.com/problems/greatest-common-divisor-of-strings/)*

**Crux:** if `s1 + s2 != s2 + s1` → `""`; else answer is the prefix of length `gcd(len1, len2)`.
- ✍️ *better impl* — your prefix enumeration is O(n²); the concatenation test + gcd is O(n). 🔁 1 WA — must check divisibility for *both* strings.

### 1318. Minimum Flips to Make a OR b Equal to c

*M · Bit Manipulation · 3 sub · 2 AC / 1 WA / 0 TLE · last AC 2026-05-20 · [LC](https://leetcode.com/problems/minimum-flips-to-make-a-or-b-equal-to-c/)*

**Crux:** per bit: if `c=1` and `a|b == 0` → 1 flip; if `c=0` → flips = number of set bits among `a,b` (0/1/2). Sum over 32 bits.
- 🔁 1 WA — the `c=0, a=1, b=1` case needs **2** flips. Loop while any of a,b,c non-zero.

### 2571. Minimum Operations to Reduce an Integer to 0

*M · Dynamic Programming, Greedy, Bit Manipulation · 6 sub · 2 AC / 1 WA / 0 TLE · last AC 2025-05-19 · [LC](https://leetcode.com/problems/minimum-operations-to-reduce-an-integer-to-0/)*

**Crux:** at the lowest set bit, either subtract it or add it (carrying into a run of ones); greedy/recursive `1 + min(f(n - low), f(high - n))` where `low ≤ n < high` are neighbouring powers of two.
- 🔁 6 submissions, 3 RE — infinite recursion when `n` is exactly a power of two (`lowPower*2 < x` loop) and base cases `x ≤ 1`. Cleaner bit trick: iterate bits; on a `1` followed by another `1`, add (`n += lowbit`), else subtract.


## Strings / Simulation

### 6. Zigzag Conversion

*M · String · 3 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-07-23 · [LC](https://leetcode.com/problems/zigzag-conversion/)*

**Crux:** walk the string with a row pointer bouncing between `0` and `numRows-1`; append each char to `rows[r]`; join rows.
- ✍️ *edge case numRows == 1 → return s* (otherwise the direction flip never happens / divides by zero in the period formula). Your `char[numRows][n]` grid works but wastes space; a `StringBuilder[]` per row is the idiom. 🔁 1 RE — that edge.

### 28. Find the Index of the First Occurrence in a String

*E · Two Pointers, String, String Matching, Z Algorithm, Knuth–Morris–Pratt Algorithm, Boyer–Moore String-Search Algorithm · 10 sub · 3 AC / 4 WA / 1 TLE · last AC 2018-05-09 · [LC](https://leetcode.com/problems/find-the-index-of-the-first-occurrence-in-a-string/)*

**Crux:** KMP: build `lps` (longest proper prefix that is also a suffix) for the needle, then scan with fallback `i = lps[i-1]`. O(n+m). Naive O(nm) is acceptable in most interviews.
- 🔁 10 submissions (2017–18), 4 WA, 1 TLE — your `lps` stores `matchedIndex` (−1-based) instead of the standard length-based table, which makes the fallback `lps[i-1]+1`. Use the standard: `lps[j] = length`, fallback `i = lps[i-1]`.
- Empty needle → 0.

### 43. Multiply Strings

*M · Math, String, Simulation · 2 sub · 1 AC / 1 WA / 0 TLE · last AC 2025-04-27 · [LC](https://leetcode.com/problems/multiply-strings/)*

**Crux:** `res[i+j+1] += d1*d2`, then normalise carries from the right; strip leading zeros; `"0"` when either is `"0"`.
- 🔁 1 WA — leading zeros / the zero product. Your row-per-digit then sum approach works; the single `int[m+n]` array is the compact form. No `Integer.parseInt` — inputs are up to 200 digits.

### 67. Add Binary

*E · Math, String, Bit Manipulation, Simulation · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-07-25 · [LC](https://leetcode.com/problems/add-binary/)*

**Crux:** walk both from the end with carry; append `sum%2`, carry `sum/2`; reverse at the end.
- Your 4-way `if` on sum ∈ {0,1,2,3} is fine; `%2` / `/2` collapses it. Don't forget the final carry.

### 214. Shortest Palindrome

*H · String, Rolling Hash, String Matching, Hash Function, Manacher, Z Algorithm, Knuth–Morris–Pratt Algorithm · 2 sub · 1 AC / 1 WA / 0 TLE · last AC 2017-11-19 · [LC](https://leetcode.com/problems/shortest-palindrome/)*

**Crux:** longest palindromic *prefix* of `s` = `lps` of `s + "#" + reverse(s)` at the last position; prepend `reverse(s.substring(k))`.
- The separator `#` must not occur in `s`. 🔁 1 WA — building the wrong side.


## Design

### 146. LRU Cache

*M · Hash Table, Linked List, Design, Doubly-Linked List · 5 sub · 2 AC / 3 WA / 0 TLE · last AC 2025-05-24 · [LC](https://leetcode.com/problems/lru-cache/)*

**Crux:** `HashMap<key, Node>` + doubly-linked list with head/tail sentinels. `get` → unlink and append to tail; `put` existing → update + move; `put` new at capacity → evict `head.next` (remove from map too) then append.
- 🔁 3 WA — the exact ordering: on an *existing* key do **not** evict; check capacity only for a new key; `get` must also refresh recency.
- Sentinels avoid all null checks in `delete`/`insertLast`.
- Java shortcut for interviews: `LinkedHashMap(cap, 0.75f, true)` + `removeEldestEntry`.

### 362. Design Hit Counter

*M · Array, Binary Search, Design, Queue, Data Stream · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-04-13 · [LC](https://leetcode.com/problems/design-hit-counter/)*

**Crux:** queue of timestamps; on `getHits(t)` evict while `front <= t - 300`. Timestamps monotonic.
- Follow-up (many hits per second): bucket array of 300 with `(time, count)`.

### 933. Number of Recent Calls

*E · Design, Queue, Data Stream · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2026-05-29 · [LC](https://leetcode.com/problems/number-of-recent-calls/)*

**Crux:** queue; evict while `t - front > 3000` (window inclusive).
- ✍️ *keep popping from front*.

### 981. Time Based Key-Value Store

*M · Hash Table, String, Binary Search, Design · 2 sub · 1 AC / 0 WA / 0 TLE · last AC 2026-08-16 · [LC](https://leetcode.com/problems/time-based-key-value-store/)*

**Crux:** `map<key, TreeMap<timestamp, value>>`; `get` = `floorEntry(ts)`. ✍️ *use floor to put upper bound; think of [9,8] → 9*.
- Timestamps are strictly increasing per key, so an `ArrayList` + binary search also works and is faster.
- Handle missing key / null floor → `""`.
- Your `TreeSet<VersionedValue>` with comparator on version works but `TreeMap` is the natural structure.


## Concurrency

### 1114. Print in Order

*E · Concurrency · 3 sub · 1 AC / 0 WA / 1 TLE · last AC 2025-04-03 · [LC](https://leetcode.com/problems/print-in-order/)*

**Crux:** one lock, flags `secondRun/thirdRun`, conditions; each stage `while(!flag) cond.await()`, then run, set flag, `signal`.
- Always `await` in a **while** (spurious wakeups). 🔁 1 TLE — a lost wakeup when signalling before the waiter checked the flag → the flag pattern fixes it. Alternatives: two `Semaphore(0)`, or `CountDownLatch`.

### 1115. Print FooBar Alternately

*M · Concurrency · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-04-03 · [LC](https://leetcode.com/problems/print-foobar-alternately/)*

**Crux:** lock + `isFoo` flag + two conditions; foo waits `while(!isFoo)`, prints, flips, signals bar.
- Two semaphores (`foo=1, bar=0`) is the shortest version.

### 1116. Print Zero Even Odd

*M · Concurrency · 2 sub · 2 AC / 0 WA / 0 TLE · last AC 2025-04-03 · [LC](https://leetcode.com/problems/print-zero-even-odd/)*

**Crux:** `zero` alternates with `odd/even`; flags `isZero`, `isOdd`; three conditions. `zero` runs `n` times, `odd` `(n+1)/2`, `even` `n/2`.
- Signal *both* odd and even after zero (only the right one passes its while). Three semaphores (`zero=1, odd=0, even=0`) is cleaner: zero releases `odd` or `even` based on parity.

### 1117. Building H2O

*M · Concurrency · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-04-03 · [LC](https://leetcode.com/problems/building-h2o/)*

**Crux:** counts `h`, `o` under one lock; hydrogen waits while `h == 2`, oxygen while `o == 1`; when `h==2 && o==1` reset both and signal all.
- Semaphore version: `H = Semaphore(2)`, `O = Semaphore(1)`, `CyclicBarrier(3)` to release a molecule.

### 1226. The Dining Philosophers

*M · Concurrency · 1 sub · 1 AC / 0 WA / 0 TLE · last AC 2025-04-04 · [LC](https://leetcode.com/problems/the-dining-philosophers/)*

**Crux:** simplest deadlock-free solution: one global lock around pick-eat-put (serialises everyone). Better: order forks (lowest-numbered first) or a semaphore allowing at most 4 philosophers to try.
- Say the trade-off out loud: global lock = no parallelism.
