package no.nav.familie.ef.sak.klage

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.arbeidsfordeling.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.EksternBehandlingId
import no.nav.familie.ef.sak.brev.BrevsignaturService.Companion.ENHET_NAY
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.EksternFagsakId
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.klage.dto.OpprettKlageDto
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakPerson
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResultat
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class KlageServiceTest {

    private val behandlingService = mockk<BehandlingService>()

    private val fagsakService = mockk<FagsakService>()

    private val fagsakPersonService = mockk<FagsakPersonService>()

    private val klageClient = mockk<KlageClient>()

    private val infotrygdService = mockk<InfotrygdService>()

    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()

    private val service =
        KlageService(
            behandlingService,
            fagsakService,
            fagsakPersonService,
            klageClient,
            infotrygdService,
            arbeidsfordelingService
        )

    private val eksternFagsakId = 11L
    private val eksternBehandlingId = 22L
    private val personIdent = "100"
    private val fagsakPerson = fagsakPerson()
    private val saksbehandling = saksbehandling(
        fagsak(eksternId = EksternFagsakId(eksternFagsakId), person = fagsakPerson),
        behandling(eksternId = EksternBehandlingId(eksternBehandlingId))
    )

    private val opprettKlageSlot = slot<OpprettKlagebehandlingRequest>()

    @BeforeEach
    internal fun setUp() {
        opprettKlageSlot.clear()
        every { behandlingService.hentSaksbehandling(any<UUID>()) } returns saksbehandling
        every { fagsakService.hentAktivIdent(saksbehandling.fagsakId) } returns personIdent
        every { fagsakPersonService.hentPerson(any()) } returns fagsakPerson
        every { arbeidsfordelingService.hentNavEnhet(any()) } returns Arbeidsfordelingsenhet(ENHET_NAY, "enhet")
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
            assertThat(request.stønadstype).isEqualTo(Stønadstype.OVERGANGSSTØNAD)
            assertThat(request.klageMottatt).isEqualTo(LocalDate.now())
            assertThat(request.behandlendeEnhet).isEqualTo(ENHET_NAY)
        }
    }

    @Nested
    inner class HarÅpenKlage {

        @Test
        internal fun `har ikke noen åpen sak`() {
            every { infotrygdService.hentÅpneKlagesaker(fagsakPerson.hentAktivIdent()) } returns listOf()

            val harÅpenKlage = service.harÅpenKlage(fagsakPerson.id)

            assertThat(harÅpenKlage.infotrygd.overgangsstønad).isFalse
            assertThat(harÅpenKlage.infotrygd.barnetilsyn).isFalse
            assertThat(harÅpenKlage.infotrygd.skolepenger).isFalse
        }

        @Test
        internal fun `har åpen overgangsstønad`() {
            every { infotrygdService.hentÅpneKlagesaker(fagsakPerson.hentAktivIdent()) } returns listOf(åpenSak())

            val harÅpenKlage = service.harÅpenKlage(fagsakPerson.id)

            assertThat(harÅpenKlage.infotrygd.overgangsstønad).isTrue
            assertThat(harÅpenKlage.infotrygd.barnetilsyn).isFalse
            assertThat(harÅpenKlage.infotrygd.skolepenger).isFalse
        }

        @Test
        internal fun `har åpen barnetilsyn`() {
            every { infotrygdService.hentÅpneKlagesaker(fagsakPerson.hentAktivIdent()) } returns
                listOf(åpenSak(StønadType.BARNETILSYN))

            val harÅpenKlage = service.harÅpenKlage(fagsakPerson.id)

            assertThat(harÅpenKlage.infotrygd.overgangsstønad).isFalse
            assertThat(harÅpenKlage.infotrygd.barnetilsyn).isTrue
            assertThat(harÅpenKlage.infotrygd.skolepenger).isFalse
        }

        @Test
        internal fun `har åpen skolepenger`() {
            every { infotrygdService.hentÅpneKlagesaker(fagsakPerson.hentAktivIdent()) } returns
                listOf(åpenSak(StønadType.SKOLEPENGER))

            val harÅpenKlage = service.harÅpenKlage(fagsakPerson.id)

            assertThat(harÅpenKlage.infotrygd.overgangsstønad).isFalse
            assertThat(harÅpenKlage.infotrygd.barnetilsyn).isFalse
            assertThat(harÅpenKlage.infotrygd.skolepenger).isTrue
        }

        private fun åpenSak(stønadstype: StønadType = StønadType.OVERGANGSSTØNAD) = InfotrygdSak(
            "1",
            stønadType = stønadstype,
            resultat = InfotrygdSakResultat.ÅPEN_SAK
        )
    }
}
