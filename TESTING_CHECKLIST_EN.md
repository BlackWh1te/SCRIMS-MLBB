# MLBB Scrim Host - Testing Checklist

## 📱 Installation & Launch
- [ ] APK installed successfully on Android device
- [ ] App launches without errors
- [ ] Splash screen displays correctly
- [ ] App doesn't crash on launch

---

## 🔐 Authentication

### Signup (OTP Verification)
- [ ] Signup screen opens correctly
- [ ] All fields display (Email, Password, Confirm Password, Username, In-Game ID)
- [ ] Validation works:
  - [ ] Empty fields → error "Fill all fields"
  - [ ] Invalid email → error "Enter valid email"
  - [ ] Password < 6 chars → error "Min 6 characters"
  - [ ] Passwords don't match → error "Passwords don't match"
- [ ] After "Create Account" → OTP code sent to email
- [ ] Verification screen opens with 6-digit code input
- [ ] Code field accepts only digits (max 6)
- [ ] Correct code → verification successful → navigate to Home
- [ ] Incorrect code → error "Invalid code"
- [ ] "Resend Code" button works
- [ ] 60-second countdown for resend works
- [ ] "Check spam folder" text displays
- [ ] "Back to Login" returns to login screen

### Login
- [ ] Login screen opens correctly
- [ ] Email and Password fields display
- [ ] Valid credentials → successful login → Home screen
- [ ] Invalid credentials → error
- [ ] "Forgot Password?" opens reset screen
- [ ] "Don't have account?" → signup screen
- [ ] "Take a tour" opens onboarding

### Forgot Password
- [ ] Reset password screen opens
- [ ] Enter email → send reset link
- [ ] "Check your email" message displays

### Logout
- [ ] Logout works correctly
- [ ] After logout → login screen
- [ ] Data cleared (tokens, etc.)

---

## 👤 Profile

### View Profile
- [ ] Profile screen opens correctly
- [ ] Username displays
- [ ] Email displays
- [ ] In-Game ID displays
- [ ] Member since date displays
- [ ] XP displays
- [ ] Rank (Tier) displays
- [ ] Stats display (wins, losses, matches)

### Edit Profile
- [ ] "Edit" button opens edit form
- [ ] Can change username
- [ ] Can change In-Game ID
- [ ] Save changes works
- [ ] Cancel changes works

### Account Security
- [ ] "Change Email" button works
- [ ] "Change Password" button works
- [ ] Current password required for changes
- [ ] Password validation works

---

## 👥 Teams

### Create Team
- [ ] Create team screen opens
- [ ] Fields: Name, Description, Min/Max players
- [ ] Field validation works
- [ ] Team creation successful
- [ ] Team appears in "My Teams"

### Team List
- [ ] Team list screen opens
- [ ] User's teams display
- [ ] Can navigate to team details
- [ ] Can create new team

### Team Details
- [ ] Team details screen opens
- [ ] Team name displays
- [ ] Description displays
- [ ] Team rank displays
- [ ] Team stats display
- [ ] Player list displays
- [ ] Can add player
- [ ] Can change player role
- [ ] Can remove player
- [ ] Can disband team (leader only)
- [ ] Can leave team

### Add Player
- [ ] Add player dialog opens
- [ ] Can enter player name
- [ ] Can enter player email
- [ ] Add player successful

### Invite by Code
- [ ] Can get invite code
- [ ] Can enter invite code to join
- [ ] Join team successful

### Join Team Screen
- [ ] Screen opens
- [ ] Can enter invite code
- [ ] Join successful

---

## 🎮 Scrims

### Create Scrim
- [ ] Create scrim screen opens
- [ ] Select team from list
- [ ] Select date and time
- [ ] Select format (Best of)
- [ ] Select game mode
- [ ] Select region
- [ ] Add description
- [ ] Scrim creation successful
- [ ] Scrim appears in list

### Scrim List
- [ ] Scrim list screen opens
- [ ] Upcoming scrims display
- [ ] Scrim status displays
- [ ] Can filter by status
- [ ] Can refresh list (pull-to-refresh)
- [ ] Can navigate to scrim details

### Scrim Details
- [ ] Scrim details screen opens
- [ ] Scrim info displays
- [ ] Organizing team displays
- [ ] Status displays (Open, Filled, In Progress, etc.)
- [ ] Can apply (if open)
- [ ] Can cancel scrim (if leader)
- [ ] Can open chat (if allowed)

### LFG Board
- [ ] LFG screen opens
- [ ] Players looking for teams display
- [ ] Can create post
- [ ] Can respond to post

### Scrim Roster
- [ ] Roster screen opens
- [ ] Player list displays
- [ ] Can mark ready
- [ ] Can upload screenshot
- [ ] Can complete scrim

---

## 💬 Chat

### Message List
- [ ] Message list screen opens
- [ ] All conversations display
- [ ] Unread count displays
- [ ] Can open chat

### Chat
- [ ] Chat screen opens
- [ ] Messages display
- [ ] Can send message
- [ ] Messages send successfully
- [ ] Messages update in real-time
- [ ] Contact name displays

---

## 🏆 Leaderboard

### View Leaderboard
- [ ] Leaderboard screen opens
- [ ] Top players display
- [ ] Can filter by rank
- [ ] XP, wins, losses display
- [ ] Can sort by different parameters

---

## 📰 News

### View News
- [ ] News screen opens
- [ ] News list displays
- [ ] Can refresh list
- [ ] Can open news article
- [ ] Can read full article
- [ ] Can open in browser

---

## 🔔 Notifications

### View Notifications
- [ ] Notifications screen opens
- [ ] Notification list displays
- [ ] Unread count displays
- [ ] Can mark all as read
- [ ] Match notifications work
- [ ] Message notifications work

---

## ⚙️ Settings

### General Settings
- [ ] Settings screen opens
- [ ] Enable/disable notifications works
- [ ] Enable/disable sound works
- [ ] Enable/disable vibration works

### Appearance
- [ ] Dark/light theme toggle works
- [ ] Language selection works
- [ ] Theme applies correctly

### Security
- [ ] Change email works
- [ ] Change password works
- [ ] Current password required

### About
- [ ] App version displays
- [ ] Privacy policy opens
- [ ] Terms of service opens
- [ ] "Clear cache" button works
- [ ] "Logout" button works
- [ ] "Delete account" button works (with confirmation)

---

## 📜 Match History

### View History
- [ ] Match history screen opens
- [ ] Match list displays
- [ ] Match status displays
- [ ] Winner displays
- [ ] Can navigate to match details

### Match Details
- [ ] Match details screen opens
- [ ] Match info displays
- [ ] Winner displays
- [ ] Both team reports display
- [ ] Can dispute result
- [ ] Admin decision displays

### Report Match Result
- [ ] Report screen opens
- [ ] Can select winner
- [ ] Can upload screenshot
- [ ] Report submits successfully
- [ ] Waiting for other team report works
- [ ] Dispute result works

---

## 🎖️ Achievements

### View Achievements
- [ ] Achievements screen opens
- [ ] Achievement list displays
- [ ] Unlock progress displays
- [ ] Unlocked achievements display
- [ ] Locked achievements display

---

## 🐨 Error Testing

### Crash Testing
- [ ] App doesn't crash on rapid tapping
- [ ] App doesn't crash on screen rotation
- [ ] App doesn't crash with no internet
- [ ] App doesn't crash with slow internet

### UX Testing
- [ ] Animations are smooth
- [ ] Loading states display (skeletons)
- [ ] Errors display to user
- [ ] Buttons respond to taps
- [ ] Navigation is intuitive

---

## 📝 Additional Notes

```
Test Date: _______________
Tester: _______________
Device: _______________
Android Version: _______________
App Version: _______________

Overall Impression: _______________

Bugs Found:
1. _______________
2. _______________
3. _______________

Suggestions for Improvement:
1. _______________
2. _______________
3. _______________
```

---

## ✅ Success Criteria

App is considered tested when:
- All checklist items verified
- Critical bugs (crashes, signup/login issues) absent
- Core functionality works stably
- UX meets expectations
