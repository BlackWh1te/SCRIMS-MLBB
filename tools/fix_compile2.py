import re
from pathlib import Path

screens_dir = Path(r"C:\Users\Shukhrat\Desktop\New folder\git\Android\app\src\main\java\com\mlbb\scrim\ui\screens")

# 1. Fix HomeScreen imports
home = screens_dir / "HomeScreen.kt"
with open(home, "r", encoding="utf-8") as f:
    content = f.read()

if "import androidx.compose.ui.res.stringResource" not in content:
    content = content.replace(
        "import com.mlbb.scrim.ui.theme.*",
        "import com.mlbb.scrim.R\nimport androidx.compose.ui.res.stringResource\nimport com.mlbb.scrim.ui.theme.*"
    )
    with open(home, "w", encoding="utf-8") as f:
        f.write(content)
    print("Added imports to HomeScreen.kt")

# 2. Fix ProfileScreen
profile = screens_dir / "ProfileScreen.kt"
with open(profile, "r", encoding="utf-8") as f:
    content = f.read()

# Revert LaunchedEffect stringResource to hardcoded
content = content.replace(
    'snackbarHostState.showSnackbar(stringResource(R.string.changes_saved))',
    'snackbarHostState.showSnackbar("Changes saved successfully!")'
)

# Remove vals added at wrong scope
content = content.replace(
    '''    val errorPasswordMinLength = stringResource(R.string.error_password_min_length)
    val errorPasswordsNotMatch = stringResource(R.string.error_passwords_not_match)
    val errorPasswordMustDiffer = stringResource(R.string.error_password_must_differ)
''',
    ''
)

# Fix ChangeEmailDialog (lines ~893-895) - add vals inside the dialog composable
# Find ChangeEmailDialog function and add stringResource vals at its start
email_dialog_marker = "fun ChangeEmailDialog("
if email_dialog_marker in content:
    # Add vals after the dialog's parameter list / before first var
    content = content.replace(
        '''fun ChangeEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var newEmail by remember { mutableStateOf("") }''',
        '''fun ChangeEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val errorEnterNewEmail = stringResource(R.string.error_enter_new_email)
    val errorEnterValidEmail = stringResource(R.string.error_enter_valid_email)
    val errorEnterCurrentPassword = stringResource(R.string.error_enter_current_password)
    var newEmail by remember { mutableStateOf("") }'''
    )
    # Replace usages
    content = content.replace(
        'errorMessage = stringResource(R.string.error_enter_new_email)',
        'errorMessage = errorEnterNewEmail'
    )
    content = content.replace(
        'errorMessage = stringResource(R.string.error_enter_valid_email)',
        'errorMessage = errorEnterValidEmail'
    )
    content = content.replace(
        'errorMessage = stringResource(R.string.error_enter_current_password)',
        'errorMessage = errorEnterCurrentPassword'
    )

# Fix ChangePasswordDialog - add vals inside the dialog composable
password_dialog_marker = "fun ChangePasswordDialog("
if password_dialog_marker in content:
    content = content.replace(
        '''fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }''',
        '''fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    val errorPasswordMinLength = stringResource(R.string.error_password_min_length)
    val errorPasswordsNotMatch = stringResource(R.string.error_passwords_not_match)
    val errorPasswordMustDiffer = stringResource(R.string.error_password_must_differ)
    var currentPassword by remember { mutableStateOf("") }'''
    )

with open(profile, "w", encoding="utf-8") as f:
    f.write(content)
print("Fixed ProfileScreen.kt")

# 3. Fix ScrimDetailScreen exhaustive when
scrim_detail = screens_dir / "ScrimDetailScreen.kt"
with open(scrim_detail, "r", encoding="utf-8") as f:
    content = f.read()

# Find the when expression and add else branch or READY_CHECK
# The error is at line 881 which is inside a when on ScrimStatus
# Let's search for the pattern
old_when = '''        when (scrim.status) {
            ScrimStatus.OPEN -> "Open"
            ScrimStatus.FILLED -> "Filled"
            ScrimStatus.IN_PROGRESS -> "In Progress"
            ScrimStatus.COMPLETED -> "Completed"
            ScrimStatus.CANCELLED -> "Cancelled"
        }'''
new_when = '''        when (scrim.status) {
            ScrimStatus.OPEN -> "Open"
            ScrimStatus.FILLED -> "Filled"
            ScrimStatus.IN_PROGRESS -> "In Progress"
            ScrimStatus.COMPLETED -> "Completed"
            ScrimStatus.CANCELLED -> "Cancelled"
            else -> "Open"
        }'''

if old_when in content:
    content = content.replace(old_when, new_when)
    with open(scrim_detail, "w", encoding="utf-8") as f:
        f.write(content)
    print("Fixed ScrimDetailScreen.kt exhaustive when")
else:
    print("WARNING: Could not find exhaustive when pattern in ScrimDetailScreen.kt")

print("Done!")
