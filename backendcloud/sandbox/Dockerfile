# Bot 代码沙箱：go-judge 官方镜像（基于 Debian，支持 amd64 / arm64）+ 四种语言的编译器
# 运行时必须加 --privileged 和 --cgroupns=host（go-judge 要创建隔离环境和子 cgroup），
# 且端口只能绑定到本机，不能暴露到公网：
#   docker build -t kob-sandbox backendcloud/sandbox
#   docker run -d --name kob-sandbox --privileged --cgroupns=host --shm-size=256m -p 127.0.0.1:5050:5050 kob-sandbox
FROM criyle/go-judge:v1.13.0

RUN apt-get update \
    && apt-get install -y --no-install-recommends g++ openjdk-21-jdk-headless python3 golang-go \
    && rm -rf /var/lib/apt/lists/*

# 预编译 Bot 常用的 Go 标准库包。沙箱每次都是全新环境，没有这份缓存的话每次编译都要从头编译标准库（5~6 秒）
# 包不要加太多：编译时要整个复制进沙箱的 /tmp（上限 128MB）。环境变量要和 Sandbox.java 里的一致，否则缓存对不上
RUN GOCACHE=/opt/gocache GOTOOLCHAIN=local CGO_ENABLED=0 go build \
        bufio container/heap container/list errors fmt maps math math/bits math/rand \
        os slices sort strconv strings time unicode \
    && chmod -R a+rX /opt/gocache

# 官方 mount.yaml 加上 /opt/gocache 的只读挂载
COPY mount.yaml /opt/mount.yaml
