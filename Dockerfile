# ═══════════════════════════════════════════
#  BALAJI PHOTO FRAMES — Dockerfile
#  Multi-stage build for smallest image size
# ═══════════════════════════════════════════

# ── Stage 1: Build ──────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build
COPY pom.xml .
COPY src ./src

# Download deps first (cached layer)
RUN mvn dependency:go-offline -q

# Build JAR (skip tests — run in CI separately)
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime (small image) ──────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="BALAJI Photo Frames"
LABEL description="Photo Frame Ordering Application"

# ── Non-root user for security ────────────────────────────────────
RUN addgroup -S balaji && adduser -S balaji -G balaji

# ── App directories ───────────────────────────────────────────────
RUN mkdir -p /app /opt/balaji/uploads /opt/balaji/logs \
    && chown -R balaji:balaji /app /opt/balaji

WORKDIR /app

# ── Copy JAR from builder ─────────────────────────────────────────
COPY --from=builder /build/target/balaji-frames-1.0.0.jar app.jar
RUN chown balaji:balaji app.jar

# ── Switch to non-root ────────────────────────────────────────────
USER balaji

# ── Expose port ───────────────────────────────────────────────────
EXPOSE 8080

# ── All secrets passed as ENV at runtime — NEVER baked in ─────────
# docker run -e RAZORPAY_KEY_ID=xxx -e DB_PASSWORD=yyy ...
ENV APP_ENV=prod \
    UPLOAD_DIR=/opt/balaji/uploads \
    LOG_LEVEL=WARN

# ── Health check ──────────────────────────────────────────────────
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/ || exit 1

# ── Start command ─────────────────────────────────────────────────
ENTRYPOINT ["java", \
    "-Xmx512m", "-Xms256m", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar", \
    "--spring.profiles.active=prod"]
