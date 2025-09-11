package com.aidestinymaster.app.prefs

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPrefsTest {
    @Test
    fun testUserPrefsSetAndGet() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

        // defaults
        val defLang = UserPrefs.langFlow(ctx).first()
        val defTheme = UserPrefs.themeFlow(ctx).first()
        val defNotif = UserPrefs.notifEnabledFlow(ctx).first()
        val defSync = UserPrefs.syncEnabledFlow(ctx).first()
        assertEquals("zh-TW", defLang)
        assertEquals("system", defTheme)
        assertEquals(false, defNotif)
        assertEquals(false, defSync)

        // set values
        UserPrefs.setLang(ctx, "en")
        UserPrefs.setTheme(ctx, "dark")
        UserPrefs.setNotifEnabled(ctx, true)
        UserPrefs.setSyncEnabled(ctx, true)

        // verify
        assertEquals("en", UserPrefs.langFlow(ctx).first())
        assertEquals("dark", UserPrefs.themeFlow(ctx).first())
        assertEquals(true, UserPrefs.notifEnabledFlow(ctx).first())
        assertEquals(true, UserPrefs.syncEnabledFlow(ctx).first())
    }
}
