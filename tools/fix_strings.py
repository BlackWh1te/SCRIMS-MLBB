import re
from pathlib import Path

base = Path(r"C:\Users\Shukhrat\Desktop\New folder\git\Android\app\src\main")
strings_xml = base / "res" / "values" / "strings.xml"
screens_dir = base / "java" / "com" / "mlbb" / "scrim" / "ui" / "screens"

# Read existing strings.xml
existing_strings = {}
existing_values = set()
with open(strings_xml, "r", encoding="utf-8") as f:
    strings_content = f.read()

for m in re.finditer(r'<string[^>]*name="([^"]+)"[^>]*>([^<]*)</string>', strings_content):
    name, value = m.group(1), m.group(2)
    existing_strings[name] = value
    existing_values.add(value)

# (file, old_text, new_text, string_name_or_None, string_value_or_None)
tasks = []

# ========== HomeScreen.kt ==========
tasks.append(("HomeScreen.kt", 'hour < 6  -> "Late night,"', 'hour < 6  -> stringResource(R.string.greeting_late_night)', "greeting_late_night", "Late night,"))
tasks.append(("HomeScreen.kt", 'hour < 12 -> "Good morning,"', 'hour < 12 -> stringResource(R.string.greeting_good_morning)', "greeting_good_morning", "Good morning,"))
tasks.append(("HomeScreen.kt", 'hour < 17 -> "Good afternoon,"', 'hour < 17 -> stringResource(R.string.greeting_good_afternoon)', "greeting_good_afternoon", "Good afternoon,"))
tasks.append(("HomeScreen.kt", 'else      -> "Good evening,"', 'else      -> stringResource(R.string.greeting_good_evening)', "greeting_good_evening", "Good evening,"))
tasks.append(("HomeScreen.kt", 'userProfile?.username ?: "Player"', 'userProfile?.username ?: stringResource(R.string.player_default)', None, None))
tasks.append(("HomeScreen.kt", 'contentDescription = "Notifications"', 'contentDescription = stringResource(R.string.notifications)', None, None))
tasks.append(("HomeScreen.kt", 'label    = "Matches"', 'label    = stringResource(R.string.matches)', None, None))
tasks.append(("HomeScreen.kt", 'label    = "Wins"', 'label    = stringResource(R.string.wins)', None, None))
tasks.append(("HomeScreen.kt", 'label    = "Win Rate"', 'label    = stringResource(R.string.win_rate)', None, None))
tasks.append(("HomeScreen.kt", '"Upcoming Scrims"', 'stringResource(R.string.upcoming_scrims)', None, None))
tasks.append(("HomeScreen.kt", '"See all"', 'stringResource(R.string.see_all)', None, None))
tasks.append(("HomeScreen.kt", '"Quick Actions"', 'stringResource(R.string.quick_actions)', None, None))
tasks.append(("HomeScreen.kt", 'Icon(Icons.Default.Add, "Post Scrim"', 'Icon(Icons.Default.Add, stringResource(R.string.post_scrim)', None, None))
tasks.append(("HomeScreen.kt", '"Post a Scrim"', 'stringResource(R.string.post_scrim)', None, None))
tasks.append(("HomeScreen.kt", '"List your team for a match"', 'stringResource(R.string.post_scrim_sub)', None, None))
tasks.append(("HomeScreen.kt", 'title    = "Leaderboard"', 'title    = stringResource(R.string.leaderboard)', None, None))
tasks.append(("HomeScreen.kt", 'subtitle = "Top players"', 'subtitle = stringResource(R.string.leaderboard_sub)', None, None))
tasks.append(("HomeScreen.kt", 'title    = "History"', 'title    = stringResource(R.string.match_history)', None, None))
tasks.append(("HomeScreen.kt", 'subtitle = "Past matches"', 'subtitle = stringResource(R.string.match_history_sub)', None, None))
tasks.append(("HomeScreen.kt", 'title    = "Schedule"', 'title    = stringResource(R.string.schedule)', None, None))
tasks.append(("HomeScreen.kt", 'subtitle = "Upcoming"', 'subtitle = stringResource(R.string.schedule_sub)', None, None))
tasks.append(("HomeScreen.kt", 'title    = "Create Team"', 'title    = stringResource(R.string.create_team)', None, None))
tasks.append(("HomeScreen.kt", 'subtitle = "Build squad"', 'subtitle = stringResource(R.string.create_team_sub)', None, None))

# ========== SignupScreen.kt ==========
tasks.append(("SignupScreen.kt", 'contentDescription = "App Logo"', 'contentDescription = stringResource(R.string.content_desc_app_logo)', None, None))
tasks.append(("SignupScreen.kt", '"Please slide to verify you are human"', 'stringResource(R.string.captcha_verify_human)', "captcha_verify_human", "Please slide to verify you are human"))

# ========== SettingsScreen.kt ==========
tasks.append(("SettingsScreen.kt", 'text = "Version $appVersion"', 'text = stringResource(R.string.version_label, appVersion)', "version_label", "Version %s"))

# ========== SplashScreen.kt ==========
tasks.append(("SplashScreen.kt", 'contentDescription = "App Logo"', 'contentDescription = stringResource(R.string.content_desc_app_logo)', None, None))
tasks.append(("SplashScreen.kt", 'text = "MLBB Scrim Host"', 'text = stringResource(R.string.app_title)', None, None))
tasks.append(("SplashScreen.kt", 'text = "Compete. Rank. Dominate."', 'text = stringResource(R.string.tagline)', None, None))
tasks.append(("SplashScreen.kt", 'text = "Loading..."', 'text = stringResource(R.string.loading)', "loading", "Loading..."))

# ========== ProfileScreen.kt ==========
tasks.append(("ProfileScreen.kt", 'snackbarHostState.showSnackbar("Changes saved successfully!")', 'snackbarHostState.showSnackbar(stringResource(R.string.changes_saved))', None, None))
tasks.append(("ProfileScreen.kt", 'contentDescription = if (isEditing) "Save" else "Edit"', 'contentDescription = if (isEditing) stringResource(R.string.save) else stringResource(R.string.edit)', None, None))
tasks.append(("ProfileScreen.kt", 'contentDescription = "Profile Avatar"', 'contentDescription = stringResource(R.string.content_desc_profile_avatar)', "content_desc_profile_avatar", "Profile Avatar"))
tasks.append(("ProfileScreen.kt", 'contentDescription = "Change avatar"', 'contentDescription = stringResource(R.string.content_desc_change_avatar)', "content_desc_change_avatar", "Change avatar"))
tasks.append(("ProfileScreen.kt", 'placeholder = "Username"', 'placeholder = stringResource(R.string.username)', None, None))
tasks.append(("ProfileScreen.kt", 'placeholder = "In-Game ID"', 'placeholder = stringResource(R.string.in_game_id)', None, None))
tasks.append(("ProfileScreen.kt", 'placeholder = "Bio (Something about yourself)"', 'placeholder = stringResource(R.string.bio_placeholder)', "bio_placeholder", "Bio (Something about yourself)"))
tasks.append(("ProfileScreen.kt", 'placeholder = "Main Role (e.g., Jungler, Roamer)"', 'placeholder = stringResource(R.string.role_placeholder)', "role_placeholder", "Main Role (e.g., Jungler, Roamer)"))
tasks.append(("ProfileScreen.kt", 'label = "Email"', 'label = stringResource(R.string.email)', None, None))
tasks.append(("ProfileScreen.kt", 'value = userProfile?.email ?: "Not set"', 'value = userProfile?.email ?: stringResource(R.string.not_set)', None, None))
tasks.append(("ProfileScreen.kt", 'label = "Member Since"', 'label = stringResource(R.string.member_since)', None, None))
tasks.append(("ProfileScreen.kt", '} ?: "Not set"', '} ?: stringResource(R.string.not_set)', None, None))
tasks.append(("ProfileScreen.kt", 'label = "In-Game ID"', 'label = stringResource(R.string.in_game_id)', None, None))
tasks.append(("ProfileScreen.kt", 'value = userProfile?.inGameId ?: "Not set"', 'value = userProfile?.inGameId ?: stringResource(R.string.not_set)', None, None))
tasks.append(("ProfileScreen.kt", 'label = "Matches"', 'label = stringResource(R.string.matches)', None, None))
tasks.append(("ProfileScreen.kt", 'label = "Wins"', 'label = stringResource(R.string.wins)', None, None))
tasks.append(("ProfileScreen.kt", 'label = "Losses"', 'label = stringResource(R.string.losses)', None, None))
tasks.append(("ProfileScreen.kt", 'label = "Win Rate"', 'label = stringResource(R.string.win_rate)', None, None))
tasks.append(("ProfileScreen.kt", 'label = "XP"', 'label = stringResource(R.string.xp_label)', None, None))
tasks.append(("ProfileScreen.kt", 'label = "PTS"', 'label = stringResource(R.string.pts_label)', "pts_label", "PTS"))
tasks.append(("ProfileScreen.kt", 'title = "Change Email"', 'title = stringResource(R.string.change_email)', None, None))
tasks.append(("ProfileScreen.kt", 'subtitle = userProfile?.email ?: "Not set"', 'subtitle = userProfile?.email ?: stringResource(R.string.not_set)', None, None))
tasks.append(("ProfileScreen.kt", 'title = "Change Password"', 'title = stringResource(R.string.change_password)', None, None))
tasks.append(("ProfileScreen.kt", 'subtitle = "Update your account password"', 'subtitle = stringResource(R.string.update_password_sub)', "update_password_sub", "Update your account password"))
tasks.append(("ProfileScreen.kt", 'title = "Sign Out"', 'title = stringResource(R.string.sign_out)', None, None))
tasks.append(("ProfileScreen.kt", 'subtitle = "Log out of your account"', 'subtitle = stringResource(R.string.sign_out_sub)', "sign_out_sub", "Log out of your account"))
tasks.append(("ProfileScreen.kt", '"Please enter a new email"', 'stringResource(R.string.error_enter_new_email)', "error_enter_new_email", "Please enter a new email"))
tasks.append(("ProfileScreen.kt", '"Please enter a valid email"', 'stringResource(R.string.error_enter_valid_email)', "error_enter_valid_email", "Please enter a valid email"))
tasks.append(("ProfileScreen.kt", '"Please enter your current password"', 'stringResource(R.string.error_enter_current_password)', "error_enter_current_password", "Please enter your current password"))
tasks.append(("ProfileScreen.kt", '"New password must be at least 6 characters"', 'stringResource(R.string.error_password_min_length)', "error_password_min_length", "New password must be at least 6 characters"))
tasks.append(("ProfileScreen.kt", '"New passwords do not match"', 'stringResource(R.string.error_passwords_not_match)', "error_passwords_not_match", "New passwords do not match"))
tasks.append(("ProfileScreen.kt", '"New password must be different"', 'stringResource(R.string.error_password_must_differ)', "error_password_must_differ", "New password must be different"))

# ========== ChatScreen.kt ==========
tasks.append(("ChatScreen.kt", '"Start the conversation with $otherTeamName"', 'stringResource(R.string.start_conversation_with, otherTeamName)', "start_conversation_with", "Start the conversation with %s"))
tasks.append(("ChatScreen.kt", '"Start the conversation with the other team"', 'stringResource(R.string.start_conversation)', "start_conversation", "Start the conversation with the other team"))

# ========== ScrimDetailScreen.kt ==========
tasks.append(("ScrimDetailScreen.kt", 'label = "Game Mode"', 'label = stringResource(R.string.game_mode)', None, None))
tasks.append(("ScrimDetailScreen.kt", 'label = "Region"', 'label = stringResource(R.string.region)', None, None))
tasks.append(("ScrimDetailScreen.kt", 'label = "Skill Level"', 'label = stringResource(R.string.skill_level)', None, None))
tasks.append(("ScrimDetailScreen.kt", 'label = "Format"', 'label = stringResource(R.string.format_label)', "format_label", "Format"))
tasks.append(("ScrimDetailScreen.kt", 'label = "Scheduled Time"', 'label = stringResource(R.string.scheduled_time)', "scheduled_time", "Scheduled Time"))
tasks.append(("ScrimDetailScreen.kt", 'label = "Players"', 'label = stringResource(R.string.players)', None, None))
tasks.append(("ScrimDetailScreen.kt", 'text = "READY"', 'text = stringResource(R.string.ready)', "ready", "READY"))
tasks.append(("ScrimDetailScreen.kt", 'uploadError = "Failed to read image"', 'uploadError = stringResource(R.string.error_failed_read_image)', "error_failed_read_image", "Failed to read image"))
tasks.append(("ScrimDetailScreen.kt", 'text = "Match In Progress"', 'text = stringResource(R.string.match_in_progress)', "match_in_progress", "Match In Progress"))
tasks.append(("ScrimDetailScreen.kt", 'text = "Step 1: Attach Screenshot"', 'text = stringResource(R.string.step_1_attach_screenshot)', "step_1_attach_screenshot", "Step 1: Attach Screenshot"))
tasks.append(("ScrimDetailScreen.kt", 'text = "Attach Screenshot"', 'text = stringResource(R.string.attach_screenshot)', "attach_screenshot", "Attach Screenshot"))
tasks.append(("ScrimDetailScreen.kt", 'text = "Step 2: Complete Scrim"', 'text = stringResource(R.string.step_2_complete_scrim)', "step_2_complete_scrim", "Step 2: Complete Scrim"))
tasks.append(("ScrimDetailScreen.kt", '"Complete Scrim"', 'stringResource(R.string.complete_scrim)', "complete_scrim", "Complete Scrim"))
tasks.append(("ScrimDetailScreen.kt", '"Upload screenshot first"', 'stringResource(R.string.upload_screenshot_first)', "upload_screenshot_first", "Upload screenshot first"))

# ========== TeamListScreen.kt ==========
tasks.append(("TeamListScreen.kt", 'contentDescription = "Refresh"', 'contentDescription = stringResource(R.string.refresh)', None, None))
tasks.append(("TeamListScreen.kt", 'contentDescription = "Find Teams"', 'contentDescription = stringResource(R.string.find_teams)', "find_teams", "Find Teams"))
tasks.append(("TeamListScreen.kt", 'contentDescription = "Join Team"', 'contentDescription = stringResource(R.string.join_team)', None, None))
tasks.append(("TeamListScreen.kt", 'contentDescription = "Create Team"', 'contentDescription = stringResource(R.string.create_team)', None, None))
tasks.append(("TeamListScreen.kt", 'title = "No teams yet"', 'title = stringResource(R.string.no_teams_yet)', "no_teams_yet", "No teams yet"))
tasks.append(("TeamListScreen.kt", 'subtitle = "Create your first team or join one with an invite code"', 'subtitle = stringResource(R.string.no_teams_subtitle)', "no_teams_subtitle", "Create your first team or join one with an invite code"))
tasks.append(("TeamListScreen.kt", 'contentDescription = "Players"', 'contentDescription = stringResource(R.string.players)', None, None))
tasks.append(("TeamListScreen.kt", 'contentDescription = "View team"', 'contentDescription = stringResource(R.string.view_team)', "view_team", "View team"))

# ========== MatchHistoryScreen.kt ==========
tasks.append(("MatchHistoryScreen.kt", 'contentDescription = "Refresh"', 'contentDescription = stringResource(R.string.refresh)', None, None))

# ========== MessageListScreen.kt ==========
tasks.append(("MessageListScreen.kt", 'Icons.Default.Refresh, "Refresh"', 'Icons.Default.Refresh, stringResource(R.string.refresh)', None, None))
tasks.append(("MessageListScreen.kt", 'title    = "No messages yet"', 'title    = stringResource(R.string.no_messages_yet)', None, None))
tasks.append(("MessageListScreen.kt", 'subtitle = "When teams apply to your scrims or you apply to theirs, conversations will appear here."', 'subtitle = stringResource(R.string.no_messages_subtitle)', "no_messages_subtitle", "When teams apply to your scrims or you apply to theirs, conversations will appear here."))

# ========== LeaderboardScreen.kt ==========
tasks.append(("LeaderboardScreen.kt", 'contentDescription = "Refresh"', 'contentDescription = stringResource(R.string.refresh)', None, None))
tasks.append(("LeaderboardScreen.kt", 'contentDescription = "Error"', 'contentDescription = stringResource(R.string.error)', "error", "Error"))
tasks.append(("LeaderboardScreen.kt", 'contentDescription = "Dismiss"', 'contentDescription = stringResource(R.string.dismiss)', "dismiss", "Dismiss"))
tasks.append(("LeaderboardScreen.kt", 'text = "Complete scrims to earn points and climb the ranks"', 'text = stringResource(R.string.leaderboard_empty_hint)', "leaderboard_empty_hint", "Complete scrims to earn points and climb the ranks"))

# ========== NotificationScreen.kt ==========
tasks.append(("NotificationScreen.kt", 'contentDescription = "Error"', 'contentDescription = stringResource(R.string.error)', None, None))
tasks.append(("NotificationScreen.kt", 'contentDescription = "Dismiss"', 'contentDescription = stringResource(R.string.dismiss)', None, None))
tasks.append(("NotificationScreen.kt", 'contentDescription = "Delete"', 'contentDescription = stringResource(R.string.delete)', "delete", "Delete"))
tasks.append(("NotificationScreen.kt", 'text = "You\'ll receive notifications when someone invites you to a team or scrim, or when match results are ready"', 'text = stringResource(R.string.notifications_hint)', "notifications_hint", "You'll receive notifications when someone invites you to a team or scrim, or when match results are ready"))

# ========== ScrimListScreen.kt ==========
tasks.append(("ScrimListScreen.kt", 'contentDescription = "Filters"', 'contentDescription = stringResource(R.string.filters)', "filters", "Filters"))
tasks.append(("ScrimListScreen.kt", 'text = "Open"', 'text = stringResource(R.string.open)', None, None))
tasks.append(("ScrimListScreen.kt", 'text = "EU/NA"', 'text = stringResource(R.string.region_eu_na)', "region_eu_na", "EU/NA"))
tasks.append(("ScrimListScreen.kt", 'placeholder = "Search teams..."', 'placeholder = stringResource(R.string.search_teams)', "search_teams", "Search teams..."))
tasks.append(("ScrimListScreen.kt", 'Text("Clear Filters"', 'Text(stringResource(R.string.clear_filters)', None, None))
tasks.append(("ScrimListScreen.kt", 'title = "No scrims found"', 'title = stringResource(R.string.no_scrims_found)', "no_scrims_found", "No scrims found"))
tasks.append(("ScrimListScreen.kt", 'subtitle = "Be the first to post a scrim"', 'subtitle = stringResource(R.string.be_first_to_post_scrim)', "be_first_to_post_scrim", "Be the first to post a scrim"))
tasks.append(("ScrimListScreen.kt", 'contentDescription = "Post Scrim"', 'contentDescription = stringResource(R.string.post_scrim)', None, None))

# ========== MatchResultListScreen.kt ==========
tasks.append(("MatchResultListScreen.kt", 'contentDescription = "Refresh"', 'contentDescription = stringResource(R.string.refresh)', None, None))
tasks.append(("MatchResultListScreen.kt", 'title = "No matches yet"', 'title = stringResource(R.string.no_matches_yet)', None, None))
tasks.append(("MatchResultListScreen.kt", 'subtitle = "Complete a scrim to see match results here."', 'subtitle = stringResource(R.string.complete_scrim_to_see_results)', "complete_scrim_to_see_results", "Complete a scrim to see match results here."))

# ========== CreateScrimScreen.kt ==========
tasks.append(("CreateScrimScreen.kt", 'val teamName = selectedTeam?.name ?: "My Team"', 'val teamName = selectedTeam?.name ?: stringResource(R.string.my_team_default)', "my_team_default", "My Team"))

# ========== CreateTeamScreen.kt ==========
tasks.append(("CreateTeamScreen.kt", 'contentDescription = "Team logo"', 'contentDescription = stringResource(R.string.content_desc_team_logo)', "content_desc_team_logo", "Team logo"))
tasks.append(("CreateTeamScreen.kt", 'contentDescription = "Upload logo"', 'contentDescription = stringResource(R.string.content_desc_upload_logo)', "content_desc_upload_logo", "Upload logo"))

# ========== LfgBoardScreen.kt ==========
tasks.append(("LfgBoardScreen.kt", 'contentDescription = "Create Post"', 'contentDescription = stringResource(R.string.create_post)', None, None))

# ========== LoginScreen.kt ==========
tasks.append(("LoginScreen.kt", 'stringResource(R.string.signing_in)', 'stringResource(R.string.signing_in)', "signing_in", "Signing in..."))

new_strings = {}
file_changes = {}

for task in tasks:
    file_name, old_text, new_text, str_name, str_value = task
    if str_name and str_value and str_name not in existing_strings:
        new_strings[str_name] = str_value

    file_path = screens_dir / file_name
    if file_path not in file_changes:
        with open(file_path, "r", encoding="utf-8") as f:
            file_changes[file_path] = f.read()

    content = file_changes[file_path]
    if old_text in content:
        file_changes[file_path] = content.replace(old_text, new_text, 1)
    else:
        print(f"  WARNING: Could not find in {file_name}: {old_text[:60]}...")

# Add new strings to strings.xml
if new_strings:
    additions = []
    for name, value in sorted(new_strings.items()):
        additions.append(f'    <string name="{name}">{value}</string>')

    insert_text = "\n".join(additions) + "\n"
    strings_content = strings_content.replace("</resources>", insert_text + "</resources>")

    with open(strings_xml, "w", encoding="utf-8") as f:
        f.write(strings_content)

    print(f"Added {len(new_strings)} new strings to strings.xml")
    for name, value in sorted(new_strings.items()):
        print(f"  + {name} = {value}")
else:
    print("No new strings to add.")

# Write changed files
for file_path, content in file_changes.items():
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"Updated {file_path.name}")

print("Done!")
