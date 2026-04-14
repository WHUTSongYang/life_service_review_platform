# 从 server 模块导入 main 函数
from mcp_shop_search.server import main

# 判断是否为直接运行的主程序入口
if __name__ == "__main__":
    # 调用 main 函数启动服务器
    main()
