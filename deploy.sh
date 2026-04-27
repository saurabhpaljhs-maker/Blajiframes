#!/bin/bash
# ═══════════════════════════════════════════════════════════════
#  deploy.sh — Local se GCP pe deploy karo
#  Usage: ./deploy.sh <GCP-IP> <SSH-USER>
#  Example: ./deploy.sh 34.100.200.50 ubuntu
# ═══════════════════════════════════════════════════════════════
set -e

GCP_IP=${1:?"Usage: ./deploy.sh <GCP-IP> <SSH-USER>"}
SSH_USER=${2:-ubuntu}
JAR="target/balaji-frames-1.0.0.jar"

echo "🚀 Deploying BALAJI Frames to GCP: $GCP_IP"

# Step 1: Build JAR
echo "[1/4] Building JAR..."
mvn clean package -DskipTests -q
echo "  JAR built: $JAR ✅"

# Step 2: Upload JAR to GCP
echo "[2/4] Uploading to GCP..."
scp "$JAR" "$SSH_USER@$GCP_IP:/opt/balaji/app/balaji-frames.jar"
echo "  Upload done ✅"

# Step 3: Restart service
echo "[3/4] Restarting service..."
ssh "$SSH_USER@$GCP_IP" "sudo systemctl restart balaji-frames"
echo "  Service restarted ✅"

# Step 4: Health check
echo "[4/4] Health check..."
sleep 15
for i in 1 2 3 4 5; do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://$GCP_IP/ 2>/dev/null || echo "000")
    echo "  Attempt $i/5 — HTTP $STATUS"
    if [ "$STATUS" = "200" ]; then
        echo ""
        echo "✅ Deployment successful!"
        echo "🌐 App live at: http://$GCP_IP"
        echo "🔐 Admin panel: http://$GCP_IP/admin/orders"
        exit 0
    fi
    sleep 5
done

echo "❌ Health check failed — check logs:"
echo "   ssh $SSH_USER@$GCP_IP 'journalctl -u balaji-frames -n 50'"
exit 1
