# LeetCode — Quick Index

Generated 2026-09-22 from LeetCode account `mundale`. 198 solved · 54 easy / 128 medium / 16 hard · 565 submissions on solved problems (581 total).

Each row links to the LeetCode problem and to the crux/pitfalls entry in [notes.md](notes.md). **Attempts** = total submissions (AC/WA/TLE). ⚠️ = struggled (≥3 WA, ≥2 TLE or ≥6 submissions) — re-drill first. ✍️ = covered in handwritten notebook.

Jump: [Arrays & Hashing](#arrays-hashing) · [Prefix Sum](#prefix-sum) · [Two Pointers](#two-pointers) · [Sliding Window](#sliding-window) · [Stack](#stack) · [Monotonic Stack](#monotonic-stack) · [Binary Search](#binary-search) · [Linked List](#linked-list) · [Trees](#trees) · [BST](#bst) · [Trie](#trie) · [Heap](#heap) · [Backtracking](#backtracking) · [Graphs](#graphs) · [1-D DP](#1-d-dp) · [2-D DP](#2-d-dp) · [Greedy / Intervals](#greedy-intervals) · [Math & Bits](#math-bits) · [Strings / Simulation](#strings-simulation) · [Design](#design) · [Concurrency](#concurrency)


## Arrays & Hashing

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 1 | [Two Sum](https://leetcode.com/problems/two-sum/) · [notes](notes.md#1-two-sum) | E | 1 (1/0/0) | 2017-08-08 | hashmap value→index, one pass |
| 36 | [Valid Sudoku](https://leetcode.com/problems/valid-sudoku/) · [notes](notes.md#36-valid-sudoku) ✍️ | M | 4 (2/2/0) | 2026-07-23 | 3 checks: row, col, box (box idx = r/3*3+c/3) |
| 49 | [Group Anagrams](https://leetcode.com/problems/group-anagrams/) · [notes](notes.md#49-group-anagrams) | M | 1 (1/0/0) | 2025-05-24 | key = 26-count encoding (or sorted str) → bucket |
| 128 | [Longest Consecutive Sequence](https://leetcode.com/problems/longest-consecutive-sequence/) · [notes](notes.md#128-longest-consecutive-sequence) ✍️ | M | 3 (3/0/0) | 2026-07-23 | HashSet; expand ±1 from each element and delete as you go |
| 169 | [Majority Element](https://leetcode.com/problems/majority-element/) · [notes](notes.md#169-majority-element) | E | 1 (1/0/0) | 2025-07-25 | sort → nums[n/2]; or Boyer-Moore vote |
| 189 | [Rotate Array](https://leetcode.com/problems/rotate-array/) · [notes](notes.md#189-rotate-array) ✍️ | M | 1 (1/0/0) | 2026-07-20 | k%=n; reverse all, reverse [0,k), reverse [k,n) |
| 205 | [Isomorphic Strings](https://leetcode.com/problems/isomorphic-strings/) · [notes](notes.md#205-isomorphic-strings) | E | 2 (1/1/0) | 2025-09-07 | two maps s→t and t→s (bijection) |
| 219 | [Contains Duplicate II](https://leetcode.com/problems/contains-duplicate-ii/) · [notes](notes.md#219-contains-duplicate-ii) ✍️ | E | 1 (1/0/0) | 2026-07-02 | map value→latest index, check abs(i-j)≤k |
| 228 | [Summary Ranges](https://leetcode.com/problems/summary-ranges/) · [notes](notes.md#228-summary-ranges) ✍️ | E | 2 (1/0/0) | 2026-07-20 | walk; close range when nums[i]≠prev+1; flush last |
| 242 | [Valid Anagram](https://leetcode.com/problems/valid-anagram/) · [notes](notes.md#242-valid-anagram) | E | 1 (1/0/0) | 2025-07-27 | sort both / 26-count |
| 383 | [Ransom Note](https://leetcode.com/problems/ransom-note/) · [notes](notes.md#383-ransom-note) | E | 1 (1/0/0) | 2025-07-25 | 26-count of magazine, decrement |
| 628 | [Maximum Product of Three Numbers](https://leetcode.com/problems/maximum-product-of-three-numbers/) · [notes](notes.md#628-maximum-product-of-three-numbers) | E | 2 (2/0/0) | 2025-04-26 | sort; max(top3, min2×max) |
| 1207 | [Unique Number of Occurrences](https://leetcode.com/problems/unique-number-of-occurrences/) · [notes](notes.md#1207-unique-number-of-occurrences) ✍️ | E | 3 (3/0/0) | 2026-05-27 | freq map → set of freqs must be same size |
| 1431 | [Kids With the Greatest Number of Candies](https://leetcode.com/problems/kids-with-the-greatest-number-of-candies/) · [notes](notes.md#1431-kids-with-the-greatest-number-of-candies) ✍️ | E | 2 (2/0/0) | 2026-05-24 | max once, compare c+extra ≥ max |
| 1497 | [Check If Array Pairs Are Divisible by k](https://leetcode.com/problems/check-if-array-pairs-are-divisible-by-k/) · [notes](notes.md#1497-check-if-array-pairs-are-divisible-by-k) ⚠️ | M | 6 (1/5/0) | 2025-05-19 | normalize mod ((x%k)+k)%k; cnt[r]==cnt[k-r]; r=0 & r=k/2 need even |
| 1657 | [Determine if Two Strings Are Close](https://leetcode.com/problems/determine-if-two-strings-are-close/) · [notes](notes.md#1657-determine-if-two-strings-are-close) ⚠️ ✍️ | M | 5 (2/3/0) | 2026-05-28 | same char set AND same multiset of frequencies |
| 2215 | [Find the Difference of Two Arrays](https://leetcode.com/problems/find-the-difference-of-two-arrays/) · [notes](notes.md#2215-find-the-difference-of-two-arrays) | E | 3 (3/0/0) | 2026-05-21 | two sets, difference each way |
| 2352 | [Equal Row and Column Pairs](https://leetcode.com/problems/equal-row-and-column-pairs/) · [notes](notes.md#2352-equal-row-and-column-pairs) | M | 3 (2/0/0) | 2026-05-21 | hash rows (string/sum) then compare cols |
| 3046 | [Split the Array](https://leetcode.com/problems/split-the-array/) · [notes](notes.md#3046-split-the-array) | E | 1 (1/0/0) | 2025-05-22 | each value ≤2 times; even split |
| 3076 | [Shortest Uncommon Substring in an Array](https://leetcode.com/problems/shortest-uncommon-substring-in-an-array/) · [notes](notes.md#3076-shortest-uncommon-substring-in-an-array) | M | 4 (1/2/0) | 2025-02-09 | all substrings → set of owners; pick shortest unique, lexicographic tiebreak |

## Prefix Sum

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 238 | [Product of Array Except Self](https://leetcode.com/problems/product-of-array-except-self/) · [notes](notes.md#238-product-of-array-except-self) ✍️ | M | 3 (3/0/0) | 2026-05-24 | prefix product × suffix product |
| 724 | [Find Pivot Index](https://leetcode.com/problems/find-pivot-index/) · [notes](notes.md#724-find-pivot-index) ✍️ | E | 2 (2/0/0) | 2026-05-24 | prefix/suffix sums; left(i)==right(i) |
| 1732 | [Find the Highest Altitude](https://leetcode.com/problems/find-the-highest-altitude/) · [notes](notes.md#1732-find-the-highest-altitude) ✍️ | E | 2 (2/0/0) | 2026-05-28 | running sum, track max (start 0) |
| 1894 | [Find the Student that Will Replace the Chalk](https://leetcode.com/problems/find-the-student-that-will-replace-the-chalk/) · [notes](notes.md#1894-find-the-student-that-will-replace-the-chalk) | M | 1 (1/0/0) | 2025-05-19 | k %= total; then walk prefix |

## Two Pointers

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 11 | [Container With Most Water](https://leetcode.com/problems/container-with-most-water/) · [notes](notes.md#11-container-with-most-water) ✍️ | M | 4 (4/0/0) | 2026-07-23 | l/r; move the SMALLER side inward |
| 15 | [3Sum](https://leetcode.com/problems/3sum/) · [notes](notes.md#15-3sum) ✍️ | M | 3 (2/1/0) | 2026-07-23 | sort; fix i, 2-pointer on rest; skip duplicates |
| 26 | [Remove Duplicates from Sorted Array](https://leetcode.com/problems/remove-duplicates-from-sorted-array/) · [notes](notes.md#26-remove-duplicates-from-sorted-array) ✍️ | E | 2 (2/0/0) | 2026-06-22 | write-index; write when value changes |
| 42 | [Trapping Rain Water](https://leetcode.com/problems/trapping-rain-water/) · [notes](notes.md#42-trapping-rain-water) ✍️ | H | 1 (1/0/0) | 2026-07-23 | water[i]=min(maxL,maxR)-h[i]; prefix arrays or 2-pointer |
| 125 | [Valid Palindrome](https://leetcode.com/problems/valid-palindrome/) · [notes](notes.md#125-valid-palindrome) ✍️ | E | 1 (1/0/0) | 2026-06-24 | skip non-alnum from both ends, lowercase |
| 151 | [Reverse Words in a String](https://leetcode.com/problems/reverse-words-in-a-string/) · [notes](notes.md#151-reverse-words-in-a-string) ✍️ | M | 4 (4/0/0) | 2026-07-23 | tokenize on spaces, join reversed |
| 283 | [Move Zeroes](https://leetcode.com/problems/move-zeroes/) · [notes](notes.md#283-move-zeroes) ✍️ | E | 3 (2/1/0) | 2026-05-27 | write-index for non-zeros, then fill zeros |
| 345 | [Reverse Vowels of a String](https://leetcode.com/problems/reverse-vowels-of-a-string/) · [notes](notes.md#345-reverse-vowels-of-a-string) ✍️ | E | 2 (2/0/0) | 2026-05-27 | l/r swap vowels; include uppercase vowels |
| 392 | [Is Subsequence](https://leetcode.com/problems/is-subsequence/) · [notes](notes.md#392-is-subsequence) ✍️ | E | 4 (3/1/0) | 2026-07-23 | pointer into s advances on match |
| 443 | [String Compression](https://leetcode.com/problems/string-compression/) · [notes](notes.md#443-string-compression) ✍️ | M | 3 (2/1/0) | 2026-05-28 | write-index; write char + count digits on state change; flush last group |
| 844 | [Backspace String Compare](https://leetcode.com/problems/backspace-string-compare/) · [notes](notes.md#844-backspace-string-compare) | E | 2 (1/0/0) | 2025-04-27 | stack build, or two pointers from the back skipping # |
| 1679 | [Max Number of K-Sum Pairs](https://leetcode.com/problems/max-number-of-k-sum-pairs/) · [notes](notes.md#1679-max-number-of-k-sum-pairs) | M | 4 (3/0/1) | 2026-05-21 | sort; l/r sum vs k |
| 1768 | [Merge Strings Alternately](https://leetcode.com/problems/merge-strings-alternately/) · [notes](notes.md#1768-merge-strings-alternately) ✍️ | E | 2 (2/0/0) | 2026-05-27 | interleave to min len, append remainder |

## Sliding Window

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 3 | [Longest Substring Without Repeating Characters](https://leetcode.com/problems/longest-substring-without-repeating-characters/) · [notes](notes.md#3-longest-substring-without-repeating-characters) | M | 1 (1/0/0) | 2025-07-25 | set; shrink from left while duplicate |
| 30 | [Substring with Concatenation of All Words](https://leetcode.com/problems/substring-with-concatenation-of-all-words/) · [notes](notes.md#30-substring-with-concatenation-of-all-words) | H | 4 (1/2/0) | 2025-04-27 | for each offset<wordLen tokenize; word-count window of size k |
| 76 | [Minimum Window Substring](https://leetcode.com/problems/minimum-window-substring/) · [notes](notes.md#76-minimum-window-substring) ✍️ | H | 2 (2/0/0) | 2026-07-23 | need-map + matched counter; expand right, shrink left while fully matched |
| 209 | [Minimum Size Subarray Sum](https://leetcode.com/problems/minimum-size-subarray-sum/) · [notes](notes.md#209-minimum-size-subarray-sum) | M | 1 (1/0/0) | 2025-07-26 | grow until ≥target, shrink while ≥target |
| 643 | [Maximum Average Subarray I](https://leetcode.com/problems/maximum-average-subarray-i/) · [notes](notes.md#643-maximum-average-subarray-i) ⚠️ ✍️ | E | 7 (3/4/0) | 2026-05-24 | fixed k window; init max with FIRST window |
| 1004 | [Max Consecutive Ones III](https://leetcode.com/problems/max-consecutive-ones-iii/) · [notes](notes.md#1004-max-consecutive-ones-iii) ⚠️ ✍️ | M | 6 (4/2/0) | 2026-05-24 | window with ≤k zeros; queue of zero indices |
| 1456 | [Maximum Number of Vowels in a Substring of Given Length](https://leetcode.com/problems/maximum-number-of-vowels-in-a-substring-of-given-length/) · [notes](notes.md#1456-maximum-number-of-vowels-in-a-substring-of-given-length) ✍️ | M | 2 (2/0/0) | 2026-05-27 | fixed k window count |
| 1493 | [Longest Subarray of 1's After Deleting One Element](https://leetcode.com/problems/longest-subarray-of-1s-after-deleting-one-element/) · [notes](notes.md#1493-longest-subarray-of-1s-after-deleting-one-element) ✍️ | M | 2 (2/0/0) | 2026-05-27 | window with ≤1 zero; answer len-1; all-ones → n-1 |

## Stack

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 20 | [Valid Parentheses](https://leetcode.com/problems/valid-parentheses/) · [notes](notes.md#20-valid-parentheses) | E | 1 (1/0/0) | 2025-07-27 | push opens; pop on matching close; empty at end |
| 71 | [Simplify Path](https://leetcode.com/problems/simplify-path/) · [notes](notes.md#71-simplify-path) | M | 1 (1/0/0) | 2025-04-27 | split '/', pop on '..', ignore '.' and '' |
| 155 | [Min Stack](https://leetcode.com/problems/min-stack/) · [notes](notes.md#155-min-stack) ✍️ | M | 2 (2/0/0) | 2026-08-16 | stack of (value, minSoFar) |
| 394 | [Decode String](https://leetcode.com/problems/decode-string/) · [notes](notes.md#394-decode-string) ✍️ | M | 3 (2/1/0) | 2026-05-28 | push chars; on ']' pop to '[' then pop digits, repeat |
| 735 | [Asteroid Collision](https://leetcode.com/problems/asteroid-collision/) · [notes](notes.md#735-asteroid-collision) ⚠️ ✍️ | M | 7 (3/4/0) | 2026-05-28 | collide only when top>0 and cur<0; loop until stable |
| 2390 | [Removing Stars From a String](https://leetcode.com/problems/removing-stars-from-a-string/) · [notes](notes.md#2390-removing-stars-from-a-string) ✍️ | M | 2 (2/0/0) | 2026-05-28 | pop on * |

## Monotonic Stack

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 84 | [Largest Rectangle in Histogram](https://leetcode.com/problems/largest-rectangle-in-histogram/) · [notes](notes.md#84-largest-rectangle-in-histogram) ✍️ | H | 2 (1/1/0) | 2026-06-19 | prev-smaller & next-smaller per bar; width × height |
| 239 | [Sliding Window Maximum](https://leetcode.com/problems/sliding-window-maximum/) · [notes](notes.md#239-sliding-window-maximum) | H | 2 (1/0/1) | 2025-04-26 | monotonic deque (or TreeSet of (val,idx)) |
| 739 | [Daily Temperatures](https://leetcode.com/problems/daily-temperatures/) · [notes](notes.md#739-daily-temperatures) ✍️ | M | 2 (2/0/0) | 2026-05-24 | iterate from right; pop ≤ current; sentinel |
| 901 | [Online Stock Span](https://leetcode.com/problems/online-stock-span/) · [notes](notes.md#901-online-stock-span) ⚠️ ✍️ | M | 5 (2/3/0) | 2026-05-24 | stack of (price,idx); pop ≤ price; span = i - top.idx |

## Binary Search

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 33 | [Search in Rotated Sorted Array](https://leetcode.com/problems/search-in-rotated-sorted-array/) · [notes](notes.md#33-search-in-rotated-sorted-array) ✍️ | M | 1 (1/0/0) | 2026-07-06 | one half is sorted; check if target in sorted half else go other |
| 34 | [Find First and Last Position of Element in Sorted Array](https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/) · [notes](notes.md#34-find-first-and-last-position-of-element-in-sorted-array) ✍️ | M | 1 (1/0/0) | 2026-06-30 | 2 BS: keep candidate, keep going left / right |
| 74 | [Search a 2D Matrix](https://leetcode.com/problems/search-a-2d-matrix/) · [notes](notes.md#74-search-a-2d-matrix) ✍️ | M | 2 (2/0/0) | 2026-07-23 | BS on rows (last row with row[0]≤t), then BS in row |
| 153 | [Find Minimum in Rotated Sorted Array](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/) · [notes](notes.md#153-find-minimum-in-rotated-sorted-array) ⚠️ ✍️ | M | 9 (5/4/0) | 2026-07-23 | if nums[mid]≤nums[end] min is at/left of mid; else right |
| 162 | [Find Peak Element](https://leetcode.com/problems/find-peak-element/) · [notes](notes.md#162-find-peak-element) ⚠️ ✍️ | M | 10 (3/3/0) | 2026-07-23 | compare mid with neighbours (−∞ outside); move toward incline |
| 374 | [Guess Number Higher or Lower](https://leetcode.com/problems/guess-number-higher-or-lower/) · [notes](notes.md#374-guess-number-higher-or-lower) ⚠️ ✍️ | E | 4 (2/0/2) | 2026-05-27 | classic; mid = s+(e-s)/2 |
| 704 | [Binary Search](https://leetcode.com/problems/binary-search/) · [notes](notes.md#704-binary-search) | E | 2 (1/0/0) | 2026-08-12 | classic; start≤end |
| 875 | [Koko Eating Bananas](https://leetcode.com/problems/koko-eating-bananas/) · [notes](notes.md#875-koko-eating-bananas) ✍️ | M | 3 (2/1/0) | 2026-05-29 | BS on answer k∈[1,max]; feasible(k)=Σceil(p/k)≤h |
| 2300 | [Successful Pairs of Spells and Potions](https://leetcode.com/problems/successful-pairs-of-spells-and-potions/) · [notes](notes.md#2300-successful-pairs-of-spells-and-potions) ✍️ | M | 2 (1/1/0) | 2025-06-24 | sort potions; first idx with potion×spell ≥ success (long) |

## Linked List

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 2 | [Add Two Numbers](https://leetcode.com/problems/add-two-numbers/) · [notes](notes.md#2-add-two-numbers) ✍️ | M | 2 (2/0/0) | 2026-07-23 | dummy head; loop while l1 or l2 or carry |
| 19 | [Remove Nth Node From End of List](https://leetcode.com/problems/remove-nth-node-from-end-of-list/) · [notes](notes.md#19-remove-nth-node-from-end-of-list) | M | 1 (1/0/0) | 2025-07-27 | dummy; count then walk to size-n |
| 21 | [Merge Two Sorted Lists](https://leetcode.com/problems/merge-two-sorted-lists/) · [notes](notes.md#21-merge-two-sorted-lists) | E | 1 (1/0/0) | 2025-07-25 | dummy head; splice smaller |
| 25 | [Reverse Nodes in k-Group](https://leetcode.com/problems/reverse-nodes-in-k-group/) · [notes](notes.md#25-reverse-nodes-in-k-group) ✍️ | H | 2 (2/0/0) | 2026-07-23 | find k-th tail; reverse block; reconnect prevTail→newHead, newTail→next |
| 61 | [Rotate List](https://leetcode.com/problems/rotate-list/) · [notes](notes.md#61-rotate-list) | M | 1 (1/0/0) | 2025-07-25 | make circular; walk n-k%n; cut |
| 82 | [Remove Duplicates from Sorted List II](https://leetcode.com/problems/remove-duplicates-from-sorted-list-ii/) · [notes](notes.md#82-remove-duplicates-from-sorted-list-ii) ✍️ | M | 3 (2/1/0) | 2026-07-23 | keep node only if it ≠ prev and ≠ next; terminate tail |
| 86 | [Partition List](https://leetcode.com/problems/partition-list/) · [notes](notes.md#86-partition-list) ✍️ | M | 1 (1/0/0) | 2026-07-02 | two dummy lists small/large; null-terminate large |
| 141 | [Linked List Cycle](https://leetcode.com/problems/linked-list-cycle/) · [notes](notes.md#141-linked-list-cycle) ✍️ | E | 1 (1/0/0) | 2026-07-06 | fast/slow |
| 143 | [Reorder List](https://leetcode.com/problems/reorder-list/) · [notes](notes.md#143-reorder-list) ✍️ | M | 2 (1/0/0) | 2026-06-20 | count → find pre-mid → reverse 2nd half → merge with 3rd pointer |
| 206 | [Reverse Linked List](https://leetcode.com/problems/reverse-linked-list/) · [notes](notes.md#206-reverse-linked-list) ✍️ | E | 2 (2/0/0) | 2026-05-29 | p,q iterate; head.next=null FIRST |
| 328 | [Odd Even Linked List](https://leetcode.com/problems/odd-even-linked-list/) · [notes](notes.md#328-odd-even-linked-list) ⚠️ ✍️ | M | 7 (3/1/0) | 2026-05-29 | two chains p/q; track last odd; attach evenHead |
| 2095 | [Delete the Middle Node of a Linked List](https://leetcode.com/problems/delete-the-middle-node-of-a-linked-list/) · [notes](notes.md#2095-delete-the-middle-node-of-a-linked-list) ✍️ | M | 4 (2/1/0) | 2026-05-29 | count; walk to n/2 - 1; n==1 → null |
| 2130 | [Maximum Twin Sum of a Linked List](https://leetcode.com/problems/maximum-twin-sum-of-a-linked-list/) · [notes](notes.md#2130-maximum-twin-sum-of-a-linked-list) ✍️ | M | 2 (2/0/0) | 2026-05-29 | reverse 2nd half; walk both |

## Trees

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 102 | [Binary Tree Level Order Traversal](https://leetcode.com/problems/binary-tree-level-order-traversal/) · [notes](notes.md#102-binary-tree-level-order-traversal) | M | 1 (1/0/0) | 2025-09-02 | BFS with level tag; flush last level |
| 103 | [Binary Tree Zigzag Level Order Traversal](https://leetcode.com/problems/binary-tree-zigzag-level-order-traversal/) · [notes](notes.md#103-binary-tree-zigzag-level-order-traversal) | M | 2 (1/0/0) | 2017-11-28 | level order; reverse odd levels |
| 104 | [Maximum Depth of Binary Tree](https://leetcode.com/problems/maximum-depth-of-binary-tree/) · [notes](notes.md#104-maximum-depth-of-binary-tree) ✍️ | E | 3 (3/0/0) | 2026-07-23 | DFS with depth; global max |
| 105 | [Construct Binary Tree from Preorder and Inorder Traversal](https://leetcode.com/problems/construct-binary-tree-from-preorder-and-inorder-traversal/) · [notes](notes.md#105-construct-binary-tree-from-preorder-and-inorder-traversal) | M | 2 (1/1/0) | 2025-08-12 | inorder index map; left size = idx-inStart |
| 110 | [Balanced Binary Tree](https://leetcode.com/problems/balanced-binary-tree/) · [notes](notes.md#110-balanced-binary-tree) ⚠️ | E | 5 (1/3/0) | 2024-08-04 | height that returns -1 if unbalanced (avoid O(n²)) |
| 112 | [Path Sum](https://leetcode.com/problems/path-sum/) · [notes](notes.md#112-path-sum) | E | 1 (1/0/0) | 2017-10-05 | DFS with running sum; check at leaf only |
| 113 | [Path Sum II](https://leetcode.com/problems/path-sum-ii/) · [notes](notes.md#113-path-sum-ii) | M | 2 (2/0/0) | 2017-10-05 | DFS with path copy; check at leaf |
| 114 | [Flatten Binary Tree to Linked List](https://leetcode.com/problems/flatten-binary-tree-to-linked-list/) · [notes](notes.md#114-flatten-binary-tree-to-linked-list) | M | 1 (1/0/0) | 2025-07-27 | recursive: returns tail; leftTail.right=right; right=left; left=null |
| 124 | [Binary Tree Maximum Path Sum](https://leetcode.com/problems/binary-tree-maximum-path-sum/) · [notes](notes.md#124-binary-tree-maximum-path-sum) ⚠️ | H | 6 (2/4/0) | 2017-11-26 | return best single branch (≥0); global = L+node+R |
| 199 | [Binary Tree Right Side View](https://leetcode.com/problems/binary-tree-right-side-view/) · [notes](notes.md#199-binary-tree-right-side-view) ✍️ | M | 3 (3/0/0) | 2026-07-23 | BFS; last node per level |
| 226 | [Invert Binary Tree](https://leetcode.com/problems/invert-binary-tree/) · [notes](notes.md#226-invert-binary-tree) | E | 1 (1/0/0) | 2025-08-12 | swap children recursively |
| 236 | [Lowest Common Ancestor of a Binary Tree](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree/) · [notes](notes.md#236-lowest-common-ancestor-of-a-binary-tree) ✍️ | M | 3 (3/0/0) | 2026-07-23 | root-to-node paths, last common; or recursive LCA |
| 437 | [Path Sum III](https://leetcode.com/problems/path-sum-iii/) · [notes](notes.md#437-path-sum-iii) ⚠️ ✍️ | M | 11 (4/5/0) | 2026-05-25 | prefix-sum hashmap along path (O(n)); or DFS from every node (O(n²)) |
| 543 | [Diameter of Binary Tree](https://leetcode.com/problems/diameter-of-binary-tree/) · [notes](notes.md#543-diameter-of-binary-tree) | E | 2 (1/1/0) | 2017-11-28 | return depth, track L+R at each node |
| 637 | [Average of Levels in Binary Tree](https://leetcode.com/problems/average-of-levels-in-binary-tree/) · [notes](notes.md#637-average-of-levels-in-binary-tree) | E | 2 (1/1/0) | 2025-07-25 | level sums with long |
| 687 | [Longest Univalue Path](https://leetcode.com/problems/longest-univalue-path/) · [notes](notes.md#687-longest-univalue-path) | M | 3 (2/1/0) | 2017-10-05 | extend child only if same value; global L+R |
| 872 | [Leaf-Similar Trees](https://leetcode.com/problems/leaf-similar-trees/) · [notes](notes.md#872-leaf-similar-trees) ⚠️ ✍️ | E | 6 (4/2/0) | 2026-05-25 | collect leaves in order, compare (equals, not ==) |
| 1161 | [Maximum Level Sum of a Binary Tree](https://leetcode.com/problems/maximum-level-sum-of-a-binary-tree/) · [notes](notes.md#1161-maximum-level-sum-of-a-binary-tree) ✍️ | M | 3 (2/1/0) | 2026-05-29 | level sums; return smallest level with max |
| 1372 | [Longest ZigZag Path in a Binary Tree](https://leetcode.com/problems/longest-zigzag-path-in-a-binary-tree/) · [notes](notes.md#1372-longest-zigzag-path-in-a-binary-tree) ⚠️ ✍️ | M | 4 (2/0/2) | 2026-05-24 | DFS carrying (goLeft, goRight) lengths |
| 1448 | [Count Good Nodes in Binary Tree](https://leetcode.com/problems/count-good-nodes-in-binary-tree/) · [notes](notes.md#1448-count-good-nodes-in-binary-tree) | M | 2 (2/0/0) | 2026-05-21 | pass max-so-far down; count if val ≥ max |

## BST

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 98 | [Validate Binary Search Tree](https://leetcode.com/problems/validate-binary-search-tree/) · [notes](notes.md#98-validate-binary-search-tree) | M | 2 (1/1/0) | 2025-07-19 | pass (min,max) bounds down (use long) / return subtree (min,max,valid) |
| 108 | [Convert Sorted Array to Binary Search Tree](https://leetcode.com/problems/convert-sorted-array-to-binary-search-tree/) · [notes](notes.md#108-convert-sorted-array-to-binary-search-tree) | E | 1 (1/0/0) | 2025-07-25 | mid as root, recurse |
| 173 | [Binary Search Tree Iterator](https://leetcode.com/problems/binary-search-tree-iterator/) · [notes](notes.md#173-binary-search-tree-iterator) | M | 4 (3/1/0) | 2025-09-07 | stack of left spine; O(h) memory |
| 230 | [Kth Smallest Element in a BST](https://leetcode.com/problems/kth-smallest-element-in-a-bst/) · [notes](notes.md#230-kth-smallest-element-in-a-bst) ✍️ | M | 2 (2/0/0) | 2026-06-30 | inorder count; or subtree sizes |
| 235 | [Lowest Common Ancestor of a Binary Search Tree](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-search-tree/) · [notes](notes.md#235-lowest-common-ancestor-of-a-binary-search-tree) ✍️ | M | 1 (1/0/0) | 2026-08-16 | both smaller → left, both larger → right, else current |
| 450 | [Delete Node in a BST](https://leetcode.com/problems/delete-node-in-a-bst/) · [notes](notes.md#450-delete-node-in-a-bst) ⚠️ ✍️ | M | 6 (3/3/0) | 2026-05-27 | find parent; 0/1 child → splice; 2 children → replace with successor, delete successor |
| 700 | [Search in a Binary Search Tree](https://leetcode.com/problems/search-in-a-binary-search-tree/) · [notes](notes.md#700-search-in-a-binary-search-tree) | E | 2 (2/0/0) | 2026-05-21 | go left/right by value |

## Trie

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 208 | [Implement Trie (Prefix Tree)](https://leetcode.com/problems/implement-trie-prefix-tree/) · [notes](notes.md#208-implement-trie-prefix-tree) ✍️ | M | 3 (3/0/0) | 2026-07-23 | node = map<char,node> + isLeaf |
| 212 | [Word Search II](https://leetcode.com/problems/word-search-ii/) · [notes](notes.md#212-word-search-ii) | H | 5 (1/1/0) | 2025-02-09 | trie of words + DFS on board carrying trie node; unmark visited |
| 1268 | [Search Suggestions System](https://leetcode.com/problems/search-suggestions-system/) · [notes](notes.md#1268-search-suggestions-system) ⚠️ | M | 7 (2/3/0) | 2026-05-22 | trie walk per prefix + DFS in lexical order, stop at 3 |

## Heap

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 23 | [Merge k Sorted Lists](https://leetcode.com/problems/merge-k-sorted-lists/) · [notes](notes.md#23-merge-k-sorted-lists) ✍️ | H | 2 (2/0/0) | 2026-07-23 | PQ of list heads; poll, push next |
| 215 | [Kth Largest Element in an Array](https://leetcode.com/problems/kth-largest-element-in-an-array/) · [notes](notes.md#215-kth-largest-element-in-an-array) | M | 3 (3/0/0) | 2026-07-23 | min-heap of size k |
| 295 | [Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/) · [notes](notes.md#295-find-median-from-data-stream) ✍️ | H | 1 (1/0/0) | 2026-08-12 | max-heap left / min-heap right; balance; route via right then move |
| 347 | [Top K Frequent Elements](https://leetcode.com/problems/top-k-frequent-elements/) · [notes](notes.md#347-top-k-frequent-elements) ✍️ | M | 3 (3/0/0) | 2026-08-15 | freq map → min-heap of size k by freq |
| 2336 | [Smallest Number in Infinite Set](https://leetcode.com/problems/smallest-number-in-infinite-set/) · [notes](notes.md#2336-smallest-number-in-infinite-set) | M | 2 (2/0/0) | 2026-05-21 | counter + TreeSet of re-added (< counter) |
| 2462 | [Total Cost to Hire K Workers](https://leetcode.com/problems/total-cost-to-hire-k-workers/) · [notes](notes.md#2462-total-cost-to-hire-k-workers) ⚠️ ✍️ | M | 5 (2/3/0) | 2026-05-28 | two min-heaps with L/R index pointers; never overlap |
| 2542 | [Maximum Subsequence Score](https://leetcode.com/problems/maximum-subsequence-score/) · [notes](notes.md#2542-maximum-subsequence-score) ⚠️ ✍️ | M | 13 (1/10/0) | 2026-05-28 | sort by nums2 desc; keep k largest nums1 in min-heap; score only when size==k |

## Backtracking

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 17 | [Letter Combinations of a Phone Number](https://leetcode.com/problems/letter-combinations-of-a-phone-number/) · [notes](notes.md#17-letter-combinations-of-a-phone-number) | M | 3 (3/0/0) | 2026-07-23 | add → recurse → removeLast |
| 22 | [Generate Parentheses](https://leetcode.com/problems/generate-parentheses/) · [notes](notes.md#22-generate-parentheses) ✍️ | M | 3 (1/2/0) | 2026-07-21 | track open/close; prune when close>open or open>n |
| 39 | [Combination Sum](https://leetcode.com/problems/combination-sum/) · [notes](notes.md#39-combination-sum) ✍️ | M | 1 (1/0/0) | 2026-07-23 | include (stay at i) / exclude (i-1); DP on lists tricky |
| 46 | [Permutations](https://leetcode.com/problems/permutations/) · [notes](notes.md#46-permutations) ✍️ | M | 1 (1/0/0) | 2026-07-06 | swap(fromIdx,i) → recurse → swap back |
| 51 | [N-Queens](https://leetcode.com/problems/n-queens/) · [notes](notes.md#51-n-queens) ✍️ | H | 1 (1/0/0) | 2026-07-24 | place column by column; check row + 2 diagonals |
| 77 | [Combinations](https://leetcode.com/problems/combinations/) · [notes](notes.md#77-combinations) | M | 1 (1/0/0) | 2025-07-25 | pick next > last |
| 78 | [Subsets](https://leetcode.com/problems/subsets/) · [notes](notes.md#78-subsets) ✍️ | M | 1 (1/0/0) | 2026-08-16 | take / don't take idx |
| 140 | [Word Break II](https://leetcode.com/problems/word-break-ii/) · [notes](notes.md#140-word-break-ii) | H | 1 (1/0/0) | 2025-02-06 | recurse on prefix in dict; join sentences |
| 216 | [Combination Sum III](https://leetcode.com/problems/combination-sum-iii/) · [notes](notes.md#216-combination-sum-iii) ⚠️ | M | 7 (7/0/0) | 2026-05-21 | pick from startIndex..9, k left, sum left |
| 679 | [24 Game](https://leetcode.com/problems/24-game/) · [notes](notes.md#679-24-game) | H | 3 (1/2/0) | 2025-04-25 | pick 2 numbers, apply 6 ops, recurse on smaller list; eps compare |

## Graphs

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 127 | [Word Ladder](https://leetcode.com/problems/word-ladder/) · [notes](notes.md#127-word-ladder) ⚠️ | H | 4 (2/0/2) | 2026-08-13 | BFS on words; neighbours by 26×L mutation lookups |
| 130 | [Surrounded Regions](https://leetcode.com/problems/surrounded-regions/) · [notes](notes.md#130-surrounded-regions) ⚠️ ✍️ | M | 5 (1/4/0) | 2026-07-06 | mark border-connected O's first, flip the rest |
| 133 | [Clone Graph](https://leetcode.com/problems/clone-graph/) · [notes](notes.md#133-clone-graph) | M | 1 (1/0/0) | 2025-07-26 | map original→copy; DFS |
| 200 | [Number of Islands](https://leetcode.com/problems/number-of-islands/) · [notes](notes.md#200-number-of-islands) ✍️ | M | 2 (2/0/0) | 2026-07-23 | DFS flood fill on '1's with visited |
| 207 | [Course Schedule](https://leetcode.com/problems/course-schedule/) · [notes](notes.md#207-course-schedule) ✍️ | M | 2 (1/0/0) | 2026-08-16 | cycle detection DFS with recursion stack |
| 210 | [Course Schedule II](https://leetcode.com/problems/course-schedule-ii/) · [notes](notes.md#210-course-schedule-ii) | M | 3 (2/1/0) | 2025-09-02 | DFS post-order = topo order; cycle → [] |
| 399 | [Evaluate Division](https://leetcode.com/problems/evaluate-division/) · [notes](notes.md#399-evaluate-division) ✍️ | M | 2 (2/0/0) | 2026-05-27 | weighted graph a→b=v, b→a=1/v; DFS multiply |
| 433 | [Minimum Genetic Mutation](https://leetcode.com/problems/minimum-genetic-mutation/) · [notes](notes.md#433-minimum-genetic-mutation) | M | 4 (3/0/0) | 2025-04-14 | BFS on genes; 4×L mutations in bank |
| 547 | [Number of Provinces](https://leetcode.com/problems/number-of-provinces/) · [notes](notes.md#547-number-of-provinces) | M | 2 (2/0/0) | 2026-05-21 | count DFS components |
| 684 | [Redundant Connection](https://leetcode.com/problems/redundant-connection/) · [notes](notes.md#684-redundant-connection) | M | 2 (1/1/0) | 2017-10-05 | union-find; first edge with same root |
| 841 | [Keys and Rooms](https://leetcode.com/problems/keys-and-rooms/) · [notes](notes.md#841-keys-and-rooms) ✍️ | M | 2 (2/0/0) | 2026-05-27 | DFS from 0; visited count == n |
| 994 | [Rotting Oranges](https://leetcode.com/problems/rotting-oranges/) · [notes](notes.md#994-rotting-oranges) ⚠️ | M | 5 (2/3/0) | 2026-05-22 | multi-source BFS; steps; check fresh left |
| 1466 | [Reorder Routes to Make All Paths Lead to the City Zero](https://leetcode.com/problems/reorder-routes-to-make-all-paths-lead-to-the-city-zero/) · [notes](notes.md#1466-reorder-routes-to-make-all-paths-lead-to-the-city-zero) ✍️ | M | 2 (2/0/0) | 2026-05-28 | undirected BFS from 0; count edges pointing away |
| 1926 | [Nearest Exit from Entrance in Maze](https://leetcode.com/problems/nearest-exit-from-entrance-in-maze/) · [notes](notes.md#1926-nearest-exit-from-entrance-in-maze) ⚠️ ✍️ | M | 8 (3/1/4) | 2026-05-27 | BFS; mark visited on enqueue; entrance isn't an exit |

## 1-D DP

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 45 | [Jump Game II](https://leetcode.com/problems/jump-game-ii/) · [notes](notes.md#45-jump-game-ii) ✍️ | M | 1 (1/0/0) | 2026-07-24 | greedy BFS levels (DP is O(n²)) |
| 53 | [Maximum Subarray](https://leetcode.com/problems/maximum-subarray/) · [notes](notes.md#53-maximum-subarray) | M | 3 (1/2/0) | 2025-08-12 | Kadane: cur=max(x, cur+x) |
| 55 | [Jump Game](https://leetcode.com/problems/jump-game/) · [notes](notes.md#55-jump-game) ✍️ | M | 1 (1/0/0) | 2026-07-24 | greedy max reach (DP is O(n²)) |
| 70 | [Climbing Stairs](https://leetcode.com/problems/climbing-stairs/) · [notes](notes.md#70-climbing-stairs) | E | 1 (1/0/0) | 2025-07-29 | fib |
| 121 | [Best Time to Buy and Sell Stock](https://leetcode.com/problems/best-time-to-buy-and-sell-stock/) · [notes](notes.md#121-best-time-to-buy-and-sell-stock) | E | 1 (1/0/0) | 2025-07-29 | min so far; max(price-min) |
| 139 | [Word Break](https://leetcode.com/problems/word-break/) · [notes](notes.md#139-word-break) | M | 1 (1/0/0) | 2025-09-11 | dp[start] = any dict word prefix && dp[end] |
| 198 | [House Robber](https://leetcode.com/problems/house-robber/) · [notes](notes.md#198-house-robber) ✍️ | M | 5 (4/0/1) | 2026-07-23 | max(take nums[i]+dp[i-2], skip dp[i-1]) |
| 213 | [House Robber II](https://leetcode.com/problems/house-robber-ii/) · [notes](notes.md#213-house-robber-ii) | M | 4 (1/1/1) | 2025-04-13 | max(rob(0..n-2), rob(1..n-1)) |
| 300 | [Longest Increasing Subsequence](https://leetcode.com/problems/longest-increasing-subsequence/) · [notes](notes.md#300-longest-increasing-subsequence) | M | 2 (1/1/0) | 2025-09-11 | O(n²) LIS ending at i; or patience O(n log n) |
| 322 | [Coin Change](https://leetcode.com/problems/coin-change/) · [notes](notes.md#322-coin-change) | M | 4 (2/0/0) | 2024-07-28 | min coins; unbounded; INF sentinel |
| 678 | [Valid Parenthesis String](https://leetcode.com/problems/valid-parenthesis-string/) · [notes](notes.md#678-valid-parenthesis-string) ✍️ | M | 2 (1/0/1) | 2026-08-16 | memo (idx, open) / greedy lo–hi range |
| 714 | [Best Time to Buy and Sell Stock with Transaction Fee](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-with-transaction-fee/) · [notes](notes.md#714-best-time-to-buy-and-sell-stock-with-transaction-fee) ✍️ | M | 1 (1/0/0) | 2026-05-30 | state DP (day, holding); sell pays fee |
| 746 | [Min Cost Climbing Stairs](https://leetcode.com/problems/min-cost-climbing-stairs/) · [notes](notes.md#746-min-cost-climbing-stairs) ✍️ | E | 2 (2/0/0) | 2026-05-24 | dp[i]=min(dp[i-1]+c[i-1], dp[i-2]+c[i-2]); answer dp[n] |
| 790 | [Domino and Tromino Tiling](https://leetcode.com/problems/domino-and-tromino-tiling/) · [notes](notes.md#790-domino-and-tromino-tiling) ⚠️ ✍️ | M | 8 (1/6/0) | 2026-05-29 | dp[n]=2dp[n-1]+dp[n-3]; MOD |
| 1137 | [N-th Tribonacci Number](https://leetcode.com/problems/n-th-tribonacci-number/) · [notes](notes.md#1137-n-th-tribonacci-number) ✍️ | E | 2 (2/0/0) | 2026-05-24 | rolling 3 vars |

## 2-D DP

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 5 | [Longest Palindromic Substring](https://leetcode.com/problems/longest-palindromic-substring/) · [notes](notes.md#5-longest-palindromic-substring) | M | 1 (1/0/0) | 2025-07-23 | expand around centre (odd & even) |
| 62 | [Unique Paths](https://leetcode.com/problems/unique-paths/) · [notes](notes.md#62-unique-paths) | M | 2 (2/0/0) | 2026-05-21 | paths(x,y)=right+down |
| 63 | [Unique Paths II](https://leetcode.com/problems/unique-paths-ii/) · [notes](notes.md#63-unique-paths-ii) ✍️ | M | 3 (1/2/0) | 2026-07-20 | same with obstacle check incl. start/end |
| 64 | [Minimum Path Sum](https://leetcode.com/problems/minimum-path-sum/) · [notes](notes.md#64-minimum-path-sum) | M | 1 (1/0/0) | 2025-07-19 | dp from top-left, min(up,left)+grid |
| 72 | [Edit Distance](https://leetcode.com/problems/edit-distance/) · [notes](notes.md#72-edit-distance) ⚠️ ✍️ | M | 8 (5/3/0) | 2026-09-22 | match → (x-1,y-1); else 1+min(replace,delete,insert); base = other length+1 |
| 120 | [Triangle](https://leetcode.com/problems/triangle/) · [notes](notes.md#120-triangle) ✍️ | M | 2 (1/0/1) | 2026-07-06 | top-down min(x+1,y),(x+1,y+1) |
| 518 | [Coin Change II](https://leetcode.com/problems/coin-change-ii/) · [notes](notes.md#518-coin-change-ii) ✍️ | M | 2 (1/1/0) | 2026-07-23 | combinations: include (stay) + exclude (x-1) |
| 1105 | [Filling Bookcase Shelves](https://leetcode.com/problems/filling-bookcase-shelves/) · [notes](notes.md#1105-filling-bookcase-shelves) | M | 1 (1/0/0) | 2025-05-19 | for each book: same shelf (width left, max h) vs new shelf |
| 1143 | [Longest Common Subsequence](https://leetcode.com/problems/longest-common-subsequence/) · [notes](notes.md#1143-longest-common-subsequence) ✍️ | M | 4 (2/0/1) | 2026-05-27 | match → 1+lcs(x-1,y-1); else max(lcs(x,y-1),lcs(x-1,y)) |

## Greedy / Intervals

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 56 | [Merge Intervals](https://leetcode.com/problems/merge-intervals/) · [notes](notes.md#56-merge-intervals) | M | 1 (1/0/0) | 2025-06-06 | sort by start; merge if last.end ≥ start |
| 135 | [Candy](https://leetcode.com/problems/candy/) · [notes](notes.md#135-candy) | H | 1 (1/0/0) | 2025-02-21 | two passes L→R and R→L, take max |
| 334 | [Increasing Triplet Subsequence](https://leetcode.com/problems/increasing-triplet-subsequence/) · [notes](notes.md#334-increasing-triplet-subsequence) ⚠️ ✍️ | M | 7 (2/5/0) | 2026-05-27 | min1, min2 running; else found |
| 435 | [Non-overlapping Intervals](https://leetcode.com/problems/non-overlapping-intervals/) · [notes](notes.md#435-non-overlapping-intervals) ✍️ | M | 5 (3/2/0) | 2026-09-22 | sort by END; keep if start ≥ prevEnd; answer n-kept |
| 452 | [Minimum Number of Arrows to Burst Balloons](https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/) · [notes](notes.md#452-minimum-number-of-arrows-to-burst-balloons) ⚠️ ✍️ | M | 8 (3/5/0) | 2026-07-23 | sort by end (or start + shrink); new arrow when start > curEnd |
| 605 | [Can Place Flowers](https://leetcode.com/problems/can-place-flowers/) · [notes](notes.md#605-can-place-flowers) ⚠️ ✍️ | E | 5 (2/3/0) | 2026-05-25 | plant greedily when both neighbours empty (out of bounds = empty) |
| 649 | [Dota2 Senate](https://leetcode.com/problems/dota2-senate/) · [notes](notes.md#649-dota2-senate) ⚠️ ✍️ | M | 10 (1/9/0) | 2025-04-23 | two queues of indices; winner re-queued with idx+n |

## Math & Bits

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 7 | [Reverse Integer](https://leetcode.com/problems/reverse-integer/) · [notes](notes.md#7-reverse-integer) ✍️ | M | 3 (1/2/0) | 2026-07-23 | pop digits; overflow check before push (or long) |
| 9 | [Palindrome Number](https://leetcode.com/problems/palindrome-number/) · [notes](notes.md#9-palindrome-number) | E | 1 (1/0/0) | 2025-07-26 | reverse and compare; negatives false |
| 12 | [Integer to Roman](https://leetcode.com/problems/integer-to-roman/) · [notes](notes.md#12-integer-to-roman) ✍️ | M | 1 (1/0/0) | 2026-07-02 | greedy over 13 values desc (incl. 900,400,90,40,9,4) |
| 13 | [Roman to Integer](https://leetcode.com/problems/roman-to-integer/) · [notes](notes.md#13-roman-to-integer) ✍️ | E | 2 (2/0/0) | 2026-07-23 | right→left; subtract if smaller than prev |
| 50 | [Pow(x, n)](https://leetcode.com/problems/powx-n/) · [notes](notes.md#50-powx-n) ✍️ | M | 2 (1/0/0) | 2026-07-20 | fast pow; negative n; n/2 recursion |
| 136 | [Single Number](https://leetcode.com/problems/single-number/) · [notes](notes.md#136-single-number) | E | 3 (3/0/0) | 2026-07-23 | XOR everything |
| 172 | [Factorial Trailing Zeroes](https://leetcode.com/problems/factorial-trailing-zeroes/) · [notes](notes.md#172-factorial-trailing-zeroes) ✍️ | M | 1 (1/0/0) | 2026-07-04 | n/5+n/25+… |
| 190 | [Reverse Bits](https://leetcode.com/problems/reverse-bits/) · [notes](notes.md#190-reverse-bits) ✍️ | E | 1 (1/0/0) | 2026-08-16 | 32 iterations: res<<=1 | (n&1); n>>=1 |
| 191 | [Number of 1 Bits](https://leetcode.com/problems/number-of-1-bits/) · [notes](notes.md#191-number-of-1-bits) | E | 1 (1/0/0) | 2025-07-25 | count n&1; shift |
| 201 | [Bitwise AND of Numbers Range](https://leetcode.com/problems/bitwise-and-of-numbers-range/) · [notes](notes.md#201-bitwise-and-of-numbers-range) ✍️ | M | 1 (1/0/0) | 2026-07-02 | common prefix: shift right until equal, shift back |
| 202 | [Happy Number](https://leetcode.com/problems/happy-number/) · [notes](notes.md#202-happy-number) | E | 1 (1/0/0) | 2025-08-12 | set cycle detect (or Floyd) |
| 338 | [Counting Bits](https://leetcode.com/problems/counting-bits/) · [notes](notes.md#338-counting-bits) ✍️ | E | 5 (4/0/0) | 2026-05-26 | dp[i]=dp[i>>1]+(i&1) |
| 1071 | [Greatest Common Divisor of Strings](https://leetcode.com/problems/greatest-common-divisor-of-strings/) · [notes](notes.md#1071-greatest-common-divisor-of-strings) ✍️ | E | 3 (2/1/0) | 2026-05-28 | s+t==t+s ? prefix of length gcd(len) : '' |
| 1318 | [Minimum Flips to Make a OR b Equal to c](https://leetcode.com/problems/minimum-flips-to-make-a-or-b-equal-to-c/) · [notes](notes.md#1318-minimum-flips-to-make-a-or-b-equal-to-c) | M | 3 (2/1/0) | 2026-05-20 | per bit: c=1 & both 0 → 1; c=0 → count of set bits in a,b |
| 2571 | [Minimum Operations to Reduce an Integer to 0](https://leetcode.com/problems/minimum-operations-to-reduce-an-integer-to-0/) · [notes](notes.md#2571-minimum-operations-to-reduce-an-integer-to-0) ⚠️ | M | 6 (2/1/0) | 2025-05-19 | nearest power of 2 above/below; recurse min |

## Strings / Simulation

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 6 | [Zigzag Conversion](https://leetcode.com/problems/zigzag-conversion/) · [notes](notes.md#6-zigzag-conversion) ✍️ | M | 3 (2/0/0) | 2026-07-23 | row pointer bounce; numRows==1 edge |
| 28 | [Find the Index of the First Occurrence in a String](https://leetcode.com/problems/find-the-index-of-the-first-occurrence-in-a-string/) · [notes](notes.md#28-find-the-index-of-the-first-occurrence-in-a-string) ⚠️ | E | 10 (3/4/1) | 2018-05-09 | KMP lps |
| 43 | [Multiply Strings](https://leetcode.com/problems/multiply-strings/) · [notes](notes.md#43-multiply-strings) | M | 2 (1/1/0) | 2025-04-27 | schoolbook per digit rows, then sum with carry; strip leading zeros |
| 67 | [Add Binary](https://leetcode.com/problems/add-binary/) · [notes](notes.md#67-add-binary) | E | 1 (1/0/0) | 2025-07-25 | reverse, add with carry |
| 214 | [Shortest Palindrome](https://leetcode.com/problems/shortest-palindrome/) · [notes](notes.md#214-shortest-palindrome) | H | 2 (1/1/0) | 2017-11-19 | KMP on s+'#'+rev(s); prepend unmatched suffix reversed |

## Design

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 146 | [LRU Cache](https://leetcode.com/problems/lru-cache/) · [notes](notes.md#146-lru-cache) ⚠️ | M | 5 (2/3/0) | 2025-05-24 | HashMap + doubly linked list with sentinels; get moves to tail |
| 362 | [Design Hit Counter](https://leetcode.com/problems/design-hit-counter/) · [notes](notes.md#362-design-hit-counter) | M | 1 (1/0/0) | 2025-04-13 | queue; evict ≤ t-300 |
| 933 | [Number of Recent Calls](https://leetcode.com/problems/number-of-recent-calls/) · [notes](notes.md#933-number-of-recent-calls) ✍️ | E | 2 (2/0/0) | 2026-05-29 | queue; evict < t-3000 |
| 981 | [Time Based Key-Value Store](https://leetcode.com/problems/time-based-key-value-store/) · [notes](notes.md#981-time-based-key-value-store) ✍️ | M | 2 (1/0/0) | 2026-08-16 | map key → TreeMap/TreeSet by timestamp; floor() |

## Concurrency

| # | Problem | Diff | Attempts | Last AC | Crux |
|---|---|---|---|---|---|
| 1114 | [Print in Order](https://leetcode.com/problems/print-in-order/) · [notes](notes.md#1114-print-in-order) | E | 3 (1/0/1) | 2025-04-03 | lock + 2 conditions + flags; while-loop await |
| 1115 | [Print FooBar Alternately](https://leetcode.com/problems/print-foobar-alternately/) · [notes](notes.md#1115-print-foobar-alternately) | M | 1 (1/0/0) | 2025-04-03 | lock + isFoo flag + 2 conditions |
| 1116 | [Print Zero Even Odd](https://leetcode.com/problems/print-zero-even-odd/) · [notes](notes.md#1116-print-zero-even-odd) | M | 2 (2/0/0) | 2025-04-03 | lock + isZero/isOdd flags + 3 conditions |
| 1117 | [Building H2O](https://leetcode.com/problems/building-h2o/) · [notes](notes.md#1117-building-h2o) | M | 1 (1/0/0) | 2025-04-03 | lock + counts; reset at 2H+1O |
| 1226 | [The Dining Philosophers](https://leetcode.com/problems/the-dining-philosophers/) · [notes](notes.md#1226-the-dining-philosophers) | M | 1 (1/0/0) | 2025-04-04 | one global lock (simple, serialised) |

## ⚠️ Re-drill list (by pain)

| # | Problem | Subs | WA | TLE | Other | Pattern |
|---|---|---|---|---|---|---|
| 2542 | [Maximum Subsequence Score](notes.md#2542-maximum-subsequence-score) | 13 | 10 | 0 | 2 | Heap |
| 649 | [Dota2 Senate](notes.md#649-dota2-senate) | 10 | 9 | 0 | 0 | Greedy / Intervals |
| 790 | [Domino and Tromino Tiling](notes.md#790-domino-and-tromino-tiling) | 8 | 6 | 0 | 1 | 1-D DP |
| 437 | [Path Sum III](notes.md#437-path-sum-iii) | 11 | 5 | 0 | 2 | Trees |
| 28 | [Find the Index of the First Occurrence in a String](notes.md#28-find-the-index-of-the-first-occurrence-in-a-string) | 10 | 4 | 1 | 2 | Strings / Simulation |
| 452 | [Minimum Number of Arrows to Burst Balloons](notes.md#452-minimum-number-of-arrows-to-burst-balloons) | 8 | 5 | 0 | 0 | Greedy / Intervals |
| 1926 | [Nearest Exit from Entrance in Maze](notes.md#1926-nearest-exit-from-entrance-in-maze) | 8 | 1 | 4 | 0 | Graphs |
| 334 | [Increasing Triplet Subsequence](notes.md#334-increasing-triplet-subsequence) | 7 | 5 | 0 | 0 | Greedy / Intervals |
| 1497 | [Check If Array Pairs Are Divisible by k](notes.md#1497-check-if-array-pairs-are-divisible-by-k) | 6 | 5 | 0 | 0 | Arrays & Hashing |
| 153 | [Find Minimum in Rotated Sorted Array](notes.md#153-find-minimum-in-rotated-sorted-array) | 9 | 4 | 0 | 0 | Binary Search |
| 643 | [Maximum Average Subarray I](notes.md#643-maximum-average-subarray-i) | 7 | 4 | 0 | 0 | Sliding Window |
| 735 | [Asteroid Collision](notes.md#735-asteroid-collision) | 7 | 4 | 0 | 0 | Stack |
| 124 | [Binary Tree Maximum Path Sum](notes.md#124-binary-tree-maximum-path-sum) | 6 | 4 | 0 | 0 | Trees |
| 130 | [Surrounded Regions](notes.md#130-surrounded-regions) | 5 | 4 | 0 | 0 | Graphs |
| 162 | [Find Peak Element](notes.md#162-find-peak-element) | 10 | 3 | 0 | 4 | Binary Search |
| 72 | [Edit Distance](notes.md#72-edit-distance) | 8 | 3 | 0 | 0 | 2-D DP |
| 1268 | [Search Suggestions System](notes.md#1268-search-suggestions-system) | 7 | 3 | 0 | 2 | Trie |
| 450 | [Delete Node in a BST](notes.md#450-delete-node-in-a-bst) | 6 | 3 | 0 | 0 | BST |
| 110 | [Balanced Binary Tree](notes.md#110-balanced-binary-tree) | 5 | 3 | 0 | 1 | Trees |
| 146 | [LRU Cache](notes.md#146-lru-cache) | 5 | 3 | 0 | 0 | Design |
| 605 | [Can Place Flowers](notes.md#605-can-place-flowers) | 5 | 3 | 0 | 0 | Greedy / Intervals |
| 901 | [Online Stock Span](notes.md#901-online-stock-span) | 5 | 3 | 0 | 0 | Monotonic Stack |
| 994 | [Rotting Oranges](notes.md#994-rotting-oranges) | 5 | 3 | 0 | 0 | Graphs |
| 1657 | [Determine if Two Strings Are Close](notes.md#1657-determine-if-two-strings-are-close) | 5 | 3 | 0 | 0 | Arrays & Hashing |
| 2462 | [Total Cost to Hire K Workers](notes.md#2462-total-cost-to-hire-k-workers) | 5 | 3 | 0 | 0 | Heap |
| 872 | [Leaf-Similar Trees](notes.md#872-leaf-similar-trees) | 6 | 2 | 0 | 0 | Trees |
| 1004 | [Max Consecutive Ones III](notes.md#1004-max-consecutive-ones-iii) | 6 | 2 | 0 | 0 | Sliding Window |
| 127 | [Word Ladder](notes.md#127-word-ladder) | 4 | 0 | 2 | 0 | Graphs |
| 374 | [Guess Number Higher or Lower](notes.md#374-guess-number-higher-or-lower) | 4 | 0 | 2 | 0 | Binary Search |
| 1372 | [Longest ZigZag Path in a Binary Tree](notes.md#1372-longest-zigzag-path-in-a-binary-tree) | 4 | 0 | 2 | 0 | Trees |
| 328 | [Odd Even Linked List](notes.md#328-odd-even-linked-list) | 7 | 1 | 0 | 3 | Linked List |
| 2571 | [Minimum Operations to Reduce an Integer to 0](notes.md#2571-minimum-operations-to-reduce-an-integer-to-0) | 6 | 1 | 0 | 3 | Math & Bits |
| 216 | [Combination Sum III](notes.md#216-combination-sum-iii) | 7 | 0 | 0 | 0 | Backtracking |

## Coverage by pattern

| Pattern | Solved | Easy/Med/Hard |
|---|---|---|
| Arrays & Hashing | 20 | 12/8/0 |
| Prefix Sum | 4 | 2/2/0 |
| Two Pointers | 13 | 7/5/1 |
| Sliding Window | 8 | 1/5/2 |
| Stack | 6 | 1/5/0 |
| Monotonic Stack | 4 | 0/2/2 |
| Binary Search | 9 | 2/7/0 |
| Linked List | 13 | 3/9/1 |
| Trees | 20 | 7/12/1 |
| BST | 7 | 2/5/0 |
| Trie | 3 | 0/2/1 |
| Heap | 7 | 0/5/2 |
| Backtracking | 10 | 0/7/3 |
| Graphs | 14 | 0/13/1 |
| 1-D DP | 15 | 4/11/0 |
| 2-D DP | 9 | 0/9/0 |
| Greedy / Intervals | 7 | 1/5/1 |
| Math & Bits | 15 | 8/7/0 |
| Strings / Simulation | 5 | 2/2/1 |
| Design | 4 | 1/3/0 |
| Concurrency | 5 | 1/4/0 |
