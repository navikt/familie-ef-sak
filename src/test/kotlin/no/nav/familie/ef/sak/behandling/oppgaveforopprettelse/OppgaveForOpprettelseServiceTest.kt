package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class OppgaveForOpprettelseServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var oppgaverForOpprettelseService: OppgaverForOpprettelseService

    @Autowired
    private lateinit var oppgaverForOpprettelseRepository: OppgaverForOpprettelseRepository

    @Test
    internal fun `insert oppgave for opprettelse`() {
        val uuid = UUID.randomUUID()
        val oppgave = OppgaverForOpprettelse(
            uuid,
            listOf(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)
        )
        oppgaverForOpprettelseService.opprettEllerErstattFremleggsoppgave(
            oppgave
        )
        assertThat(oppgaverForOpprettelseRepository.existsById(uuid))
    }

    @Test
    internal fun `update oppgave for opprettelse`() {
        val uuid = UUID.randomUUID()
        val oppgave = OppgaverForOpprettelse(
            uuid,
            listOf(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)
        )
        val oppdatertOppgave = OppgaverForOpprettelse(
            uuid,
            listOf()
        )
        oppgaverForOpprettelseService.opprettEllerErstattFremleggsoppgave(
            oppgave
        )
        oppgaverForOpprettelseService.opprettEllerErstattFremleggsoppgave(
            oppdatertOppgave
        )
        assertThat(oppgaverForOpprettelseService.hentOppgaverForOpprettelseEllerNull(uuid)).isEqualTo(oppdatertOppgave)
    }
}
