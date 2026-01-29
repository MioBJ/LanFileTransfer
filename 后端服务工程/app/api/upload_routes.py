"""
文件上传API路由

提供分片上传相关的HTTP接口
"""
import hashlib  # 添加hashlib导入
from fastapi import APIRouter, UploadFile, File, Form  # FastAPI核心组件
from app.models.upload_models import (  # 数据模型
    UploadInitRequest,         # 初始化请求模型
    UploadInitResponse,        # 初始化响应模型
    ChunkUploadResponse,       # 分片响应模型
    UploadCompleteRequest,     # 完成请求模型
    UploadCompleteResponse     # 完成响应模型
)
from app.utils.api_response import ApiResponse  # 统一响应模型

from app.services.chunk_file_service import (  # 核心服务
    init_upload,  # 初始化分片上传任务
    save_chunk,  # 保存分片
    merge_chunks,  # 合并分片
)
from app.utils.logger_config import logger  # 日志配置
from app.config import settings  # 应用配置

router = APIRouter(
    prefix="/api/upload", #API路由前缀
    tags=["文件上传"]#API路由标签
)

# API接口

@router.post("/", response_model=ApiResponse[UploadCompleteResponse])
async def upload_file(
    device_name: str = Form(...),        # 表单字段：设备名称
    device_type: str = Form(...),        # 表单字段：设备类型
    file: UploadFile = File(...)         # 文件字段
):
    """
    小文件直接上传接口（不分片）
    
    功能说明：
    - 适用于小于10MB的文件
    - 一次性上传，不需要分片
    - 自动计算文件hash
    - 支持秒传功能
    
    请求（multipart/form-data）：
    - device_name: 设备名称
    - device_type: 设备类型
    - file: 文件数据
    
    响应体：
    {
        "code": 200,
        "message": "上传成功",
        "data": {
            "upload_id": "abc123...",
            "success": true,
            "message": "上传成功"
        }
    }
    """
    try:
        filename = file.filename or "unknown_file"
        logger.info(f"收到文件上传请求: {filename}, 设备: {device_name}")
        
        # 步骤1: 读取文件内容
        file_data = await file.read()
        filesize = len(file_data)
        
        # 检查文件大小
        if filesize > settings.CHUNK_THRESHOLD:
            return ApiResponse[UploadCompleteResponse](
                code=400,
                message=f"文件过大（{filesize / 1024 / 1024:.2f}MB），请使用分片上传接口",
                data=None
            )
        
        logger.info(f"文件大小: {filesize / 1024 / 1024:.2f}MB")
        
        # 步骤2: 计算文件hash
        file_hash = hashlib.md5(file_data).hexdigest()
        logger.info(f"文件hash: {file_hash[:8]}...")
        
        # 步骤3: 检查文件是否已存在（秒传）
        init_result = await init_upload(
            filename=filename,
            filesize=filesize,
            file_hash=file_hash,
            device_name=device_name,
            device_type=device_type
        )
        
        upload_id = init_result['upload_id']
        uploaded_chunks = init_result['uploaded_chunks']
        total_chunks = init_result['total_chunks']
        
        # 如果文件已存在（秒传）
        if len(uploaded_chunks) == total_chunks:
            logger.info(f"文件已存在，秒传成功: {filename}")
            response_data = UploadCompleteResponse(
                upload_id=upload_id,
                success=True,
                message="文件已存在，秒传成功"
            )
            return ApiResponse[UploadCompleteResponse](
                code=200,
                message="秒传成功",
                data=response_data
            )
        
        # 步骤4: 保存文件
        logger.info(f"保存文件: {filename}")
        
        # 使用save_chunk保存（虽然只有一个分片）
        success = await save_chunk(
            upload_id=upload_id,
            chunk_index=0,
            chunk_data=file_data
        )
        
        if not success:
            return ApiResponse[UploadCompleteResponse](
                code=500,
                message="文件保存失败",
                data=None
            )
        
        # 步骤5: 合并（虽然只有一个分片）
        merge_result = await merge_chunks(upload_id)
        
        if merge_result:
            response_data = UploadCompleteResponse(
                upload_id=upload_id,
                success=True,
                message="上传成功"
            )
            return ApiResponse[UploadCompleteResponse](
                code=200,
                message="上传成功",
                data=response_data
            )
        else:
            return ApiResponse[UploadCompleteResponse](
                code=500,
                message="文件处理失败",
                data=None
            )
        
    except Exception as e:
        logger.error(f"文件上传失败: {e}")
        return ApiResponse[UploadCompleteResponse](
            code=500,
            message=f"上传失败: {str(e)}",
            data=None
        )


@router.post("/init", response_model=ApiResponse[UploadInitResponse])
async def upload_init(request: UploadInitRequest):
    """
    初始化分片上传任务
    
    功能说明：
    - 客户端在上传前调用此接口
    - 检查文件是否已存在（秒传）
    - 检查是否有未完成的上传（断点续传）
    - 返回upload_id和已上传的分片列表
    
    请求体：
    {
        "filename": "video.mp4",
        "filesize": 104857600,
        "file_hash": "abc123...",
        "device_name": "iPhone",
        "device_type": "iOS"
    }
    
    响应体：
    {
        "code": 200,
        "message": "success",
        "data": {
            "upload_id": "abc123...",
            "uploaded_chunks": [0, 1, 2],
            "total_chunks": 10,
            "chunk_size": 10485760
        }
    }
    """
    try:
        logger.info(f"收到分片上传初始化请求: {request.filename}, 文件大小: {request.filesize}字节, 文件hash: {request.file_hash[:8]}, 设备名称: {request.device_name}, 设备类型: {request.device_type}")

        # 调用核心服务初始化上传
        result = await init_upload(
            filename=request.filename,
            filesize=request.filesize,
            file_hash=request.file_hash,
            device_name=request.device_name,
            device_type=request.device_type
        )

        #返回成功响应
        response_data = UploadInitResponse(**result)
        return ApiResponse[UploadInitResponse](
            code=200,
            message="初始化成功",
            data=response_data
        )
    except Exception as e:
        logger.error(f"初始化分片上传任务失败: {e}")
        return ApiResponse[UploadInitResponse](
            code=500,
            message=f"初始化失败: {str(e)}",
            data=None
        )
    
@router.post("/chunk", response_model=ApiResponse[ChunkUploadResponse])
async def upload_chunk(
    upload_id: str = Form(...), #表单字段：上传任务ID
    chunk_index: int = Form(...), #表单字段：分片索引
    chunk_hash: str = Form(...), #表单字段：分片hash值
    file: UploadFile = File(...), #文件字段：分片文件
):
    """
    上传单个分片
    
    功能说明：
    - 接收并保存单个分片
    - 更新上传进度
    - 返回是否上传成功
    
    请求（multipart/form-data）：
    - upload_id: 上传任务ID
    - chunk_index: 分片索引（从0开始）
    - chunk_hash: 分片hash值
    - file: 分片文件数据
    
    响应体：
    {
        "code": 200,
        "message": "success",
        "data": {
            "chunk_index": 0,
            "uploaded": true
        }
    }
    """
    try:
        logger.info(f"收到分片上传请求: {upload_id}, 分片索引: {chunk_index}, 分片hash: {chunk_hash[:8]}")

        # 读取分片数据
        chunk_data = await file.read()

        # 调用核心服务保存分片
        result = await save_chunk(
            upload_id=upload_id,
            chunk_index=chunk_index,
            chunk_data=chunk_data
        )

        if result:
            # 返回成功响应
            response_data = ChunkUploadResponse(
                chunk_index=chunk_index,
                uploaded=True
            )
            return ApiResponse[ChunkUploadResponse](
                code=200,
                message="分片上传成功",
                data=response_data
            )
        else:
            # 返回失败响应
            return ApiResponse[ChunkUploadResponse](
                code=500,
                message="分片上传失败",
                data=None
            )
    except Exception as e:
        logger.error(f"上传单个分片失败: {e}")
        return ApiResponse[ChunkUploadResponse](
            code=500,
            message=f"上传失败: {str(e)}",
            data=None
        )
    
@router.post("/complete", response_model=ApiResponse[UploadCompleteResponse])
async def upload_complete(
    request: UploadCompleteRequest
):
    """
    完成上传（合并分片）
    
    功能说明：
    - 所有分片上传完成后调用
    - 验证所有分片是否完整
    - 合并分片为完整文件
    - 清理临时文件
    
    请求体：
    {
        "upload_id": "abc123...",
        "total_chunks": 10
    }
    
    响应体：
    {
        "code": 200,
        "message": "上传完成",
        "data": {
            "upload_id": "abc123...",
            "success": true,
            "message": "上传完成"
        }
    }
    """
    try:
        logger.info(f"收到完成上传请求: {request.upload_id[:8]}...")
        
        # 调用核心服务合并分片
        result = await merge_chunks(request.upload_id)
        
        if result:
            # 合并成功
            response_data = UploadCompleteResponse(
                upload_id=request.upload_id,
                success=True,
                message="上传完成"
            )
            return ApiResponse[UploadCompleteResponse](
                code=200,
                message="上传完成",
                data=response_data
            )
        else:
            # 合并失败
            return ApiResponse[UploadCompleteResponse](
                code=500,
                message="文件合并失败",
                data=None
            )
        
    except Exception as e:
        logger.error(f"完成上传失败: {e}")
        return ApiResponse[UploadCompleteResponse](
            code=500,
            message=f"完成上传失败: {str(e)}",
            data=None
        )