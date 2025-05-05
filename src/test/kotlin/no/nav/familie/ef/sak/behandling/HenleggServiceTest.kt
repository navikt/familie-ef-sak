package no.nav.familie.ef.sak.behandling

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.dto.HenlagtDto
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.ef.sak.behandling.henlegg.HenleggService
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.brev.BrevsignaturService
import no.nav.familie.ef.sak.brev.FamilieDokumentClient
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class HenleggServiceTest {
    private val behandlingService: BehandlingService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val brevClient: BrevClient = mockk()
    private val familieDokumentClient: FamilieDokumentClient = mockk()
    private val personopplysningerService: PersonopplysningerService = mockk()
    private val brevsignaturService: BrevsignaturService = mockk()
    private val henleggService = HenleggService(behandlingService, oppgaveService, brevClient, familieDokumentClient, personopplysningerService, brevsignaturService)

    @Test
    fun `skal henlegge behandling og ferdigstille tilhørende oppgave`() {
        val behandlingId = UUID.randomUUID()
        val henlagtDto = HenlagtDto(HenlagtÅrsak.FEILREGISTRERT)
        val behandling = behandling(id = behandlingId)
        every {
            behandlingService.henleggBehandling(behandlingId, henlagtDto)
        } returns behandling.copy(resultat = BehandlingResultat.HENLAGT)
        every { oppgaveService.ferdigstillOppgaveHvisOppgaveFinnes(any(), any(), any()) } just Runs

        val henalgtBehandling = henleggService.henleggBehandling(behandlingId, henlagtDto)

        assertThat(henalgtBehandling.resultat).isEqualTo(BehandlingResultat.HENLAGT)
        verify(exactly = 1) { oppgaveService.ferdigstillOppgaveHvisOppgaveFinnes(any(), Oppgavetype.BehandleSak, any()) }
        verify(exactly = 1) { oppgaveService.ferdigstillOppgaveHvisOppgaveFinnes(any(), Oppgavetype.BehandleUnderkjentVedtak, any()) }
    }

    @Test
    fun `skal henlegge behandling uten å ferdigstille tilhørende oppgave`() {
        val behandlingId = UUID.randomUUID()
        val henlagtDto = HenlagtDto(HenlagtÅrsak.FEILREGISTRERT)
        val behandling = behandling(id = behandlingId)
        every {
            behandlingService.henleggBehandling(behandlingId, henlagtDto, false)
        } returns behandling.copy(resultat = BehandlingResultat.HENLAGT)
        every { oppgaveService.settEfOppgaveTilFerdig(any(), any()) } returns oppgave(behandling)

        val henalgtBehandling = henleggService.henleggBehandlingUtenOppgave(behandlingId, henlagtDto)

        assertThat(henalgtBehandling.resultat).isEqualTo(BehandlingResultat.HENLAGT)
        verify(exactly = 1) { oppgaveService.settEfOppgaveTilFerdig(any(), Oppgavetype.BehandleSak) }
        verify(exactly = 1) { oppgaveService.settEfOppgaveTilFerdig(any(), Oppgavetype.BehandleUnderkjentVedtak) }
    }
}
