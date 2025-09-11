package com.aidestinymaster.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportDaoTest : DataDbTestBase() {
    @Test
    fun upsertAndQuery() = runBlocking {
        val dao = db.reportDao()
        val r = ReportEntity(
            id = "r1",
            type = "demo",
            title = "T",
            createdAt = 1L,
            updatedAt = 1L,
            summary = "S",
            contentEnc = "C",
            chartRef = "c1"
        )
        dao.upsert(r)
        val loaded = dao.getById("r1")
        assertNotNull(loaded)
        assertEquals("demo", loaded!!.type)

        val ids = dao.listRecent(10)
        assertEquals(true, ids.isNotEmpty())
    }
}
