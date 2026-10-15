"""기기 검사 결과(JUnit XML)에서 실패한 검사를 모아 찍는다 — tools/run_device_qa.sh 가 마지막에 부른다.

기기 검사 로그는 수천 줄이라 앞쪽 묶음(interaction 등)이 깨지면 그 검사 이름이 로그 앞부분에만 남는다.
작업 로그를 끝에서부터 읽는 도구로는 거기까지 닿지 못해(PR #87 Experience QA 38788035304) 실패 이름을
로그 맨 끝에 한 번 더 모은다. 검사 결과를 바꾸지 않고 읽기만 한다.
"""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'screen-gallery')
seen = set()
failures = []
for path in sorted(root.rglob('TEST-*.xml')):
    try:
        tree = ET.parse(path)
    except ET.ParseError as error:
        failures.append((path.parent, '?', path.name, [f'unreadable result file: {error}']))
        continue
    for case in tree.iter('testcase'):
        for node in [*case.findall('failure'), *case.findall('error')]:
            lines = (node.text or node.get('message') or '').strip().splitlines()
            key = (case.get('classname'), case.get('name'), tuple(lines[:2]))
            if key in seen:
                continue
            seen.add(key)
            failures.append((path.parent, case.get('classname'), case.get('name'), lines[:15]))

print(f'== 기기 검사 실패 {len(failures)}건 ({root}) ==')
for where, cls, name, lines in failures:
    print(f'--- {cls}#{name}  [{where}]')
    for line in lines:
        print(f'    {line}')
