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
# 크루 명함형은 따로 된 길 묶음(crewGraph)에 오르고, 정책은 CrewRoutes.ALL 을 한꺼번에 준다 — 묶음에 올린 길이
# 모두 CrewRoutes.ALL 에 있어야(또 그 반대) 모든 크루 화면이 머리 · 탭 규칙을 받는다
if 'CrewRoutes.ALL.map' in policy:
    crew_graph = (UI/'screens/community/crew/CrewNavGraph.kt').read_text(encoding='utf-8')
    crew_registered = set(re.findall(r'composable\(\s*(CrewRoutes\.[A-Z_]+)', crew_graph))
    crew_all = re.search(r'val ALL: List<String> = listOf\(([^)]*)\)', crew_graph)
    crew_listed = {f'CrewRoutes.{name.strip()}' for name in crew_all.group(1).split(',') if name.strip()} if crew_all else set()
    if not crew_registered or crew_registered != crew_listed:
        errors.append(f'Crew route policy mismatch: missing={crew_registered-crew_listed}, stale={crew_listed-crew_registered}')
    if 'it' in declared:
        declared.remove('it')
# 크루 채팅도 같은 방식(chatGraph · ChatRoutes.ALL)
if 'ChatRoutes.ALL.map' in policy:
    chat_graph = (UI/'screens/community/chat/ChatNavGraph.kt').read_text(encoding='utf-8')
    chat_registered = set(re.findall(r'composable\(\s*(ChatRoutes\.[A-Z_]+)', chat_graph))
    chat_all = re.search(r'val ALL: List<String> = listOf\(([^)]*)\)', chat_graph)
    chat_listed = {f'ChatRoutes.{name.strip()}' for name in chat_all.group(1).split(',') if name.strip()} if chat_all else set()
    if not chat_registered or chat_registered != chat_listed:
        errors.append(f'Chat route policy mismatch: missing={chat_registered-chat_listed}, stale={chat_listed-chat_registered}')
    if 'it' in declared:
        declared.remove('it')
# 내 크루 홈(확정 4번)도 같은 방식(crewHomeGraph · CrewHomeRoutes.ALL)
if 'CrewHomeRoutes.ALL.map' in policy:
    home_graph = (UI/'screens/community/home/CrewHomeNavGraph.kt').read_text(encoding='utf-8')
    home_registered = set(re.findall(r'composable\(\s*(CrewHomeRoutes\.[A-Z_]+)', home_graph))
    home_all = re.search(r'val ALL: List<String> = listOf\(([^)]*)\)', home_graph)
    home_listed = {f'CrewHomeRoutes.{name.strip()}' for name in home_all.group(1).split(',') if name.strip()} if home_all else set()
    if not home_registered or home_registered != home_listed:
        errors.append(f'Crew home route policy mismatch: missing={home_registered-home_listed}, stale={home_listed-home_registered}')
    if 'it' in declared:
        declared.remove('it')
if registered != set(declared):
    errors.append(f'Route policy mismatch: missing={registered-set(declared)}, stale={set(declared)-registered}')
if len(declared) != len(set(declared)):
    errors.append('Duplicate route policies')
inventory = json.loads((ROOT/'docs/redesign/screen-inventory.json').read_text(encoding='utf-8'))
# 목록에는 뿌리 길과 함께 기능별 길 묶음(크루 명함 · 크루 채팅 · 내 크루 홈)도 오른다
grouped = set()
for name in ('crew_registered', 'chat_registered', 'home_registered'):
    grouped |= globals().get(name, set())
if {row['id'] for row in inventory['routes']} != registered | grouped:
    errors.append('Screen inventory routes are stale; run tools/ui_inventory.py')

for path in UI.rglob('*.kt'):
    text = path.read_text(encoding='utf-8')
    rel = path.relative_to(ROOT).as_posix()
    if re.search(r'Wordmark\([^)]*fontSize\s*=', text):
        errors.append(f'{rel}: numeric per-screen wordmark size')
    if 'screens' in path.parts:
        if re.search(r'\bWordmark\(', text) and path.name not in ('SplashScreen.kt', 'LoginScreen.kt'):
            errors.append(f'{rel}: screen-local wordmark outside the approved launch/login roles')
        if re.search(r'Icons\.AutoMirrored\.Filled\.ArrowBack', text):
            errors.append(f'{rel}: screen-local back chrome; use a shared header')
        for pattern, message in [
            (r'R\.drawable\.logo_wordmark', 'direct logo resource; use the shared wordmark'),
            (r'\bMainHeader\(', 'main header belongs to the root shell'),
            (r'\b(?:NavigationBar|NavigationBarItem|VoltNavBar|NavTab)\(', 'screen-local navigation'),
        ]:
            if re.search(pattern, text):
                errors.append(f'{rel}: {message}')
    if 'settings' in path.parts and re.search(r'Icons\.AutoMirrored\.Filled\.ArrowBack|\bWordmark\(', text):
        errors.append(f'{rel}: settings chrome must come from DetailPage/SecondaryHeader')

# 파란 톤 v4(2026-10-03) — 설정 상세는 공용 파란 상세 화면(components/BlueSettingsParts.kt 의 BluePage, 머리 · 뒤로 포함)을 쓴다
for name in ('Language', 'Theme', 'NotificationSettings', 'Privacy', 'Support', 'ExperienceSettings'):
    path = UI/'screens/settings'/f'{name}Screen.kt'
    if not any(marker in path.read_text(encoding='utf-8') for marker in ('DetailPage(', 'BluePage(')):
        errors.append(f'{path.name}: use the fixed shared detail page')
notifications = (UI/'screens/notifications/NotificationsScreen.kt').read_text(encoding='utf-8')
for relative in ('map/MapScreen.kt', 'profile/HistoryMapScreen.kt'):
    detail = (UI/'screens'/relative).read_text(encoding='utf-8')
    if 'SecondaryHeader(' not in detail or 'ArrowBack' in detail:
        errors.append(f'{relative}: map chrome must use the shared detail header')
for relative in ('community/RankingScreen.kt', 'profile/AchievementsScreen.kt',
                 'community/FlashRunDetailScreen.kt', 'items/SneakerDexScreen.kt', 'profile/AnalyticsScreen.kt',
                 'items/SneakerDetailScreen.kt', 'market/MarketModelScreen.kt',
                 'community/CrewBoardScreen.kt', 'items/ItemsScreen.kt'):
    detail = (UI/'screens'/relative).read_text(encoding='utf-8')
    if 'DetailPage(' not in detail or 'ArrowBack' in detail:
        errors.append(f'{relative}: use shared detail chrome')
# 러닝 전체 리메이크(2026-10-02) — 코스 허브 · 크루 러닝 대기실은 러닝 화면 공통 머리(RunTopBar · RunPage, 뒤로 · 공식 로고)를 쓴다.
# 공용 머리를 쓰는지만 본다 — 화면마다 따로 그린 뒤로 화살표는 여전히 막는다.
for relative in ('walk/CourseHubScreen.kt', 'community/PartyLobbyScreen.kt'):
    detail = (UI/'screens'/relative).read_text(encoding='utf-8')
    shared = any(marker in detail for marker in ('DetailPage(', 'RunTopBar(', 'RunPage('))
    if not shared or 'ArrowBack' in detail:
        errors.append(f'{relative}: use shared detail or run chrome')
# 알림·공지 v1: 공통 상세 머리(SecondaryHeader — DetailPage 와 같은 것) 아래에 고정 메뉴(내 알림 · 공지)와
# 메뉴마다 스크롤 위치를 따로 둔 목록 둘이라 DetailPage 의 목록 하나 대신 머리를 직접 쓴다. 자체 뒤로 버튼은 여전히 금지.
# 파란 톤 v4 — 알림은 시작 · 알림 공용 제목 줄(BlueTitleBar, 공용 RunBackButton)을 쓴다
if not any(marker in notifications for marker in ('DetailPage(', 'SecondaryHeader(', 'BlueTitleBar(')) or 'ArrowBack' in notifications:
    errors.append('Notifications must use the shared detail page')
if 'markAllRead()' in notifications and 'LaunchedEffect(Unit) { viewModel.markAllRead() }' in notifications:
    errors.append('Opening the inbox must not mark everything read')
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
filters = (UI/'components/FilterSheet.kt').read_text(encoding='utf-8')
for name in ('ToolbarButton', 'ChoiceChip'):
    body = filters.split(f'fun {name}(', 1)[1].split('\n/**', 1)[0]
    for token in ('StepUpDesign.TouchTarget', 'StepUpDesign.SecondaryLabel',
                  'StepUpDesign.SecondaryHorizontalPadding', 'StepUpDesign.SecondaryVerticalPadding',
                  'StepUpDesign.ControlRadius'):
        if token not in body:
            errors.append(f'{name}: must use shared control token {token}')
    if 'TextOverflow.Ellipsis' in body:
        errors.append(f'{name}: preserve complete control labels with wrapping')
if 'barHiddenRoutes' in root:
    errors.append('Bottom visibility must come from AppChromePolicy')

if errors:
    print('\n'.join(errors))
    sys.exit(1)
print(f'Design contract: {len(registered | grouped)} routes covered; shared main header/nav and fixed logo roles verified.')
print('This is source validation only. Native visual/interaction validation remains required.')
