package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.domain.Endret
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.iverksett.BehandlingKategori
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

internal class FagsakServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Nested
    inner class FagsakerMedOppdatertePersonIdenter {
        private val fagsakTilknyttetPesonIdent123 = fagsak(setOf(PersonIdent("123")))
        private val fagsakTilknyttetPesonIdent456 = fagsak(setOf(PersonIdent("456")))
        private val fagsakTilknyttetPesonIdent111 = fagsak(setOf(PersonIdent("111")))
        private val fagsakTilknyttetPesonIdent222 = fagsak(setOf(PersonIdent("222")))

        @BeforeEach
        fun setUp() {
            testoppsettService.lagreFagsak(fagsakTilknyttetPesonIdent123)
            testoppsettService.lagreFagsak(fagsakTilknyttetPesonIdent456)
            testoppsettService.lagreFagsak(fagsakTilknyttetPesonIdent111)
            testoppsettService.lagreFagsak(fagsakTilknyttetPesonIdent222)
        }

        @Test
        fun `skal returnere fagsaker med oppdatert peronIdent fra Pdl når det finnes ny ident`() {
            val fagsakerMedOppdatertePersonIdenter =
                fagsakService.fagsakerMedOppdatertePersonIdenter(
                    listOf(
                        fagsakTilknyttetPesonIdent123.id,
                        fagsakTilknyttetPesonIdent456.id,
                    ),
                )

            assertThat(
                fagsakerMedOppdatertePersonIdenter.first { it.id == fagsakTilknyttetPesonIdent123.id }
                    .hentAktivIdent(),
            ).isEqualTo("ny123")
            assertThat(
                fagsakerMedOppdatertePersonIdenter.first { it.id == fagsakTilknyttetPesonIdent456.id }
                    .hentAktivIdent(),
            ).isEqualTo("ny456")
        }

        @Test
        fun `skal returnere fagsaker med eksiterende peronIdent når det ikke finnes ny ident i pdl`() {
            val fagsakerMedOppdatertePersonIdenter =
                fagsakService.fagsakerMedOppdatertePersonIdenter(
                    listOf(
                        fagsakTilknyttetPesonIdent111.id,
                        fagsakTilknyttetPesonIdent222.id,
                    ),
                )

            assertThat(
                fagsakerMedOppdatertePersonIdenter.first { it.id == fagsakTilknyttetPesonIdent111.id }
                    .hentAktivIdent(),
            ).isEqualTo("111")
            assertThat(
                fagsakerMedOppdatertePersonIdenter.first { it.id == fagsakTilknyttetPesonIdent222.id }
                    .hentAktivIdent(),
            ).isEqualTo("222")
        }
    }

    @Nested
    inner class HentEllerOpprettFagsakMedBehandlinger {
        @Test
        internal fun `skal hente fagsak med tilhørende behandlinger som ikke finnes fra før`() {
            val personIdent = "23118612345"
            val fagsak = fagsakService.hentEllerOpprettFagsakMedBehandlinger(personIdent, StønadType.OVERGANGSSTØNAD)
            assertThat(fagsak.behandlinger.size).isEqualTo(0)
            assertThat(fagsak.stønadstype).isEqualTo(StønadType.OVERGANGSSTØNAD)
            assertThat(fagsak.personIdent).isEqualTo(personIdent)
        }

        @Test
        internal fun `skal hente fagsak med tilhørende behandlinger som finnes fra før`() {
            val personIdent = "23118612345"

            val fagsakRequest =
                fagsak(
                    stønadstype = StønadType.BARNETILSYN,
                    identer = setOf(PersonIdent(ident = personIdent)),
                )
            val fagsakDB = testoppsettService.lagreFagsak(fagsakRequest)

            val behandling1 =
                Behandling(
                    fagsakId = fagsakDB.id,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.FERDIGSTILT,
                    steg = StegType.BEHANDLING_FERDIGSTILT,
                    resultat = BehandlingResultat.INNVILGET,
                    årsak = BehandlingÅrsak.SØKNAD,
                    vedtakstidspunkt = SporbarUtils.now(),
                    kategori = BehandlingKategori.NASJONAL,
                )
            val behandling2 =
                Behandling(
                    fagsakId = fagsakDB.id,
                    type = BehandlingType.REVURDERING,
                    status = BehandlingStatus.UTREDES,
                    steg = StegType.VILKÅR,
                    resultat = BehandlingResultat.INNVILGET,
                    årsak = BehandlingÅrsak.SØKNAD,
                    vedtakstidspunkt = SporbarUtils.now(),
                    kategori = BehandlingKategori.NASJONAL,
                )

            behandlingRepository.insert(behandling1)
            behandlingRepository.insert(behandling2)

            val fagsak = fagsakService.hentEllerOpprettFagsakMedBehandlinger(personIdent, StønadType.BARNETILSYN)
            assertThat(fagsak.behandlinger.size).isEqualTo(2)
            assertThat(fagsak.stønadstype).isEqualTo(fagsakRequest.stønadstype)
            assertThat(fagsak.personIdent).isEqualTo(personIdent)

            val førstegangsbehandling = fagsak.behandlinger.single { it.type == BehandlingType.FØRSTEGANGSBEHANDLING }
            assertThat(førstegangsbehandling.status).isEqualTo(behandling1.status)
            assertThat(førstegangsbehandling.type).isEqualTo(behandling1.type)

            val revurdering = fagsak.behandlinger.single { it.type == BehandlingType.REVURDERING }
            assertThat(revurdering.status).isEqualTo(behandling2.status)
            assertThat(revurdering.type).isEqualTo(behandling2.type)
        }
    }

    @Test
    internal fun `finnAktiveIdenter - skal kaste feil hvis den ikke finner ident til fagsak`() {
        assertThrows<Feil> { fagsakService.hentAktiveIdenter(setOf(UUID.randomUUID())) }
    }

    @Nested
    inner class HentEllerOpprettFagsak {
        @Test
        internal fun `skal oppdatere personident på fagsak dersom personen har fått ny ident som er tidligere registrert`() {
            val iGår =
                Sporbar(
                    opprettetAv = "XY",
                    opprettetTid = LocalDateTime.now().minusDays(1),
                    endret = Endret(endretAv = "XY", endretTid = LocalDateTime.now().minusDays(1)),
                )
            val iDag =
                Sporbar(
                    opprettetAv = "XY",
                    opprettetTid = LocalDateTime.now().minusHours(1),
                    endret = Endret(endretAv = "XY", endretTid = LocalDateTime.now().minusHours(1)),
                )

            val gjeldendeIdent = "12345678901"
            val feilRegistrertIdent = "99988877712"
            val fagsakMedFeilregistrertIdent =
                testoppsettService.lagreFagsak(
                    fagsak(
                        eksternId = 1234,
                        stønadstype = StønadType.OVERGANGSSTØNAD,
                        identer =
                            setOf(
                                PersonIdent(
                                    ident = gjeldendeIdent,
                                    sporbar = iGår,
                                ),
                                PersonIdent(
                                    ident = feilRegistrertIdent,
                                    sporbar = iDag,
                                ),
                            ),
                    ),
                )

            assertThat(fagsakMedFeilregistrertIdent.hentAktivIdent()).isEqualTo(feilRegistrertIdent)

            val oppdatertFagsak = fagsakService.hentEllerOpprettFagsak(gjeldendeIdent, StønadType.OVERGANGSSTØNAD)
            assertThat(oppdatertFagsak.personIdenter.map { it.ident }).containsExactlyInAnyOrder(
                gjeldendeIdent,
                feilRegistrertIdent,
            )
            assertThat(oppdatertFagsak.hentAktivIdent()).isEqualTo(gjeldendeIdent)
            val fagsakEtterOppdatering = fagsakService.hentFagsak(fagsakMedFeilregistrertIdent.id).hentAktivIdent()
            assertThat(fagsakEtterOppdatering).isEqualTo(gjeldendeIdent)
        }

        @Test
        internal fun `skal oppdatere personident hvis fagsak har gammel ident`() {
            val iGår =
                Sporbar(
                    opprettetAv = "XY",
                    opprettetTid = LocalDateTime.now().minusDays(1),
                    endret = Endret(endretAv = "XY", endretTid = LocalDateTime.now().minusDays(1)),
                )

            val gjeldendeIdent = "12345678901"
            val historiskIdent = "98765432109"
            val fagsakMedHistoriskIdent =
                testoppsettService.lagreFagsak(
                    fagsak(
                        eksternId = 1234,
                        stønadstype = StønadType.OVERGANGSSTØNAD,
                        identer =
                            setOf(
                                PersonIdent(
                                    ident = historiskIdent,
                                    sporbar = iGår,
                                ),
                            ),
                    ),
                )

            assertThat(fagsakMedHistoriskIdent.hentAktivIdent()).isEqualTo(historiskIdent)

            val oppdatertFagsak = fagsakService.hentEllerOpprettFagsak(gjeldendeIdent, StønadType.OVERGANGSSTØNAD)
            assertThat(oppdatertFagsak.personIdenter.map { it.ident }).contains(historiskIdent)
            assertThat(oppdatertFagsak.hentAktivIdent()).isEqualTo(gjeldendeIdent)

            val fagsakEtterOppdatering = fagsakService.hentFagsak(fagsakMedHistoriskIdent.id)
            assertThat(fagsakEtterOppdatering.hentAktivIdent()).isEqualTo(gjeldendeIdent)
        }
    }
}
