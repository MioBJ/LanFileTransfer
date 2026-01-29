# ============================================
# 局域网文件传输服务 - Dockerfile
# ============================================

# 第一阶段：基础镜像
# 使用官方Python 3.9-slim镜像（体积更小）
FROM python:3.9-slim as base

# 设置工作目录
WORKDIR /app

# 设置环境变量
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# 安装系统依赖（编译psutil等包需要gcc）
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    python3-dev \
    && rm -rf /var/lib/apt/lists/*

# ============================================
# 第二阶段：安装依赖
# ============================================

FROM base as dependencies

# 复制依赖文件
COPY requirements.txt .

# 安装Python依赖
RUN pip install --no-cache-dir -r requirements.txt

# ============================================
# 第三阶段：运行环境
# ============================================

FROM base as runtime

# 从dependencies阶段复制已安装的包
COPY --from=dependencies /usr/local/lib/python3.9/site-packages /usr/local/lib/python3.9/site-packages
COPY --from=dependencies /usr/local/bin /usr/local/bin

# 复制应用代码
COPY app /app/app

# 创建必要的目录
RUN mkdir -p /app/storage/uploads \
    /app/storage/temp \
    /app/logs

# 设置目录权限
RUN chmod -R 755 /app/storage /app/logs

# 暴露端口
EXPOSE 8000

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD python -c "import requests; requests.get('http://localhost:8000/api/health', timeout=5)"

# 启动命令
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]

