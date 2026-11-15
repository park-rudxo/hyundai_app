package io.changyeoga.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class DepartureCheckStoreTest {
    @Test fun savesAcrossInstancesAndClearsForNextTrip() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val store=DepartureCheckStore(context)
        try {
            store.clear()
            assertEquals(0L,store.checkedAt())
            store.save(1794700800000L)
            assertEquals(1794700800000L,DepartureCheckStore(context).checkedAt())
            store.clear()
            assertEquals(0L,DepartureCheckStore(context).checkedAt())
        } finally { store.clear() }
    }
}
