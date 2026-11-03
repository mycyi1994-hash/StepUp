package com.stepup.android.data.repo

/**
 * 크루 대표 사진 사본이 크루 명함 저장소에 해 주는 일(2026-11-03 iOS C5) — 그림 다루기는 기기마다 다르다
 * (안드로이드 CrewImageStore · CrewPhotos 의 Bitmap, iOS 는 UIImage).
 */
interface CrewPhotoMemory {
    /** 사진 파일 → 올릴 모양(JPEG base64, 너무 크면 화질을 낮춘다). 못 읽으면 null */
    fun encodeFile(path: String): String?

    /** 방금 올린 사진(파일)을 받지 않고 바로 쓴다 */
    fun rememberFile(crewId: String, version: Int, path: String)
}
