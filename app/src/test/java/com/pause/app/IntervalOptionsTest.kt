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
    fun `arbitrary value snaps to the nearest option`() {
        assertThat(IntervalOptions.sanitize(6)).isEqualTo(5)
        assertThat(IntervalOptions.sanitize(8)).isEqualTo(10)
        assertThat(IntervalOptions.sanitize(13)).isEqualTo(15)
        assertThat(IntervalOptions.sanitize(99)).isEqualTo(30)
        assertThat(IntervalOptions.sanitize(0)).isEqualTo(5)
    }

    @Test
    fun `default is one of the supported options`() {
        assertThat(IntervalOptions.minutes).contains(IntervalOptions.DEFAULT)
    }
}
