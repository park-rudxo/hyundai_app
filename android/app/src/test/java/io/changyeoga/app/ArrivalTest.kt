package io.changyeoga.app

import org.junit.Assert.*
import org.junit.Test

class ArrivalTest {
    private val stops=listOf(Stop("a","외가",37.0,127.0))
    private fun fix(t:Long,lat:Double=37.0,accuracy:Float=10f)=Position(lat,127.0,accuracy,t)
    @Test fun dwellOnceAndRearmOutside() {
        val detector=ArrivalDetector()
        assertNull(detector.update(stops,fix(0),0))
        assertNull(detector.update(stops,fix(10000),10000))
        assertEquals("a",detector.update(stops,fix(20000),20000)?.id)
        assertNull(detector.update(stops,fix(30000),30000))
        detector.update(stops,fix(40000,38.0),40000)
        detector.update(stops,fix(50000),50000)
        detector.update(stops,fix(60000),60000)
        assertEquals("a",detector.update(stops,fix(70000),70000)?.id)
    }
    @Test fun rejectsStaleInaccurateAndDiscontinuousReadings() {
        val detector=ArrivalDetector()
        assertNull(detector.update(stops,fix(0),20000))
        assertNull(detector.update(stops,fix(20000,accuracy=100f),20000))
        detector.update(stops,fix(30000),30000)
        assertNull(detector.update(stops,fix(60000),60000))
        assertNull(detector.update(stops,fix(60000),60000))
        assertFalse(fix(0,Double.NaN).usable(0))
    }
    @Test fun nearbyDestinationsAreNotGuessed() {
        val detector=ArrivalDetector()
        val ambiguous=stops+Stop("b","친가",37.0001,127.0)
        for(t in 0L..30000L step 10000L) assertNull(detector.update(ambiguous,fix(t),t))
    }
    @Test fun voiceUsesExactUnambiguousNamesAndNeverCompletesItems() {
        assertEquals(VoiceAction.Arrive("a"),parseVoice("외가 도착!",stops))
        assertEquals(VoiceAction.Read,parseVoice("짐 알려 줘",stops))
        assertEquals(VoiceAction.Unknown,parseVoice("외가 도착하지 않았어",stops))
        assertEquals(VoiceAction.Unknown,parseVoice("외가 도착",stops+Stop("b","외가")))
        assertEquals(VoiceAction.Unknown,parseVoice("다 건넸어",stops))
    }
    @Test fun coordinatesMustBeCompleteAndInRange() {
        assertTrue(runCatching { Stop(name="bad",latitude=91.0,longitude=127.0) }.isFailure)
        assertTrue(runCatching { Stop(name="bad",latitude=37.0) }.isFailure)
        assertTrue(distanceMeters(37.0,127.0,37.0,127.0)<0.01)
    }
}
