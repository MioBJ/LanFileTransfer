"""
应用配置管理模块 

该模块使用Pydantic的BaseSettings来管理应用配置
支持从环境变量和.env文件中读取配置
"""

import os
from pydantic_settings import BaseSettings # Pydantic的配置管理基类  # pyright: ignore[reportMissingImports]
from typing import Optional # 类型提示，用于可选参数

# 配置类定义

class Settings(BaseSettings):
    """
    应用配置类

    功能说明：
    - 继承自BaseSettings，自动读取环境变量
    - 提供所有配置项的默认值
    - 支持类型验证
    
    类比理解：
    - 类似iOS中的配置管理单例类
    - 类似Java中的@ConfigurationProperties
    
    使用方式：
    settings = Settings()
    print(settings.APP_NAME)  # 获取配置值
    """
    # 服务配置
    APP_NAME: str = "局域网文件传输服务" # 应用名称
    APP_DESCRIPTION: str = "局域网文件传输服务，支持文件上传、下载、分片上传、断点续传等功能" # 应用描述
    APP_AUTHOR: str = "董思远" # 作者
    VERSION: str = "1.0.0" # 应用版本
    HOST: str = "0.0.0.0" # 主机地址
    PORT: int = 8000 # 端口号
    WORKERS: int = 2 # 工作进程数

    # 存储配置
    STORAGE_PATH: str = "storage" # 存储路径
    TEMP_PATH: str = "storage/temp" # 临时文件路径
    UPLOADS_PATH: str = "storage/uploads" # 上传文件路径

    # 上传配置
    CHUNK_SIZE: int = 10 * 1024 * 1024 # 分片大小（10MB）
    CHUNK_THRESHOLD: int = 10 * 1024 * 1024 # 分片阈值（10MB,超过则自动启用分片上传）
    MAX_CONCURRENT_UPLOADS: int = 10 # 最大并发上传数

    # 清理配置（手动清理脚本使用）
    TEMP_FILE_EXPIRY: int = 24 * 3600 # 临时文件过期时间（秒，默认24小时）

    # 安全配置
    SAFETY_BUFFER: int = 10 * 1024 * 1024 * 1024 # 磁盘保留空间（10GB）

    # 日志配置
    LOG_LEVEL: str = "INFO" # 日志级别
    LOG_PATH: str = "logs" # 日志路径

    class Config:
        """
        Pydantic配置类

        功能说明：
        - 指定从.env文件读取环境变量
        - 设置环境变量编码
        - 设置是否区分大小写
        """
        env_file = ".env" # 环境变量文件路径

settings = Settings()