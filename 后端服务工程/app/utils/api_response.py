"""
API响应格式化工具

该模块提供了一个统一的API响应格式化工具，用于将API响应格式化为JSON格式。
"""

from typing import Dict, Any, Optional, Generic, TypeVar
from pydantic import BaseModel

# 定义泛型类型变量
T = TypeVar('T')


# ============================================
# Pydantic响应模型
# ============================================

class ApiResponse(BaseModel, Generic[T]):
    """
    统一的API响应模型（泛型）
    
    功能说明：
    - 使用Pydantic模型定义响应结构
    - 支持泛型，可以指定data的类型
    - FastAPI会自动生成详细的API文档
    
    参数说明：
    - code: 响应码（200表示成功，其他表示错误）
    - message: 响应消息
    - data: 业务数据（泛型，可以是任何类型）
    
    使用示例：
    ApiResponse[UploadInitResponse](
        code=200,
        message="success",
        data=UploadInitResponse(...)
    )
    """
    code: int
    message: str
    data: Optional[T] = None


#def 函数名(参数1: 类型 = 默认值, 参数2: 类型) -> 返回类型:
#    """文档字符串"""
#    函数体
def success_response(data: Any = None, message: str = "success") -> Dict:
    """
    成功响应格式化函数

    功能说明：
    - 将数据封装成统一的成功响应格式
    - 返回格式：{"code": 200, "message": "success", "data": {...}}
    
    参数说明：
    - data: 要返回的业务数据，可以是任何类型（字典、列表、字符串等）
    - message: 响应消息，默认值为"success"
    
    返回值：
    - Dict: 字典类型，包含code、message、data三个字段
    
    类比理解：
    - 类似iOS中的：+ (NSDictionary *)successResponseWithData:(id)data
    - 类似Java中的：public static Result<T> success(T data)
    """
    return {
        "code": 200, # HTTP状态码：200表示成功
        "message": message, # 响应消息：默认值为"success"
        "data": data  # 业务数据：可以是任何类型（字典、列表、字符串等）
    }

def error_response(message: str, code: int = 400, data: Any = None) -> Dict:
    """
    错误响应格式化函数

    功能说明：
    - 将错误信息封装成统一的错误响应格式
    - 返回格式：{"code": 错误码, "message": "错误消息", "data": {...}}
    
    参数说明：
    - message: 错误消息，必填
    - code: 错误码，默认值为400
    - data: 业务数据，可以是任何类型（字典、列表、字符串等）
    """
    return {
        "code": code, # 错误码：默认值为400
        "message": message, # 错误消息：必填
        "data": data # 业务数据：可以是任何类型（字典、列表、字符串等）
    }