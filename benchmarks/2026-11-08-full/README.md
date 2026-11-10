# Complete retest — 2026-11-08

Source commit `365778555caa7ee80b18ffff77567831612995f8` with uncommitted documentation and benchmark files; application source was unchanged. Host: Intel Core i5-8259U, 16 GiB, 8 logical CPUs, macOS x86-64, Java 25.0.4. Docker Desktop Linux x86-64; sandbox: go-judge v1.13.3, Go 1.24.4, g++ 14.2.0, OpenJDK 21.0.12.1, Python 3.13.5. PostgreSQL 18. Docker Compose built the current source in a dedicated project with a fresh database and separate ports. Timings are local wall-clock observations and are not production latency or guarantees.

Docker image IDs: sandbox `sha256:6d31ba1a1682b5e45aa4603213fa52faa1e5e7247aee7b0a249681ae064c9f40`; bot service `sha256:08832391878a80595efbc1d930f5577af0907b41dd90a3cb4d2f6741ac18dc0e`; backend `sha256:4fcf04065f032427450731d01966ddcbf2b32b32f24b5c2a95473c24d60feaec`; matching `sha256:110d2a316f76edf53010a50bad24e2ee66e4d68f02ea2c987478cc7320d3e69c`; web `sha256:6dc12e12e53461082d70f6c5bfbc50245b47ae6f75b7d8521c1db51bbb17230c`; postgres `sha256:edb0c08dc33befd0262ab3b5cf1c4f3f8cfd8b09e88c05b45adee29d6761ddb6`.

## Full-stack integration

[integration.py](integration.py) and [integration-results.jsonl](integration-results.jsonl) record a homepage request, two registrations and logins, saving minimal bots in all four languages, invalid Python rejection, blocking an external internal-endpoint request, and two WebSocket games (C++ versus Java, Python versus Go). Both clients received match, move, and result events in each game; the public records API showed the isolated database's count increasing from zero to two. These games use constant-output bots, not all starter templates or a browser rendering test. No production accounts or matches were created.

## Production counts

The read-only public ranklist and record APIs returned **12 accounts and 126 saved matches**; [production-counts.json](production-counts.json) includes the UTC timestamp and URLs. [production_counts.py](production_counts.py) saves only totals, not individual account or match data. Counts can change and may include test accounts and games.

## Compiled output cache

[cache_bench.py](cache_bench.py) sends ten pairs of internal compile-check requests per language using minimal valid programs. Each pair has a unique source: the first request misses and the immediately repeated request hits the language-and-source cache. The 40 language pairs are shuffled using seed 20261108. The Go standard-library cache is enabled on misses and hits. There is no separate warm-up; requests include network and cached-file-presence checks. All 80 requests succeeded; every request, response, timing and the summary are in [cache-results.jsonl](cache-results.jsonl).

| Language | Miss median | Hit median | Ratio of medians |
|---|---:|---:|---:|
| C++ | 399.245 ms | 6.448 ms | 61.91× |
| Java | 781.326 ms | 6.171 ms | 126.62× |
| Python | 52.678 ms | 5.814 ms | 9.06× |
| Go | 256.292 ms | 6.495 ms | 39.46× |

Python's cached output is its syntax-checked source, not a compiled binary. The ratios compare medians, not individual requests, and do not measure user-facing save latency. They vary by machine and system load; the [earlier run](../2026-11-08/README.md#compiled-bot-cache) produced different ratios.

## Go standard-library cache

[sandbox_bench.py](sandbox_bench.py) uses the Go starter template embedded in the 2026-10-26 script and the project's compiler options. It performs ten cold compiles followed by ten compiles with the prebuilt standard-library cache in fresh sandbox workspaces. All 20 succeeded. The cold median was **4,644.41 ms (4.644 s)** and the prebuilt median was **249.75 ms (0.250 s)**, a **94.6% reduction**. The timed prebuilt request includes copying the cache. Runs were not interleaved or separately warmed up; ordering may affect results. Every duration and status is in [sandbox-results.txt](sandbox-results.txt), which also records minimal execution tests for all four languages and sandbox syntax, CPU-time and memory-limit checks. These are not game-start timings. An earlier observation of approximately 6 s to 0.9 s used different conditions and is not a result of this retest; the [previous recorded run](../2026-11-08/README.md#go-standard-library-cache) measured 4.324 s to 0.246 s.

## Map connectivity

[MapBench.java](MapBench.java) reproduces old DFS and current BFS with the map generation logic. After warming BFS with 1,000 maps, it checks 200 maps using fixed seed 20261026. Every result is in [map-results.txt](map-results.txt). BFS median was **0.008 ms**, p95 **0.027 ms**, max **0.111 ms**: 199 of 200 were below 0.1 ms, so this run does **not** support saying every check was below 0.1 ms. Four old DFS checks hit a nominal 100 ms cutoff; the remaining 196 matched BFS. Deadline checks are injected into DFS every 4,096 calls and add overhead. These measure connectivity, not complete game startup. The prior run's maximum was 0.033 ms; both observed values are retained rather than treated as identical.

[SlowMapBench.java](SlowMapBench.java) reruns six fixed-seed maps that had previously exceeded 100 ms, using a nominal 30-second DFS cutoff. On case 52 DFS returned in 10,889.244 ms; on case 172 it timed out at 30,054.601 ms. See [slow-map-results.txt](slow-map-results.txt) for all six. This shows that the reproduced old algorithm can run for over 20 seconds on a map; it does not independently establish the exact duration of any historical game-start stall. Deadline instrumentation adds overhead, and other integration work ran concurrently during some of these trials.

## ARM64 upstream issue

The upstream [issue #202](https://github.com/criyle/go-judge/issues/202), filed by Wildlife7609, documents ARM64 HTTP 500 errors involving `newfstatat`; the [v1.13.3 release notes](https://github.com/criyle/go-judge/releases/tag/v1.13.3) list the associated fixes. The local Docker environment is x86-64, so this run did not independently reproduce the ARM64 bug.

## Reproduction

Use a fresh, uniquely named Docker Compose project and disposable database/JWT/internal secrets. Set unused local HTTP/HTTPS ports, then run `docker compose -p <project> up -d --build`. Never reuse a production sandbox: the bot service clears its cached files on startup. `integration.py` requires Python's `websockets` package and defaults to localhost port 18081; it creates two accounts and matches in the disposable database. Run `cache_bench.py` inside the sandbox container with the test `INTERNAL_TOKEN` in its environment. Run `sandbox_bench.py` inside the sandbox container. Compile `MapBench.java` and `SlowMapBench.java` to a temporary directory, then run `MapBench` and `SlowMapBench <case>`. Run `production_counts.py` to query public totals. Stop the disposable Compose project afterward; only remove its volumes if they contain no data you need.
