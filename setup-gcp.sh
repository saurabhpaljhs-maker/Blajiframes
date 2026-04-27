#!/bin/bash
# ═══════════════════════════════════════════════════════════════
#  BALAJI Photo Frames — GCP Ubuntu Setup Script
#  Run once on fresh GCP VM:  bash setup-gcp.sh
# ═══════════════════════════════════════════════════════════════
set -e  # exit on any error

echo "═══════════════════════════════════════"
echo "  BALAJI Frames — GCP Setup Starting"
echo "═══════════════════════════════════════"

# ── 1. System Update ──────────────────────────────────────────
echo "[1/8] Updating system..."
sudo apt update -y && sudo apt upgrade -y

# ── 2. Java 17 ────────────────────────────────────────────────
echo "[2/8] Installing Java 17..."
sudo apt install -y openjdk-17-jdk
java -version

# ── 3. MySQL ──────────────────────────────────────────────────
echo "[3/8] Installing MySQL..."
sudo apt install -y mysql-server
sudo systemctl enable mysql
sudo systemctl start mysql

# Create DB and user
sudo mysql -e "CREATE DATABASE IF NOT EXISTS balajiframes CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql -e "CREATE USER IF NOT EXISTS 'balaji_db_user'@'localhost' IDENTIFIED BY 'ChangeThisDBPassword@123';"
sudo mysql -e "GRANT ALL PRIVILEGES ON balajiframes.* TO 'balaji_db_user'@'localhost';"
sudo mysql -e "FLUSH PRIVILEGES;"
echo "  MySQL setup done ✅"

# ── 4. Nginx ──────────────────────────────────────────────────
echo "[4/8] Installing Nginx..."
sudo apt install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx

# ── 5. Create app directories ─────────────────────────────────
echo "[5/8] Creating app directories..."
sudo mkdir -p /opt/balaji/uploads
sudo mkdir -p /opt/balaji/logs
sudo mkdir -p /opt/balaji/app

# Allow current user to write
sudo chown -R $USER:$USER /opt/balaji
echo "  Directories created ✅"

# ── 6. Create environment file ────────────────────────────────
echo "[6/8] Creating environment config..."
sudo tee /etc/balaji.env > /dev/null << 'ENVEOF'
# BALAJI Photo Frames — Environment Variables
# Edit this file with your actual values:
#   sudo nano /etc/balaji.env

RAZORPAY_KEY_ID=rzp_live_XXXXXXXXXXXXXXXX
RAZORPAY_KEY_SECRET=your_razorpay_secret_here

ADMIN_USERNAME=balaji_admin
ADMIN_PASSWORD=YourStrongAdminPassword@2024

DB_URL=jdbc:mysql://localhost:3306/balajiframes?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true
DB_USERNAME=balaji_db_user
DB_PASSWORD=ChangeThisDBPassword@123

UPLOAD_DIR=/opt/balaji/uploads
ENVEOF

sudo chmod 600 /etc/balaji.env   # only root can read
echo "  Environment file created at /etc/balaji.env ✅"
echo "  ⚠️  Edit it now: sudo nano /etc/balaji.env"

# ── 7. Create systemd service ─────────────────────────────────
echo "[7/8] Creating systemd service..."
sudo tee /etc/systemd/system/balaji-frames.service > /dev/null << 'SVCEOF'
[Unit]
Description=BALAJI Photo Frames Spring Boot App
After=network.target mysql.service
Requires=mysql.service

[Service]
Type=simple
User=ubuntu
Group=ubuntu
WorkingDirectory=/opt/balaji/app

# Load secrets from environment file
EnvironmentFile=/etc/balaji.env

# Run the JAR — profile=prod, external secrets via env vars
ExecStart=/usr/bin/java \
    -Xms256m -Xmx512m \
    -Djava.security.egd=file:/dev/./urandom \
    -jar /opt/balaji/app/balaji-frames.jar \
    --spring.profiles.active=prod

# Auto-restart on failure
Restart=on-failure
RestartSec=10

# Logs go to journald (view: journalctl -u balaji-frames -f)
StandardOutput=journal
StandardError=journal
SyslogIdentifier=balaji-frames

[Install]
WantedBy=multi-user.target
SVCEOF

sudo systemctl daemon-reload
echo "  Systemd service created ✅"

# ── 8. Configure Nginx ────────────────────────────────────────
echo "[8/8] Configuring Nginx reverse proxy..."
sudo tee /etc/nginx/sites-available/balaji-frames > /dev/null << 'NGXEOF'
server {
    listen 80;
    server_name _;  # Replace with your domain/IP

    # Upload file size limit
    client_max_body_size 10M;

    # Proxy all requests to Spring Boot
    location / {
        proxy_pass         http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout    60s;
    }

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN";
    add_header X-Content-Type-Options "nosniff";
    add_header X-XSS-Protection "1; mode=block";
}
NGXEOF

sudo ln -sf /etc/nginx/sites-available/balaji-frames /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
echo "  Nginx configured ✅"

# ── GCP Firewall ports ────────────────────────────────────────
echo ""
echo "═══════════════════════════════════════"
echo "  ✅ Setup Complete!"
echo "═══════════════════════════════════════"
echo ""
echo "NEXT STEPS:"
echo ""
echo "1. Edit secrets:"
echo "   sudo nano /etc/balaji.env"
echo ""
echo "2. Upload JAR:"
echo "   scp target/balaji-frames-1.0.0.jar user@<GCP-IP>:/opt/balaji/app/"
echo ""
echo "3. Start app:"
echo "   sudo systemctl start balaji-frames"
echo "   sudo systemctl enable balaji-frames"
echo ""
echo "4. Check status:"
echo "   sudo systemctl status balaji-frames"
echo "   journalctl -u balaji-frames -f"
echo ""
echo "5. GCP Firewall (run on your laptop):"
echo "   gcloud compute firewall-rules create allow-http --allow tcp:80"
echo ""
echo "6. Access: http://<GCP-EXTERNAL-IP>"
