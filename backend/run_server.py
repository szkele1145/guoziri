# -*- coding: utf-8 -*-
"""
Android 入口：供 Chaquopy 的 Java 层调用。

MainActivity 只调用本模块的 start_server(data_dir, web_dir, port)。
不直接用 app.py 的 __main__（那里的 app.run 是桌面调试用）。
"""
import os
import threading

# Android 上没有 werkzeug 的终端，纯静默跑
import logging

logging.getLogger("werkzeug").setLevel(logging.ERROR)


def start_server(data_dir, web_dir, port=5000):
    """在当前线程跑 Flask（Java 侧已开好后台线程）"""
    os.environ["DATA_DIR"] = data_dir

    # 延迟导入：DATA_DIR 必须在 app.py 读它之前设好
    import app as flask_app

    flask_app.WEB_DIR = web_dir  # 覆盖桌面默认路径
    flask_app.app.run(
        host="127.0.0.1",
        port=port,
        threaded=True,
        use_reloader=False,
    )
