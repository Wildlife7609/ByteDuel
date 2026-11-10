# ByteDuel measurements — 2026-11-08

Source commit: `365778555caa7ee80b18ffff77567831612995f8` (the working tree also contained README changes). Host: Intel Core i5-8259U, 16 GiB RAM, 8 logical CPUs, macOS x86-64, Java 25.0.4. Docker Desktop's Linux x86-64 sandbox used go-judge v1.13.3, Go 1.24.4, g++ 14.2.0, OpenJDK 21.0.12.1 and Python 3.13.5. Images were built from this checkout: sandbox `sha256:033fc0068f44d742d9c0e7de8ef1b46c9fea08c1dec05c5e3dba636a8256f62c`, bot service `sha256:e710e905fc6ebd526dfa27d73508ae628e353b08b11d37413525da7eadd40592`. Results are local observations, not production latency or throughput.

## Production counts

The public ranklist and records APIs returned **12 accounts** and **126 saved matches**, respectively, at the UTC time in [production-counts.json](production-counts.json). [production_counts.py](production_counts.py) reproduces the read-only query and saves only the totals, not account or match details. Totals can change and do not distinguish test accounts or test matches.

## Compiled bot cache

[cache_bench.py](cache_bench.py) sends ten pairs of internal compile requests for each language to a freshly started, isolated bot service and sandbox. Each pair contains a unique minimal valid program (a comment includes a run ID and pair number): the first request misses the source cache and the second repeats the identical source and hits it. The 40 language pairs are shuffled with seed 20261108; each miss is immediately followed by its hit. Go's prebuilt standard-library cache is active on both. There is no separate warm-up; request time includes network and checking cached-file presence in go-judge. All 80 requests succeeded. Each request and its response is in [cache-results.jsonl](cache-results.jsonl).

| Language | Miss median | Hit median | Ratio of medians |
|---|---:|---:|---:|
| C++ | 405.762 ms | 6.696 ms | 60.60× |
| Java | 783.885 ms | 7.619 ms | 102.89× |
| Python | 56.746 ms | 7.309 ms | 7.76× |
| Go | 272.686 ms | 7.552 ms | 36.11× |

These are ratios of median request times, not individual speedups or user-facing save latency. Programs are minimal and measurements can vary with hardware, workload and system load.

## Go standard-library cache

The [sandbox benchmark script](../2026-10-26/sandbox_bench.py) makes ten cold Go compiles followed by ten compiles with the image's prebuilt Go cache, using the same starter-template source and project compiler options. Each compile runs in a fresh sandbox workspace; the measured request includes the cache copy in the prebuilt case. All 20 compiles succeeded. Median cold time was **4,323.84 ms**, versus **245.69 ms** with the prebuilt cache, a **94.3% reduction**. The runs were not interleaved or separately warmed up, so run order may affect results. Per-run values and statuses are in [sandbox-results.txt](sandbox-results.txt); the same file also records minimal bot execution timings and sandbox limit checks. These are not end-to-end game-start or production measurements.

## Map connectivity

[MapBench.java](MapBench.java) reproduces the earlier DFS and current BFS logic, including map generation. It warms up BFS on 1,000 generated maps, then tests the same 200 fixed-seed maps used in the previous benchmark. Each map's result and time is in [map-results.txt](map-results.txt). BFS completed all 200, with median 0.012 ms, p95 0.015 ms and maximum 0.033 ms. Of the maps, 8 were disconnected; the old DFS reached a nominal 100 ms cutoff on 6. All 194 completed DFS results matched BFS. DFS has an added deadline check every 4,096 calls, which adds overhead, and its completed-only timing excludes timeouts. This measures connectivity checks, not entire game startup. The prior report of an up-to-20-second stall is a historical observation and was not reproduced by this cutoff-based test.

## Upstream ARM64 issue

[go-judge issue #202](https://github.com/criyle/go-judge/issues/202) was filed by Wildlife7609 and documents HTTP 500 errors on aarch64 caused by the seccomp handling of `newfstatat`. The [v1.13.3 release notes](https://github.com/criyle/go-judge/releases/tag/v1.13.3) list fixes for the issue. This run used x86-64 and did not independently reproduce the ARM64 failure.

## Reproduction

Build `sandbox` and `botrunningsystem` from the current checkout, then start them in a separate Docker Compose project with disposable environment secrets and no published ports. Do not point the test at an existing sandbox: the bot service deletes sandbox cached files during startup. Once both services are ready, run the cache script inside the sandbox container, supplying that test environment's `INTERNAL_TOKEN` as an environment variable. The Go/sandbox test uses [the previous benchmark script](../2026-10-26/sandbox_bench.py) inside the sandbox container. Both scripts send real compile requests. For the map benchmark, run `javac -d /tmp/byteduel-map MapBench.java && java -cp /tmp/byteduel-map MapBench` from this directory. Measurements are machine-dependent.
