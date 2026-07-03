"""
Upload the v1.4.0 APK to the public website so a phone can download it
directly — no GitHub login needed.

Runs in CI (apk-to-site.yml): the workflow downloads the release APK first,
then this script FTPs it to public_html/guidedmeditation/.
Password comes from the FTP_PASS environment variable.
"""
import os
import sys
from ftplib import FTP

HOST = "ftp.photon-bounce.com"
USER = "photonb"
APK = "GuidedMeditationPortal-v1.4.1.apk"
REMOTE_DIR = "public_html/guidedmeditation"


def main():
    password = os.environ["FTP_PASS"]
    size = os.path.getsize(APK)
    print(f"Uploading {APK} ({size / 1e6:.0f} MB) to {HOST}/{REMOTE_DIR}/ ...")

    ftp = FTP(HOST, timeout=60)
    ftp.login(USER, password)
    print("[OK] Logged in")

    # cd into the target dir, creating path segments as needed
    for part in REMOTE_DIR.split("/"):
        try:
            ftp.cwd(part)
        except Exception:
            ftp.mkd(part)
            ftp.cwd(part)

    with open(APK, "rb") as f:
        ftp.storbinary(f"STOR {APK}", f, blocksize=1024 * 64)

    remote_size = ftp.size(APK)
    print(f"[OK] Uploaded — remote size {remote_size} bytes (local {size})")
    if remote_size != size:
        print("[FAIL] Size mismatch!")
        sys.exit(1)

    ftp.quit()
    print(f"Live at: https://www.photon-bounce.com/guidedmeditation/{APK}")


if __name__ == "__main__":
    main()
