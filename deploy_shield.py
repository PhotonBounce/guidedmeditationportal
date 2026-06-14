"""
Recursively upload the Mind & Body Shield microsite to the shared FTP host.

Runs on a GitHub Actions runner (which can reach FTP port 21 — the cloud dev
container cannot). The password is read from the FTP_PASS environment variable
and is never hardcoded or printed.

Target: public_html/pom  ->  http://photon-bounce.com/pom/
"""
import os
from ftplib import FTP

HOST = "ftp.photon-bounce.com"
USER = "photonb"
LOCAL_DIR = "microsite"
REMOTE_DIR = "public_html/pom"


def main():
    pw = os.environ.get("FTP_PASS")
    if not pw:
        raise SystemExit("FTP_PASS not set — pass the password as the workflow input.")

    print(f"Connecting to {HOST} ...")
    ftp = FTP(HOST, timeout=90)
    ftp.login(USER, pw)
    print("[OK] Logged in")

    made = set()

    def ensure_dir(remote):
        parts = remote.strip("/").split("/")
        cur = ""
        for p in parts:
            cur = f"{cur}/{p}" if cur else p
            if cur in made:
                continue
            try:
                ftp.mkd(cur)
                print("mkdir", cur)
            except Exception:
                pass  # already exists
            made.add(cur)

    ensure_dir(REMOTE_DIR)

    count = 0
    for root, _dirs, files in os.walk(LOCAL_DIR):
        rel = os.path.relpath(root, LOCAL_DIR)
        remote_root = REMOTE_DIR if rel == "." else f"{REMOTE_DIR}/{rel.replace(os.sep, '/')}"
        ensure_dir(remote_root)
        for fn in sorted(files):
            local_path = os.path.join(root, fn)
            remote_path = f"{remote_root}/{fn}"
            with open(local_path, "rb") as f:
                ftp.storbinary(f"STOR {remote_path}", f)
            print("[OK] uploaded", remote_path)
            count += 1

    ftp.quit()
    print(f"=== Done: {count} files. Live at http://photon-bounce.com/pom/ ===")


if __name__ == "__main__":
    main()
