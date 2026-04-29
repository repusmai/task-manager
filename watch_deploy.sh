#!/bin/bash

REPO="repusmai/task-manager"
TOKEN_FILE="$HOME/.github_deploy_token"

if [ -f "$TOKEN_FILE" ]; then
    TOKEN=$(cat "$TOKEN_FILE")
else
    echo "Enter your GitHub Personal Access Token:"
    read -s TOKEN
    echo "$TOKEN" > "$TOKEN_FILE"
    chmod 600 "$TOKEN_FILE"
fi

AUTH="Authorization: Bearer $TOKEN"
gh_api() { curl -s -H "$AUTH" -H "Accept: application/vnd.github+json" "$@"; }

echo ""
echo "┌─────────────────────────────────────────┐"
echo "│        Task Manager Deploy Tool         │"
echo "└─────────────────────────────────────────┘"
echo ""

if [ -n "$(git status --porcelain)" ]; then
    echo "📦 Changes detected, committing and pushing..."
    echo "Enter commit message:"
    read COMMIT_MSG
    git add .
    git commit -m "$COMMIT_MSG"
    git push origin main
    echo ""
    echo "✅ Pushed to GitHub!"
else
    echo "ℹ️  No local changes to push."
    echo "   Watching for any currently running deploy..."
fi

echo ""
echo "⏳ Waiting for GitHub Actions to start..."

RUN_ID=""
ATTEMPTS=0
while [ -z "$RUN_ID" ] && [ $ATTEMPTS -lt 20 ]; do
    sleep 2
    ATTEMPTS=$((ATTEMPTS + 1))
    RUN_ID=$(gh_api "https://api.github.com/repos/$REPO/actions/runs?per_page=1&branch=main" \
        | python3 -c "
import sys, json
data = json.load(sys.stdin)
runs = data.get('workflow_runs', [])
if runs:
    r = runs[0]
    if r['status'] in ('queued', 'in_progress'):
        print(r['id'])
" 2>/dev/null)
done

if [ -z "$RUN_ID" ]; then
    echo "❌ Could not detect a running Actions workflow."
    exit 1
fi

echo "🚀 Build started! Run ID: $RUN_ID"
echo "   https://github.com/$REPO/actions/runs/$RUN_ID"
echo ""

# ── Spinner runs entirely on internal clock ──────────────────────────────────
SPINNER=('⠋' '⠙' '⠹' '⠸' '⠼' '⠴' '⠦' '⠧' '⠇' '⠏')
START_TIME=$(date +%s)

spinner_loop() {
    local idx=0
    while true; do
        local now=$(date +%s)
        local elapsed=$(( now - START_TIME ))
        local mins=$(( elapsed / 60 ))
        local secs=$(( elapsed % 60 ))
        printf "\r${SPINNER[$idx]} Building... [%dm %02ds elapsed]   " $mins $secs
        idx=$(( (idx + 1) % 10 ))
        sleep 0.1
    done
}

# Start spinner in background, save its PID
spinner_loop &
SPINNER_PID=$!

# ── API polling loop — every 2 seconds ───────────────────────────────────────
STATUS="in_progress"
CONCLUSION=""

while [ "$STATUS" != "completed" ]; do
    sleep 2
    RESULT=$(gh_api "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID" \
        | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(data.get('status', 'unknown'))
print(data.get('conclusion', ''))
" 2>/dev/null)
    STATUS=$(echo "$RESULT" | head -1)
    CONCLUSION=$(echo "$RESULT" | tail -1)
done

# ── Stop spinner ──────────────────────────────────────────────────────────────
kill $SPINNER_PID 2>/dev/null
wait $SPINNER_PID 2>/dev/null

ELAPSED=$(( $(date +%s) - START_TIME ))
MINS=$(( ELAPSED / 60 ))
SECS=$(( ELAPSED % 60 ))
ELAPSED_STR=$(printf "%dm %02ds" $MINS $SECS)

echo ""
echo ""
if [ "$CONCLUSION" = "success" ]; then
    echo "┌─────────────────────────────────────────┐"
    echo "│  ✅ Deploy successful! (${ELAPSED_STR})       │"
    echo "│  APK is live on GitHub Releases         │"
    echo "│  Open your app → Settings → Update      │"
    echo "└─────────────────────────────────────────┘"
    command -v notify-send &>/dev/null && \
        notify-send "Task Manager Deployed ✅" "Built in ${ELAPSED_STR}. New APK is live." --icon=dialog-information
else
    echo "┌─────────────────────────────────────────┐"
    printf "│  ❌ Build failed (%-22s│\n" "${CONCLUSION})"
    echo "│  Check Actions tab for details          │"
    echo "└─────────────────────────────────────────┘"
    echo "   https://github.com/$REPO/actions/runs/$RUN_ID"
    command -v notify-send &>/dev/null && \
        notify-send "Task Manager Build Failed ❌" "Check GitHub Actions." --icon=dialog-error
fi
