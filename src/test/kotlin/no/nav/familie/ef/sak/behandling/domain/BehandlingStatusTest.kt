package no.nav.familie.ef.sak.behandling.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class BehandlingStatusTest {

    @EnumSource(
        value = BehandlingStatus::class,
        names = ["UTREDES", "OPPRETTET"],
        mode = EnumSource.Mode.EXCLUDE
    )
    @ParameterizedTest
    internal fun `behandling skal være låst`(status: BehandlingStatus) {
        assertThat(status.behandlingErLåstForVidereRedigering()).isTrue
    }

    @EnumSource(
        value = BehandlingStatus::class,
        names = ["FATTER_VEDTAK", "IVERKSETTER_VEDTAK", "FERDIGSTILT", "SATT_PÅ_VENT"],
        mode = EnumSource.Mode.EXCLUDE
    )
    @ParameterizedTest
    internal fun `behandling skal være låst hvis en behandling utredes eller opprettes`(status: BehandlingStatus) {
        assertThat(status.behandlingErLåstForVidereRedigering()).isFalse
    }
}