#!/bin/bash
#
# Run local integration validation test for nf-lims
#

set -e

# Make sure we are in the repo root
cd "$(dirname "$0")/.."

echo "===================================================="
echo "Step 1: Installing plugin locally..."
echo "===================================================="
./install-plugin.sh

echo "===================================================="
echo "Step 2: Starting Mock LIMS server..."
echo "===================================================="
python3 validation/mock_lims.py > validation_server.log 2>&1 &
MOCK_PID=$!

# Ensure mock server is stopped on exit
cleanup() {
    echo "Stopping Mock LIMS server (PID: $MOCK_PID)..."
    kill $MOCK_PID || true
    rm -f validation_server.log
}
trap cleanup EXIT

# Wait a moment for server to start
sleep 2

echo "===================================================="
echo "Step 3: Running successful Nextflow pipeline..."
echo "===================================================="
set +e
nextflow run validation/test_lims.nf -c validation/nextflow.config > validation_run.log 2>&1
NF_EXIT=$?
set -e

# Print nextflow output log for visibility
cat validation_run.log

echo "===================================================="
echo "Step 4: Verifying LIMS Observer Log Output..."
echo "===================================================="

# Print server log for visibility
echo "--- Mock LIMS Server Log Output ---"
cat validation_server.log || true
echo "-----------------------------------"

# Check status PATCH request
if grep -q "custom_status_field" validation_server.log; then
    echo "✓ Custom status field successfully received by server."
else
    echo "❌ FAILURE: Custom status field not found in server logs."
    exit 1
fi

# Check file upload POST request
if grep -q "LIMS file upload response: SUCCESS (200)" .nextflow.log; then
    echo "✓ File upload logged successfully."
else
    echo "❌ FAILURE: LIMS file upload success log not found."
    exit 1
fi

echo ""
echo "🎉 SUCCESS: The LIMS Observer successfully updated the status and uploaded the file!"
echo ""
exit 0
