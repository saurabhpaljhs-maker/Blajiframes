# 🖼 BALAJI Photo Frames
> Premium Custom Photo Frame Ordering System — Spring Boot + MySQL + Razorpay

**Address:** Rajpur, Shamshabad Road, Agra 282001  
**Phone/WhatsApp:** +91 82995 76949

---

## ⚡ Quick Start (Local Dev)

```bash
# 1. Secrets setup karo
cp .env.example .env
# .env file mein apni values daalo

# 2. Run karo (H2 in-memory DB auto use hoga)
export $(cat .env | xargs)
mvn spring-boot:run

# 3. Browser mein kholo
# http://localhost:8080           ← Customer site
# http://localhost:8080/login     ← Admin login
# http://localhost:8080/h2-console ← DB console (dev only)
```

---

## 🔐 Security (Important)

| File | GitHub pe? | Kya hai |
|------|-----------|---------|
| `.env` | ❌ NEVER | Real secrets |
| `.env.example` | ✅ Yes | Template only |
| `application.properties` | ✅ Yes | No secrets |
| `application-prod.properties` | ❌ NEVER | Prod config |

### Secrets kahan rakho:
- **Dev:** `.env` file (root folder mein, gitignored)
- **Prod:** `/etc/balaji.env` (GCP VM pe, chmod 600)

---

## 🚀 GCP Production Deploy

```bash
# 1. GCP VM pe ek baar setup karo
bash setup-gcp.sh

# 2. Secrets edit karo
sudo nano /etc/balaji.env

# 3. Deploy karo (local laptop se)
./deploy.sh <GCP-IP> ubuntu

# 4. Logs dekho
ssh ubuntu@<GCP-IP> 'journalctl -u balaji-frames -f'
```

---

## 📁 Project Structure

```
balaji-frames-app/
├── src/main/java/com/balaji/
│   ├── config/          ← SecurityConfig, DataInitializer, WebConfig
│   ├── controller/      ← Home, Order, Payment, Admin controllers
│   ├── dto/             ← Request/Response DTOs with validation
│   ├── exception/       ← GlobalExceptionHandler, custom exceptions
│   ├── model/           ← JPA entities (Order, FrameSize, etc.)
│   ├── repository/      ← Spring Data JPA repos
│   ├── security/        ← CustomUserDetailsService, LoginAttemptService
│   └── service/         ← Business logic (Frame, Order, Payment, File)
├── src/main/resources/
│   ├── application.properties       ← No secrets (safe for GitHub)
│   ├── application-dev.properties   ← H2 DB config
│   ├── application-prod.properties  ← MySQL + env vars (gitignored)
│   └── templates/                   ← Thymeleaf HTML templates
├── .env.example    ← Copy to .env and fill values
├── .gitignore      ← .env and prod properties excluded
├── setup-gcp.sh    ← One-time GCP server setup
├── deploy.sh       ← One-command deploy to GCP
└── pom.xml
```

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.2, Java 17 |
| Security | Spring Security + BCrypt + Brute Force Protection |
| Database | H2 (dev) / MySQL (prod) |
| Payment | Razorpay (HMAC signature verification) |
| Frontend | Thymeleaf, CSS3, Vanilla JS |
| Server | Nginx (reverse proxy) |
| Deploy | GCP Ubuntu VM + systemd |

---

## 🔑 Admin Panel

- URL: `http://your-domain/login`
- Credentials: Set in `.env` → `ADMIN_USERNAME` / `ADMIN_PASSWORD`
- Features: View all orders, update order status, revenue stats
