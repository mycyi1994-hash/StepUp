"""Regenerate source-backed screen/state evidence; review status is never inferred."""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / 'app/src/main/java/com/stepup/android/ui'
OUT = ROOT / 'docs/redesign'


def build():
    root = (UI / 'StepUpRoot.kt').read_text(encoding='utf-8')
    policy = (UI / 'AppChromePolicy.kt').read_text(encoding='utf-8')
    routes = re.findall(r'composable\(\s*(?:route\s*=\s*)?((?:Routes\.[A-Z_]+)|(?:Screen\.\w+\.route))', root)
    entries = {}
    for route, parent, header in re.findall(r'Destination\(([^,]+), Screen\.(\w+), Header\.(\w+)\)', policy):
        entries[route] = {'parent': parent, 'chrome': header}
    screens, overlays = [], []
    for path in sorted((UI/'screens').rglob('*.kt')):
        text = path.read_text(encoding='utf-8')
        rel = path.relative_to(ROOT).as_posix()
        functions = list(re.finditer(r'^(?:private |internal )?fun (\w+)\(', text, re.M))
        for f in functions:
            if f.group(1).endswith('Screen'):
                screens.append({'id': f.group(1), 'source': rel, 'line': text[:f.start()].count('\n')+1, 'implementation': 'pending', 'verification': 'pending'})
        for match in re.finditer(r'\b(AlertDialog|DialogPanel|Dialog|ModalBottomSheet|DropdownMenu|Popup)\(', text):
            preceding = [f for f in functions if f.start() < match.start()]
            overlays.append({'kind': match.group(1), 'owner': preceding[-1].group(1) if preceding else 'unknown', 'source': rel, 'line': text[:match.start()].count('\n')+1, 'verification': 'pending'})
    data = {
        'schema': 1,
        'note': 'Source inventory, not completion proof. A function may serve multiple routes/states. All rows require actual runtime review.',
        'routes': [{'id': route, **entries.get(route, {'parent': 'MISSING', 'chrome': 'MISSING'}), 'implementation': 'pending', 'verification': 'pending'} for route in routes],
        'screen_functions': screens,
        'overlays': overlays,
        'required_state_matrix': ['initializing', 'ready/data', 'empty', 'loading', 'error/retry', 'permission denied', 'signed out', 'signed in', 'offline/reconnect', 'restore/back/relaunch', 'large font', 'light/dark'],
        'additional_entry_states': ['system launch', 'initialization', 'logo reveal', 'login', 'first-use tour'],
        'additional_run_states': ['ready', 'permission denied', 'acquiring GPS', 'active', 'paused', 'confirm finish', 'result', 'pending server reward', 'reward rejected', 'restored active session'],
    }
    # Preserve human-reviewed status by stable source identities across regeneration.
    destination = OUT/'screen-inventory.json'
    if destination.exists():
        old = json.loads(destination.read_text(encoding='utf-8'))
        for group in ('routes', 'screen_functions'):
            prior = {row['id']: row for row in old.get(group, [])}
            for row in data[group]:
                for key in ('implementation', 'verification', 'evidence', 'notes'):
                    if key in prior.get(row['id'], {}):
                        row[key] = prior[row['id']][key]
    OUT.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    lines = ['# Source-backed screen inventory', '', data['note'], '', f"{len(routes)} registered navigation routes; {len(screens)} screen functions; {len(overlays)} overlay declarations. These counts are different measures, not completed screens.", '', '## Routes', '', '| Route | Parent tab | Chrome | Implementation | Verification |', '|---|---|---|---|---|']
    lines += [f"| {r['id']} | {r['parent']} | {r['chrome']} | {r['implementation']} | {r['verification']} |" for r in data['routes']]
    lines += ['', '## Screen functions', '', '| Function | Source | Implementation | Verification |', '|---|---|---|---|']
    lines += [f"| {s['id']} | {s['source']}:{s['line']} | {s['implementation']} | {s['verification']} |" for s in screens]
    lines += ['', '## Dialog / sheet / menu declarations', '', '| Owner | Type | Source | Verification |', '|---|---|---|---|']
    lines += [f"| {o['owner']} | {o['kind']} | {o['source']}:{o['line']} | pending |" for o in overlays]
    lines += ['', '## State review', '', 'For every route: ' + ', '.join(data['required_state_matrix']) + '.', 'Mark genuinely inapplicable states with a reason during review. Source detection is not runtime evidence.', '', 'Startup: ' + ', '.join(data['additional_entry_states']) + '.', 'Running: ' + ', '.join(data['additional_run_states']) + '.', '']
    (OUT/'SCREEN-INVENTORY.md').write_text('\n'.join(lines), encoding='utf-8')
    print(f'Inventory: {len(routes)} routes, {len(screens)} screen functions, {len(overlays)} overlays. Review pending.')


if __name__ == '__main__':
    build()
