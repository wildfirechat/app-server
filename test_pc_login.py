"""
PC 扫码登录压力测试脚本（仅依赖标准库）。

启动指定数量的线程，每个线程模拟一个 PC 客户端：
  1. 创建 PC 会话（/pc_session），获取 token
  2. 长轮询等待扫码/确认（/session_login/{token}），阻塞最多 50 秒
  3. 打印各自结果

用法：
  python test_pc_login.py [--base-url http://localhost:8888] [--threads 100]
"""
import argparse
import json
import threading
import time
import uuid
import urllib.request
import urllib.error


BASE_URL = "http://localhost:8888"
NUM_THREADS = 100
REQUEST_TIMEOUT = 55  # 稍大于服务端 50 秒超时


def _post_json(url: str, body: dict, timeout: int):
    """发送 JSON POST 请求，返回 (status_code, dict)"""
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, method="POST")
    req.add_header("Content-Type", "application/json")
    try:
        resp = urllib.request.urlopen(req, timeout=timeout)
        return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8"))
    except urllib.error.URLError as e:
        return -1, {"error": str(e.reason)}


def do_pc_login(thread_id: int, base_url: str):
    """单个线程的 PC 扫码登录流程"""
    client_id = f"test-client-{thread_id}-{uuid.uuid4().hex[:8]}"

    try:
        # ---------- Step 1: 创建 PC 会话 ----------
        create_body = {
            "token": None,
            "device_name": f"test-pc-{thread_id}",
            "clientId": client_id,
            "platform": 0,
            "flag": 0,
        }
        t0 = time.time()
        status, data = _post_json(f"{base_url}/pc_session", create_body, timeout=10)
        t1 = time.time()

        if status != 200 or data.get("code") != 0:
            print(f"[T{thread_id:3d}] FAIL /pc_session status={status}, body={data}")
            return

        token = data["result"]["token"]
        print(f"[T{thread_id:3d}] STEP1 /pc_session ok, token={token[:12]}... ({t1-t0:.2f}s)")

        # ---------- Step 2: 调用 /session_login/{token} 等待扫码/确认 ----------
        t2 = time.time()
        status2, data2 = _post_json(f"{base_url}/session_login/{token}", {}, timeout=REQUEST_TIMEOUT)
        t3 = time.time()
        elapsed = t3 - t2

        code = data2.get("code", -1)
        summary = (
            f"SUCCESS" if code == 0 else
            f"NOT_VERIFIED(9)" if code == 9 else
            f"NOT_SCANED(10)" if code == 10 else
            f"CANCELED(18)" if code == 18 else
            f"EXPIRED(8)" if code == 8 else
            f"code={code}"
        )
        print(f"[T{thread_id:3d}] STEP2 /session_login/{token[:12]}... -> {summary} ({elapsed:.2f}s)")

    except Exception as e:
        print(f"[T{thread_id:3d}] ERROR {type(e).__name__}: {e}")


def main():
    parser = argparse.ArgumentParser(description="PC 扫码登录压力测试")
    parser.add_argument("--base-url", default=BASE_URL, help=f"服务地址 (default: {BASE_URL})")
    parser.add_argument("--threads", type=int, default=NUM_THREADS, help=f"并发线程数 (default: {NUM_THREADS})")
    args = parser.parse_args()

    print(f"启动 {args.threads} 个线程并发测试 PC 扫码登录")
    print(f"目标服务: {args.base_url}")
    print(f"{'='*60}")

    threads = []
    start_time = time.time()

    for i in range(args.threads):
        t = threading.Thread(target=do_pc_login, args=(i, args.base_url), daemon=True)
        threads.append(t)

    for t in threads:
        t.start()

    for t in threads:
        t.join()

    total_time = time.time() - start_time
    print(f"{'='*60}")
    print(f"全部线程结束，总耗时: {total_time:.2f}s")


if __name__ == "__main__":
    main()
