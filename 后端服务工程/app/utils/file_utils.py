"""
文件操作工具模块

提供文件相关的工具函数
包括hash计算、路径生成等
"""

import os # 操作系统模块，用于文件路径操作
import hashlib # hash计算模块
from datetime import datetime # 日期时间模块
from pathlib import Path # 路径模块
from app.config import settings # 应用配置对象
from app.utils.logger_config import logger # 日志配置函数

# Hash计算函数
def calculate_file_hash(file_path: str) -> str:
    """
    计算文件的MD5 hash值
    
    功能说明：
    - 使用MD5算法计算文件hash
    - 分块读取，适用于大文件
    - 返回32位的十六进制字符串
    
    参数说明：
    - file_path: 文件路径
    
    返回值：
    - str: 32位MD5 hash值（小写）
    
    类比理解：
    - 类似给文件生成"指纹"
    - 相同内容的文件会得到相同的hash
    - iOS中可以用CC_MD5实现类似功能
    
    使用示例：
    hash_value = calculate_file_hash("/path/to/file.jpg")
    # 返回: "5d41402abc4b2a76b9719d911017c592"
    """
    # 步骤1: 创建MD5 hash对象
    md5_hash = hashlib.md5()

    # 步骤2: 分块读取文件并更新hash
    # 为什么要分块读取文件？因为大文件一次性读入内存会占用太多资源
    with open(file_path, 'rb') as f: # rb表示二进制只读模式，f是文件对象
        #每次读取8KB的块
        for chunk in iter(lambda: f.read(8192), b''): # 每次读取8KB的块，如果读到空字节，则结束
            md5_hash.update(chunk) # 更新hash值，update()方法接收一个字节序列，累加计算，并更新hash值

    # 步骤3: 获取最终的hash值,hexdigest()方法返回32位的十六进制字符串
    hash_value = md5_hash.hexdigest()

    logger.debug(f"文件{file_path}的MD5 hash值为: {hash_value}")
    return hash_value

# 路径生成函数
def generate_upload_path(device_name: str, filename: str) -> str:    
    """
    生成上传文件的存储路径
    
    功能说明：
    - 按照"日期/设备名/文件名"的结构组织文件
    - 自动创建不存在的目录
    - 如果文件已存在，自动重命名（添加序号）
    
    参数说明：
    - device_name: 设备名称（如："iPhone 15 Pro"）
    - filename: 原始文件名（如："IMG_1234.jpg"）
    
    返回值：
    - str: 完整的文件存储路径
    
    路径示例：
    storage/uploads/2025-12-13/iPhone/IMG_1234.jpg
    storage/uploads/2025-12-13/iPhone/IMG_1234_1.jpg  # 重名文件
    
    类比理解：
    - 类似iOS中的Documents目录结构
    - 按日期和设备分类，便于管理
    """
    # 步骤1: 获取当前日期 格式为YYYY-MM-DD,strftime()方法将日期时间对象转换为字符串
    # %Y表示四位数的年份，%m表示两位数的月份，%d表示两位数的日期
    today = datetime.now().strftime("%Y-%m-%d")

    # 步骤2:构建目录路径
    # 路径结构：storage/uploads/YYYY-MM-DD/device_name/filename
    dir_path = os.path.join(settings.UPLOADS_PATH, today, device_name)

    # 步骤3: 创建目录（如果目录不存在）
    os.makedirs(dir_path, exist_ok=True) # exist_ok=True表示如果目录存在，则不创建，如果目录不存在，则创建

    # 步骤4：构建完整文件路径
    file_path = os.path.join(dir_path, filename)
    
    # 步骤5: 如果文件已经存在，增加后缀（如：_1.jpg）
    if os.path.exists(file_path):
        # 分离文件名和扩展名
        # 例如：filename = "IMG_1234.jpg"，则name = "IMG_1234"，ext = ".jpg"
        name, ext = os.path.splitext(filename)

        # 步骤6: 递归增加后缀，直到找到一个不存在的文件名
        counter = 1
        while os.path.exists(file_path):
            new_filename = f"{name}_{counter}{ext}"
            file_path = os.path.join(dir_path, new_filename)
            counter += 1
        
        logger.info(f"文件重名，自动重命名为: {os.path.basename(file_path)}")

    logger.debug(f"文件{filename}的存储路径为: {file_path}")
    return file_path

# 临时文件路径生成函数
def generate_temp_path(upload_id: str) -> str:
    """
    生成分片上传的临时目录路径
    
    功能说明：
    - 为每个上传任务创建独立的临时目录
    - 用于存储上传过程中的分片文件
    - 上传完成后会合并分片并删除此目录
    
    参数说明：
    - upload_id: 上传任务ID（文件hash值）
    
    返回值：
    - str: 临时目录路径
    
    路径示例：
    storage/temp/abc123def456.../
    
    类比理解：
    - 类似iOS中的临时缓存目录NSTemporaryDirectory()
    - 用于存放临时数据，使用后可以删除
    """
    # 步骤1:构建临时目录路径
    temp_dir = os.path.join(settings.TEMP_PATH, upload_id)

    # 步骤2：确保目录存在
    os.makedirs(temp_dir,exist_ok=True)

    logger.debug(f"临时目录路径为: {temp_dir}")
    return temp_dir

def get_chunk_path(upload_id: str, chunk_index: int) -> str:
    """
    获取分片文件的完整路径
    
    功能说明：
    - 根据upload_id和chunk_index，返回指定分片的存储路径
    - 用于保存或读取单个分片
    
    参数说明：
    - upload_id: 上传任务ID（文件hash值）
    - chunk_index: 分片索引（从0开始）
    
    返回值：
    - str: 分片文件路径
    
    路径示例：
    storage/temp/abc123.../chunk_0
    storage/temp/abc123.../chunk_1
    storage/temp/abc123.../chunk_2
    """
    # 步骤1:获取临时目录
    temp_dir = generate_temp_path(upload_id)

    # 步骤2：构建分片文件路径
    chunk_path = os.path.join(temp_dir, f"chunk_{chunk_index}")

    return chunk_path