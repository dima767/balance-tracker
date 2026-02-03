#!/bin/bash
# ╔════════════════════════════════════════════════════════════╗
# ║  BALANCE TRACKER - Stop Standalone Environment             ║
# ╚════════════════════════════════════════════════════════════╝
#
# Usage:
#   ./compose-down-standalone.sh      - Stop (preserve data)
#   ./compose-down-standalone.sh -v   - Stop and remove all data
#

VOLUMES_FLAG=""
if [ "$1" = "-v" ]; then
    VOLUMES_FLAG="-v"
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Stopping Balance Tracker (with volume removal)            ║"
    echo "╚════════════════════════════════════════════════════════════╝"
else
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Stopping Balance Tracker                                  ║"
    echo "╚════════════════════════════════════════════════════════════╝"
fi
echo ""

docker compose -f docker-compose.standalone.yml down $VOLUMES_FLAG

if [ $? -eq 0 ]; then
    echo ""
    if [ -n "$VOLUMES_FLAG" ]; then
        echo "  Balance Tracker stopped and all data removed."
        echo ""
        echo "  Removed volumes:"
        echo "    - postgres_data (database)"
        echo "    - postgres_config (database config)"
        echo "    - app_logs (application logs)"
        echo "    - app_certs (SSL certificates)"
        echo ""
    else
        echo "  Balance Tracker stopped. Data preserved in Docker volumes."
        echo ""
        echo "  Persistent volumes:"
        echo "    - postgres_data (database)"
        echo "    - app_logs (application logs)"
        echo "    - app_certs (SSL certificates)"
        echo ""
        echo "  To remove all data: ./compose-down-standalone.sh -v"
        echo ""
    fi
else
    echo ""
    echo "  Failed to stop Balance Tracker."
    echo ""
    exit 1
fi
