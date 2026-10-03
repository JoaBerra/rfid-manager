package com.joakim.rfidmanager

import org.junit.Assert.assertNotNull
import org.junit.Test

class RoomGeneratedTest {
    @Test fun generatedImplsExist() {
        assertNotNull(Class.forName("com.joakim.rfidmanager.data.local.AppDatabase_Impl"))
        assertNotNull(Class.forName("com.joakim.rfidmanager.data.local.dao.PersistedReadingDao_Impl"))
    }
}
