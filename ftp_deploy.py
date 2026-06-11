import os
from ftplib import FTP

def main():
    print("=== Deploying Policy Pages to Shared FTP Server ===")
    
    # Local paths
    docs_dir = r"d:\sleepapprepo\docs"
    soundpad_privacy_path = os.path.join(docs_dir, "soundpad", "privacy.html")
    soundpad_terms_path = os.path.join(docs_dir, "soundpad", "terms.html")
    
    root_index_path = os.path.join(docs_dir, "index.html")
    root_privacy_path = os.path.join(docs_dir, "privacy.html")
    root_terms_path = os.path.join(docs_dir, "terms.html")
    
    # Read files
    with open(soundpad_privacy_path, "r", encoding="utf-8") as f:
        privacy_html = f.read()
    with open(soundpad_terms_path, "r", encoding="utf-8") as f:
        terms_html = f.read()
        
    with open(root_index_path, "r", encoding="utf-8") as f:
        root_index_html = f.read()
    with open(root_privacy_path, "r", encoding="utf-8") as f:
        root_privacy_html = f.read()
    with open(root_terms_path, "r", encoding="utf-8") as f:
        root_terms_html = f.read()
    
    # Connect to shared hosting FTP
    print("Connecting to ftp.photon-bounce.com...")
    ftp = FTP("ftp.photon-bounce.com")
    ftp.login("photonb", "Nepidaras25!!??")
    print("[OK] Logged in successfully!")
    
    # Helper to create folders recursively if needed
    def ensure_dir(path):
        parts = path.strip("/").split("/")
        current = ""
        for part in parts:
            current = f"{current}/{part}" if current else part
            try:
                ftp.mkd(current)
                print(f"Created remote directory: {current}")
            except Exception:
                pass  # Directory already exists or permission denied
                
    def upload_file(local_content, remote_path):
        # Write to temporary file for upload
        temp_name = "temp_upload.html"
        with open(temp_name, "w", encoding="utf-8") as temp_f:
            temp_f.write(local_content)
            
        with open(temp_name, "rb") as f:
            ftp.storbinary(f"STOR {remote_path}", f)
        print(f"[OK] Uploaded: {remote_path}")
        
        try:
            os.remove(temp_name)
        except Exception:
            pass

    # 1. Ensure public_html directories exist
    ensure_dir("public_html")
    ensure_dir("public_html/soundpad")
    ensure_dir("public_html/soundpad/privacy")
    ensure_dir("public_html/soundpad/terms")
    
    ensure_dir("public_html/ausis")
    ensure_dir("public_html/ausis/privacy")
    ensure_dir("public_html/ausis/terms")
    
    # 2. Upload to root public_html directory
    print("Uploading root pages...")
    upload_file(root_index_html, "public_html/index.html")
    upload_file(root_privacy_html, "public_html/privacy.html")
    upload_file(root_terms_html, "public_html/terms.html")
    
    # 3. Upload to /public_html/soundpad for backward compatibility
    print("Uploading soundpad backward compatibility pages...")
    upload_file(privacy_html, "public_html/soundpad/privacy.html")
    upload_file(terms_html, "public_html/soundpad/terms.html")
    upload_file(privacy_html, "public_html/soundpad/privacy/index.html")
    upload_file(terms_html, "public_html/soundpad/terms/index.html")
    
    # 4. Upload to /public_html/ausis for new branded pages
    print("Uploading ausis branded pages...")
    upload_file(root_index_html, "public_html/ausis/index.html")
    upload_file(privacy_html, "public_html/ausis/privacy.html")
    upload_file(terms_html, "public_html/ausis/terms.html")
    upload_file(privacy_html, "public_html/ausis/privacy/index.html")
    upload_file(terms_html, "public_html/ausis/terms/index.html")
    
    ftp.quit()
    print("=== Deployment Completed Successfully ===")

if __name__ == "__main__":
    main()
