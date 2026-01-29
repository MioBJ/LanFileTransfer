"""
健康检查接口

提供服务健康状态检查功能
"""
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

# 健康检查接口
@router.get("/health")
async def health_check():
    """
    健康检查接口

    功能说明：
    - 检查服务是否正常运行
    - 返回服务状态、时间戳和版本号
    - 供前端和运维监控使用
    
    返回示例：
    {
        "code": 200,
        "message": "服务正常",
        "data": {
            "status": "healthy",
            "timestamp": "2025-12-13T20:10:00Z",
            "version": "1.0.0"
        }
    }
    """
    # 记录健康检查请求
    logger.debug("收到健康检查请求")

    # 构造响应函数
    data = {
        "status": "healthy",#服务状态：healthy表示健康
        "timestamp": datetime.now().isoformat()+"Z",#服务器当前时间(ISO 8601格式,带时区信息,Z表示UTC时间)
        "version": settings.VERSION,#服务版本号
    }

    # 返回成功响应
    return success_response(data,"服务正常")