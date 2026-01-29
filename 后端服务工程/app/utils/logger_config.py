"""
日志配置模块

该模块配置loguru日志系统
提供统一的日志输出格式和文件管理
"""

import sys # 系统相关功能（stdout/stderr重定向）
from loguru import logger # loguru日志库  # pyright: ignore[reportMissingImports]
from app.config import settings # 应用配置

# 日志配置函数
def setup_logger():
    """
    配置日志系统

    功能说明：
    - 移除loguru的默认处理器
    - 添加控制台输出（带颜色）
    - 添加文件输出（自动轮转）
    - 设置日志级别和格式

    日志级别说明：
    - DEBUG: 调试信息（最详细）
    - INFO: 一般信息（默认）
    - WARNING: 警告信息
    - ERROR: 错误信息
    - CRITICAL: 严重错误

    使用方式：
    setup_logger()
    """
    # 步骤1:移除默认的日志处理器
    # loguru默认会自动将日志输出到控制台，我们要自定义，所以先移除。
    logger.remove() 

    # 步骤2:添加控制台输出
    # logger.add(sink, level=None, format=None, filter=None, backtrace=False, diagnose=False, enqueue=False, catch=None, serialize=None, colorize=None, raw=False, serialize_exception=None, **kwargs)
    # sink: 输出目标，可以是sys.stdout（控制台）或文件路径
    # level: 日志级别，可以是DEBUG、INFO、WARNING、ERROR、CRITICAL
    # format: 日志格式，可以是字符串或函数
    # filter: 过滤器，可以是函数或字符串
    # backtrace: 是否显示堆栈跟踪
    # diagnose: 是否显示诊断信息
    # enqueue: 是否启用队列
    # catch: 是否捕获异常
    logger.add(
        sys.stdout,
        format= "<green>{time:YYYY-MM-DD HH:mm:ss}</green> | " # 时间(绿色)
                "<level>{level: <8}</level> | " # 级别(8个字符宽度)
                "<cyan>{name}</cyan>:<cyan>{function}</cyan>:" # 模块名:函数名(青色)
                "<cyan>{line}</cyan> - " # 行号(青色)
                "<level>{message}</level>", # 日志消息
        level=settings.LOG_LEVEL, #从配置文件中获取日志级别
        colorize=True, # 是否启用颜色
    )

    # 步骤3:添加文件输出
    # 日志会写入文件，方便后续查看和分析
    logger.add(
        f"{settings.LOG_PATH}/app.log",
        format="<green>{time:YYYY-MM-DD HH:mm:ss}</green> | " # 时间(绿色)
                "<level>{level: <8}</level> | " # 级别(8个字符宽度)
                "<cyan>{name}</cyan>:<cyan>{function}</cyan>:" # 模块名:函数名(青色)
                "<cyan>{line}</cyan> - " # 行号(青色)
                "<level>{message}</level>", # 日志消息
        level=settings.LOG_LEVEL, #从配置文件中获取日志级别
        rotation="00:00", # 日志文件大小达到10MB时自动轮转
        retention="30 days", # 日志文件保留30天
        compression="zip", # 日志文件压缩格式
        encoding="utf-8", # 日志文件编码
    )

    # 步骤4：记录一条初始化日志
    logger.info(f"日志系统初始化完成，日志级别: {settings.LOG_LEVEL}")

if __name__ == "__main__":
    """
    测试代码：直接运行此文件进行测试

    运行方式：
    python -m app.utils.logger_config
    """
    setup_logger()
    logger.debug("这是调试信息")
    logger.info("这是一般信息")
    logger.warning("这是警告信息")
    logger.error("这是错误信息")
    logger.critical("这是严重错误信息")

    #测试带变量的日志
    filename = "test.jpg"
    filesize = 1024 * 1024 * 1 # 1MB
    logger.info(f"文件上传完成: {filename}, 大小: {filesize} 字节")