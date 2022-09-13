package no.nav.familie.ef.sak.klage

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.EksternBehandlingId
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.EksternFagsakId
import no.nav.familie.ef.sak.klage.dto.OpprettKlageDto
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ytelsestype
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class KlageServiceTest {

    private val behandlingService = mockk<BehandlingService>()

    private val fagsakService = mockk<FagsakService>()

    private val klageClient = mockk<KlageClient>()

    private val service = KlageService(behandlingService, fagsakService, klageClient)

    private val eksternFagsakId = 11L
    private val eksternBehandlingId = 22L
    private val personIdent = "100"
    private val saksbehandling = saksbehandling(
        fagsak(eksternId = EksternFagsakId(eksternFagsakId)),
        behandling(eksternId = EksternBehandlingId(eksternBehandlingId))
    )

    private val opprettKlageSlot = slot<OpprettKlagebehandlingRequest>()

    @BeforeEach
    internal fun setUp() {
        opprettKlageSlot.clear()
        every { behandlingService.hentSaksbehandling(any<UUID>()) } returns saksbehandling
        every { fagsakService.hentAktivIdent(saksbehandling.fagsakId) } returns personIdent
        justRun { klageClient.opprettKlage(capture(opprettKlageSlot)) }
    }

    @Nested
    inner class OpprettKlage {

        @Test
        internal fun `skal mappe riktige verdier`() {
            service.opprettKlage(UUID.randomUUID(), OpprettKlageDto(LocalDate.now()))

            val request = opprettKlageSlot.captured
            assertThat(request.ident).isEqualTo(personIdent)
            assertThat(request.eksternFagsakId).isEqualTo(eksternFagsakId.toString())
            assertThat(request.eksternBehandlingId).isEqualTo(eksternBehandlingId.toString())
            assertThat(request.fagsystem).isEqualTo(Fagsystem.EF)
            assertThat(request.ytelsestype).isEqualTo(Ytelsestype.OVERGANGSSTØNAD)
            assertThat(request.klageMottatt).isEqualTo(LocalDate.now())
        }
    }
}