package no.nav.familie.ef.sak.behandling.migrering

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class MigreringsstatusRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    lateinit var migreringsstatusRepository: MigreringsstatusRepository

    @Test
    internal fun `skal finne status for identene man sender inn`() {
        migreringsstatusRepository.insert(Migreringsstatus("1", MigreringResultat.OK, null))
        assertThat(migreringsstatusRepository.findAllByIdentIn(setOf("1"))).hasSize(1)
        assertThat(migreringsstatusRepository.findAllByIdentIn(setOf("2"))).isEmpty()
    }

    @Test
    internal fun `skal finne de med gitt årsak`() {
        migreringsstatusRepository.insert(Migreringsstatus("1", MigreringResultat.OK, MigreringExceptionType.ALLEREDE_MIGRERT))
        assertThat(migreringsstatusRepository.findAllByÅrsak(MigreringExceptionType.ALLEREDE_MIGRERT)).hasSize(1)
        assertThat(migreringsstatusRepository.findAllByÅrsak(MigreringExceptionType.FLERE_IDENTER)).isEmpty()
    }
}
