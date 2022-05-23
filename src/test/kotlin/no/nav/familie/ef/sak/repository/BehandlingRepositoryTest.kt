package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.FATTER_VEDTAK
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.FERDIGSTILT
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.OPPRETTET
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.UTREDES
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.domain.Endret
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil
import no.nav.familie.kontrakter.felles.ef.StønadType.BARNETILSYN
import no.nav.familie.kontrakter.felles.ef.StønadType.OVERGANGSSTØNAD
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import java.time.LocalDateTime
import java.util.UUID

internal class BehandlingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    private val ident = "123"

    @Test
    fun `skal ikke være mulig å legge inn en behandling med referanse til en behandling som ikke eksisterer`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        assertThatThrownBy { behandlingRepository.insert(behandling(fagsak, forrigeBehandlingId = UUID.randomUUID())) }
                .isInstanceOf(DbActionExecutionException::class.java)
    }

    @Test
    fun findByFagsakId() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        assertThat(behandlingRepository.findByFagsakId(UUID.randomUUID())).isEmpty()
        assertThat(behandlingRepository.findByFagsakId(fagsak.id)).containsOnly(behandling)
    }

    @Test
    fun findByFagsakAndStatus() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, status = OPPRETTET))

        assertThat(behandlingRepository.findByFagsakIdAndStatus(UUID.randomUUID(), OPPRETTET)).isEmpty()
        assertThat(behandlingRepository.findByFagsakIdAndStatus(fagsak.id, FERDIGSTILT)).isEmpty()
        assertThat(behandlingRepository.findByFagsakIdAndStatus(fagsak.id, OPPRETTET)).containsOnly(behandling)
    }

    @Test
    fun `finnBehandlingServiceObject returnerer korrekt konstruert BehandlingServiceObject`() {
        val fagsak = testoppsettService
                .lagreFagsak(fagsak(setOf(PersonIdent(ident = "1"),
                                          PersonIdent(ident = "2",
                                                      sporbar = Sporbar(endret = Endret(endretTid = LocalDateTime.now()
                                                              .plusDays(2)))),
                                          PersonIdent(ident = "3"))))
        val behandling = behandlingRepository.insert(behandling(fagsak, status = OPPRETTET))

        val behandlingServiceObject = behandlingRepository.finnSaksbehandling(behandling.id)

        assertThat(behandlingServiceObject.id).isEqualTo(behandling.id)
        assertThat(behandlingServiceObject.eksternId).isEqualTo(behandling.eksternId.id)
        assertThat(behandlingServiceObject.forrigeBehandlingId).isEqualTo(behandling.forrigeBehandlingId)
        assertThat(behandlingServiceObject.type).isEqualTo(behandling.type)
        assertThat(behandlingServiceObject.status).isEqualTo(behandling.status)
        assertThat(behandlingServiceObject.steg).isEqualTo(behandling.steg)
        assertThat(behandlingServiceObject.årsak).isEqualTo(behandling.årsak)
        assertThat(behandlingServiceObject.kravMottatt).isEqualTo(behandling.kravMottatt)
        assertThat(behandlingServiceObject.resultat).isEqualTo(behandling.resultat)
        assertThat(behandlingServiceObject.henlagtÅrsak).isEqualTo(behandling.henlagtÅrsak)
        assertThat(behandlingServiceObject.ident).isEqualTo("2")
        assertThat(behandlingServiceObject.fagsakId).isEqualTo(fagsak.id)
        assertThat(behandlingServiceObject.eksternFagsakId).isEqualTo(fagsak.eksternId.id)
        assertThat(behandlingServiceObject.stønadstype).isEqualTo(fagsak.stønadstype)
        assertThat(behandlingServiceObject.migrert).isEqualTo(fagsak.migrert)
        assertThat(behandlingServiceObject.opprettetAv).isEqualTo(behandling.sporbar.opprettetAv)
        assertThat(behandlingServiceObject.opprettetTid).isEqualTo(behandling.sporbar.opprettetTid)
        assertThat(behandlingServiceObject.endretTid).isEqualTo(behandling.sporbar.endret.endretTid)
    }

    @Test
    fun finnMedEksternId() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val findByBehandlingId = behandlingRepository.findById(behandling.id)
        val findByEksternId = behandlingRepository.finnMedEksternId(behandling.eksternId.id)

        assertThat(findByEksternId).isEqualTo(behandling)
        assertThat(findByEksternId).isEqualTo(findByBehandlingId.get())
    }

    @Test
    fun `finnFnrForBehandlingId(sql) skal finne gjeldende fnr for behandlingsid`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(setOf(
                PersonIdent(ident = "1"),
                PersonIdent(ident = "2",
                            sporbar = Sporbar(endret = Endret(endretTid = LocalDateTime.now().plusDays(2)))),
                PersonIdent(ident = "3"))))
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val fnr = behandlingRepository.finnAktivIdent(behandling.id)
        assertThat(fnr).isEqualTo("2")
    }

    @Test
    fun `finnMedEksternId skal gi null når det ikke finnes behandling for gitt id`() {
        val findByEksternId = behandlingRepository.finnMedEksternId(1000000L)
        assertThat(findByEksternId).isEqualTo(null)
    }

    @Test
    fun finnSisteBehandlingSomIkkeErBlankett() {
        val personidenter = setOf("1", "2")
        val fagsak = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1"))))
        val behandling = behandlingRepository.insert(behandling(fagsak))

        assertThat(behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(OVERGANGSSTØNAD, personidenter))
                .isEqualTo(behandling)
        assertThat(behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(OVERGANGSSTØNAD, setOf("3"))).isNull()
        assertThat(behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(BARNETILSYN, personidenter)).isNull()
    }

    @Test
    fun `finnSisteBehandlingSomIkkeErBlankett - skal returnere teknisk opphør`() {
        val personidenter = setOf("1", "2")
        val fagsak = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1"))))
        val behandling = behandlingRepository.insert(behandling(fagsak, type = BehandlingType.TEKNISK_OPPHØR))

        assertThat(behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(OVERGANGSSTØNAD, personidenter))
                .isEqualTo(behandling)
    }

    @Test
    fun `skal ikke returnere behandling hvis det er blankett`() {
        val personidenter = setOf("1", "2")
        val fagsak = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1"))))
        behandlingRepository.insert(behandling(fagsak, type = BehandlingType.BLANKETT))

        assertThat(behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(OVERGANGSSTØNAD, personidenter)).isNull()
    }

    @Test
    fun `finnSisteIverksatteBehandling - skal returnere teknisk opphør hvis siste behandling er teknisk opphør`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(ident))))
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
    fun `finnSisteIverksatteBehandling - skal ikke returnere noe hvis behandlingen ikke er ferdigstilt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(ident))))
        behandlingRepository.insert(behandling(fagsak,
                                               status = UTREDES,
                                               opprettetTid = LocalDateTime.now().minusDays(2)))
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)).isNull()
    }

    @Test
    fun `finnSisteIverksatteBehandling - skal ikke returnere noe hvis behandlingen er type blankett`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(ident))))
        behandlingRepository.insert(behandling(fagsak,
                                               status = FERDIGSTILT,
                                               type = BehandlingType.BLANKETT,
                                               opprettetTid = LocalDateTime.now().minusDays(2)))
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)).isNull()
    }

    @Test
    fun `finnSisteIverksatteBehandling skal finne id til siste ferdigstilte behandling, ikke henlagt eller blankett`() {
        val førstegangsbehandling = BehandlingOppsettUtil.iverksattFørstegangsbehandling
        val fagsak = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1"))).copy(id = førstegangsbehandling.fagsakId))

        val behandlinger = BehandlingOppsettUtil.lagBehandlingerForSisteIverksatte()
        behandlingRepository.insertAll(behandlinger)

        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)?.id)
                .isEqualTo(førstegangsbehandling.id)
    }

    @Test
    fun `finnEksterneIder - skal hente eksterne ider`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
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
    fun `finnEksterneIder - send inn én behandlingId som finnes, forvent én eksternId `() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val annenFagsak = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1"))))
        val annenBehandling = behandlingRepository.insert(behandling(annenFagsak))

        val eksterneIder = behandlingRepository.finnEksterneIder(setOf(annenBehandling.id))

        assertThat(fagsak.eksternId.id).isNotEqualTo(0L)
        assertThat(behandling.eksternId.id).isNotEqualTo(0L)

        assertThat(eksterneIder).hasSize(1)
        val first = eksterneIder.first()
        assertThat(first.behandlingId).isEqualTo(annenBehandling.id)
        assertThat(first.eksternBehandlingId).isEqualTo(annenBehandling.eksternId.id)
        assertThat(first.eksternFagsakId).isEqualTo(annenFagsak.eksternId.id)
    }

    @Test
    fun `finnEksterneIder - send inn behandlingIder som ikke finnes, forvent ingen treff `() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        behandlingRepository.insert(behandling(fagsak))
        val eksterneIder = behandlingRepository.finnEksterneIder(setOf(UUID.randomUUID(), UUID.randomUUID()))
        assertThat(eksterneIder.isEmpty())
    }

    @Test
    fun `finnEksterneIder - send inn tomt sett, forvent unntak `() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        behandlingRepository.insert(behandling(fagsak))
        assertThrows<Exception> {
            assertThat(behandlingRepository.finnEksterneIder(emptySet()))
        }
    }

    @Test
    fun `skal finne behandlingsider til behandlinger som er iverksatte`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
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

    @Nested
    inner class FinnSisteIverksatteBehandlinger {

        @Test
        fun `skal ikke finne behandling hvis siste er avslått eller henlagt`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            behandlingRepository.insert(behandling(fagsak, status = FERDIGSTILT, resultat = BehandlingResultat.AVSLÅTT))
            behandlingRepository.insert(behandling(fagsak, status = FERDIGSTILT, resultat = BehandlingResultat.HENLAGT))
            assertThat(behandlingRepository.finnSisteIverksatteBehandlinger(OVERGANGSSTØNAD)).isEmpty()
        }

        @Test
        fun `skal filtrere vekk blankett før den henter siste behandling`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
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
        fun `skal filtrere vekk henlagte-, avslåtte- eller blankettbehandlinger før den henter siste behandling`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
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
    @Nested
    inner class ExistsByFagsakIdAndTypeIn {

        @Test
        fun `existsByFagsakIdAndTypeIn - finner ikke når det ikke finnes noen behandlinger`() {
            assertThat(behandlingRepository.existsByFagsakIdAndTypeIn(UUID.randomUUID(),
                                                                      setOf(BehandlingType.REVURDERING))).isFalse
        }

        @Test
        fun `existsByFagsakIdAndTypeIn - finner ikke når det kun finnes av annen type`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            behandlingRepository.insert(behandling(fagsak,
                                                   status = FERDIGSTILT,
                                                   type = BehandlingType.BLANKETT))
            assertThat(behandlingRepository.existsByFagsakIdAndTypeIn(UUID.randomUUID(),
                                                                      setOf(BehandlingType.REVURDERING))).isFalse
        }

        @Test
        fun `existsByFagsakIdAndTypeIn - true når det av typen man spør etter`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            behandlingRepository.insert(behandling(fagsak,
                                                   status = FERDIGSTILT,
                                                   type = BehandlingType.REVURDERING))
            assertThat(behandlingRepository.existsByFagsakIdAndTypeIn(UUID.randomUUID(),
                                                                      setOf(BehandlingType.REVURDERING))).isFalse
        }

    }

    @Nested
    inner class Maks1UtredesPerFagsak {

        @Test
        fun `skal ikke kunne ha flere behandlinger på samma fagsak med annen status enn ferdigstilt`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            behandlingRepository.insert(behandling(fagsak, status = FERDIGSTILT))
            behandlingRepository.insert(behandling(fagsak, status = UTREDES))
            behandlingRepository.insert(behandling(fagsak, status = FERDIGSTILT))

            listOf(UTREDES, OPPRETTET, FATTER_VEDTAK).forEach {
                val cause = assertThatThrownBy {
                    behandlingRepository.insert(behandling(fagsak, status = it))
                }.cause
                cause.isInstanceOf(DuplicateKeyException::class.java)
                cause.hasMessageContaining("duplicate key value violates unique constraint \"behandlinger_i_arbeid\"")
            }
        }

        @Test
        fun `skal kunne ha flere behandlinger på ulike fagsak med status utredes`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("1")))
            val fagsak2 = testoppsettService.lagreFagsak(fagsak(identer = fagsakpersoner("2")))
            behandlingRepository.insert(behandling(fagsak, status = UTREDES))
            behandlingRepository.insert(behandling(fagsak2, status = UTREDES))
        }

    }

    @Nested
    inner class ExistsByFagsakIdAndStatusIsNot {

        @Test
        fun `returnerer true hvis behandling med annen status finnes og false om behandling med annen status ikke finnes`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("1")))
            behandlingRepository.insert(behandling(fagsak, status = UTREDES))

            val ikkeFerdigstiltFinnes = behandlingRepository.existsByFagsakIdAndStatusIsNot(fagsak.id, FERDIGSTILT)
            val ikkeUtredesFinnesIkke = behandlingRepository.existsByFagsakIdAndStatusIsNot(fagsak.id, UTREDES)

            assertThat(ikkeFerdigstiltFinnes).isTrue()
            assertThat(ikkeUtredesFinnesIkke).isFalse()
        }
    }

}