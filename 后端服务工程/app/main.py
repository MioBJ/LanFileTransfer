"""
FastAPI应用入口

该模块创建FastAPI应用实例
配置CORS跨域请求
配置路由和中间件
启动应用
"""

# ============================================
# 导入模块
# ============================================
import os  # 操作系统功能
from contextlib import asynccontextmanager  # 异步上下文管理器
from fastapi import FastAPI  # FastAPI核心框架
from fastapi.middleware.cors import CORSMiddleware  # CORS跨域请求中间件
from app.config import settings  # 应用配置对象
from app.utils.logger_config import setup_logger, logger  # 日志配置函数和日志对象
from app.api.health_check import router as health_check_router  # 健康检查接口路由器
from app.api.server_info_routes import router as server_info_router  # 服务器信息接口路由器
from app.api.upload_routes import router as upload_router  # 文件上传接口路由器

# ============================================
# 初始化日志系统
# ============================================
setup_logger()
logger.info("应用启动中...")


# ============================================
# 应用生命周期管理（必须在创建app之前定义）
# ============================================

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    应用生命周期管理器
    
    功能说明：
    - 替代已弃用的@app.on_event()
    - yield之前的代码在启动时执行
    - yield之后的代码在关闭时执行
    
    类比理解：
    - 类似iOS的applicationDidFinishLaunching（启动）
    - 类似iOS的applicationWillTerminate（关闭）
    """
    # ========== 启动时执行 ==========
    # 创建必要的目录
    os.makedirs(settings.TEMP_PATH, exist_ok=True)
    os.makedirs(settings.UPLOADS_PATH, exist_ok=True)
    os.makedirs(settings.LOG_PATH, exist_ok=True)
    
    logger.info("=" * 50)
    logger.info("应用启动成功")
    logger.info(f"应用名称: {settings.APP_NAME}")
    logger.info(f"版本号: {settings.VERSION}")
    logger.info(f"监听地址: {settings.HOST}:{settings.PORT}")
    logger.info(f"API文档: http://localhost:{settings.PORT}/docs")
    logger.info("=" * 50)
    
    yield  # 应用运行中
    
    # ========== 关闭时执行 ==========
    logger.info("=" * 50)
    logger.info("应用正在关闭...")
    logger.info("=" * 50)


# ============================================
# 创建FastAPI应用实例
# ============================================
app = FastAPI(
    title=settings.APP_NAME,  # 应用名称(会显示在API文档的标题中)
    version=settings.VERSION,  # 应用版本(会显示在API文档的版本中)
    description=settings.APP_DESCRIPTION,  # 应用描述(会显示在API文档的描述中)
    docs_url="/docs",  # API文档URL(会显示在API文档的URL中)
    redoc_url="/redoc",  # API文档URL(会显示在API文档的URL中）
    lifespan=lifespan,  # 应用生命周期管理器（必须在FastAPI()之前定义）
)

#配置CORS跨域请求
app.add_middleware(
    CORSMiddleware,#CORS跨域请求中间件
    allow_origins=["*"],#允许所有来源的跨域请求
    allow_credentials=True,#允许携带凭证(如Cookie)
    allow_methods=["*"],#允许所有HTTP方法(GET、POST、PUT、DELETE等) 
    allow_headers=["*"],#允许所有请求头(如Content-Type、Authorization等)
)
# 记录应用启动日志
logger.info(f"FastAPI应用创建成功：{settings.APP_NAME} v{settings.VERSION}")

# 注册路由
app.include_router(health_check_router)  # 注册健康检查路由
logger.info(f"已注册健康检查路由: {health_check_router.prefix}")

app.include_router(server_info_router)  # 注册服务器信息路由
logger.info(f"已注册服务器信息路由: {server_info_router.prefix}")

app.include_router(upload_router)  # 注册文件上传路由
logger.info(f"已注册文件上传路由: {upload_router.prefix}")
