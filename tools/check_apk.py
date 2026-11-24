import zipfile
import re

apk = r'C:\Users\一只屑\Desktop\guoziri-apk-extract\app-release.apk'
with zipfile.ZipFile(apk, 'r') as z:
    manifest = z.read('AndroidManifest.xml')
    print('APK size:', len(open(apk,'rb').read())/1048576, 'MB')
    content = str(manifest)
    m = re.search(r'versionCode=(\d+)', content)
    if m:
        print('versionCode:', m.group(1))
    m = re.search(r'versionName=([^"\s]+)', content)
    if m:
        print('versionName:', m.group(1))