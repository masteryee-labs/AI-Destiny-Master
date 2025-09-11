package com.aidestinymaster.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChartDaoTest : DataDbTestBase() {
    @Test
    fun upsertAndObserve() = runBlocking {
        val dao = db.chartDao()
        val c = ChartEntity(
            id = "c1",
            kind = "natal",
            birthDate = "2000-01-01",
            birthTime = "12:00",
            tz = "+08:00",
            place = "Taipei",
            computedJson = "{}",
            snapshotJson = "{}"
        )
        dao.upsert(c)
        val loaded = dao.getById("c1")
        assertNotNull(loaded)
        val obs = dao.observeById("c1").first()
        assertEquals("natal", obs?.kind)
        val ids = dao.listIds()
        assertEquals(listOf("c1"), ids)
    }
}
