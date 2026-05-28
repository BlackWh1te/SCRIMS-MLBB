#!/bin/bash
# Changelog Guardian Verification Script
# Run this to check if changelogs.md is up to date with git commits

set -e

CHANGELOG="changelogs.md"

if [ ! -f "$CHANGELOG" ]; then
    echo "❌ FAIL: $CHANGELOG not found"
    exit 1
fi

# Get the latest commit hash
LATEST_COMMIT=$(git log -1 --format=%h)
LATEST_COMMIT_FULL=$(git log -1 --format=%H)
LATEST_MSG=$(git log -1 --format=%s)

# Check if the latest commit is mentioned in the changelog
if grep -q "$LATEST_COMMIT" "$CHANGELOG" 2>/dev/null || \
   grep -q "$LATEST_COMMIT_FULL" "$CHANGELOG" 2>/dev/null; then
    echo "✅ PASS: Latest commit ($LATEST_COMMIT) is recorded in $CHANGELOG"
else
    echo "⚠️  WARN: Latest commit ($LATEST_COMMIT) is NOT in $CHANGELOG"
    echo "   Commit: $LATEST_MSG"
    echo "   Run: git show --stat HEAD && git log -1 --format=%B"
    echo "   Then append the details to $CHANGELOG"
    exit 1
fi

# Count DO NOT UNDO entries
UNDO_COUNT=$(grep -c '\[DO NOT UNDO\]' "$CHANGELOG" 2>/dev/null || echo 0)
FIX_COUNT=$(grep -c '\[INTENTIONAL FIX\]' "$CHANGELOG" 2>/dev/null || echo 0)
TRADE_COUNT=$(grep -c '\[INTENTIONAL TRADE-OFF\]' "$CHANGELOG" 2>/dev/null || echo 0)

echo ""
echo "=== Changelog Summary ==="
echo "[DO NOT UNDO] entries:        $UNDO_COUNT"
echo "[INTENTIONAL FIX] entries:    $FIX_COUNT"
echo "[INTENTIONAL TRADE-OFF] entries: $TRADE_COUNT"
echo "========================="
