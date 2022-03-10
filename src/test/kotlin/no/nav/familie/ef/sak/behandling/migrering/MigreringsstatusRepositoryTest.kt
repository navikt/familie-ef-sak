package no.nav.familie.ef.sak.behandling.migrering

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.migrering.MigreringResultat
import no.nav.familie.ef.sak.behandling.migrering.Migreringsstatus
import no.nav.familie.ef.sak.behandling.migrering.MigreringsstatusRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class MigreringsstatusRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var migreringsstatusRepository: MigreringsstatusRepository

    @Test
    internal fun `skal finne status for identene man sender inn`() {
        migreringsstatusRepository.insert(Migreringsstatus("1", MigreringResultat.OK, null))
        assertThat(migreringsstatusRepository.findAllByIdentIn(setOf("1"))).hasSize(1)
        assertThat(migreringsstatusRepository.findAllByIdentIn(setOf("2"))).isEmpty()
    }
}