# 테스트 APK(qa) 전용 — release 규칙(proguard-rules.pro)에 더한다.
#
# 코드는 release 처럼 줄이고 최적화하되 이름은 바꾸지 않는다. 테스터가 보낸 오류 기록을 매핑 파일 없이
# 바로 읽고, 이름으로 찾는 코드가 있어도 테스트 APK 에서만 깨지는 일이 없게 한다.
-dontobfuscate
