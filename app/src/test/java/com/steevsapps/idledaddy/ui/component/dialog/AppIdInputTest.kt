package com.steevsapps.idledaddy.ui.component.dialog

import org.junit.Assert.assertEquals
import org.junit.Test

class AppIdInputTest {
    @Test
    fun singleAppId() {
        assertEquals(AppIdInput(listOf(440), ""), parseAppIdInput(" 440 "))
    }

    @Test
    fun spacesAndCommas() {
        assertEquals(
            AppIdInput(listOf(440, 570, 730, 10), ""),
            parseAppIdInput("440 570,730 , 10,"),
        )
    }

    @Test
    fun duplicatesAreDropped() {
        assertEquals(AppIdInput(listOf(440, 570), ""), parseAppIdInput("440, 570, 440"))
    }

    @Test
    fun invalidTokensAreKept() {
        assertEquals(
            AppIdInput(listOf(440, 5, 730), "0 99999999999"),
            parseAppIdInput("440 abc,-5 0 730 99999999999"),
        )
    }

    @Test
    fun plainCommas() {
        assertEquals(AppIdInput(listOf(111, 222, 333), ""), parseAppIdInput("111,222,333"))
    }

    @Test
    fun otherSeparators() {
        assertEquals(
            AppIdInput(listOf(111, 222, 333, 444, 555), ""),
            parseAppIdInput("111，222،333.444\n555"),
        )
    }

    @Test
    fun emptyInput() {
        assertEquals(AppIdInput(emptyList(), ""), parseAppIdInput("  , "))
    }
}
