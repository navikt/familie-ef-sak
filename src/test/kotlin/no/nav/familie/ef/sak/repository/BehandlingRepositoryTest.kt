package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.FERDIGSTILT
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.UTREDES
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype.BARNETILSYN
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype.OVERGANGSSTØNAD
import no.nav.familie.ef.sak.felles.domain.Endret
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadOvergangsstønadRepository
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class BehandlingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository
    @Autowired private lateinit var søknadOvergangsstønadRepository: SøknadOvergangsstønadRepository
    @Autowired private lateinit var søknadService: SøknadService

    private val ident = "123"

    @Test
    internal fun `skal ikke være mulig å legge inn en behandling med referanse til en behandling som ikke eksisterer`() {
        val fagsak = fagsakRepository.insert(fagsak())
        assertThatThrownBy { behandlingRepository.insert(behandling(fagsak, forrigeBehandlingId = UUID.randomUUID())) }
                .isInstanceOf(DbActionExecutionException::class.java)
    }

    @Test
    internal fun findByFagsakId() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        assertThat(behandlingRepository.findByFagsakId(UUID.randomUUID())).isEmpty()
        assertThat(behandlingRepository.findByFagsakId(fagsak.id)).containsOnly(behandling)
    }

    @Test
    internal fun findByFagsakAndStatus() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.OPPRETTET))

        assertThat(behandlingRepository.findByFagsakIdAndStatus(UUID.randomUUID(), BehandlingStatus.OPPRETTET)).isEmpty()
        assertThat(behandlingRepository.findByFagsakIdAndStatus(fagsak.id, FERDIGSTILT)).isEmpty()
        assertThat(behandlingRepository.findByFagsakIdAndStatus(fagsak.id, BehandlingStatus.OPPRETTET)).containsOnly(behandling)
    }

    @Test
    internal fun finnMedEksternId() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val findByBehandlingId = behandlingRepository.findById(behandling.id)
        val findByEksternId = behandlingRepository.finnMedEksternId(behandling.eksternId.id)
                              ?: throw error("Behandling med id ${behandling.eksternId.id} finnes ikke")

        assertThat(findByEksternId).isEqualTo(behandling)
        assertThat(findByEksternId).isEqualTo(findByBehandlingId.get())
    }

    @Test
    internal fun `finnFnrForBehandlingId(sql) skal finne gjeldende fnr for behandlingsid`() {
        val fagsak = fagsakRepository.insert(fagsak(setOf(FagsakPerson(ident = "1"),
                                                          FagsakPerson(ident = "2",
                                                                       sporbar = Sporbar(endret = Endret(endretTid = LocalDateTime.now()
                                                                               .plusDays(2)))),
                                                          FagsakPerson(ident = "3"))))
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val fnr = behandlingRepository.finnAktivIdent(behandling.id)
        assertThat(fnr).isEqualTo("2")
    }

    @Test
    internal fun `finnMedEksternId skal gi null når det ikke finnes behandling for gitt id`() {
        val findByEksternId = behandlingRepository.finnMedEksternId(1000000L)
        assertThat(findByEksternId).isEqualTo(null)
    }

    @Test
    internal fun `finnSisteBehandlingSomIkkeErBlankett`() {
        val personidenter = setOf("1", "2")
        val fagsak = fagsakRepository.insert(fagsak(setOf(FagsakPerson("1"))))
        val behandling = behandlingRepository.insert(behandling(fagsak))

        assertThat(behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(OVERGANGSSTØNAD, personidenter))
                .isEqualTo(behandling)
        assertThat(behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(OVERGANGSSTØNAD, setOf("3"))).isNull()
        assertThat(behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(BARNETILSYN, personidenter)).isNull()
    }

    @Test
    internal fun `finnSisteBehandlingSomIkkeErBlankett - skal returnere teknisk opphør`() {
        val personidenter = setOf("1", "2")
        val fagsak = fagsakRepository.insert(fagsak(setOf(FagsakPerson("1"))))
        val behandling = behandlingRepository.insert(behandling(fagsak, type = BehandlingType.TEKNISK_OPPHØR))

        assertThat(behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(OVERGANGSSTØNAD, personidenter))
                .isEqualTo(behandling)
    }

    @Test
    internal fun `skal ikke returnere behandling hvis det er blankett`() {
        val personidenter = setOf("1", "2")
        val fagsak = fagsakRepository.insert(fagsak(setOf(FagsakPerson("1"))))
        behandlingRepository.insert(behandling(fagsak, type = BehandlingType.BLANKETT))

        assertThat(behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(OVERGANGSSTØNAD, personidenter)).isNull()
    }

    @Test
    internal fun `finnSisteIverksatteBehandling - skal returnere teknisk opphør hvis siste behandling er teknisk opphør`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(ident))))
        behandlingRepository.insert(behandling(fagsak,
                                               status = FERDIGSTILT,
                                               opprettetTid = LocalDateTime.now().minusDays(2)))
        val tekniskOpphørBehandling = behandlingRepository.insert(behandling(fagsak,
                                                                             status = FERDIGSTILT,
                                                                             type = BehandlingType.TEKNISK_OPPHØR,
                                                                             resultat = BehandlingResultat.OPPHØRT))
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id))
                .isEqualTo(tekniskOpphørBehandling)
    }

    @Test
    internal fun `finnSisteIverksatteBehandling - skal ikke returnere noe hvis behandlingen ikke er ferdigstilt`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(ident))))
        behandlingRepository.insert(behandling(fagsak,
                                               status = UTREDES,
                                               opprettetTid = LocalDateTime.now().minusDays(2)))
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)).isNull()
    }

    @Test
    internal fun `finnSisteIverksatteBehandling - skal ikke returnere noe hvis behandlingen er type blankett`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(ident))))
        behandlingRepository.insert(behandling(fagsak,
                                               status = FERDIGSTILT,
                                               type = BehandlingType.BLANKETT,
                                               opprettetTid = LocalDateTime.now().minusDays(2)))
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)).isNull()
    }

    @Test
    internal fun `finnSisteIverksatteBehandling skal finne id til siste ferdigstilte behandling, ikke henlagt eller blankett`() {
        val førstegangsbehandling = BehandlingOppsettUtil.iverksattFørstegangsbehandling
        val fagsak = fagsakRepository.insert(fagsak(setOf(FagsakPerson("1"))).copy(id = førstegangsbehandling.fagsakId))

        val behandlinger = BehandlingOppsettUtil.lagBehandlingerForSisteIverksatte()
        behandlingRepository.insertAll(behandlinger)

        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)?.id)
                .isEqualTo(førstegangsbehandling.id)
    }

    @Test
    internal fun `finnEksterneIder - skal hente eksterne ider`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val eksterneIder = behandlingRepository.finnEksterneIder(setOf(behandling.id))

        assertThat(fagsak.eksternId.id).isNotEqualTo(0L)
        assertThat(behandling.eksternId.id).isNotEqualTo(0L)

        assertThat(eksterneIder).hasSize(1)
        val first = eksterneIder.first()
        assertThat(first.behandlingId).isEqualTo(behandling.id)
        assertThat(first.eksternBehandlingId).isEqualTo(behandling.eksternId.id)
        assertThat(first.eksternFagsakId).isEqualTo(fagsak.eksternId.id)
    }

    @Test
    internal fun `skal finne behandlingsider til behandlinger som er iverksatte`() {
        val fagsak = fagsakRepository.insert(fagsak())
        behandlingRepository.insert(behandling(fagsak,
                                               status = FERDIGSTILT,
                                               resultat = BehandlingResultat.INNVILGET,
                                               opprettetTid = LocalDateTime.now().minusDays(2)))
        val behandling2 = behandlingRepository.insert(behandling(fagsak,
                                                                 status = FERDIGSTILT,
                                                                 resultat = BehandlingResultat.INNVILGET))
        assertThat(behandlingRepository.finnSisteIverksatteBehandlinger(OVERGANGSSTØNAD))
                .containsExactly(behandling2.id)
    }

    @Test
    internal fun `finnSisteIverksatteBehandlinger - skal ikke finne behandling hvis siste er avslått eller henlagt`() {
        val fagsak = fagsakRepository.insert(fagsak())
        behandlingRepository.insert(behandling(fagsak, status = FERDIGSTILT, resultat = BehandlingResultat.AVSLÅTT))
        behandlingRepository.insert(behandling(fagsak, status = FERDIGSTILT, resultat = BehandlingResultat.HENLAGT))
        assertThat(behandlingRepository.finnSisteIverksatteBehandlinger(OVERGANGSSTØNAD)).isEmpty()
    }

    @Test
    internal fun `finnSisteIverksatteBehandlinger - skal filtrere vekk blankett før den henter siste behandling`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                status = FERDIGSTILT,
                                                                resultat = BehandlingResultat.INNVILGET,
                                                                opprettetTid = LocalDateTime.now().minusDays(2)))
        behandlingRepository.insert(behandling(fagsak,
                                               type = BehandlingType.BLANKETT,
                                               status = FERDIGSTILT,
                                               resultat = BehandlingResultat.INNVILGET))
        assertThat(behandlingRepository.finnSisteIverksatteBehandlinger(OVERGANGSSTØNAD))
                .containsExactly(behandling.id)
    }

    @Test
    internal fun `finnSisteIverksatteBehandlinger - skal filtrere vekk henlagte-, avslåtte- eller blankettbehandlinger før den henter siste behandling`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                status = FERDIGSTILT,
                                                                resultat = BehandlingResultat.INNVILGET,
                                                                opprettetTid = LocalDateTime.now().minusDays(2)))
        behandlingRepository.insert(behandling(fagsak, type = BehandlingType.BLANKETT,
                                               status = FERDIGSTILT,
                                               resultat = BehandlingResultat.INNVILGET))
        behandlingRepository.insert(behandling(fagsak, status = FERDIGSTILT, resultat = BehandlingResultat.AVSLÅTT))
        behandlingRepository.insert(behandling(fagsak, status = FERDIGSTILT, resultat = BehandlingResultat.HENLAGT))
        assertThat(behandlingRepository.finnSisteIverksatteBehandlinger(OVERGANGSSTØNAD)).containsExactly(
                behandling.id)
    }
}