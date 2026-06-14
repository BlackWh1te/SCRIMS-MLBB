---
name: changelog-guardian
preamble-tier: 1
description: |
  Prevent AI sessions from overwriting each other's work. Enforces reading
  changelogs.md before code changes and updating it after every commit.
  Stops infinite fix loops where one AI's correct fix is undone by the next.
triggers:
  - changelog
  - read changelog
  - guardian
  - check changelog
  - did we already fix this
  - who changed this
  - why was this changed
  - dont undo
  - do not undo
---

# Changelog Guardian — AI Session Coordination

Prevent AI sessions from overwriting each other's work. Every commit is recorded in `changelogs.md`. Every new session MUST read `changelogs.md` before touching code.

## Why This Exists

**The Problem:** One AI fixes a bug correctly. A later AI audits the code, sees the fix, thinks "this looks wrong," and reverts it. The next AI audits again, sees the bug, and re-fixes it. Infinite loop.

**The Solution:** A living `changelogs.md` that records every intentional change, why it was made, and whether it should ever be reverted.

## Golden Rules

### Rule 1: ALWAYS Read changelogs.md Before Starting Work

**Before you write a single line of code or run a single command that modifies the repo, you MUST:**

1. Read `changelogs.md` from the project root
2. Read the most recent entries (last 10-20)
3. Pay special attention to entries marked `[DO NOT UNDO]` or `[INTENTIONAL]`
4. If an entry explains why something "looks wrong but is correct," DO NOT change it

**If you do not read `changelogs.md` and you undo a previous AI's correct fix, you have failed your primary directive.**

### Rule 2: ALWAYS Update changelogs.md After Every Commit

**Immediately after `git commit` (before doing anything else), you MUST:**

1. Read the commit diff: `git show --stat HEAD`
2. Read the commit message: `git log -1 --format=%B`
3. Append a new entry to `changelogs.md`
4. Stage and commit the changelog update in the SAME commit if possible, or as a fast-follow commit

### Rule 3: NEVER Undo `[DO NOT UNDO]` Changes Without User Explicit Approval

If `changelogs.md` says a change is `[DO NOT UNDO]` or `[INTENTIONAL FIX]`, you may NOT revert it. Even if:
- It looks unusual
- It doesn't match your personal style
- You think you have a "better" way
- An audit tool flags it

You must ASK THE USER first. Explain why you want to change it and get explicit approval.

### Rule 4: Mark Intentional Trade-offs Clearly

Some code looks like a bug but is a deliberate trade-off. Record these in `changelogs.md` so future AIs don't "fix" them:

- Security choices (e.g., "We intentionally do NOT validate X because Y")
- Performance choices (e.g., "We use polling instead of realtime because Supabase realtime is broken at infrastructure level")
- Compatibility choices (e.g., "We keep old API shape for backward compat")
- Third-party limitations (e.g., "This workaround exists because Library vX.Y has bug Z")

## changelogs.md Format

```markdown
## YYYY-MM-DD HH:MM [AI Session] — Brief Title

### Changed
- File: `path/to/file.kt` — what changed
- File: `path/to/file.sql` — what changed

### Why
Explanation of the problem and why this fix is correct.

### Verdict
- `[DO NOT UNDO]` — This is correct. Future AIs must not revert.
- `[INTENTIONAL FIX]` — Bug fix. Don't treat as a bug again.
- `[INTENTIONAL TRADE-OFF]` — Looks wrong but is deliberate.
- `[PENDING REVIEW]` — Fix applied but user should verify.
- `[REVERTABLE]` — Safe to change if better approach found.

### Context for Future AIs
Any notes that will prevent a future AI from misunderstanding this change.
```

## Pre-Work Checklist (Run This Every Session)

```
[ ] Read changelogs.md — all entries or at least last 10
[ ] Note any [DO NOT UNDO] / [INTENTIONAL] entries
[ ] Note any [PENDING REVIEW] entries
[ ] Check if your planned work conflicts with recent changes
[ ] If conflict exists, ask user before proceeding
```

## Post-Commit Checklist (Run This After Every Commit)

```
[ ] Run: git show --stat HEAD
[ ] Run: git log -1 --format=%B
[ ] Append entry to changelogs.md following the format above
[ ] Stage: git add changelogs.md
[ ] Commit: git commit -m "docs: update changelogs.md"
[ ] If this was a fix for a previous AI's mistake, note that
```

## Common Anti-Patterns to Prevent

| Anti-Pattern | What to Write in changelogs.md |
|--------------|-------------------------------|
| "This looks weird, I'll clean it up" | `[DO NOT UNDO] The 'weird' code is a workaround for Library X bug. See issue #123.` |
| "Tests are failing, I'll delete them" | `[INTENTIONAL TRADE-OFF] Tests fail because they test old mock repos. App uses Supabase repos now. Rewriting tests is planned.` |
| "Security audit says this is bad" | `[INTENTIONAL TRADE-OFF] Certificate pinning disabled because real cert not yet provisioned. Enable before production release.` |
| "This field is empty, I'll fill it" | `[DO NOT UNDO] EXPECTED_SIGNATURE_SHA256 is empty because app is in dev mode. Filled via CI for release builds.` |
| "I'll bump this dependency" | `[REVERTABLE] Compose BOM bumped from 2024.02.00 to 2024.06.00. Can upgrade further if needed.` |

## Special Cases

### Migration Files
Migrations are append-only. Never edit a migration that was already pushed to remote (`supabase db push`). If a migration is wrong, create a NEW migration that fixes it. Record both in changelogs.md.

### Supabase Remote Changes
If you run `supabase db push`, record:
- Which migrations were pushed
- Whether they succeeded or failed
- Any manual fixes applied on the remote

### Build/Gradle Changes
Gradle changes affect the entire build. Mark these as `[DO NOT UNDO]` unless the user explicitly wants to revert.

### When You Accidentally Undo Something
1. STOP immediately
2. Revert your revert: `git revert HEAD`
3. Add to changelogs.md: `[CORRECTION] Reverted accidental undo of <previous change>. Restored original fix.`
4. Explain what you learned so future AIs don't make the same mistake

## Preamble (runs when skill is invoked)

```bash
# Check if changelogs.md exists and show its status
if [ -f "changelogs.md" ]; then
    echo "=== CHANGELOG STATUS ==="
    echo "File: changelogs.md"
    echo "Last modified: $(git log -1 --format=%cd --date=iso -- changelogs.md 2>/dev/null || echo 'Not tracked by git')"
    echo ""
    echo "Latest entry heading:"
    grep "^## " changelogs.md | head -1 || echo "No entries found"
    echo ""
    echo "DO NOT UNDO count: $(grep -c '\[DO NOT UNDO\]' changelogs.md 2>/dev/null || echo 0)"
    echo "INTENTIONAL FIX count: $(grep -c '\[INTENTIONAL FIX\]' changelogs.md 2>/dev/null || echo 0)"
    echo ""
    echo "Latest commits not yet in changelog (if any):"
    # Check if latest commit is mentioned in changelogs.md
    LATEST_COMMIT=$(git log -1 --format=%h 2>/dev/null)
    if [ -n "$LATEST_COMMIT" ]; then
        if grep -q "$LATEST_COMMIT" changelogs.md 2>/dev/null; then
            echo "✅ Latest commit ($LATEST_COMMIT) is recorded in changelog"
        else
            echo "⚠️  Latest commit ($LATEST_COMMIT) is NOT in changelog — update required!"
        fi
    fi
    echo "========================"
else
    echo "❌ changelogs.md NOT FOUND — this should exist! Create it immediately."
fi
```

## Invocation

**To invoke this skill directly:** say `/changelog-guardian`

**Voice triggers:** "changelog", "read changelog", "guardian", "check changelog", "did we already fix this", "who changed this", "why was this changed", "don't undo", "do not undo"

**This skill is also enforced via `CLAUDE.md`** — every AI session on this repo automatically reads the changelog rules from `CLAUDE.md` at session start.
