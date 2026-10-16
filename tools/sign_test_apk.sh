#!/usr/bin/env bash
# 테스트 APK(qa)를 저장소 밖의 새 서명 키로 다시 서명한다 — 옛 키에서 새 키로 넘어간다는 서명 이력을 붙여서.
#
#   tools/sign_test_apk.sh <서명할 APK> <내보낼 APK> <새 키(PKCS#8 DER)> <새 인증서(PEM)>
#
# 왜(2026-10-16 사용자 결정 — 보안 쪽으로): app/debug.keystore 는 공개 저장소에 있어 누구나 같은 키로 "업데이트"를
# 만들어 폰의 테스트 앱 위에 덮어 깔 수 있었다(앱 데이터 · 로그인이 그대로 넘어간다). 새 키는 Actions 비밀에만 있다.
#
# 서명 이력(APK Signature Scheme v3 키 회전)을 붙이므로 이미 깔린 테스트 앱(옛 키) 위에 그대로 덮어 설치된다(안드로이드 9 이상).
# 옛 키에는 "앱 데이터 이어받기"만 남기고 되돌리기 · 서명 권한 · 공유 UID · 인증 신뢰는 뺀다 — 새 키로 한 번 올라간 폰에는
# 옛 키로만 서명한 APK 가 더는 덮어 깔리지 않는다. 안드로이드 8(API 26 · 27)은 v3 를 몰라 v1 · v2(옛 키)로 확인한다.
set -euo pipefail

if [[ $# -ne 4 ]]; then
  echo "사용법: $0 <서명할 APK> <내보낼 APK> <새 키(PKCS#8 DER)> <새 인증서(PEM)>" >&2
  exit 2
fi
in=$1
out=$2
key=$3
cert=$4
root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
old_ks="$root/app/debug.keystore"

sdk=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
apksigner=""
if [[ -n "$sdk" && -d "$sdk/build-tools" ]]; then
  apksigner=$(find -L "$sdk/build-tools" -mindepth 2 -maxdepth 2 -name apksigner | sort -V | tail -1)
fi
if [[ -z "$apksigner" ]]; then
  echo "::error::apksigner 를 찾지 못했습니다(ANDROID_HOME 의 build-tools)" >&2
  exit 1
fi

# 새 키와 인증서가 짝인지 먼저 — 다른 키로 서명하면 구글 로그인에 등록한 지문과 어긋난다
if ! cmp -s <(openssl pkey -inform DER -in "$key" -pubout -outform DER) \
            <(openssl x509 -in "$cert" -noout -pubkey | openssl pkey -pubin -outform DER); then
  echo "::error::새 서명 키가 인증서($(basename "$cert"))와 짝이 아닙니다 — 비밀에 넣은 값을 확인하세요" >&2
  exit 1
fi

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

# 서명 이력: 옛 키(공개 debug 키)가 새 키를 이어받는 키로 서명한다. 옛 키에 남기는 신뢰는 앱 데이터 이어받기 하나
"$apksigner" rotate --out "$work/lineage" \
  --old-signer --ks "$old_ks" --ks-pass pass:android --ks-key-alias androiddebugkey \
    --set-installed-data true --set-shared-uid false --set-permission false --set-rollback false --set-auth false \
  --new-signer --key "$key" --cert "$cert"

# v1 · v2 는 서명 이력의 처음(옛 키), v3 는 새 키 + 서명 이력. 회전은 안드로이드 9(API 28)부터 쓴다
# (기본값은 13 부터라 9 ~ 12 폰은 옛 키로 남는다). 정렬은 Gradle 이 만든 그대로 둔다(그 옵션을 아는 apksigner 일 때)
align=()
if "$apksigner" sign --help 2>&1 | grep -q -- '--alignment-preserved'; then
  align=(--alignment-preserved true)
fi
"$apksigner" sign --in "$in" --out "$out" \
  --ks "$old_ks" --ks-pass pass:android --ks-key-alias androiddebugkey \
  --next-signer --key "$key" --cert "$cert" \
  --lineage "$work/lineage" --rotation-min-sdk-version 28 \
  --v4-signing-enabled false "${align[@]}"

# 확인 — 서명이 맞고, v3 서명자는 새 키, 서명 이력은 옛 키 → 새 키
verify=$("$apksigner" verify --verbose --print-certs "$out")
cert_sha256() { openssl x509 -in "$1" -noout -fingerprint -sha256 | sed 's/.*=//; s/://g' | tr 'A-F' 'a-f'; }
new_sha=$(cert_sha256 "$cert")
old_sha=$(keytool -exportcert -keystore "$old_ks" -storepass android -alias androiddebugkey -rfc 2>/dev/null \
  | openssl x509 -noout -fingerprint -sha256 | sed 's/.*=//; s/://g' | tr 'A-F' 'a-f')
grep -q "Verified using v3 scheme (APK Signature Scheme v3): true" <<<"$verify" \
  || { echo "::error::v3 서명이 확인되지 않습니다" >&2; echo "$verify" >&2; exit 1; }
# apksigner 판마다 서명자 줄 모양이 다르다 — 35 는 "Signer #1 certificate …", 37 은 "V3.0 Signer: certificate …"
grep -Eq "^(Signer #1|V3[.0-9]* Signer:) certificate SHA-256 digest: ${new_sha}\$" <<<"$verify" \
  || { echo "::error::v3 서명자가 새 키가 아닙니다" >&2; echo "$verify" >&2; exit 1; }
lineage=$("$apksigner" lineage --in "$out" --print-certs 2>/dev/null)
first=$(grep -m1 "certificate SHA-256 digest" <<<"$lineage" | sed 's/.*: //')
last=$(grep "certificate SHA-256 digest" <<<"$lineage" | tail -1 | sed 's/.*: //')
if [[ "$first" != "$old_sha" || "$last" != "$new_sha" ]]; then
  echo "::error::서명 이력이 옛 키 → 새 키가 아닙니다" >&2
  echo "$lineage" >&2
  exit 1
fi
echo "✓ 새 키로 서명 · 서명 이력 옛 키 → 새 키 (v3 SHA-256 ${new_sha:0:16}…)"
