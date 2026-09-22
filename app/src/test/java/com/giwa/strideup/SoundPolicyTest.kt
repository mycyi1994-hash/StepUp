package com.giwa.strideup

import com.giwa.strideup.ui.experience.SoundConditions
import org.junit.Assert.*
import org.junit.Test

class SoundPolicyTest {
    @Test fun everyQuietSettingIndependentlySuppressesSound() {
        // Exhaust all 128 combinations, including mute while returning to the foreground.
        for (mask in 0 until 128) {
            val c = SoundConditions(
                mask and 1 != 0, mask and 2 != 0, mask and 4 != 0,
                mask and 8 != 0, mask and 16 != 0, mask and 32 != 0, mask and 64 != 0)
            assertEquals("conditions=$mask", mask == 63, c.mayPlay)
        }
    }
}
