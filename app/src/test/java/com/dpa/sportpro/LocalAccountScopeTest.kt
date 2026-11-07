package com.dpa.sportpro

import com.dpa.sportpro.data.repository.LocalAccountScope
import org.junit.Assert.*
import org.junit.Test

class LocalAccountScopeTest {
    @Test fun accountsAndModulesHaveIndependentStorageNames() {
        val first = LocalAccountScope.fileName("fees", "uid-1")
        assertEquals(first, LocalAccountScope.fileName("fees", "uid-1"))
        assertNotEquals(first, LocalAccountScope.fileName("fees", "uid-2"))
        assertNotEquals(first, LocalAccountScope.fileName("players", "uid-1"))
        assertNotEquals("fees", first)
    }
    @Test(expected = IllegalArgumentException::class)
    fun emptyIdentityCannotOpenLocalStorage() {
        LocalAccountScope.fileName("fees", "")
    }
}
