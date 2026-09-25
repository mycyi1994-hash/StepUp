package com.stepup.android

import com.stepup.android.core.WalletPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalletPageTest {
    private val jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhIn0.sig-_x"

    @Test fun tokenGoesAfterTheHashSoItNeverReachesAServer() {
        assertEquals(
            "https://stepupcrew.com/wallet.html#t=$jwt",
            WalletPage.url("https://stepupcrew.com/wallet.html", jwt),
        )
    }

    @Test fun anExistingFragmentIsReplaced() {
        assertEquals(
            "https://stepupcrew.com/wallet.html#t=$jwt",
            WalletPage.url("https://stepupcrew.com/wallet.html#old", jwt),
        )
    }

    @Test fun onlyHttpsPagesAndPlainTokens() {
        assertNull(WalletPage.url("http://stepupcrew.com/wallet.html", jwt))
        assertNull(WalletPage.url("", jwt))
        assertNull(WalletPage.url("https://stepupcrew.com/wallet.html", ""))
        assertNull(WalletPage.url("https://stepupcrew.com/wallet.html", "a b&c=d"))
    }
}
