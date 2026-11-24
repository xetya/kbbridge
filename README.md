# KB Bridge
Keyboard OTG di HP -> diteruskan lewat Wi-Fi ke laptop.
1. Push repo ke GitHub, tab Actions -> Build APK -> download artifact `kbbridge-apk`.
2. Laptop: `pip install pynput` lalu `python server.py`.
3. HP & laptop satu jaringan (atau hotspot HP). Buka app, isi IP laptop, Connect.
