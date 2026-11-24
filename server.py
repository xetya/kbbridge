# Jalankan di laptop (Windows 10 / Linux Mint XFCE):  pip install pynput  lalu  python server.py
import socket
from pynput.keyboard import Controller, Key

kb = Controller()
M = {
    "ENTER": Key.enter, "NUMPAD_ENTER": Key.enter, "DEL": Key.backspace,
    "FORWARD_DEL": Key.delete, "SPACE": Key.space, "TAB": Key.tab,
    "ESCAPE": Key.esc, "CAPS_LOCK": Key.caps_lock, "INSERT": Key.insert,
    "SHIFT_LEFT": Key.shift, "SHIFT_RIGHT": Key.shift_r,
    "CTRL_LEFT": Key.ctrl, "CTRL_RIGHT": Key.ctrl_r,
    "ALT_LEFT": Key.alt, "ALT_RIGHT": Key.alt_r,
    "META_LEFT": Key.cmd, "META_RIGHT": Key.cmd,
    "DPAD_UP": Key.up, "DPAD_DOWN": Key.down,
    "DPAD_LEFT": Key.left, "DPAD_RIGHT": Key.right,
    "MOVE_HOME": Key.home, "MOVE_END": Key.end,
    "PAGE_UP": Key.page_up, "PAGE_DOWN": Key.page_down,
    "COMMA": ",", "PERIOD": ".", "MINUS": "-", "EQUALS": "=",
    "SEMICOLON": ";", "APOSTROPHE": "'", "SLASH": "/", "BACKSLASH": "\\",
    "LEFT_BRACKET": "[", "RIGHT_BRACKET": "]", "GRAVE": "`",
    "NUMPAD_ADD": "+", "NUMPAD_SUBTRACT": "-", "NUMPAD_MULTIPLY": "*",
    "NUMPAD_DIVIDE": "/", "NUMPAD_DOT": ".",
}
for i in range(1, 13):
    M[f"F{i}"] = getattr(Key, f"f{i}")
for i in range(10):
    M[f"NUMPAD_{i}"] = str(i)

def conv(n):
    if n in M:
        return M[n]
    return n.lower() if len(n) == 1 else None

pressed = set()

def release_all():
    for k in list(pressed):
        try: kb.release(k)
        except Exception: pass
    pressed.clear()

srv = socket.socket()
srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
srv.bind(("0.0.0.0", 5005))
srv.listen(1)
print("Menunggu HP di port 5005...")
while True:
    c, addr = srv.accept()
    print("Terhubung dari", addr)
    try:
        for line in c.makefile():
            try:
                act, name = line.strip().split(":", 1)
                k = conv(name)
                if k is None:
                    continue
                if act == "d":
                    kb.press(k); pressed.add(k)
                else:
                    kb.release(k); pressed.discard(k)
            except Exception as e:
                print(e)
    finally:
        release_all()  # cegah tombol nyangkut saat koneksi putus
        print("Terputus")
