package com.stepup.android

import com.stepup.android.domain.AvatarArt
import com.stepup.android.domain.AvatarArtCatalog
import com.stepup.android.domain.AvatarGender
import com.stepup.android.domain.AvatarLook
import com.stepup.android.domain.AvatarPose
import com.stepup.android.domain.Outfits
import com.stepup.android.domain.SneakerMint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 캐릭터 그림을 고를 때의 약속.
 *
 *   * 성별은 절대 바뀌지 않는다 — 없는 자세는 같은 성별의 다른 자세로.
 *   * 그림 속 착장이 실제 착장과 다르면 그렇다고 알린다.
 */
class AvatarArtTest {

    private val allLooks = AvatarGender.entries.flatMap { g ->
        Outfits.ALL.flatMap { o ->
            listOf(null, SneakerMint.starter()).map { shoe -> AvatarLook(gender = g, outfit = o, shoe = shoe) }
        }
    }

    @Test
    fun `성별마다 그림이 적어도 한 장 있다`() {
        AvatarGender.entries.forEach { g ->
            assertTrue("$g 그림이 없다", AvatarArt.entries.any { it.gender == g })
        }
    }

    @Test
    fun `어떤 자세를 달라고 해도 성별이 바뀌지 않는다`() {
        allLooks.forEach { look ->
            AvatarPose.entries.forEach { pose ->
                val r = AvatarArtCatalog.resolve(look, pose)
                assertEquals("${look.gender}/$pose", look.gender, r.art.gender)
            }
        }
    }

    @Test
    fun `기본 후드와 기본 운동화만 그림 그대로 보인다`() {
        val base = AvatarLook(gender = AvatarGender.MALE, outfit = Outfits.STARTER_HOODIE, shoe = null)
        assertTrue(AvatarArtCatalog.resolve(base, AvatarPose.RUN).lookShown)
        assertTrue(AvatarArtCatalog.resolve(base.copy(gender = AvatarGender.FEMALE), AvatarPose.IDLE).lookShown)
    }

    @Test
    fun `NFT 신발을 신으면 그림이 그 신발을 보여 준다고 하지 않는다`() {
        val look = AvatarLook(gender = AvatarGender.FEMALE, outfit = Outfits.STARTER_HOODIE, shoe = SneakerMint.starter())
        val r = AvatarArtCatalog.resolve(look, AvatarPose.IDLE)
        assertTrue(r.outfitShown)
        assertFalse(r.shoeShown)
        assertFalse(r.lookShown)
    }

    @Test
    fun `다른 의상을 입으면 그림이 그 의상을 보여 준다고 하지 않는다`() {
        Outfits.ALL.filter { it.id != Outfits.STARTER_HOODIE.id }.forEach { o ->
            val r = AvatarArtCatalog.resolve(AvatarLook(outfit = o), AvatarPose.RUN)
            assertFalse("${o.id} 가 그림에 보인다고 한다", r.outfitShown)
        }
    }

    @Test
    fun `없는 자세는 exactPose 로 드러난다`() {
        val male = AvatarLook(gender = AvatarGender.MALE)
        assertTrue(AvatarArtCatalog.resolve(male, AvatarPose.RUN).exactPose)
        assertFalse(AvatarArtCatalog.resolve(male, AvatarPose.IDLE).exactPose)
        assertFalse(AvatarArtCatalog.resolve(male, AvatarPose.CHEER).exactPose)
        val female = AvatarLook(gender = AvatarGender.FEMALE)
        assertTrue(AvatarArtCatalog.resolve(female, AvatarPose.IDLE).exactPose)
        assertFalse(AvatarArtCatalog.resolve(female, AvatarPose.RUN).exactPose)
    }

    @Test
    fun `추가로 필요한 자세 목록`() {
        val missing = AvatarArtCatalog.missingPoses().toSet()
        assertEquals(
            setOf(
                AvatarGender.MALE to AvatarPose.IDLE,
                AvatarGender.MALE to AvatarPose.CHEER,
                AvatarGender.FEMALE to AvatarPose.RUN,
                AvatarGender.FEMALE to AvatarPose.CHEER,
            ),
            missing,
        )
    }
}
