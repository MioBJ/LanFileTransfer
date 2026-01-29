"""
分片上传核心服务模块

提供分片上传的核心业务逻辑
包括初始化、保存分片、合并文件等功能
"""

import os # 操作系统模块，用于文件路径操作
import json # JSON模块，用于读写JSON文件
import aiofiles # 异步文件操作模块，用于异步读写文件
import shutil # 文件操作模块，用于删除文件和目录
from typing import Dict, List, Optional # 类型提示，用于可选参数
from datetime import datetime # 日期时间模块
from app.config import settings # 应用配置对象
from app.utils.logger_config import logger # 日志配置函数
from app.utils.file_utils import (
    generate_temp_path, # 生成临时目录路径
    generate_upload_path, # 生成上传文件路径
    get_chunk_path, # 获取分片文件路径
    calculate_file_hash # 计算文件hash值
)

# 元数据管理函数
def get_metadata_path(upload_id: str) -> str:
    """
    获取元数据文件路径
    
    功能说明：
    - 返回指定上传任务的元数据文件路径
    - 元数据文件存储在临时目录中
    
    参数说明：
    - upload_id: 上传任务ID（文件hash值）
    
    返回值：
    - str: metadata.json的完整路径
    
    路径示例：
    storage/temp/abc123.../metadata.json
    
    类比理解：
    - 类似给每个上传任务创建一个"档案袋"
    - 记录这个任务的所有信息
    """
    temp_dir = generate_temp_path(upload_id)
    return os.path.join(temp_dir, "metadata.json")

# 保存元数据函数,返回值为None可以省略么？答：可以，因为返回值为None表示函数执行成功，不需要返回任何值
async def save_metadata(upload_id: str, metadata: Dict):
    """
    保存元数据到JSON文件
    
    功能说明：
    - 将上传任务的元数据保存到metadata.json
    - 使用异步方式写入，不阻塞其他请求
    - 每次更新都会覆盖整个文件
    
    参数说明：
    - upload_id: 上传任务ID
    - metadata: 元数据字典
    
    元数据示例：
    {
        "filename": "video.mp4",
        "filesize": 104857600,
        "file_hash": "abc123...",
        "total_chunks": 10,
        "uploaded_chunks": [0, 1, 2],
        "device_name": "iPhone",
        "device_type": "iOS",
        "created_at": "2025-12-13T21:00:00Z"
    }
    
    类比理解：
    - 类似iOS中用NSUserDefaults保存数据
    - 但这里保存到文件系统中
    """
    # 步骤1：获取元数据文件路径
    metadata_path = get_metadata_path(upload_id)

    # 步骤2:异步写入元数据到JSON文件
    async with aiofiles.open(metadata_path, mode='w', encoding='utf-8') as f:
        # josn.dumps()方法将字典转换为JSON字符串
        # ensure_ascii=False表示不使用ASCII编码,支持中文
        # indent=4表示缩进4个空格，使JSON文件更易读
        await f.write(json.dumps(metadata, ensure_ascii=False, indent=4))
    
    logger.debug(f"元数据保存成功: {metadata_path}")

# 加载元数据函数,返回值为Optional[Dict]表示可以返回None，也可以返回字典
async def load_metadata(upload_id: str) -> Optional[Dict]:
    """
    加载元数据从JSON文件
    
    功能说明：
    - 从metadata.json读取元数据
    - 如果文件不存在，返回None
    - 使用异步方式读取
    
    参数说明：
    - upload_id: 上传任务ID
    
    返回值：
    - Dict: 元数据字典
    - None: 文件不存在
    
    类比理解：
    - 类似从NSUserDefaults读取数据
    - 需要处理数据不存在的情况
    """
    # 步骤1：获取元数据文件路径
    metadata_path = get_metadata_path(upload_id)

    # 步骤2:检查文件是否存在
    if not os.path.exists(metadata_path):
        logger.warning(f"元数据文件不存在: {metadata_path}")
        return None

    # 步骤3:异步读取元数据从JSON文件
    try:
        async with aiofiles.open(metadata_path, mode='r', encoding='utf-8') as f:
            content = await f.read() # 读取文件内容
            metadata = json.loads(content) # 将JSON字符串转换为字典
            logger.debug(f"元数据加载成功: {upload_id}")
            return metadata
    except Exception as e:
        logger.error(f"加载元数据失败: {upload_id}, 错误：{e}")
        return None

# 核心业务函数
async def init_upload(
    filename: str,
    filesize: int,
    file_hash: str,
    device_name: str,
    device_type: str
) -> Dict:
    """
    初始化分片上传任务
    
    功能说明：
    - 检查文件是否已存在（秒传功能）
    - 检查是否有未完成的上传（断点续传）
    - 创建新的上传任务
    - 返回upload_id和已上传的分片列表
    
    参数说明：
    - filename: 文件名
    - filesize: 文件大小（字节）
    - file_hash: 文件MD5 hash值（用作upload_id）
    - device_name: 设备名称
    - device_type: 设备类型
    
    返回值：
    {
        "upload_id": "abc123...",
        "uploaded_chunks": [0, 1, 2],  # 已上传的分片索引
        "total_chunks": 10,             # 总分片数
        "chunk_size": 10485760          # 每个分片大小
    }
    
    三种场景：
    1. 文件已存在 → 秒传（返回所有分片都已上传）
    2. 有未完成的上传 → 断点续传（返回部分分片已上传）
    3. 全新上传 → 创建新任务（返回空的已上传列表）
    """
    # upload_id就是file_hash
    upload_id = file_hash

    logger.info(f"初始化分片上传任务: {filename}, 文件大小: {filesize}字节, 文件hash: {file_hash[:8]}, 设备名称: {device_name}, 设备类型: {device_type}")

    # 步骤1:计算分片数量
    # 例如：文件大小为100MB，每个分片大小为10MB，则总分片数为10
    chunk_size = settings.CHUNK_SIZE # 每个分片大小
    total_chunks = (filesize + chunk_size - 1) // chunk_size # 总分片数
    
    # 步骤2: 检查是否有已存在的元数据（断点续传）
    metadata = await load_metadata(upload_id)
    
    if metadata:
        # 场景A: 找到了元数据，说明之前上传过
        logger.info(f"发现已存在的上传任务: {upload_id[:8]}...")
        
        # 获取已上传的分片列表
        uploaded_chunks = metadata.get("uploaded_chunks", [])
        
        # 判断是秒传还是断点续传
        if len(uploaded_chunks) == total_chunks:
            logger.info(f"文件已存在，秒传成功: {upload_id[:8]}...")
        else:
            logger.info(f"断点续传: 已上传 {len(uploaded_chunks)}/{total_chunks} 个分片")
    else:
        # 场景B: 没有找到元数据，说明是全新上传
        logger.info(f"全新上传: 创建新任务: {filename}")
        
        # 创建元数据
        metadata = {
            "filename": filename,
            "filesize": filesize,
            "file_hash": file_hash,
            "total_chunks": total_chunks,
            "chunk_size": chunk_size,
            "uploaded_chunks": [],  # 空列表
            "device_name": device_name,
            "device_type": device_type,
            "created_at": datetime.now().isoformat() + "Z"  # ISO 8601格式时间
        }
        
        # 保存元数据
        await save_metadata(upload_id, metadata)
        
        # 空的已上传列表
        uploaded_chunks = []
    
    # 统一返回（不管是哪种场景，返回结构都一样）
    return {
        "upload_id": upload_id,
        "uploaded_chunks": uploaded_chunks,
        "total_chunks": total_chunks,
        "chunk_size": chunk_size
    }

async def save_chunk(
    upload_id: str, 
    chunk_index: int, 
    chunk_data: bytes) -> bool:
    """
    保存单个分片到临时目录
    
    功能说明：
    - 将分片数据写入临时文件
    - 更新元数据中的uploaded_chunks列表
    - 返回是否保存成功
    
    参数说明：
    - upload_id: 上传任务ID
    - chunk_index: 分片索引（从0开始）
    - chunk_data: 分片的二进制数据
    
    返回值：
    - bool: True表示保存成功，False表示失败
    
    文件示例：
    storage/temp/abc123.../chunk_0  ← 保存第0个分片
    storage/temp/abc123.../chunk_1  ← 保存第1个分片
    """
    try:
        # 步骤1：生成分片文件路径
        chunk_path = get_chunk_path(upload_id, chunk_index)

        # 步骤2：异步写入分片数据到分片文件
        async with aiofiles.open(chunk_path, mode='wb') as f: # wb表示以二进制写入模式打开文件
            await f.write(chunk_data)

        # 步骤3：更新元数据中的uploaded_chunks已上传分片列表
        # 读取元数据
        metadata = await load_metadata(upload_id)
        if metadata:
            # 获取已上传分片列表
            uploaded_chunks = metadata.get("uploaded_chunks", [])
            if chunk_index not in uploaded_chunks:
                # 添加当前分片索引到已上传分片列表
                uploaded_chunks.append(chunk_index)
                # 排序已上传分片列表
                uploaded_chunks.sort() 
                # 更新元数据中的uploaded_chunks列表
                metadata["uploaded_chunks"] = uploaded_chunks
                # 保存元数据
                await save_metadata(upload_id, metadata)
                logger.debug(f"分片保存成功: {chunk_path},进度: {len(uploaded_chunks)}/{metadata.get('total_chunks', 0)}")
            return True
        else:
            logger.error(f"加载元数据失败: {upload_id}")
            return False
    except Exception as e:
        logger.error(f"保存分片失败: {upload_id[:8]}... chunk_{chunk_index}, 错误: {e}")
        return False

async def merge_chunks(upload_id: str) -> Optional[Dict]:
    """
    合并所有分片为完整文件
    
    功能说明：
    - 检查所有分片是否已上传完成
    - 按顺序读取所有分片并合并
    - 保存到最终的存储路径
    - 清理临时文件和目录
    
    参数说明：
    - upload_id: 上传任务ID
    
    返回值：
    - Dict: 包含文件信息
      {
          "filename": "video.mp4",
          "file_path": "storage/uploads/2025-12-13/iPhone/video.mp4",
          "filesize": 104857600
      }
    - None: 合并失败
    
    流程说明：
    storage/temp/abc123.../
    ├── chunk_0  ────┐
    ├── chunk_1  ────┤  合并
    ├── chunk_2  ────┤   ↓
    └── metadata.json ─→ storage/uploads/2025-12-13/iPhone/video.mp4
    """
    try:
        # 步骤1:加载元数据
        metadata = await load_metadata(upload_id)
        if not metadata:
            logger.error(f"合并失败：元数据不存在 {upload_id[:8]}...")
            return None
        
        # 步骤2:检查所有分片是否已上传完成
        total_chunks = metadata.get("total_chunks", 0)
        uploaded_chunks = metadata.get("uploaded_chunks", [])

        if len(uploaded_chunks) != total_chunks:
            logger.error(f"合并失败：分片不完整 {upload_id[:8]}... 缺少分片: {list(set(range(total_chunks)) - set(uploaded_chunks))}")
            return None
        
        # 步骤3：获取文件信息
        filename = metadata.get("filename", "")
        filesize = metadata.get("filesize", 0)
        device_name = metadata.get("device_name", "")

        # 步骤4：生成最终文件路径
        final_path = generate_upload_path( device_name, filename)

        #步骤5: 合并分片到最终文件
        async with aiofiles.open(final_path, mode='wb') as merged_file:
            #按照顺序读取每个分片并写入
            for chunk_index in range(total_chunks):
                chunk_path = get_chunk_path(upload_id, chunk_index)

                #检查分片文件是否存在
                if not os.path.exists(chunk_path):
                    logger.error(f"合并失败：分片不存在 {upload_id[:8]}... chunk_{chunk_index}")
                    return None

                #读取分片数据
                async with aiofiles.open(chunk_path, mode='rb') as chunk_file: # rb表示以二进制读取模式打开文件
                    chunk_data = await chunk_file.read()
                    #写入最终文件
                    await merged_file.write(chunk_data)
        logger.info(f"合并成功：{upload_id[:8]}... 合并文件: {final_path}")

        #步骤6: 清理临时分片文件（保留元数据用于秒传）
        temp_dir = generate_temp_path(upload_id)
        if os.path.exists(temp_dir):
            # 只删除分片文件，保留metadata.json
            for chunk_index in range(total_chunks):
                chunk_path = get_chunk_path(upload_id, chunk_index)
                if os.path.exists(chunk_path):
                    os.remove(chunk_path)
            
            logger.info(f"临时分片文件已清理，元数据已保留用于秒传")

        #步骤7: 返回最终文件信息
        return{
            "filename": filename,
            "file_path": final_path,
            "filesize": filesize
        }
    except Exception as e:
        logger.error(f"合并分片失败: {upload_id[:8]}..., 错误: {e}")
        return None