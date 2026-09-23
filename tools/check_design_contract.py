"""Architecture guardrails, not a substitute for screenshots or interaction tests."""
from pathlib import Path
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT/'app/src/main/java/com/stepup/android/ui'
errors = []
root = (UI/'StepUpRoot.kt').read_text(encoding='utf-8')
policy = (UI/'AppChromePolicy.kt').read_text(encoding='utf-8')
registered = set(re.findall(r'composable\(\s*(?:route\s*=\s*)?((?:Routes\.[A-Z_]+)|(?:Screen\.\w+\.route))', root))
declared = re.findall(r'Destination\(([^,]+), Screen\.\w+, Header\.\w+\)', policy)
if registered != set(declared):
    errors.append(f'Route policy mismatch: missing={registered-set(declared)}, stale={set(declared)-registered}')
if len(declared) != len(set(declared)):
    errors.append('Duplicate route policies')
inventory = json.loads((ROOT/'docs/redesign/screen-inventory.json').read_text(encoding='utf-8'))
if {row['id'] for row in inventory['routes']} != registered:
    errors.append('Screen inventory routes are stale; run tools/ui_inventory.py')

for path in UI.rglob('*.kt'):
    text = path.read_text(encoding='utf-8')
    rel = path.relative_to(ROOT).as_posix()
    if re.search(r'Wordmark\([^)]*fontSize\s*=', text):
        errors.append(f'{rel}: numeric per-screen wordmark size')
    if 'screens' in path.parts:
        for pattern, message in [
            (r'R\.drawable\.logo_wordmark', 'direct logo resource; use the shared wordmark'),
            (r'\bMainHeader\(', 'main header belongs to the root shell'),
            (r'\b(?:NavigationBar|NavigationBarItem|VoltNavBar|NavTab)\(', 'screen-local navigation'),
        ]:
            if re.search(pattern, text):
                errors.append(f'{rel}: {message}')
    if 'settings' in path.parts and re.search(r'Icons\.AutoMirrored\.Filled\.ArrowBack|\bWordmark\(', text):
        errors.append(f'{rel}: settings chrome must come from DetailPage/SecondaryHeader')

for name in ('Language', 'Theme', 'NotificationSettings', 'Privacy', 'Support', 'ExperienceSettings'):
    path = UI/'screens/settings'/f'{name}Screen.kt'
    if 'DetailPage(' not in path.read_text(encoding='utf-8'):
        errors.append(f'{path.name}: use the fixed shared detail page')
notifications = (UI/'screens/notifications/NotificationsScreen.kt').read_text(encoding='utf-8')
for relative in ('map/MapScreen.kt', 'profile/HistoryMapScreen.kt'):
    detail = (UI/'screens'/relative).read_text(encoding='utf-8')
    if 'SecondaryHeader(' not in detail or 'ArrowBack' in detail:
        errors.append(f'{relative}: map chrome must use the shared detail header')
for relative in ('community/RankingScreen.kt', 'profile/AchievementsScreen.kt',
                 'community/FlashRunDetailScreen.kt', 'items/SneakerDexScreen.kt', 'profile/AnalyticsScreen.kt',
                 'items/SneakerDetailScreen.kt', 'market/MarketModelScreen.kt', 'community/PartyLobbyScreen.kt',
                 'community/CrewBoardScreen.kt', 'items/ItemsScreen.kt', 'walk/CourseHubScreen.kt'):
    detail = (UI/'screens'/relative).read_text(encoding='utf-8')
    if 'DetailPage(' not in detail or 'ArrowBack' in detail:
        errors.append(f'{relative}: use shared detail chrome')
if 'DetailPage(' not in notifications or 'ArrowBack' in notifications:
    errors.append('Notifications must use the shared detail page')
if 'viewModel::clearAll' in notifications:
    errors.append('Mark all read must preserve notifications, not delete them')
notification_repo = (ROOT/'app/src/main/java/com/stepup/android/data/repo/NotificationRepository.kt').read_text(encoding='utf-8')
if 'rewardRepository.credit' in notification_repo or 'seedWelcome' in notification_repo:
    errors.append('Notifications cannot fabricate or locally pay rewards without a server receipt')
if root.count('MainHeader(') != 1:
    errors.append('Root must own exactly one main header')
controls = (UI/'components/Components.kt').read_text(encoding='utf-8')
for name in ('VoltButton', 'GhostButton'):
    body = controls.split(f'fun {name}(', 1)[1].split('\n/**', 1)[0]
    for token in ('StepUpDesign.TouchTarget', 'StepUpDesign.SecondaryLabel',
                  'StepUpDesign.SecondaryHorizontalPadding', 'StepUpDesign.SecondaryVerticalPadding'):
        if token not in body:
            errors.append(f'{name}: must use shared control token {token}')
if re.search(r'fontWeight\s*=\s*if\s*\(selected\)', root):
    errors.append('Tab selection changes geometry through font weight')
if 'barHiddenRoutes' in root:
    errors.append('Bottom visibility must come from AppChromePolicy')

if errors:
    print('\n'.join(errors))
    sys.exit(1)
print(f'Design contract: {len(registered)} routes covered; shared main header/nav and fixed logo roles verified.')
print('This is source validation only. Native visual/interaction validation remains required.')
