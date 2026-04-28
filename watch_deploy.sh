#!/bin/bash

# ── Config ─────────────────────────────────────────────────────────────────
REPO="repusmai/task-manager"
TOKEN_FILE="$HOME/.github_deploy_token"

# ── Get token ──────────────────────────────────────────────────────────────
if [ -f "$TOKEN_FILE" ]; then
    TOKEN=$(cat "$TOKEN_FILE")
else
    echo "Enter your GitHub Personal Access Token:"
    read -s TOKEN
    echo "$TOKEN" > "$TOKEN_FILE"
    chmod 600 "$TOKEN_FILE"
fi

AUTH="Authorization: Bearer $TOKEN"

# ── Helper: GitHub API ─────────────────────────────────────────────────────
gh_api() {
    curl -s -H "$AUTH" -H "Accept: application/vnd.github+json" "$@"
}

# ── Step 1: Push ───────────────────────────────────────────────────────────
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

# ── Step 2: Wait for Actions run to appear ────────────────────────────────
echo ""
echo "⏳ Waiting for GitHub Actions to start..."

RUN_ID=""
ATTEMPTS=0
while [ -z "$RUN_ID" ] && [ $ATTEMPTS -lt 15 ]; do
    sleep 4
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
    echo "   Check the Actions tab on GitHub manually."
    exit 1
fi

echo "🚀 Build started! Run ID: $RUN_ID"
echo "   https://github.com/$REPO/actions/runs/$RUN_ID"
echo ""

# ── Step 3: Poll until complete ───────────────────────────────────────────
SPINNER=('⠋' '⠙' '⠹' '⠸' '⠼' '⠴' '⠦' '⠧' '⠇' '⠏')
SPIN_IDX=0
START_TIME=$(date +%s)

while true; do
    sleep 5

    RESULT=$(gh_api "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID" \
        | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(data.get('status', 'unknown'))
print(data.get('conclusion', ''))
" 2>/dev/null)

    STATUS=$(echo "$RESULT" | head -1)
    CONCLUSION=$(echo "$RESULT" | tail -1)

    ELAPSED=$(( $(date +%s) - START_TIME ))
    MINS=$((ELAPSED / 60))
    SECS=$((ELAPSED % 60))
    ELAPSED_STR=$(printf "%dm %02ds" $MINS $SECS)

    SPIN=${SPINNER[$SPIN_IDX]}
    SPIN_IDX=$(( (SPIN_IDX + 1) % 10 ))

    printf "\r${SPIN} Building... [%s elapsed]   " "$ELAPSED_STR"

    if [ "$STATUS" = "completed" ]; then
        echo ""
        echo ""
        if [ "$CONCLUSION" = "success" ]; then
            echo "┌─────────────────────────────────────────┐"
            echo "│  ✅ Deploy successful! (${ELAPSED_STR})       │"
            echo "│  APK is live on GitHub Releases         │"
            echo "│  Open your app → Settings → Update      │"
            echo "└─────────────────────────────────────────┘"

            # Try to send a desktop notification
            if command -v notify-send &> /dev/null; then
                notify-send "Task Manager Deployed ✅" "New APK is live. Open app to update." --icon=dialog-information
            fi
        else
            echo "┌─────────────────────────────────────────┐"
            printf "│  ❌ Build failed (conclusion: %-10s│\n" "${CONCLUSION})"
            echo "│  Check Actions tab for details          │"
            echo "└─────────────────────────────────────────┘"
            echo "   https://github.com/$REPO/actions/runs/$RUN_ID"

            if command -v notify-send &> /dev/null; then
                notify-send "Task Manager Build Failed ❌" "Check GitHub Actions for details." --icon=dialog-error
            fi
        fi
        exit 0
    fi
done
