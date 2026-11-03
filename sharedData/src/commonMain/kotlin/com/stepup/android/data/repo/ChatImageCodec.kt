package com.stepup.android.data.repo

/**
 * 채팅 사진을 기기에서 다루는 일(2026-11-03 iOS C5) — 저장소(공용)는 이것만 부른다.
 * 안드로이드 AndroidChatImageCodec(예전 CrewChatRepository 의 Bitmap 코드 그대로) · iOS 는 UIImage.
 */
interface ChatImageCodec<Img> {
    fun byteCount(img: Img): Int
    fun decodeFile(path: String): Img?
    fun decodeBase64(data: String): Img?

    /** 긴 변 [maxSide] 까지 줄이고 base64 [maxBase64] 자 안의 JPEG 로 — [dir] 에 파일을 남긴다. 못 줄이면 null */
    fun encode(source: Img, dir: String, maxSide: Int, maxBase64: Int): ChatPhoto?

    /** 잠깐 둔 사진을 모두 지운다 */
    fun clearDir(dir: String)
}
