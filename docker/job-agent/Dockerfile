FROM eclipse-temurin:17-jre

# 安装 Python 3 + wget（DataX 依赖）
RUN apt-get update && \
    apt-get install -y python3 python3-pip wget && \
    ln -sf /usr/bin/python3 /usr/bin/python && \
    rm -rf /var/lib/apt/lists/*

# 下载并安装 DataX（阿里云 OSS，国内快）
# 如果下载失败，手动下载 datax.tar.gz 放到 docker/job-agent/ 目录再 build
RUN wget -q --show-progress -O /tmp/datax.tar.gz \
        https://datax-opensource.oss-cn-hangzhou.aliyuncs.com/202309/datax.tar.gz && \
    mkdir -p /opt/datax && \
    tar -xzf /tmp/datax.tar.gz -C /opt/datax --strip-components=1 && \
    rm /tmp/datax.tar.gz && \
    echo "DataX installed at /opt/datax"

# 安装 MinIO Client (mc) — DataX 完成后上传 staging 至 MinIO
# 国内网络可能无法下载，失败不阻塞构建
RUN (  wget -q --tries=3 --timeout=30 -O /usr/local/bin/mc \
        https://github.com/minio/mc/releases/latest/download/mc \
    || wget -q --tries=3 --timeout=30 -O /usr/local/bin/mc \
        https://dl.min.io/client/mc/release/linux-amd64/mc \
    || wget -q --tries=3 --timeout=30 -O /usr/local/bin/mc \
        https://dl.min.io/client/mc/release/linux-amd64/archive/mc.RELEASE.2024-08-13T09-46-16Z \
    ) && chmod +x /usr/local/bin/mc && echo "MinIO Client (mc) installed" \
    || echo "WARNING: mc download failed, MinIO upload will be disabled. Install mc manually: https://min.io/docs/minio/linux/reference/minio-mc.html"

WORKDIR /app

# 复制 Agent jar
COPY relake-job-agent/target/relake-job-agent-*.jar /app/job-agent.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/app/job-agent.jar"]
