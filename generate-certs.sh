#!/bin/bash
set -e

# ═══════════════════════════════════════════════════════════════════════════
# BALANCE TRACKER - SSL Certificate Generator
# ═══════════════════════════════════════════════════════════════════════════
#
# Generates a self-signed certificate for local development.
# All settings can be customized via environment variables.
#
# Usage:
#   ./generate-certs.sh                    # Use defaults
#   CERT_HOSTS="myapp.local,localhost" ./generate-certs.sh
#
# Environment Variables:
#   CERT_HOSTS      - Comma-separated hostnames (default: localhost,dk.local)
#   CERT_IPS        - Comma-separated IPs (default: 127.0.0.1)
#   CERT_CN         - Certificate Common Name (default: first hostname)
#   CERT_PASSWORD   - Keystore password (default: changeit)
#   CERT_VALIDITY   - Validity in days (default: 3650)
#   CERT_ALIAS      - Key alias (default: balancetracker)
#
# ═══════════════════════════════════════════════════════════════════════════

# Configurable via environment variables with defaults
CERT_HOSTS="${CERT_HOSTS:-localhost,dk.local}"
CERT_IPS="${CERT_IPS:-127.0.0.1}"
CERT_CN="${CERT_CN:-}"
CERT_PASSWORD="${CERT_PASSWORD:-changeit}"
CERT_VALIDITY="${CERT_VALIDITY:-3650}"
CERT_ALIAS="${CERT_ALIAS:-balancetracker}"

# Fixed paths
CERTS_DIR="src/main/resources/ssl"
KEYSTORE_PATH="${CERTS_DIR}/keystore.p12"

# If CN not specified, use first hostname
if [ -z "$CERT_CN" ]; then
    CERT_CN=$(echo "$CERT_HOSTS" | cut -d',' -f1)
fi

# Build SAN extension string
SAN_PARTS=""
IFS=',' read -ra HOST_ARRAY <<< "$CERT_HOSTS"
for host in "${HOST_ARRAY[@]}"; do
    host=$(echo "$host" | xargs)  # trim whitespace
    if [ -n "$SAN_PARTS" ]; then
        SAN_PARTS="${SAN_PARTS},"
    fi
    SAN_PARTS="${SAN_PARTS}DNS:${host}"
done

IFS=',' read -ra IP_ARRAY <<< "$CERT_IPS"
for ip in "${IP_ARRAY[@]}"; do
    ip=$(echo "$ip" | xargs)  # trim whitespace
    SAN_PARTS="${SAN_PARTS},IP:${ip}"
done

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║  BALANCE TRACKER - Certificate Generator                   ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
echo "Configuration:"
echo "  Hosts:    ${CERT_HOSTS}"
echo "  IPs:      ${CERT_IPS}"
echo "  CN:       ${CERT_CN}"
echo "  Validity: ${CERT_VALIDITY} days"
echo ""

# Create certs directory if it doesn't exist
mkdir -p "${CERTS_DIR}"

# Check if keystore already exists
if [ -f "${KEYSTORE_PATH}" ]; then
    echo "Keystore already exists at: ${KEYSTORE_PATH}"
    read -p "Overwrite? (y/N): " confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        echo "Aborted."
        exit 0
    fi
    rm -f "${KEYSTORE_PATH}"
fi

echo "Generating self-signed certificate..."
echo ""

keytool -genkeypair \
    -alias "${CERT_ALIAS}" \
    -keyalg RSA \
    -keysize 2048 \
    -storetype PKCS12 \
    -keystore "${KEYSTORE_PATH}" \
    -storepass "${CERT_PASSWORD}" \
    -validity "${CERT_VALIDITY}" \
    -dname "CN=${CERT_CN}, OU=Balance Tracker Dev, O=Development, L=Local, ST=Dev, C=US" \
    -ext "SAN=${SAN_PARTS}"

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║  Certificate Generated Successfully                        ║"
echo "╠════════════════════════════════════════════════════════════╣"
echo "║  Path:     ${KEYSTORE_PATH}"
echo "║  Password: ${CERT_PASSWORD}"
echo "║  Alias:    ${CERT_ALIAS}"
echo "║  CN:       ${CERT_CN}"
echo "║  SAN:      ${SAN_PARTS}"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Show /etc/hosts hints for non-localhost hostnames
NEED_HOSTS_ENTRY=false
for host in "${HOST_ARRAY[@]}"; do
    host=$(echo "$host" | xargs)
    if [ "$host" != "localhost" ]; then
        NEED_HOSTS_ENTRY=true
        break
    fi
done

if [ "$NEED_HOSTS_ENTRY" = true ]; then
    echo "Add to /etc/hosts if not present:"
    for host in "${HOST_ARRAY[@]}"; do
        host=$(echo "$host" | xargs)
        if [ "$host" != "localhost" ]; then
            echo "  127.0.0.1  ${host}"
        fi
    done
    echo ""
fi
