package com.pause.app

import com.google.common.truth.Truth.assertThat
import com.pause.app.domain.model.IntervalOptions
import org.junit.Test

class IntervalOptionsTest {

    @Test
    fun `exact options are preserved`() {
        IntervalOptions.minutes.forEach { value ->
            assertThat(IntervalOptions.sanitize(value)).isEqualTo(value)
        }
    }

    @Test
    fun `in-range custom values are kept, not snapped`() {
        // Custom minutes inside the range must survive unchanged (no snapping to a preset).
        assertThat(IntervalOptions.sanitize(6)).isEqualTo(6)
        assertThat(IntervalOptions.sanitize(7)).isEqualTo(7)
        assertThat(IntervalOptions.sanitize(13)).isEqualTo(13)
        assertThat(IntervalOptions.sanitize(99)).isEqualTo(99)
    }

    @Test
    fun `out-of-range values clamp to the supported bounds`() {
        assertThat(IntervalOptions.sanitize(0)).isEqualTo(IntervalOptions.MIN_MINUTES)
        assertThat(IntervalOptions.sanitize(-5)).isEqualTo(IntervalOptions.MIN_MINUTES)
        assertThat(IntervalOptions.sanitize(1000)).isEqualTo(IntervalOptions.MAX_MINUTES)
    }

    @Test
    fun `isPreset distinguishes presets from custom values`() {
        assertThat(IntervalOptions.isPreset(15)).isTrue()
        assertThat(IntervalOptions.isPreset(7)).isFalse()
    }

    @Test
    fun `default is one of the supported options`() {
        assertThat(IntervalOptions.minutes).contains(IntervalOptions.DEFAULT)
    }
}
