#!/bin/bash
# ╔════════════════════════════════════════════════════════════╗
# ║  BALANCE TRACKER - Start Standalone Environment            ║
# ║  Full stack: PostgreSQL + Application with SSL             ║
# ╚════════════════════════════════════════════════════════════╝
#
# Usage:
#   ./compose-up-standalone.sh       - Start (use cached image)
#   ./compose-up-standalone.sh -b    - Build and start
#

BUILD_FLAG=""
while getopts "b" opt; do
    case $opt in
        b)
            BUILD_FLAG="--build"
            ;;
        \?)
            echo "Usage: $0 [-b]"
            echo "  -b    Build/rebuild the application image"
            exit 1
            ;;
    esac
done

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║  Starting Balance Tracker (Standalone Mode)                ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Load environment variables for port display
APP_HTTPS_PORT="${APP_HTTPS_PORT:-8443}"
APP_HTTP_PORT="${APP_HTTP_PORT:-8080}"

# Check for .env file and source it
if [ -f .env ]; then
    echo "  Using configuration from .env file"
    # Source .env to get port variables
    set -a
    source .env
    set +a
    APP_HTTPS_PORT="${APP_HTTPS_PORT:-8443}"
    APP_HTTP_PORT="${APP_HTTP_PORT:-8080}"
else
    echo "  No .env file found, using defaults"
    echo "  Tip: Copy .env.example to .env to customize"
fi
echo ""

# Start services
if [ -n "$BUILD_FLAG" ]; then
    echo "  Building and starting services..."
    docker compose -f docker-compose.standalone.yml up -d --build
else
    echo "  Starting services..."
    docker compose -f docker-compose.standalone.yml up -d
fi

if [ $? -eq 0 ]; then
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Balance Tracker Started Successfully!                     ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    echo "  Application URLs:"
    echo "    HTTPS: https://localhost:${APP_HTTPS_PORT}/balancetracker"
    echo "    HTTP:  http://localhost:${APP_HTTP_PORT}/balancetracker"
    echo ""
    echo "  Note: Your browser will show a security warning for the"
    echo "        self-signed certificate. This is expected for local"
    echo "        development - click 'Advanced' and proceed."
    echo ""
    echo "  Useful Commands:"
    echo "    View logs (all):    docker compose -f docker-compose.standalone.yml logs -f"
    echo "    View logs (app):    docker compose -f docker-compose.standalone.yml logs -f app"
    echo "    View logs (db):     docker compose -f docker-compose.standalone.yml logs -f postgres"
    echo "    Stop:               ./compose-down-standalone.sh"
    echo "    Stop + remove data: ./compose-down-standalone.sh -v"
    echo ""
else
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Failed to Start Balance Tracker                           ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    echo "  Check the logs for details:"
    echo "    docker compose -f docker-compose.standalone.yml logs"
    echo ""
    exit 1
fi
