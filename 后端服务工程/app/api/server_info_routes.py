"""
服务器信息接口

提供服务器状态和配置信息查询功能
"""
import psutil # 系统信息库
from datetime import datetime # 日期时间模块
from fastapi import APIRouter # FastAPI路由器
from app.config import settings # 应用配置对象
from app.utils.logger_config import logger # 日志配置函数
from app.utils.api_response import success_response # 响应格式化工具

# 创建路由器实例
router = APIRouter(
    prefix="/api",#路由前缀,所有接口都会以/api开头
    tags=["系统接口"],#接口标签,会显示在API文档的标签中
)

# 服务器信息接口
@router.get("/server-info")
async def server_info():
    """
    获取服务器详细信息接口

    功能说明：
     - 返回服务器IP地址和端口
    - 返回磁盘空间信息
    - 返回上传配置限制
    - 返回服务器当前时间

    返回示例：
    {
        "code": 200,
        "message": "success",
        "data": {
            "server_ip": "192.168.1.100",
            "server_port": 8000,
            "disk_space": {...},
            "upload_limits": {...},
            "server_time": "2025-12-13T20:50:00Z"
        }
    }
    """
    logger.debug("收到服务器信息请求")

    # 步骤1:获取磁盘空间信息
    # 使用psutil库查询磁盘使用情况,返回一个namedtuple对象,包含总空间、已使用空间、剩余空间、使用百分比
    disk_usage = psutil.disk_usage(settings.STORAGE_PATH)

    # 步骤2:获取本机IP地址
    server_ip = settings.HOST

    # 步骤3:构造响应数据
    data = {
        "server_ip": server_ip,                    # 服务器IP地址
        "server_port": settings.PORT,              # 服务端口
        "storage_path": settings.STORAGE_PATH,     # 存储路径
        "disk_space": {                            # 磁盘空间信息
            "total": disk_usage.total,             # 总空间（字节）
            "used": disk_usage.used,               # 已用空间（字节）
            "free": disk_usage.free,               # 剩余空间（字节）
            "percent": disk_usage.percent          # 使用百分比（0-100的数字）
        },
        "upload_limits": {                         # 上传限制配置
            "chunk_size": settings.CHUNK_SIZE,     # 分片大小
            "chunk_threshold": settings.CHUNK_THRESHOLD,  # 分片阈值
            "max_concurrent_uploads": settings.MAX_CONCURRENT_UPLOADS  # 最大并发数
        },
        "server_time": datetime.now().isoformat() + "Z"  # 服务器当前时间
    }

    # 步骤4:返回响应数据
    return success_response(data,"服务器信息获取成功")