package com.stepup.android.domain

import androidx.annotation.StringRes
import com.stepup.android.R

/*
 * 업데이트 소식 · 업데이트 내역(v12 디자인 전달 2026-11-26 S-337 · S-339) — 앱 안 목록이다(서버에서 받지 않는다: 버전과 함께 바뀌고,
 * 불러오기 실패가 없다). 판을 올릴 때 맨 위에 그 판의 줄을 더한다. 문구는 각 언어 values 폴더의 patch_notes.xml(4개 언어).
 *
 * 띄우는 조건([PatchNotesRules.shouldShow]): 마지막으로 본 판과 지금 판이 다르고 · 지금 판의 소식이 있고 · 새로 설치한 앱이 아니다(처음 설치한 사람은
 * 바뀐 것을 모른다 — 볼 판만 적어 둔다). 같은 판에서는 다시 뜨지 않는다.
 */

enum class PatchArt { RUN, SHOE, REPAIR, PUP }

data class PatchNote(
    @StringRes val title: Int,
    /** 소식 창의 한 줄 설명(없으면 소식 창에 띄우지 않고 내역에만) */
    @StringRes val body: Int? = null,
    val art: PatchArt? = null,
    /** 「새 기능」 표시 */
    val isNew: Boolean = false,
    /** 테스트 판(debug · qa)에만 있는 것 — 스토어 판에서는 빼고 보인다 */
    val testOnly: Boolean = false,
)

data class PatchRelease(
    val version: String,
    /** 2026.11.26 처럼 보일 날짜 */
    val date: String,
    val notes: List<PatchNote>,
    /** 소식 창 아래 「그 밖에 …」 한 줄 */
    @StringRes val more: Int? = null,
)

object PatchNotes {
    val releases: List<PatchRelease> = listOf(
        PatchRelease(
            "1.22.0", "2026.11.26",
            listOf(
                PatchNote(R.string.pn_122_run, R.string.pn_122_run_body, PatchArt.RUN),
                PatchNote(R.string.pn_122_shoe, R.string.pn_122_shoe_body, PatchArt.SHOE),
                PatchNote(R.string.pn_122_pup, R.string.pn_122_pup_body, PatchArt.PUP, isNew = true, testOnly = true),
                PatchNote(R.string.pn_122_repair, R.string.pn_122_repair_body, PatchArt.REPAIR),
                PatchNote(R.string.pn_122_lock),
                PatchNote(R.string.pn_122_steps),
                PatchNote(R.string.pn_122_stepbox, isNew = true),
                PatchNote(R.string.pn_122_gps),
                PatchNote(R.string.pn_122_design),
                PatchNote(R.string.pn_122_battery),
            ),
            more = R.string.pn_122_more,
        ),
        PatchRelease(
            "1.21.0", "2026.11.19",
            listOf(PatchNote(R.string.pn_121_forge), PatchNote(R.string.pn_121_daily), PatchNote(R.string.pn_121_runner, isNew = true), PatchNote(R.string.pn_121_info)),
        ),
        PatchRelease("1.20.1", "2026.11.18", listOf(PatchNote(R.string.pn_1201_crew), PatchNote(R.string.pn_1201_alone))),
        PatchRelease("1.20.0", "2026.11.18", listOf(PatchNote(R.string.pn_120_stats), PatchNote(R.string.pn_120_luck), PatchNote(R.string.pn_120_box, isNew = true))),
        PatchRelease("1.19.0", "2026.11.18", listOf(PatchNote(R.string.pn_119_items, isNew = true), PatchNote(R.string.pn_119_codes))),
        PatchRelease("1.18.0", "2026.11.04", listOf(PatchNote(R.string.pn_118_reason), PatchNote(R.string.pn_118_crew), PatchNote(R.string.pn_118_flash))),
    )

    fun of(version: String): PatchRelease? = releases.firstOrNull { it.version == version }

    /** 이 판에서 보일 줄 — 테스트 판이 아니면 테스트 전용 줄을 뺀다 */
    fun visible(release: PatchRelease, testBuild: Boolean): List<PatchNote> = release.notes.filter { testBuild || !it.testOnly }

    /** 소식 창에 띄울 줄 — 설명이 있는 것 가운데 셋까지 */
    fun headline(release: PatchRelease, testBuild: Boolean): List<PatchNote> =
        visible(release, testBuild).filter { it.body != null && it.art != null }.take(3)
}

object PatchNotesRules {
    /**
     * 소식 창을 띄울까 — [seen] 마지막으로 본 판(없으면 null), [current] 지금 판, [freshInstall] 처음 설치한 뒤 한 번도 업데이트하지 않았다.
     * 지금 판의 소식이 없으면 띄우지 않는다.
     */
    fun shouldShow(seen: String?, current: String, freshInstall: Boolean, hasNotes: Boolean): Boolean =
        hasNotes && seen != current && !(seen == null && freshInstall)
}
