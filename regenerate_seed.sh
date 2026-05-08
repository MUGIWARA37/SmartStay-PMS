#!/bin/bash
# Regenerate seed.sql with current date rolling window
cd "$(dirname "$0")/src/main/resources/sql"
python3 gen_seed.py > seed.sql
echo "✓ seed.sql regenerated with rolling date window ($(date '+%Y-%m-%d'))"
