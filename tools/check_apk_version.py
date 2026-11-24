import zipfile
import re

apk = r'C:\Users\一只屑\Desktop\guoziri-apk-extract\app-release.apk'
with zipfile.ZipFile(apk, 'r') as z:
    # 读取AndroidManifest.xml
    manifest = z.read('AndroidManifest.xml')
    content = str(manifest)
    print('APK文件大小:', len(open(apk,'rb').read())/1048576, 'MB')
    # 查找版本信息
    m = re.search(rb'versionCode=(\d+)', manifest)
    if m:
        print('versionCode:', m.group(1).decode())
    m = re.search(rb'versionName=([^"\s]+)', manifest)
    if m:
        print('versionName:', m.group(1).decode())