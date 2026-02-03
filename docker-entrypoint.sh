#!/bin/bash
# ╔════════════════════════════════════════════════════════════╗
# ║  BALANCE TRACKER - Docker Entrypoint                       ║
# ║  Handles SSL certificate generation and application start  ║
# ╚════════════════════════════════════════════════════════════╝

set -e

# Certificate configuration (all configurable via environment variables)
CERT_DIR="${CERT_DIR:-/app/certs}"
CERT_PASSWORD="${CERT_PASSWORD:-balancetracker-dev-cert}"
CERT_ALIAS="${CERT_ALIAS:-balancetracker-local}"
CERT_HOSTS="${CERT_HOSTS:-localhost,balancetracker.local}"
CERT_IPS="${CERT_IPS:-127.0.0.1,0.0.0.0}"
CERT_VALIDITY="${CERT_VALIDITY:-3650}"
KEYSTORE_FILE="${CERT_DIR}/balancetracker-keystore.p12"

# ============================================================
# SSL Certificate Generation
# ============================================================
generate_certificate() {
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Generating Self-Signed SSL Certificate                    ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""

    # Build SAN (Subject Alternative Names) extension
    SAN_EXT="san=dns:localhost"

    # Add DNS names
    IFS=',' read -ra HOSTS <<< "$CERT_HOSTS"
    for host in "${HOSTS[@]}"; do
        host=$(echo "$host" | xargs)  # Trim whitespace
        if [ -n "$host" ] && [ "$host" != "localhost" ]; then
            SAN_EXT="${SAN_EXT},dns:${host}"
        fi
    done

    # Add IP addresses
    SAN_EXT="${SAN_EXT},ip:127.0.0.1"
    IFS=',' read -ra IPS <<< "$CERT_IPS"
    for ip in "${IPS[@]}"; do
        ip=$(echo "$ip" | xargs)  # Trim whitespace
        if [ -n "$ip" ] && [ "$ip" != "127.0.0.1" ]; then
            SAN_EXT="${SAN_EXT},ip:${ip}"
        fi
    done

    # Get CN from first hostname
    CERT_CN=$(echo "$CERT_HOSTS" | cut -d',' -f1 | xargs)

    echo "  Certificate Configuration:"
    echo "    CN (Common Name): ${CERT_CN}"
    echo "    DNS Names: ${CERT_HOSTS}"
    echo "    IP Addresses: 127.0.0.1,${CERT_IPS}"
    echo "    Validity: ${CERT_VALIDITY} days"
    echo "    Keystore: ${KEYSTORE_FILE}"
    echo ""

    # Generate the certificate
    keytool -genkeypair \
        -alias "$CERT_ALIAS" \
        -keyalg RSA \
        -keysize 2048 \
        -storetype PKCS12 \
        -keystore "$KEYSTORE_FILE" \
        -storepass "$CERT_PASSWORD" \
        -validity "$CERT_VALIDITY" \
        -dname "CN=${CERT_CN}, OU=Balance Tracker Docker, O=Development, L=Local, ST=Dev, C=US" \
        -ext "$SAN_EXT" \
        2>/dev/null

    echo "  Certificate generated successfully!"
    echo ""
}

# Check if certificate exists, generate if not
if [ ! -f "$KEYSTORE_FILE" ]; then
    generate_certificate
else
    echo ""
    echo "  Using existing SSL certificate: ${KEYSTORE_FILE}"
    echo "  (Delete the certs volume to regenerate)"
    echo ""
fi

# ============================================================
# Start Application
# ============================================================
echo "╔════════════════════════════════════════════════════════════╗"
echo "║  Starting Balance Tracker Application                      ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Default Java options for containers
DEFAULT_JAVA_OPTS="-Xms256m -Xmx512m"

# Java 25 requires these flags for Spring Boot
JAVA_25_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED"

# Combine all options
FINAL_JAVA_OPTS="${JAVA_25_OPTS} ${JAVA_OPTS:-$DEFAULT_JAVA_OPTS}"

echo "  Java Options: ${FINAL_JAVA_OPTS}"
echo ""

# Execute the application
exec java ${FINAL_JAVA_OPTS} -jar /app/app.jar
