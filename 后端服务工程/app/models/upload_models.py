"""
上传数据模型

定义上传接口的请求和响应数据结构
使用Pydantic的BaseModel来定义数据模型
"""
from pydantic import BaseModel, Field # Pydantic的基类模型和字段验证
from typing import Optional, List # 类型提示，用于可选参数和列表

# 请求模型 （客户端 -> 服务端）
class UploadInitRequest(BaseModel):
    """
    分片上传初始化请求模型

    功能说明：
    - 客户端在开始上传前，先发送这个请求
    - 服务器返回upload_id和已上传的分片列表
    - 客户端根据upload_id和已上传的分片列表，可以判断文件上传状态（新上传/断点续传/秒传）
    """
    filename: str = Field(..., description="文件名") #...表示必填，description表示字段描述
    filesize: int = Field(..., gt=0, description="文件大小(字节)") #gt=0表示文件大小必须大于0，le=100*1024*1024表示文件大小必须小于100MB
    file_hash: str = Field(..., min_length=32, max_length=32, description="文件hash值") # 文件hash值：用于判断文件是否存在，32位字符串
    device_name: str = Field(..., description="设备名称") # 设备名称：用于区分不同设备
    device_type: str = Field(..., description="设备类型（如：iOS/Android等）") # 设备类型：iOS/Android
class ChunkUploadRequest(BaseModel):
    """
    分片上传请求模型

    功能说明：
    - 客户端上传分片时，发送这个请求
    - upload_id用于关联到之前初始化的上传任务
    - chunk_index标识这是第几个分片（从0开始）
    - 客户端根据返回的进度信息，可以判断文件上传进度

     注意：
    - 实际的文件数据通过multipart/form-data的file字段传输
    - 这个模型只定义元数据字段
    """
    upload_id: str = Field(..., min_length=32, max_length=32, description="上传任务ID") # 上传任务ID：用于标识一个上传任务，32位字符串。就是文件的hash值
    chunk_index: int = Field(..., ge=0, description="分片索引") # 分片索引：用于标识这是第几个分片（从0开始）
    chunk_hash: str = Field(..., min_length=32, max_length=32, description="分片hash值") # 分片hash值：用于校验分片数据完整性，32位字符串
class UploadCompleteRequest(BaseModel):
    """
    分片上传完成请求模型

    功能说明：
    - 所有分片上传完成后，发送这个请求
    - 客户端通知服务器所有分片已经上传完成
    - 服务器将合并分片为最终文件,后删除临时分片
    """
    upload_id: str = Field(..., min_length=32, max_length=32, description="上传任务ID") # 上传任务ID：用于标识一个上传任务，32位字符串。就是文件的hash值
    total_chunks: int = Field(..., gt=0, description="总分片数") # 总分片数：用于记录总分片数。根据文件大小和每个分片大小计算得出

# 响应模型 （服务端 -> 客户端）
class UploadInitResponse(BaseModel):
    """
    分片上传初始化响应模型

    功能说明：
    - 返回给客户端的初始化信息
    - uploaded_chunks列表告诉客户端哪些分片已经上传（断点续传）
    - 如果uploaded_chunks包含所有分片，说明文件已存在（秒传）
    """
    upload_id: str = Field(..., min_length=32, max_length=32, description="上传任务ID") # 上传任务ID：用于标识一个上传任务，32位字符串。就是文件的hash值
    uploaded_chunks: List[int] = Field(default=[], description="已上传的分片索引列表") # 已上传的分片索引列表：用于记录已上传的分片序号。如果uploaded_chunks包含所有分片，说明文件已存在（秒传）
    total_chunks: int = Field(..., gt=0, description="总分片数") # 总分片数：用于记录总分片数。根据文件大小和每个分片大小计算得出
    chunk_size: int = Field(..., gt=0, description="每个分片大小(字节)") # 每个分片大小：用于记录每个分片大小。根据文件大小和总分片数计算得出

class ChunkUploadResponse(BaseModel):
    """
    分片上传响应模型

    功能说明：
    - 单个分片上传成功后的响应
    - 客户端根据返回的进度信息，可以判断文件上传进度
    - 告诉客户端这个分片已经保存成功
    """
    chunk_index: int = Field(..., description="已上传的分片索引") # 已上传的分片索引：用于标识这是第几个分片（从0开始）
    uploaded: bool = Field(..., description="是否上传成功") # 是否上传成功：用于标识这个分片是否上传成功

class UploadCompleteResponse(BaseModel):
    """
    分片上传完成响应模型

    功能说明：
    - 所有分片上传完成并合并成功后，发送这个响应
    - 告诉客户端所有分片已经上传完成并合并成功
    - 只返回任务ID和成功状态
    - 前端不需要文件路径等详细信息
    """
    upload_id: str = Field(..., min_length=32, max_length=32, description="上传任务ID") # 上传任务ID：用于标识一个上传任务，32位字符串。就是文件的hash值
    success: bool = Field(..., description="是否上传成功") # 是否上传成功：用于标识是否上传成功
    message: str = Field(default="上传成功", description="响应消息") # 响应消息：用于记录响应消息。默认值为"上传成功"