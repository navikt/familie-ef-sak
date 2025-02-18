package no.nav.familie.ef.sak.behandling.revurdering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AutomatiskRevurderingServiceTest {
    val behandlingServiceMock = mockk<BehandlingService>(relaxed = true)
    val oppgaveServiceMock = mockk<OppgaveService>(relaxed = true)
    val automatiskRevurderingService = AutomatiskRevurderingService(mockk(relaxed = true), mockk(relaxed = true), behandlingServiceMock, oppgaveServiceMock)

    @Test
    fun `Kan automatisk revurderes`() {
        assertThat(automatiskRevurderingService.kanAutomatiskRevurderes("1")).isTrue()
    }

    @Test
    fun `Kan ikke automatisk revurderes som følge av åpen behandling`() {
        every { behandlingServiceMock.finnesÅpenBehandling(any()) } returns true

        assertThat(automatiskRevurderingService.kanAutomatiskRevurderes("1")).isFalse()
    }

    @Test
    fun `Kan ikke automatisk revurderes som følge av at behandle sak oppgave finnes`() {
        every { oppgaveServiceMock.hentOppgaver(any()) } returns
            FinnOppgaveResponseDto(
                1,
                listOf(
                    Oppgave(
                        id = 1,
                        aktoerId = "1",
                        identer = listOf(OppgaveIdentV2("11111111111", IdentGruppe.FOLKEREGISTERIDENT)),
                        tema = Tema.ENF,
                        oppgavetype = Oppgavetype.BehandleSak.toString(),
                        status = StatusEnum.AAPNET,
                        versjon = 2,
                    ),
                ),
            )

        assertThat(automatiskRevurderingService.kanAutomatiskRevurderes("1")).isFalse()
    }
}
