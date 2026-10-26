# ByteDuel local measurements — 2026-10-26

Commit: eaf6caa. Host: Intel Core i5-8259U, 16 GiB RAM, 8 logical CPUs, macOS x86-64; Java 25.0.4. Sandbox: Docker Desktop Linux amd64, go-judge v1.13.3, Debian Go 1.24.4. Tests use an isolated Docker Compose project and database. Results are local observations, not production traffic measurements.

## Go standard-library build cache

`sandbox_bench.py` calls go-judge directly, with the project's Go starter template and the same compiler options and limits. Each call starts a fresh isolated workspace and runs a real compile; the resulting executable is deleted after each run. In the cold case the Go build cache starts empty; in the prebuilt case the command first copies `/opt/gocache` into the writable cache. Wall time includes the go-judge HTTP call and the cache copy. Ten consecutive cold runs followed by ten prebuilt runs; no randomized interleaving or separate warm-up. All 20 builds succeeded.

Cold median 5267.13 ms; prebuilt median 256.04 ms (95.1% lower). See `sandbox-results.txt` for individual values. This is not a measurement of end-to-end game start or of the original production host, and the run order may affect results.

## Compiled bot cache

`cache_bench.py` sends ten pairs of internal compile requests to botrunningsystem. Each pair uses a unique Go program (a changed comment) for a cache miss and then repeats the identical code for a hit. The prebuilt Go standard library cache is active in both cases. Request wall time includes the network and cache-presence check. All returned success. Median miss 287.14 ms, median hit 8.49 ms. Individual values are in `cache-results.txt`; this is not turn latency or user-facing save latency.

## Map connectivity

`MapBench.java` reproduces the old DFS from parent of f62423b and current BFS, using the same map generation logic with seeded Java Random. The harness adds a time check to DFS every 4096 calls and a nominal 100 ms cutoff; checks add overhead. BFS is warmed up with 1000 maps. In 200 fixed-seed maps, 8 were disconnected. BFS completed all 200 (median 0.007 ms, p95 0.019 ms, max 0.066 ms); DFS timed out on 4 and completed 196 (completed-only median 0.002 ms, p95 2.439 ms, max 94.801 ms). All 196 completed DFS results matched BFS. The two medians are not directly comparable because the timed-out DFS cases are excluded. This tests connectivity alone, not total map generation or matchmaking time.

## Bot execution and checks

Ten sandbox requests per language, each executing an already compiled minimal program that prints 0. Median request wall times: C++ 6.80 ms, Java 39.92 ms, Python 19.10 ms, Go 7.62 ms. These do not measure realistic bot strategies, queueing, or an entire game turn. The sandbox reported a syntax error for malformed Python, Time Limit Exceeded for an infinite loop, and Memory Limit Exceeded for a 700 MiB allocation with the configured 512 MiB limit.

`integration-results.txt` records the isolated frontend/API checks: homepage, registration and login, valid bot creation, invalid Python rejection, an unauthenticated request to an internal endpoint, and six failed login attempts. The sixth received 429. The first integration attempt failed because the test JWT secret was incorrectly generated as URL-safe Base64; the temporary configuration was corrected before the recorded run. These checks are not a full security audit or a full browser game test.

## Reproduction

Compile and run the algorithm harness: `javac MapBench.java && java MapBench` from this directory. It reproduces algorithm logic, not the entire application.

The other Python scripts are designed to run **inside the sandbox container** of an isolated Compose deployment, via `docker compose exec -T sandbox python3 - < sandbox_bench.py` and similarly for `integration.py`. They directly invoke the sandbox or write test users/bots; use a disposable environment, not production. `cache_bench.py` requires a preceding `TOKEN = ...` assignment containing that test environment's internal service token, passed on stdin without recording it in source or logs. These scripts are experiment harnesses, not an automated comprehensive test suite. The `sandbox_bench.py` file embeds the Go template as it existed at this commit.
