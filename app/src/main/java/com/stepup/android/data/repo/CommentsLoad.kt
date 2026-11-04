package com.stepup.android.data.repo

/** 한 글의 댓글을 받는 중 · 받지 못함 · 받음 — 받기 전 · 실패를 "아직 댓글이 없어요"로 보이지 않게 */
enum class CommentsLoad { LOADING, FAILED, READY }
