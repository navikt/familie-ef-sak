package no.nav.familie.ef.sak.ekstern

import io.mockk.every
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.ef.sak.oppgave.OppgaveClient
import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.oppgave
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettetÅrsak
import no.nav.familie.kontrakter.felles.klage.KanIkkeOppretteRevurderingÅrsak
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

internal class EksternBehandlingServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var eksternBehandlingService: EksternBehandlingService

    @Autowired
    lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    lateinit var oppgaveRepository: OppgaveRepository

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var oppgaveService: OppgaveService

    @Autowired
    lateinit var oppgaveClient: OppgaveClient

    @Nested
    inner class OpprettRevurderingKlage {

        @Test
        internal fun `kan opprette revurdering hvis det finnes en ferdigstilt behandling`() {
            val fagsakMedFerdigstiltBehandling = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("3")))
            val førstegangsbehandling = behandlingRepository.insert(
                behandling(
                    fagsakMedFerdigstiltBehandling,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                ),
            )
            vilkårsvurderingRepository.insert(vilkårsvurdering(førstegangsbehandling.id))

            val result = testWithBrukerContext {
                eksternBehandlingService.opprettRevurderingKlage(fagsakMedFerdigstiltBehandling.eksternId.id)
            }

            assertThat(result.opprettetBehandling).isTrue

            val behandling = behandlingRepository.finnMedEksternId(result.opprettet!!.eksternBehandlingId.toLong())

            assertThat(behandling!!.årsak).isEqualTo(BehandlingÅrsak.KLAGE)
        }

        @Test
        internal fun `mangler behandling skal feile`() {
            val fagsakUtenBehandling = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("1")))

            val result = eksternBehandlingService.opprettRevurderingKlage(fagsakUtenBehandling.eksternId.id)

            assertThat(result.opprettetBehandling).isFalse
            assertThat(result.ikkeOpprettet!!.årsak).isEqualTo(IkkeOpprettetÅrsak.INGEN_BEHANDLING)
        }

        @Test
        internal fun `henlagt behandling skal feile`() {
            val fagsakMedHenlagtBehandling = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("4")))
            behandlingRepository.insert(
                behandling(
                    fagsakMedHenlagtBehandling,
                    resultat = BehandlingResultat.HENLAGT,
                    status = BehandlingStatus.FERDIGSTILT,
                ),
            )

            val result = eksternBehandlingService.opprettRevurderingKlage(fagsakMedHenlagtBehandling.eksternId.id)

            assertThat(result.opprettetBehandling).isFalse
            assertThat(result.ikkeOpprettet!!.årsak).isEqualTo(IkkeOpprettetÅrsak.INGEN_BEHANDLING)
        }

        @Test
        internal fun `kan ikke opprette recvurdering hvis det finnes åpen behandling`() {
            val fagsakMedÅpenBehandling = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("2")))
            behandlingRepository.insert(behandling(fagsakMedÅpenBehandling))

            val result = eksternBehandlingService.opprettRevurderingKlage(fagsakMedÅpenBehandling.eksternId.id)

            assertThat(result.opprettetBehandling).isFalse
            assertThat(result.ikkeOpprettet!!.årsak).isEqualTo(IkkeOpprettetÅrsak.ÅPEN_BEHANDLING)
        }
    }

    @Nested
    inner class KanOppretteRevurdering {

        @Test
        internal fun `kan opprette revurdering hvis det finnes en ferdigstilt behandling`() {
            val fagsakMedFerdigstiltBehandling = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("3")))
            behandlingRepository.insert(
                behandling(
                    fagsakMedFerdigstiltBehandling,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                ),
            )
            val result = eksternBehandlingService.kanOppretteRevurdering(fagsakMedFerdigstiltBehandling.eksternId.id)

            assertThat(result.kanOpprettes).isTrue
            assertThat(result.årsak).isNull()
        }

        @Test
        internal fun `kan ikke opprette recvurdering hvis det finnes åpen behandling`() {
            val fagsakMedÅpenBehandling = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("2")))
            behandlingRepository.insert(behandling(fagsakMedÅpenBehandling))

            val result = eksternBehandlingService.kanOppretteRevurdering(fagsakMedÅpenBehandling.eksternId.id)

            assertThat(result.kanOpprettes).isFalse
            assertThat(result.årsak).isEqualTo(KanIkkeOppretteRevurderingÅrsak.ÅPEN_BEHANDLING)
        }

        @Test
        internal fun `kan ikke opprette revurdering hvis det ikke finnes noen behandlinger`() {
            val fagsakUtenBehandling = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("1")))

            val result = eksternBehandlingService.kanOppretteRevurdering(fagsakUtenBehandling.eksternId.id)

            assertThat(result.kanOpprettes).isFalse
            assertThat(result.årsak).isEqualTo(KanIkkeOppretteRevurderingÅrsak.INGEN_BEHANDLING)
        }
    }

    @Nested
    inner class BehandleSakOppgaveErPåbegynt {

        @Test
        internal fun `har ingen fagsak - skal returnere false`() {
            val result = testWithBrukerContext {
                eksternBehandlingService.tilhørendeBehandleSakOppgaveErPåbegynt(
                    personIdent = personIdent,
                    stønadType = StønadType.OVERGANGSSTØNAD,
                    innsendtSøknadTidspunkt = søknadInnsendtTidspunkt,
                )
            }
            assertThat(result).isFalse
        }

        @Test
        internal fun `har fagsak men ingen behandlinger - skal returnere false`() {
            testoppsettService.lagreFagsak(fagsak(fagsakpersoner(personIdent)))

            val result = testWithBrukerContext {
                eksternBehandlingService.tilhørendeBehandleSakOppgaveErPåbegynt(
                    personIdent = personIdent,
                    stønadType = StønadType.OVERGANGSSTØNAD,
                    innsendtSøknadTidspunkt = søknadInnsendtTidspunkt,
                )
            }
            assertThat(result).isFalse
        }

        @Test
        internal fun `har fagsak med behandling opprettet før innsendt søknad - skal returnere false`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(personIdent)))
            val behandling =
                behandlingRepository.insert(
                    behandling(
                        fagsak = fagsak,
                        opprettetTid = søknadInnsendtTidspunkt.minusDays(1L),
                    ),
                )

            insertOppgave(behandling, 91L, Oppgavetype.BehandleSak)
            mockOppgaveClient(91L, "saksbehandler")

            val result = testWithBrukerContext {
                eksternBehandlingService.tilhørendeBehandleSakOppgaveErPåbegynt(
                    personIdent = personIdent,
                    stønadType = StønadType.OVERGANGSSTØNAD,
                    innsendtSøknadTidspunkt = søknadInnsendtTidspunkt,
                )
            }

            assertThat(result).isFalse
        }

        @Test
        internal fun `har fagsak med flere behandlinger og oppgave uten tilordnet ressurs - skal returnere false`() {
            val fagsakMedBehandlinger = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(personIdent)))
            val førstegangsbehandling = behandlingRepository.insert(
                behandling(
                    fagsak = fagsakMedBehandlinger,
                    opprettetTid = søknadInnsendtTidspunkt.minusDays(1),
                    status = BehandlingStatus.FERDIGSTILT,
                    vedtakstidspunkt = søknadInnsendtTidspunkt.minusHours(1),
                    resultat = BehandlingResultat.INNVILGET,
                ),
            )
            val andregangsbehandling = behandlingRepository.insert(
                behandling(
                    fagsak = fagsakMedBehandlinger,
                    opprettetTid = søknadInnsendtTidspunkt.plusDays(1),
                ),
            )
            insertOppgave(førstegangsbehandling, 91L, Oppgavetype.Journalføring)
            insertOppgave(førstegangsbehandling, 92L, Oppgavetype.BehandleSak)
            insertOppgave(andregangsbehandling, 93L, Oppgavetype.BehandleSak)
            mockOppgaveClient(91L, "saksbehandler")
            mockOppgaveClient(92L, "saksbehandler")
            mockOppgaveClient(93L)

            val result = testWithBrukerContext {
                eksternBehandlingService.tilhørendeBehandleSakOppgaveErPåbegynt(
                    personIdent = personIdent,
                    stønadType = StønadType.OVERGANGSSTØNAD,
                    innsendtSøknadTidspunkt = søknadInnsendtTidspunkt,
                )
            }

            assertThat(result).isFalse
        }

        @Test
        internal fun `har fagsak med flere behandlinger og oppgave med tilordnet ressurs - skal returnere true`() {
            val fagsakMedBehandlinger = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(personIdent)))
            val førstegangsbehandling = behandlingRepository.insert(
                behandling(
                    fagsak = fagsakMedBehandlinger,
                    opprettetTid = søknadInnsendtTidspunkt.minusDays(1),
                    status = BehandlingStatus.FERDIGSTILT,
                    vedtakstidspunkt = søknadInnsendtTidspunkt.minusHours(1),
                    resultat = BehandlingResultat.INNVILGET,
                ),
            )
            val andregangsbehandling = behandlingRepository.insert(
                behandling(
                    fagsak = fagsakMedBehandlinger,
                    opprettetTid = søknadInnsendtTidspunkt.plusDays(1),
                ),
            )
            insertOppgave(førstegangsbehandling, 91L, Oppgavetype.Journalføring)
            insertOppgave(førstegangsbehandling, 92L, Oppgavetype.BehandleSak)
            insertOppgave(andregangsbehandling, 93L, Oppgavetype.BehandleSak)
            mockOppgaveClient(91L, "saksbehandler")
            mockOppgaveClient(92L, "saksbehandler")
            mockOppgaveClient(93L, "saksbehandler")

            val result = testWithBrukerContext {
                eksternBehandlingService.tilhørendeBehandleSakOppgaveErPåbegynt(
                    personIdent = personIdent,
                    stønadType = StønadType.OVERGANGSSTØNAD,
                    innsendtSøknadTidspunkt = søknadInnsendtTidspunkt,
                )
            }

            assertThat(result).isTrue
        }
    }

    private fun insertOppgave(behandling: Behandling, oppgaveId: Long, oppgaveType: Oppgavetype) {
        oppgaveRepository.insert(
            oppgave(
                behandling = behandling,
                gsakOppgaveId = oppgaveId,
                type = oppgaveType,
            ),
        )
    }

    private fun mockOppgaveClient(oppgaveId: Long, tilordnetRessurs: String? = null) {
        every { oppgaveClient.finnOppgaveMedId(oppgaveId) } returns Oppgave(
            id = oppgaveId,
            tilordnetRessurs = tilordnetRessurs,
        )
    }

    companion object {
        private const val personIdent = "12345678910"
        private val søknadInnsendtTidspunkt = LocalDateTime.of(2023, 6, 1, 0, 0)
    }
}
