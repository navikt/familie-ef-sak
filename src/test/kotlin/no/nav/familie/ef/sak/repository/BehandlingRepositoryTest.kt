package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.HENLAGT
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.IKKE_SATT
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.INNVILGET
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.FATTER_VEDTAK
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.FERDIGSTILT
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.IVERKSETTER_VEDTAK
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.OPPRETTET
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.SATT_PÅ_VENT
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.UTREDES
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.ef.sak.fagsak.FagsakPersonRepository
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.domain.Endret
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.testutil.hasCauseMessageContaining
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.ef.StønadType.BARNETILSYN
import no.nav.familie.kontrakter.felles.ef.StønadType.OVERGANGSSTØNAD
import no.nav.familie.kontrakter.felles.ef.StønadType.SKOLEPENGER
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

internal class BehandlingRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Autowired
    private lateinit var oppgaveRepository: OppgaveRepository

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    private val ident = "123"

    @Test
    fun `skal finne alle personer med aktiv stønad som ikke er manuelt revurdert siste måneder`() {
        lagrePersonMedVedtak("1", 12)
        lagrePersonMedVedtak("2", 3)
        lagrePersonMedVedtak("3", 3, behandlingÅrsak = BehandlingÅrsak.G_OMREGNING) // Skal ikke ta hensyn til forrige revurderingsdato ved g-omregning
        lagrePersonMedVedtak("4", 3, YearMonth.now().minusMonths(1)) // Gammelt vedtak

        val resultat = behandlingRepository.finnPersonerMedAktivStonadIkkeRevurdertSisteMåneder(antallMåneder = 4)
        assertThat(resultat.size).isEqualTo(2)
        assertThat(resultat).containsAll(listOf("1", "3"))
    }

    @Test
    fun `skal finne alle personer med aktiv stønad som ikke er manuelt revurdert siste måneder - filtrer ut person med åpen behandling`() {
        val fagsak = lagrePersonMedVedtak("1", 12)
        val åpenBehandling =
            behandling(
                fagsak,
                UTREDES,
            )
        behandlingRepository.insert(åpenBehandling)
        lagrePersonMedVedtak("2", 12)
        val resultat = behandlingRepository.finnPersonerMedAktivStonadIkkeRevurdertSisteMåneder(antallMåneder = 4)
        assertThat(resultat.size).isEqualTo(1)
        assertThat(resultat).containsAll(listOf("2"))
    }

    @Test
    fun `skal finne ferdigstilte behandlinger med utdatert G-beløp som må behandles manuelt`() {
        lagrePersonMedVedtak("2", 3)
        val resultat = behandlingRepository.finnFerdigstilteBehandlingerMedUtdatertGBelopSomMåBehandlesManuelt(Grunnbeløpsperioder.nyesteGrunnbeløp.periode.fomDato)

        assertThat(resultat.size).isEqualTo(1)
    }

    private fun lagrePersonMedVedtak(
        personIdent: String,
        antallMånederSidenForrigeRevurdering: Long,
        stønadTom: YearMonth = YearMonth.now().plusMonths(2),
        behandlingÅrsak: BehandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
    ): Fagsak {
        val stønadsperiode = Månedsperiode(YearMonth.now().minusMonths(antallMånederSidenForrigeRevurdering), stønadTom)
        val person1 = fagsakPerson(identer = setOf(PersonIdent(personIdent)))
        fagsakPersonRepository.insert(person1)
        val fagsak = testoppsettService.lagreFagsak(fagsak(person = person1))
        val behandling =
            behandling(
                fagsak,
                resultat = INNVILGET,
                vedtakstidspunkt = stønadsperiode.fomDato.atStartOfDay(),
                årsak = behandlingÅrsak,
                status = FERDIGSTILT,
            )
        val vedtaksperiodeList =
            listOf(
                vedtaksperiode(
                    startDato = stønadsperiode.fomDato,
                    sluttDato = stønadsperiode.tomDato,
                    vedtaksperiodeType = VedtaksperiodeType.HOVEDPERIODE,
                ),
            )
        behandlingRepository.insert(behandling)
        val vedtak = vedtak(behandlingId = behandling.id, perioder = PeriodeWrapper(vedtaksperiodeList))
        vedtakRepository.insert(vedtak)
        val aty =
            lagAndelTilkjentYtelse(
                10000,
                stønadsperiode.fomDato,
                stønadsperiode.tomDato,
                kildeBehandlingId = behandling.id,
            )
        val ty = lagTilkjentYtelse(listOf(aty), behandlingId = behandling.id, grunnbeløpsmåned = Grunnbeløpsperioder.forrigeGrunnbeløp.periode.fom)
        tilkjentYtelseRepository.insert(ty)
        return fagsak
    }

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
    fun `hentUferdigeBehandlingerFørDato skal bare hente behandlinger før en gitt dato`() {
        val enMånedSiden = LocalDateTime.now().minusMonths(1)

        val fagsak = testoppsettService.lagreFagsak(fagsak(stønadstype = OVERGANGSSTØNAD))
        val behandling = behandling(fagsak, opprettetTid = LocalDateTime.now().minusMonths(2))
        behandlingRepository.insert(behandling)
        val annenFagsak =
            testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1")), stønadstype = OVERGANGSSTØNAD))
        behandlingRepository.insert(behandling(annenFagsak, opprettetTid = LocalDateTime.now().minusWeeks(1)))
        val sporbar = Sporbar("saksbh", enMånedSiden.minusDays(1))
        val oppgave = Oppgave(sporbar = sporbar, behandlingId = behandling.id, gsakOppgaveId = 1, type = Oppgavetype.BehandleSak, erFerdigstilt = false)
        oppgaveRepository.insert(oppgave)
        assertThat(
            behandlingRepository.hentUferdigeBehandlingerOpprettetFørDato(
                OVERGANGSSTØNAD,
                enMånedSiden,
            ),
        ).size()
            .isEqualTo(1)
    }

    @Test
    fun `hentUferdigeBehandlingerFørDato skal ikke hente behandling dersom oppgave er endret etter frist`() {
        val enMånedSiden = LocalDateTime.now().minusMonths(1)

        val fagsak = testoppsettService.lagreFagsak(fagsak(stønadstype = OVERGANGSSTØNAD))
        val behandling = behandling(fagsak, opprettetTid = LocalDateTime.now().minusMonths(2))
        behandlingRepository.insert(behandling)
        val annenFagsak =
            testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1")), stønadstype = OVERGANGSSTØNAD))
        behandlingRepository.insert(behandling(annenFagsak, opprettetTid = LocalDateTime.now().minusWeeks(1)))
        val sporbar = Sporbar("saksbh", enMånedSiden.plusDays(1))
        val oppgave = Oppgave(sporbar = sporbar, behandlingId = behandling.id, gsakOppgaveId = 1, type = Oppgavetype.BehandleSak, erFerdigstilt = false)
        oppgaveRepository.insert(oppgave)
        assertThat(
            behandlingRepository.hentUferdigeBehandlingerOpprettetFørDato(
                OVERGANGSSTØNAD,
                enMånedSiden,
            ),
        ).size()
            .isEqualTo(0)
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
        val fagsak =
            testoppsettService
                .lagreFagsak(
                    fagsak(
                        setOf(
                            PersonIdent(ident = "1"),
                            PersonIdent(
                                ident = "2",
                                sporbar =
                                    Sporbar(
                                        endret =
                                            Endret(
                                                endretTid =
                                                    LocalDateTime
                                                        .now()
                                                        .plusDays(2),
                                            ),
                                    ),
                            ),
                            PersonIdent(ident = "3"),
                        ),
                    ),
                )
        val behandling = behandlingRepository.insert(behandling(fagsak, status = OPPRETTET, resultat = INNVILGET))

        val behandlingServiceObject = behandlingRepository.finnSaksbehandling(behandling.id)

        assertThat(behandlingServiceObject.id).isEqualTo(behandling.id)
        assertThat(behandlingServiceObject.eksternId).isEqualTo(behandling.eksternId)
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
        assertThat(behandlingServiceObject.eksternFagsakId).isEqualTo(fagsak.eksternId)
        assertThat(behandlingServiceObject.stønadstype).isEqualTo(fagsak.stønadstype)
        assertThat(behandlingServiceObject.migrert).isEqualTo(fagsak.migrert)
        assertThat(behandlingServiceObject.opprettetAv).isEqualTo(behandling.sporbar.opprettetAv)
        assertThat(behandlingServiceObject.opprettetTid).isEqualTo(behandling.sporbar.opprettetTid)
        assertThat(behandlingServiceObject.endretTid).isEqualTo(behandling.sporbar.endret.endretTid)
        assertThat(behandlingServiceObject.vedtakstidspunkt).isEqualTo(behandling.vedtakstidspunkt)
    }

    @Test
    fun finnMedEksternId() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val findByBehandlingId = behandlingRepository.findById(behandling.id)
        val findByEksternId = behandlingRepository.finnMedEksternId(behandling.eksternId)

        assertThat(findByEksternId).isEqualTo(behandling)
        assertThat(findByEksternId).isEqualTo(findByBehandlingId.get())
    }

    @Test
    fun `finnFnrForBehandlingId(sql) skal finne gjeldende fnr for behandlingsid`() {
        val fagsak =
            testoppsettService.lagreFagsak(
                fagsak(
                    setOf(
                        PersonIdent(ident = "1"),
                        PersonIdent(
                            ident = "2",
                            sporbar = Sporbar(endret = Endret(endretTid = LocalDateTime.now().plusDays(2))),
                        ),
                        PersonIdent(ident = "3"),
                    ),
                ),
            )
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
    fun `finnSisteIverksatteBehandling - skal ikke returnere noe hvis behandlingen ikke er ferdigstilt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(ident))))
        behandlingRepository.insert(
            behandling(
                fagsak,
                status = UTREDES,
                opprettetTid = LocalDateTime.now().minusDays(2),
            ),
        )
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)).isNull()
    }

    @Test
    fun `finnSisteIverksatteBehandling skal finne id til siste ferdigstilte behandling`() {
        val førstegangsbehandling = BehandlingOppsettUtil.iverksattFørstegangsbehandling
        val fagsak =
            testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1"))).copy(id = førstegangsbehandling.fagsakId))

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

        assertThat(fagsak.eksternId).isNotEqualTo(0L)
        assertThat(behandling.eksternId).isNotEqualTo(0L)

        assertThat(eksterneIder).hasSize(1)
        val first = eksterneIder.first()
        assertThat(first.behandlingId).isEqualTo(behandling.id)
        assertThat(first.eksternBehandlingId).isEqualTo(behandling.eksternId)
        assertThat(first.eksternFagsakId).isEqualTo(fagsak.eksternId)
    }

    @Test
    fun `finnEksterneIder - send inn én behandlingId som finnes, forvent én eksternId `() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val annenFagsak = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1"))))
        val annenBehandling = behandlingRepository.insert(behandling(annenFagsak))

        val eksterneIder = behandlingRepository.finnEksterneIder(setOf(annenBehandling.id))

        assertThat(fagsak.eksternId).isNotEqualTo(0L)
        assertThat(behandling.eksternId).isNotEqualTo(0L)

        assertThat(eksterneIder).hasSize(1)
        val first = eksterneIder.first()
        assertThat(first.behandlingId).isEqualTo(annenBehandling.id)
        assertThat(first.eksternBehandlingId).isEqualTo(annenBehandling.eksternId)
        assertThat(first.eksternFagsakId).isEqualTo(annenFagsak.eksternId)
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

    @Nested
    inner class ExistsByFagsak {
        @Test
        fun `inner ikke når det ikke finnes noen behandlinger`() {
            assertThat(behandlingRepository.existsByFagsakId(UUID.randomUUID())).isFalse
        }

        @Test
        fun `finner ikke når det kun finnes av annen type`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            behandlingRepository.insert(
                behandling(
                    fagsak,
                    status = FERDIGSTILT,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                ),
            )
            assertThat(behandlingRepository.existsByFagsakId(UUID.randomUUID())).isFalse
        }

        @Test
        fun `true når det av typen man spør etter`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            behandlingRepository.insert(
                behandling(
                    fagsak,
                    status = FERDIGSTILT,
                    type = BehandlingType.REVURDERING,
                ),
            )
            assertThat(behandlingRepository.existsByFagsakId(UUID.randomUUID())).isFalse
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

            listOf(UTREDES, OPPRETTET, FATTER_VEDTAK, IVERKSETTER_VEDTAK).forEach { status ->
                val cause =
                    assertThatThrownBy {
                        behandlingRepository.insert(behandling(fagsak, status = status))
                    }.cause
                cause.isInstanceOf(DuplicateKeyException::class.java)
                cause.hasMessageContaining("duplicate key value violates unique constraint \"idx_behandlinger_i_arbeid\"")
            }
        }

        @Test
        fun `skal kunne ha en behandling som utredes når det finnes en behandling satt på vent`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            behandlingRepository.insert(behandling(fagsak, status = FERDIGSTILT))
            behandlingRepository.insert(behandling(fagsak, status = SATT_PÅ_VENT))
            behandlingRepository.insert(behandling(fagsak, status = SATT_PÅ_VENT))

            behandlingRepository.insert(behandling(fagsak, status = UTREDES))
        }

        @Test
        fun `kan ikke endre en behandling fra satt på vent til utredes når det allerede finnes en behandling som ikke er ferdigstilt`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            behandlingRepository.insert(behandling(fagsak, status = FERDIGSTILT))
            val påVent = behandlingRepository.insert(behandling(fagsak, status = SATT_PÅ_VENT))
            behandlingRepository.insert(behandling(fagsak, status = IVERKSETTER_VEDTAK))

            val cause =
                assertThatThrownBy {
                    behandlingRepository.update(påVent.copy(status = UTREDES))
                }.cause
            cause.isInstanceOf(DuplicateKeyException::class.java)
            cause.hasMessageContaining("duplicate key value violates unique constraint \"idx_behandlinger_i_arbeid\"")
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

    @Test
    internal fun `skal finne aktuell behandling for gjenbruk av inngangsvilkår`() {
        val fagsakPersonId = UUID.randomUUID()

        val fagsakOS = lagreFagsak(UUID.randomUUID(), OVERGANGSSTØNAD, fagsakPersonId)
        val førstegangsbehandlingOS = lagreBehandling(UUID.randomUUID(), FERDIGSTILT, INNVILGET, fagsakOS)
        lagreBehandling(UUID.randomUUID(), OPPRETTET, IKKE_SATT, fagsakOS)

        val fagsakBT = lagreFagsak(UUID.randomUUID(), BARNETILSYN, fagsakPersonId)
        val førstegangsbehandlingBT = lagreBehandling(UUID.randomUUID(), UTREDES, IKKE_SATT, fagsakBT)

        val behandlingerForGjenbruk: List<Behandling> =
            behandlingRepository.finnBehandlingerForGjenbrukAvVilkårOgSamværsavtaler(fagsakBT.fagsakPersonId)

        assertThat(behandlingerForGjenbruk).containsExactlyInAnyOrder(førstegangsbehandlingOS, førstegangsbehandlingBT)
    }

    @Test
    internal fun `skal finne alle aktuelle behandlinger for gjenbruk av inngangsvilkår`() {
        val fagsakPersonId = UUID.randomUUID()

        val fagsakOS = lagreFagsak(UUID.randomUUID(), OVERGANGSSTØNAD, fagsakPersonId)
        val førstegangsbehandlingOS = lagreBehandling(UUID.randomUUID(), FERDIGSTILT, INNVILGET, fagsakOS)
        val annengangsbehandlingOS = lagreBehandling(UUID.randomUUID(), FERDIGSTILT, INNVILGET, fagsakOS)
        lagreBehandling(UUID.randomUUID(), FERDIGSTILT, HENLAGT, fagsakOS)

        val fagsakBT = lagreFagsak(UUID.randomUUID(), BARNETILSYN, fagsakPersonId)
        val førstegangsbehandlingBT = lagreBehandling(UUID.randomUUID(), FERDIGSTILT, INNVILGET, fagsakBT)
        val revurderingUnderArbeidBT = lagreBehandling(UUID.randomUUID(), UTREDES, IKKE_SATT, fagsakBT)

        val fagsakSP = lagreFagsak(UUID.randomUUID(), SKOLEPENGER, fagsakPersonId)
        val revurderingUnderArbeidSP = lagreBehandling(UUID.randomUUID(), UTREDES, IKKE_SATT, fagsakSP)

        val behandlingerForGjenbruk: List<Behandling> =
            behandlingRepository.finnBehandlingerForGjenbrukAvVilkårOgSamværsavtaler(fagsakSP.fagsakPersonId)

        assertThat(behandlingerForGjenbruk).containsExactlyInAnyOrder(
            førstegangsbehandlingOS,
            annengangsbehandlingOS,
            førstegangsbehandlingBT,
            revurderingUnderArbeidBT,
            revurderingUnderArbeidSP,
        )
    }

    @Nested
    inner class Vedtakstidspunkt {
        private val fagsak = fagsak()

        @BeforeEach
        internal fun setUp() {
            testoppsettService.lagreFagsak(fagsak)
        }

        @Test
        internal fun `kan sette resultat med vedtakstidspunkt`() {
            behandlingRepository.insert(behandling(fagsak, resultat = INNVILGET))
        }

        @Test
        internal fun `kan ikke sette resultat uten vedtakstidspunkt`() {
            assertThatThrownBy {
                behandlingRepository.insert(behandling(fagsak, resultat = INNVILGET).copy(vedtakstidspunkt = null))
            }.has(hasCauseMessageContaining("behandling_resultat_vedtakstidspunkt_check"))
        }

        @Test
        internal fun `kan ikke sette vedtakstidspunkt uten resultat`() {
            assertThatThrownBy {
                behandlingRepository.insert(
                    behandling(
                        fagsak,
                        resultat = IKKE_SATT,
                    ).copy(vedtakstidspunkt = SporbarUtils.now()),
                )
            }.has(hasCauseMessageContaining("behandling_resultat_vedtakstidspunkt_check"))
        }

        @Test
        internal fun `kan ikke sette resultat IKKE_SATT med vedtakstidspunkt`() {
            assertThatThrownBy {
                behandlingRepository.insert(
                    behandling(
                        fagsak,
                        resultat = IKKE_SATT,
                    ).copy(vedtakstidspunkt = SporbarUtils.now()),
                )
            }.has(hasCauseMessageContaining("behandling_resultat_vedtakstidspunkt_check"))
        }
    }

    private fun lagreBehandling(
        behandlingId: UUID,
        status: BehandlingStatus,
        resultat: BehandlingResultat,
        fagsak: Fagsak,
    ): Behandling =
        behandlingRepository.insert(
            behandling(
                id = behandlingId,
                status = status,
                resultat = resultat,
                fagsak = fagsak,
            ),
        )

    private fun lagreFagsak(
        fagsakId: UUID,
        stønadType: StønadType,
        fagsakPersonId: UUID,
    ): Fagsak =
        testoppsettService.lagreFagsak(
            fagsak(
                id = fagsakId,
                stønadstype = stønadType,
                fagsakPersonId = fagsakPersonId,
            ),
        )
}
