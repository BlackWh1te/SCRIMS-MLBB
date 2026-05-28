import re
from pathlib import Path

base = Path(r"C:\Users\Shukhrat\Desktop\New folder\git\Android\app\src\main")
strings_xml = base / "res" / "values" / "strings.xml"
screens_dir = base / "java" / "com" / "mlbb" / "scrim" / "ui" / "screens"

with open(strings_xml, "r", encoding="utf-8") as f:
    strings_content = f.read()

existing = set(re.findall(r'<string[^>]*name="([^"]+)"', strings_content))

# Missing strings to add
missing = {
    "accept": "Accept",
    "apply": "Apply",
    "avg_rating": "Avg Rating",
    "clear_filters": "Clear Filters",
    "decline": "Decline",
    "disband": "Disband",
    "find_teams_title": "Find Teams",
    "invite_code": "Invite Code",
    "invite_code_placeholder": "Enter invite code...",
    "join_requests_count": "Join Requests (%d)",
    "keep_scrim": "Keep Scrim",
    "leave": "Leave",
    "no_open_teams_yet": "No open teams yet",
    "no_teams_found": "No teams found",
    "notes_placeholder": "Add notes about the match result...",
    "optional_notes": "Optional Notes",
    "player_initial_fallback": "P",
    "points": "Points",
    "ratings_and_feedback": "Ratings & Feedback",
    "recruiting": "Recruiting",
    "remove": "Remove",
    "rosters": "Rosters",
    "search_teams_hint": "Search teams...",
    "team_initial_fallback": "T",
    "teams_will_appear_here": "Teams will appear here",
    "this_week": "This Week",
    "try_different_search_term": "Try a different search term",
    "unknown_applicant_initial": "?",
    "withdraw_application": "Withdraw Application",
}

# Add missing strings
additions = []
for name, value in sorted(missing.items()):
    if name not in existing:
        additions.append(f'    <string name="{name}">{value}</string>')

if additions:
    insert = "\n".join(additions) + "\n"
    strings_content = strings_content.replace("</resources>", insert + "</resources>")
    with open(strings_xml, "w", encoding="utf-8") as f:
        f.write(strings_content)
    print(f"Added {len(additions)} missing strings to strings.xml")

# Fix SplashScreen imports
splash = screens_dir / "SplashScreen.kt"
with open(splash, "r", encoding="utf-8") as f:
    content = f.read()

if "import com.mlbb.scrim.R" not in content:
    content = content.replace(
        "import com.mlbb.scrim.ui.theme.*",
        "import com.mlbb.scrim.R\nimport androidx.compose.ui.res.stringResource\nimport com.mlbb.scrim.ui.theme.*"
    )
    with open(splash, "w", encoding="utf-8") as f:
        f.write(content)
    print("Added imports to SplashScreen.kt")

# Fix SignupScreen - stringResource in clickable lambda
signup = screens_dir / "SignupScreen.kt"
with open(signup, "r", encoding="utf-8") as f:
    content = f.read()

# Add val at top of composable for captcha error
if 'val captchaError = stringResource(R.string.captcha_verify_human)' not in content:
    # Find the line with isCaptchaVerified and add vals after it
    content = content.replace(
        '    var isCaptchaVerified by remember { mutableStateOf(false) }',
        '    var isCaptchaVerified by remember { mutableStateOf(false) }\n    val captchaError = stringResource(R.string.captcha_verify_human)'
    )
    # Replace usage in lambda
    content = content.replace(
        'errorMessage = stringResource(R.string.captcha_verify_human)',
        'errorMessage = captchaError'
    )
    with open(signup, "w", encoding="utf-8") as f:
        f.write(content)
    print("Fixed SignupScreen.kt non-composable stringResource")

# Fix ProfileScreen - stringResource in onClick lambda
profile = screens_dir / "ProfileScreen.kt"
with open(profile, "r", encoding="utf-8") as f:
    content = f.read()

# Pre-compute error strings at composable level
if 'val errorPasswordMinLength = stringResource(R.string.error_password_min_length)' not in content:
    content = content.replace(
        '    var showEmailDialog by remember { mutableStateOf(false) }',
        '    val errorPasswordMinLength = stringResource(R.string.error_password_min_length)\n    val errorPasswordsNotMatch = stringResource(R.string.error_passwords_not_match)\n    val errorPasswordMustDiffer = stringResource(R.string.error_password_must_differ)\n    var showEmailDialog by remember { mutableStateOf(false) }'
    )
    content = content.replace(
        'errorMessage = stringResource(R.string.error_password_min_length)',
        'errorMessage = errorPasswordMinLength'
    )
    content = content.replace(
        'errorMessage = stringResource(R.string.error_passwords_not_match)',
        'errorMessage = errorPasswordsNotMatch'
    )
    content = content.replace(
        'errorMessage = stringResource(R.string.error_password_must_differ)',
        'errorMessage = errorPasswordMustDiffer'
    )
    with open(profile, "w", encoding="utf-8") as f:
        f.write(content)
    print("Fixed ProfileScreen.kt non-composable stringResource")

# Fix ScrimDetailScreen - stringResource in coroutine launch
scrim_detail = screens_dir / "ScrimDetailScreen.kt"
with open(scrim_detail, "r", encoding="utf-8") as f:
    content = f.read()

if 'stringResource(R.string.error_failed_read_image)' in content:
    content = content.replace(
        'uploadError = stringResource(R.string.error_failed_read_image)',
        'uploadError = context.getString(R.string.error_failed_read_image)'
    )
    with open(scrim_detail, "w", encoding="utf-8") as f:
        f.write(content)
    print("Fixed ScrimDetailScreen.kt coroutine stringResource")

print("Done!")
