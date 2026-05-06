"""
Mock DataX 脚本 — 用于架构冒烟测试
模拟 DataX 执行：读取传入的 JSON 配置文件，打印模拟输出，返回 0
"""
import sys
import json
import time

print("[Mock DataX] ========================================")
print(f"[Mock DataX] 启动, 参数: {sys.argv}")

# 尝试读取配置文件
for arg in sys.argv[1:]:
    if arg.endswith('.json'):
        try:
            with open(arg, 'r', encoding='utf-8') as f:
                config = json.load(f)
            job_content = config.get('job', {})
            content_list = job_content.get('content', [])
            for c in content_list:
                reader_name = c.get('reader', {}).get('name', 'unknown')
                writer_name = c.get('writer', {}).get('name', 'unknown')
                tables = []
                for conn in c.get('reader', {}).get('parameter', {}).get('connection', []):
                    tables.extend(conn.get('table', []))
                print(f"[Mock DataX] Reader: {reader_name}, Tables: {tables}")
                print(f"[Mock DataX] Writer: {writer_name}")
        except Exception as e:
            print(f"[Mock DataX] 读取配置文件失败: {e}")

# 模拟 DataX 执行过程
for i in range(5):
    print(f"[Mock DataX] 进度: {(i+1)*20}% - 已同步 {100*(i+1)} 条记录")
    time.sleep(0.5)

print("[Mock DataX] 任务完成: 总计同步 500 条记录, 0 错误")
print("[Mock DataX] ========================================")
sys.exit(0)
