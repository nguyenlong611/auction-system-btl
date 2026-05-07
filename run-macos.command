#!/bin/bash
# Chuyển hướng về thư mục chứa script
cd "$(dirname "$0")"

echo "=========================================="
echo "   ADJUGE - STARTING APPLICATION (macOS)  "
echo "=========================================="
echo ""

mvn clean javafx:run
