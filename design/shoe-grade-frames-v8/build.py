from pathlib import Path


ROOT = Path(__file__).resolve().parent
ROOT.mkdir(parents=True, exist_ok=True)
ASSETS = ROOT / "assets"
ASSETS.mkdir(exist_ok=True)

TIERS = [
    ("일반", "01 / CARBON", "#91A1B8", "#CFD7E5", 0.035),
    ("레어", "02 / VELOCITY", "#4A9FF0", "#B5DAFF", 0.055),
    ("희귀", "03 / PRISM", "#A778E6", "#E1C5FF", 0.075),
    ("레전더리", "04 / PODIUM", "#E2AE52", "#FFE2A2", 0.090),
    ("레드라인", "05 / REDLINE", "#FF654D", "#FFC267", 0.145),
    ("피니시", "06 / FINISH", "#5DE9DE", "#E1FCFF", 0.165),
]

OUTER = "M53 25H387L415 53V365L387 393H53L25 365V53Z"
INNER = "M58 37H382L403 58V360L382 381H58L37 360V58Z"


def defs(i):
    _, _, color, hi, _ = TIERS[i]
    if i == 4:
        stops = '<stop stop-color="#FFCB6E"/><stop offset=".5" stop-color="#FF654D"/><stop offset="1" stop-color="#FFAA84"/>'
    elif i == 5:
        stops = '<stop stop-color="#70FFF0"/><stop offset=".47" stop-color="#D58BFF"/><stop offset="1" stop-color="#E1FCFF"/>'
    else:
        stops = f'<stop stop-color="{hi}"/><stop offset=".52" stop-color="{color}"/><stop offset="1" stop-color="{hi}"/>'
    ground_alpha = [0, .24, .29, .34, .48, .57][i]
    halo_alpha = [0, 0, .11, .16, .29, .36][i]
    beam_alpha = [0, 0, 0, .065, .085, .10][i]
    return f'''
    <linearGradient id="edge{i}" x1="0" y1="0" x2="1" y2="1">{stops}</linearGradient>
    <radialGradient id="aura{i}"><stop stop-color="{color}" stop-opacity="{TIERS[i][4]}"/><stop offset="1" stop-color="{color}" stop-opacity="0"/></radialGradient>
    <radialGradient id="ground{i}"><stop stop-color="{hi}" stop-opacity="{ground_alpha}"/><stop offset=".48" stop-color="{color}" stop-opacity="{ground_alpha * .48:.3f}"/><stop offset="1" stop-color="{color}" stop-opacity="0"/></radialGradient>
    <radialGradient id="halo{i}"><stop offset=".48" stop-color="{color}" stop-opacity="0"/><stop offset=".79" stop-color="{color}" stop-opacity="{halo_alpha}"/><stop offset="1" stop-color="{color}" stop-opacity="0"/></radialGradient>
    <linearGradient id="beam{i}" x1="0" y1="0" x2="0" y2="1"><stop stop-color="{hi}" stop-opacity="0"/><stop offset=".56" stop-color="{color}" stop-opacity="{beam_alpha}"/><stop offset="1" stop-color="{color}" stop-opacity="0"/></linearGradient>
    <filter id="blur{i}" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="4"/></filter>
    '''


def finish_ribbon():
    """A small running finish tape, separate from the orbit and speed lines."""
    tiles = []
    for row in range(2):
        for col in range(8):
            tint = '#A0FFF4' if (row + col) % 2 == 0 else '#D7A7FF'
            tiles.append(
                f'<rect x="{col * 11}" y="{row * 11}" width="9" height="9" '
                f'rx="1" fill="{tint}" opacity="{.70 if col < 5 else .42}"/>'
            )
    return (
        '<g transform="translate(287 76) rotate(18)">'
        '<path d="M-6-5H94L101 27H-6Z" fill="#80DFF1" opacity=".10"/>'
        + ''.join(tiles)
        + '</g>'
    )


def back_effects(i):
    _, _, color, hi, _ = TIERS[i]
    effect = '<ellipse cx="220" cy="318" rx="127" ry="16" fill="#536986" opacity=".16"/>'
    if i >= 1:
        effect += f'<ellipse cx="220" cy="315" rx="{124 + i * 9}" ry="{23 + i * 3}" fill="url(#ground{i})"/>'
        effect += f'<path d="M100 330h240" stroke="{color}" stroke-width="1" opacity=".26"/>'
        effect += f'<path d="M90 276l27-9 M326 158l25-8" stroke="{color}" stroke-width="1.5" opacity=".25" stroke-linecap="round"/>'
    if i >= 2:
        effect += f'<ellipse cx="220" cy="198" rx="{139 + i * 2}" ry="{142 + i * 2}" fill="url(#halo{i})"/>'
        effect += f'<path d="M73 273l32-12 M336 151l27-10" stroke="{color}" stroke-width="2" stroke-linecap="round" opacity=".34"/>'
    if i >= 3:
        effect += f'<path d="M172 42h96l83 281H89Z" fill="url(#beam{i})"/>'
        effect += f'<path d="M73 294l42-15 M331 134l35-13" stroke="{hi}" stroke-width="2.5" stroke-linecap="round" opacity=".44"/>'
        effect += f'<path d="M91 157a141 141 0 0 1 50-65 M323 294a141 141 0 0 0 34-54" fill="none" stroke="{color}" stroke-width="1.5" opacity=".23"/>'
        effect += f'<path d="M112 346h216 M134 351h172" stroke="{hi}" stroke-width="1" opacity=".24"/>'
    if i >= 4:
        effect += f'<path d="M55 233l66-24 M59 247l75-27 M310 152l71-26 M321 167l58-21" stroke="{color}" stroke-width="2.3" stroke-linecap="round" opacity=".42"/>'
        effect += f'<path d="M120 342h200" stroke="{hi}" stroke-width="1.4" opacity=".30"/>'
    if i == 4:
        effect += '<path d="M65 181l91-33 M59 200l80-30 M69 220l56-21 M294 120l82-29 M309 140l68-24" stroke="#FF8B55" stroke-width="3" stroke-linecap="round" opacity=".55"/>'
        effect += '<path d="M78 87h63 M88 95h44 M305 323h60 M318 331h39" stroke="#FFBA66" stroke-width="1.7" opacity=".35"/>'
        effect += '<ellipse cx="220" cy="318" rx="156" ry="20" fill="url(#ground4)" opacity=".78"/>'
        # New element 1: a curved heat wake from the heel, not another straight speed line.
        effect += '<path d="M318 81C378 99 400 145 389 203C381 169 357 137 318 126Z" fill="#FF704C" opacity=".14"/>'
        effect += '<path d="M347 87C396 115 405 163 389 210 M359 74C411 105 421 168 402 224 M370 68C422 104 429 170 412 228" fill="none" stroke="#FFB46A" stroke-width="2" stroke-linecap="round" opacity=".48"/>'
    if i == 5:
        effect += '<path d="M69 207a154 154 0 0 1 129-145 M242 346a154 154 0 0 0 130-120" fill="none" stroke="#68F6E8" stroke-width="3.2" opacity=".58" stroke-linecap="round"/>'
        effect += '<path d="M82 193a142 142 0 0 1 106-121 M252 334a142 142 0 0 0 111-113" fill="none" stroke="#D692FF" stroke-width="2" opacity=".51" stroke-linecap="round"/>'
        effect += '<path d="M96 177a129 129 0 0 1 85-93 M265 321a129 129 0 0 0 88-96" fill="none" stroke="#DDFBFF" stroke-width="1" opacity=".45" stroke-dasharray="10 8"/>'
        effect += '<ellipse cx="220" cy="315" rx="171" ry="26" fill="url(#ground5)" opacity=".75"/>'
        effect += '<path d="M77 287l43-17 M322 118l43-17" stroke="#B98DFF" stroke-width="2.8" opacity=".48" stroke-linecap="round"/>'
        # New element 2: a checkered finish tape, distinct from the orbiting arcs.
        effect += finish_ribbon()
    return effect


def front_effects(i):
    _, _, color, hi, _ = TIERS[i]
    effect = ''
    if i >= 1:
        effect += f'<path d="M101 102l13-4" stroke="{hi}" stroke-width="1.7" stroke-linecap="round" opacity=".60"/>'
    if i >= 2:
        effect += f'<path d="M345 282l17-5 M81 243l12-4" stroke="{hi}" stroke-width="2" stroke-linecap="round" opacity=".65"/>'
    if i >= 3:
        effect += f'<path d="M345 101v12 M339 107h12 M96 300v9 M91.5 304.5h9" stroke="{hi}" stroke-width="1.5" stroke-linecap="round" opacity=".83"/>'
    if i >= 4:
        effect += f'<path d="M365 221v11 M359.5 226.5h11 M115 147v10 M110 152h10" stroke="{hi}" stroke-width="1.5" opacity=".82"/>'
        effect += f'<circle cx="87" cy="190" r="1.7" fill="{hi}" opacity=".8"/><circle cx="350" cy="313" r="1.4" fill="{hi}" opacity=".72"/>'
    if i == 4:
        effect += '<path d="M72 116l25-8 M80 130l17-6 M343 269l24-9 M350 283l15-5" stroke="#FFC267" stroke-width="2.2" opacity=".76" stroke-linecap="round"/>'
        effect += '<path d="M341 86v13 M334.5 92.5h13 M67 316v11 M61.5 321.5h11" stroke="#FFDB89" stroke-width="1.7" opacity=".9"/>'
    if i == 5:
        effect += '<path d="M70 142v14 M63 149h14 M372 295v14 M365 302h14 M330 77v12 M324 83h12" stroke="#E9C8FF" stroke-width="1.8" opacity=".95"/>'
        effect += '<circle cx="336" cy="81" r="2.1" fill="#C0FFFF"/><circle cx="91" cy="322" r="1.9" fill="#E3C8FF"/><circle cx="353" cy="184" r="1.5" fill="#9CFFF4"/><circle cx="99" cy="177" r="1.5" fill="#E7BCFF"/>'
        effect += '<path d="M69 263l27-10 M343 151l26-10" stroke="#D0A7FF" stroke-width="2.5" stroke-linecap="round" opacity=".75"/>'
    return effect


def frame(i):
    _, _, color, hi, _ = TIERS[i]
    base = f'''
    <path d="{OUTER} {INNER}" fill="#102037" fill-rule="evenodd"/>
    <path d="{OUTER}" fill="none" stroke="#293B54" stroke-width="7" stroke-linejoin="round"/>
    <path d="{OUTER}" fill="none" stroke="url(#edge{i})" stroke-width="3.2" stroke-linejoin="round"/>
    <path d="{INNER}" fill="none" stroke="{color}" stroke-width="1.4" opacity=".68"/>
    <path d="M54 31h84 M302 31h84 M54 387h84 M302 387h84" stroke="{hi}" stroke-width="2" stroke-linecap="round" opacity=".75"/>
    '''
    if i == 0:
        extra = f'<path d="M30 112v25 M410 281v25" stroke="{hi}" stroke-width="2" opacity=".55"/>'
    elif i == 1:
        extra = f'''
        <path d="M27 107v60 M413 251v60 M103 25h54 M283 393h54" stroke="{hi}" stroke-width="3" stroke-linecap="round"/>
        <path d="M48 52l18 0 M374 366h18" stroke="{color}" stroke-width="3"/>
        <path d="M48 201h12 M380 217h12" stroke="{hi}" stroke-width="1.5" opacity=".6"/>
        '''
    elif i == 2:
        extra = f'''
        <path d="M26 100v73 M414 245v73 M102 25h60 M278 393h60" stroke="{hi}" stroke-width="3" stroke-linecap="round"/>
        <path d="M68 31l12 12 M80 31l12 12 M348 381l12 12 M360 381l12 12" stroke="{hi}" stroke-width="2"/>
        <path d="M44 208v25 M396 187v25" stroke="{color}" stroke-width="2" opacity=".75"/>
        <path d="M47 121h11 M47 131h17 M47 141h11 M382 277h11 M376 287h17 M382 297h11" stroke="{hi}" stroke-width="1.6" opacity=".55"/>
        '''
    elif i == 3:
        extra = f'''
        <path d="M27 100v75 M413 243v75 M102 25h63 M275 393h63" stroke="{hi}" stroke-width="3.4" stroke-linecap="round"/>
        <path d="M195 26l25 15 25-15 M195 392l25-15 25 15" fill="none" stroke="{hi}" stroke-width="2.6"/>
        <path d="M31 86l26 15 M409 332l-26-15" stroke="{color}" stroke-width="2"/>
        <path d="M220 18v12 M214 24h12" stroke="{hi}" stroke-width="1.6"/>
        <path d="M48 119h12 M48 130h18 M48 141h12 M380 277h12 M374 288h18 M380 299h12" stroke="{hi}" stroke-width="1.8" opacity=".68"/>
        '''
    elif i == 4:
        extra = f'''
        <path d="M25 86v103 M415 229v103 M82 25h88 M270 393h88" stroke="{hi}" stroke-width="4" stroke-linecap="round"/>
        <path d="M185 26l35 21 35-21 M185 392l35-21 35 21" fill="none" stroke="{hi}" stroke-width="3.3"/>
        <path d="M27 196l-13 41 M413 222l13-41" stroke="{color}" stroke-width="4" stroke-linecap="round"/>
        <path d="M23 295l31-38 M417 123l-31 38" stroke="{hi}" stroke-width="2.6" opacity=".8"/>
        <path d="M48 75l17 10 17-10 M358 343l17-10 17 10" fill="none" stroke="#FFB768" stroke-width="2.1" opacity=".8"/>
        <path d="M46 132h14 M46 143h21 M46 154h28 M380 264h14 M373 275h21 M366 286h28" stroke="#FF9C5D" stroke-width="2" opacity=".75"/>
        '''
    else:
        extra = f'''
        <path d="M25 80v109 M415 229v109 M82 25h89 M269 393h89" stroke="{hi}" stroke-width="4.3" stroke-linecap="round"/>
        <path d="M182 26l38 24 38-24 M182 392l38-24 38 24" fill="none" stroke="{hi}" stroke-width="3.5"/>
        <path d="M27 200l-15 31 M413 218l15-31" stroke="{color}" stroke-width="4" stroke-linecap="round"/>
        <path d="M30 74l29 26 M410 344l-29-26" stroke="#D49CFF" stroke-width="2.6" opacity=".8"/>
        <path d="M220 13v19 M210.5 22.5h19 M64 356v14 M57 363h14" stroke="{hi}" stroke-width="1.9"/>
        <path d="M46 113h13 M46 125h13 M46 137h13 M46 149h13 M381 269h13 M381 281h13 M381 293h13 M381 305h13" stroke="#9CFFF4" stroke-width="2.2" opacity=".85"/>
        <path d="M341 360h10v10h-10z M355 360h10v10h-10z M369 360h10v10h-10z" fill="#D7ADFF" opacity=".58"/>
        '''
    if i >= 3:
        glow = f'<path d="{OUTER}" fill="none" stroke="{color}" stroke-width="6" opacity=".28" filter="url(#blur{i})"/>'
    else:
        glow = ''
    plates = ''
    if i >= 2:
        opacity = [.0, .0, .28, .42, .68, .78][i]
        plates += f'''
        <path d="M53 25h96l-19 12H58Z M387 25h-96l19 12h72Z M53 393h96l-19-12H58Z M387 393h-96l19-12h72Z" fill="url(#edge{i})" opacity="{opacity}"/>
        '''
    if i >= 3:
        plates += f'''
        <path d="M25 95v79l12-18v-43Z M415 323v-79l-12 18v43Z" fill="url(#edge{i})" opacity=".28"/>
        '''
    if i >= 4:
        plates += f'''
        <path d="M19 188q-6 25 0 45 M421 230q6-25 0-45" fill="none" stroke="{color}" stroke-width="2.2" opacity=".72" stroke-linecap="round"/>
        '''
    if i == 5:
        plates += '<path d="M165 30h110 M165 388h110" stroke="#E3B8FF" stroke-width="2.5" opacity=".85"/>'
        plates += '<path d="M24 204l13-22v43l-13 22 M416 214l-13-22v43l13 22" fill="#82F7E8" opacity=".22"/>'
    return glow + base + plates + extra


def shell(w, h, body, all_defs):
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}"><defs>{all_defs}</defs><style>text{{font-family:Pretendard,"Malgun Gothic",sans-serif}}</style>{body}</svg>'


all_defs = ''.join(defs(i) for i in range(6))
for i in range(6):
    (ROOT / f'frame-{i+1:02d}.svg').write_text(shell(440, 418, frame(i), defs(i)), encoding='utf-8')
    (ROOT / f'back-effect-{i+1:02d}.svg').write_text(shell(440, 418, back_effects(i), defs(i)), encoding='utf-8')
    (ROOT / f'front-effect-{i+1:02d}.svg').write_text(shell(440, 418, front_effects(i), defs(i)), encoding='utf-8')


def card(i):
    name, english, color, hi, _ = TIERS[i]
    x = 50 + (i % 3) * 480
    y = 188 + (i // 3) * 495
    stage = ['#081320', '#081320', '#081320', '#081320', '#1B1119', '#0A1824'][i]
    border = ['#24364F', '#24364F', '#24364F', '#24364F', '#67352F', '#3B5972'][i]
    return f'''
    <g transform="translate({x} {y})">
      <rect width="440" height="468" rx="29" fill="#0C1728" stroke="{border}" stroke-width="1.5"/>
      <rect x="12" y="12" width="416" height="401" rx="23" fill="{stage}"/>
      <ellipse cx="220" cy="207" rx="173" ry="159" fill="url(#aura{i})"/>
      {back_effects(i)}
      <image href="assets/shoe-city.png" x="54" y="52" width="332" height="332"/>
      {front_effects(i)}
      {frame(i)}
      <path d="M28 421h384" stroke="#22364E"/>
      <text x="28" y="450" font-size="16" font-weight="700" fill="{color}">{english}</text>
      <text x="410" y="451" text-anchor="end" font-size="25" font-weight="700" fill="#F3F6FF">{name}</text>
    </g>'''


board_body = f'''
<rect width="1500" height="1210" fill="#050B16"/>
<path d="M50 48h77" stroke="#5A92E9" stroke-width="4" stroke-linecap="round"/>
<text x="50" y="79" font-size="16" font-weight="700" letter-spacing="4" fill="#6D9CEB">STEPUP / SHOE RARITY</text>
<text x="50" y="137" font-size="46" font-weight="700" fill="#F4F7FF">러닝의 속도감을 담은 6단계 등급 효과</text>
<text x="50" y="168" font-size="19" fill="#9BAEC8">레드라인에는 열기 파동, 피니시에는 체커 결승 테이프를 더했습니다.</text>
{''.join(card(i) for i in range(6))}
<path d="M50 1176h1400" stroke="#1F3148"/>
<text x="50" y="1201" font-size="17" fill="#8EA3C0">01–04 현재 앱의 신발 등급   ·   05–06 확장 콘셉트</text>
'''
(ROOT / '00-StepUp-6단계-등급-이펙트.svg').write_text(shell(1500, 1210, board_body, all_defs), encoding='utf-8')


def mobile_body(i):
    name, _, color, hi, _ = TIERS[i]
    return f'''
<defs><radialGradient id="phone"><stop stop-color="#10213A"/><stop offset="1" stop-color="#050912"/></radialGradient></defs>
<rect width="390" height="844" fill="url(#phone)"/>
<text x="24" y="34" font-size="16" font-weight="700" fill="#F5F7FF">9:41</text>
<text x="24" y="82" font-size="23" font-weight="900" font-style="italic" fill="#F5F7FF">STEPUP</text>
<rect x="302" y="53" width="65" height="36" rx="18" fill="#101C30"/><text x="335" y="77" text-anchor="middle" font-size="15" font-weight="700" fill="#F3F6FF">뽑기</text>
<text x="195" y="138" text-anchor="middle" font-size="15" fill="#92A8C5">내 신발</text>
<text x="195" y="178" text-anchor="middle" font-size="28" font-weight="700" fill="#F4F7FF">시티 스프린트</text>
<text x="195" y="207" text-anchor="middle" font-size="15" fill="#91A9C8">#0001 · Lv.1</text>
<g transform="translate(35 232) scale(.727)">
  <ellipse cx="220" cy="210" rx="171" ry="157" fill="url(#aura{i})"/>
  {back_effects(i)}
  <image href="assets/shoe-city.png" x="54" y="52" width="332" height="332"/>
  {front_effects(i)}
  {frame(i)}
</g>
<text x="195" y="553" text-anchor="middle" font-size="17" font-weight="700" fill="{hi}">{name}</text>
<text x="24" y="606" font-size="19" font-weight="700" fill="#F3F6FF">보유 신발 <tspan fill="#8EA6C7">3</tspan></text>
{''.join(f'<g transform="translate({24+i*115} 622) scale(.23)"><rect width="440" height="440" rx="52" fill="#101E32"/><image href="assets/shoe-city.png" x="67" y="64" width="306" height="306"/>{frame(i)}</g>' for i in range(3))}
<path d="M0 775h390" stroke="#172940"/>
<text x="50" y="811" text-anchor="middle" font-size="13" fill="#8498B5">러닝</text><text x="145" y="811" text-anchor="middle" font-size="13" fill="#F4F7FF">신발</text><text x="245" y="811" text-anchor="middle" font-size="13" fill="#8498B5">커뮤니티</text><text x="342" y="811" text-anchor="middle" font-size="13" fill="#8498B5">내 정보</text>
<rect x="125" y="825" width="41" height="3" rx="1.5" fill="#4B85FF"/>
'''


for i, filename in [(3, '07-레전더리-모바일'), (4, '08-레드라인-모바일'), (5, '09-피니시-모바일')]:
    (ROOT / f'{filename}.svg').write_text(shell(390, 844, mobile_body(i), all_defs), encoding='utf-8')
print(ROOT)
