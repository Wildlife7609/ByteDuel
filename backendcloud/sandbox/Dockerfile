# Bot code sandbox: the official go-judge image (Debian-based, amd64 / arm64) plus compilers for the four languages
# It must run with --privileged and --cgroupns=host (go-judge creates isolated environments and child cgroups),
# and its port must be bound to localhost, never exposed to the internet:
#   docker build -t kob-sandbox backendcloud/sandbox
#   docker run -d --name kob-sandbox --privileged --cgroupns=host --shm-size=256m -p 127.0.0.1:5050:5050 kob-sandbox
FROM criyle/go-judge:v1.13.3

RUN apt-get update \
    && apt-get install -y --no-install-recommends g++ openjdk-21-jdk-headless python3 golang-go \
    && rm -rf /var/lib/apt/lists/*

# Precompile the Go standard library packages bots commonly use. Each sandbox starts fresh, and without this cache every build recompiles the standard library (5-6 seconds)
# Keep the list short: the whole cache is copied into the sandbox's /tmp (128MB limit) on every build. The environment variables must match Sandbox.java, or the cache will not be used
RUN GOCACHE=/opt/gocache GOTOOLCHAIN=local CGO_ENABLED=0 go build \
        bufio container/heap container/list errors fmt maps math math/bits math/rand \
        os slices sort strconv strings time unicode \
    && chmod -R a+rX /opt/gocache

# The official mount.yaml plus a read-only mount of /opt/gocache
COPY mount.yaml /opt/mount.yaml
