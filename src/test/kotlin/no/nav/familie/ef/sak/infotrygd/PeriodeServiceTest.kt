package no.nav.familie.ef.sak.infotrygd

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeTestUtil.lagInfotrygdPeriode
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vilkår.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.felles.ef.Datakilde
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class PeriodeServiceTest {
    private val personService = mockk<PersonService>()
    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val replikaClient = mockk<InfotrygdReplikaClient>()
    private val vedtakService = mockk<VedtakService>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val barnService = mockk<BarnService>()

    private val service =
        PeriodeService(
            personService = personService,
            fagsakService = fagsakService,
            behandlingService = behandlingService,
            tilkjentYtelseService = tilkjentYtelseService,
            infotrygdService = InfotrygdService(replikaClient, personService),
            vedtakService = vedtakService,
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            barnService = barnService,
        )

    private val personIdent = "123"
    private val fagsak = fagsak()
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        mockFagsak(fagsak)
        every {
            personService.hentPersonIdenter(personIdent)
        } returns PdlIdenter(listOf(PdlIdent(personIdent, false)))
        every { replikaClient.hentSammenslåttePerioder(any()) } returns
            InfotrygdPeriodeResponse(
                emptyList(),
                emptyList(),
                emptyList(),
            )
    }

    @Test
    internal fun `skal returnere en periode med riktig barn og riktig aktivitet og periodetype fra vedtak`() {
        val barnId1 = UUID.randomUUID()
        val barnId2 = UUID.randomUUID()
        val barnMedAleneomsorgOppfylt = lagBehandlingBarn(barnId1)

        val tolvMndSiden = now().minusMonths(12)
        val enMndSiden = now().minusMonths(1)
        val nesteMnd = now().plusMonths(1)
        val nesteÅr = now().plusMonths(12)

        val andelGammel = lagAndelMedPeriode(tolvMndSiden, enMndSiden)
        val andelNå = lagAndelMedPeriode(now(), now())
        val andelframtid = lagAndelMedPeriode(nesteMnd, nesteÅr)

        val vedtaksperiodeGammel = lagVedtaksperiode(tolvMndSiden, enMndSiden, periodeType = VedtaksperiodeType.PERIODE_FØR_FØDSEL, aktivitet = AktivitetType.IKKE_AKTIVITETSPLIKT)
        val vedtaksperiodeNå = lagVedtaksperiode(now(), now(), periodeType = VedtaksperiodeType.HOVEDPERIODE, aktivitet = AktivitetType.FORSØRGER_REELL_ARBEIDSSØKER)
        val vedtaksperiodeFramtid = lagVedtaksperiode(nesteMnd, nesteÅr, periodeType = VedtaksperiodeType.UTVIDELSE, aktivitet = AktivitetType.BARNET_ER_SYKT)

        val vedtak = lagVedtak(behandlingId = behandling.id, vedtaksperiodeNå, vedtaksperiodeGammel, vedtaksperiodeFramtid)
        val vilkårsvurderinger = listOf(lagVilkårsvurdering(barnId1, Vilkårsresultat.OPPFYLT), lagVilkårsvurdering(barnId2, Vilkårsresultat.IKKE_OPPFYLT))

        mockFinnSisteIverksatteBehandling()
        mockTilkjentYtelse(andelNå, andelGammel, andelframtid)
        mockVedtak(vedtak)
        mockFinnVilkårsvurderinger(vilkårsvurderinger)
        mockFinnBarn(barnMedAleneomsorgOppfylt.id, barnMedAleneomsorgOppfylt)
        mockHentBehandling()

        val perioder = service.hentLøpendeOvergangsstønadPerioderMedAktivitetOgBehandlingsbarn(setOf(personIdent))

        val førstePeriode = perioder.first()
        val framtidsPeriode = perioder.last()
        assertThat(førstePeriode.barn.single().personIdent).isEqualTo(barnMedAleneomsorgOppfylt.personIdent)
        assertThat(førstePeriode.barn.single().fødselTermindato).isEqualTo(barnMedAleneomsorgOppfylt.fødselTermindato)
        assertThat(førstePeriode.periodeType).isEqualTo(VedtaksperiodeType.HOVEDPERIODE)
        assertThat(førstePeriode.aktivitet).isEqualTo(AktivitetType.FORSØRGER_REELL_ARBEIDSSØKER)

        assertThat(framtidsPeriode.barn.single().personIdent).isEqualTo(barnMedAleneomsorgOppfylt.personIdent)
        assertThat(framtidsPeriode.barn.single().fødselTermindato).isEqualTo(barnMedAleneomsorgOppfylt.fødselTermindato)
        assertThat(framtidsPeriode.periodeType).isEqualTo(VedtaksperiodeType.UTVIDELSE)
        assertThat(framtidsPeriode.aktivitet).isEqualTo(AktivitetType.BARNET_ER_SYKT)
    }

    @Test
    internal fun `skal finne vedtaksperiodeforAndel - lik`() {
        mockVedtak(lagVedtak(behandlingId = behandling.id, lagVedtaksperiode(fra = now(), til = now())))
        val vedtaksperiodeforAndel = service.finnVedtaksperiodeforAndel(andel = lagAndelMedPeriode(now(), now(), kildeBehandlingId = behandling.id))
        assertThat(vedtaksperiodeforAndel).isNotNull
    }

    @Test
    internal fun `skal finne periode når vedtaksperiode omslutter andel`() {
        mockVedtak(lagVedtak(behandlingId = behandling.id, lagVedtaksperiode(fra = now().minusYears(1), til = now().plusYears(1))))
        val vedtaksperiodeforAndel = service.finnVedtaksperiodeforAndel(andel = lagAndelMedPeriode(now(), now(), kildeBehandlingId = behandling.id))
        assertThat(vedtaksperiodeforAndel).isNotNull
    }

    @Test
    internal fun `skal ikke finne gammel vedtaksperiode gitt andel nå`() {
        mockVedtak(lagVedtak(behandlingId = behandling.id, lagVedtaksperiode(fra = now().minusMonths(6), til = now().minusMonths(5))))
        val vedtaksperiodeforAndel = service.finnVedtaksperiodeforAndel(andel = lagAndelMedPeriode(now(), now(), kildeBehandlingId = behandling.id))
        assertThat(vedtaksperiodeforAndel).isNull()
    }

    @Test
    internal fun `skal returnere true hvis andel har periode nå`() {
        val andel = lagAndelMedPeriode(now(), now())
        assertThat(andel.harPeriodeSomLøperNåEllerIFramtid()).isTrue
    }

    @Test
    internal fun `skal returnere true hvis andel har periode i framtiden`() {
        val andel = lagAndelMedPeriode(now().plusMonths(13), now().plusMonths(20))
        assertThat(andel.harPeriodeSomLøperNåEllerIFramtid()).isTrue
    }

    @Test
    internal fun `skal returnere false hvis andel har periode i før denne mnd`() {
        val andel = lagAndelMedPeriode(now().minusMonths(1), now().minusMonths(1))
        assertThat(andel.harPeriodeSomLøperNåEllerIFramtid()).isFalse
    }

    private fun now(): LocalDate = LocalDate.now()

    private fun lagAndelMedPeriode(
        stønadFom: LocalDate,
        stønadTom: LocalDate,
        kildeBehandlingId: UUID = behandling.id,
    ): AndelTilkjentYtelse =
        AndelTilkjentYtelse(
            beløp = 0,
            stønadFom = stønadFom,
            stønadTom = stønadTom,
            personIdent = "TODO()",
            inntekt = 1,
            inntektsreduksjon = 0,
            samordningsfradrag = 0,
            kildeBehandlingId = kildeBehandlingId,
        )

    @Test
    internal fun `skal returnere tom liste hvis det ikke finnes en fagsak for personen`() {
        mockFagsak(null)
        assertThat(service.hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent)).isEmpty()
    }

    @Test
    internal fun `skal returnere tom liste hvis det ikke finnes en behandling for personen`() {
        mockFinnSisteIverksatteBehandling(null)
        assertThat(service.hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent)).isEmpty()
    }

    @Test
    internal fun `perioder overlapper ikke - skal returnere perioder fra infotrygd og ef`() {
        mockFinnSisteIverksatteBehandling()
        val infotrygdFom = LocalDate.of(2021, 1, 1)
        val infotrygdTom = LocalDate.of(2021, 1, 31)
        val efFom = LocalDate.of(2021, 2, 1)
        val efTom = LocalDate.of(2021, 3, 31)
        mockTilkjentYtelse(lagAndelTilkjentYtelse(1, efFom, efTom))
        mockReplika(listOf(lagInfotrygdPeriode(stønadFom = infotrygdFom, stønadTom = infotrygdTom)))
        val perioder = service.hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent)

        assertThat(perioder).hasSize(2)
        assertThat(perioder[0].datakilde).isEqualTo(Datakilde.EF)
        assertThat(perioder[0].stønadFom).isEqualTo(efFom)
        assertThat(perioder[0].stønadTom).isEqualTo(efTom)

        assertThat(perioder[1].datakilde).isEqualTo(Datakilde.INFOTRYGD)
        assertThat(perioder[1].stønadFom).isEqualTo(infotrygdFom)
        assertThat(perioder[1].stønadTom).isEqualTo(infotrygdTom)
    }

    @Test
    internal fun `perioden fra EF avkorter periode fra infotrygd`() {
        mockFinnSisteIverksatteBehandling()
        val infotrygdFom = LocalDate.of(2021, 1, 1)
        val infotrygdTom = LocalDate.of(2021, 3, 31)
        val efFom = LocalDate.of(2021, 2, 1)
        val efTom = LocalDate.of(2021, 3, 31)
        mockTilkjentYtelse(lagAndelTilkjentYtelse(1, efFom, efTom))
        mockReplika(listOf(lagInfotrygdPeriode(stønadFom = infotrygdFom, stønadTom = infotrygdTom)))
        val perioder = service.hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent)

        assertThat(perioder).hasSize(2)
        assertThat(perioder[0].datakilde).isEqualTo(Datakilde.EF)
        assertThat(perioder[0].stønadFom).isEqualTo(efFom)
        assertThat(perioder[0].stønadTom).isEqualTo(efTom)

        assertThat(perioder[1].datakilde).isEqualTo(Datakilde.INFOTRYGD)
        assertThat(perioder[1].stønadFom).isEqualTo(infotrygdFom)
        assertThat(perioder[1].stønadTom).isEqualTo(efFom.minusDays(1))
    }

    @Test
    internal fun `tilkjent ytelse uten andeler fra EF avkorter periode fra infotrygd`() {
        mockFinnSisteIverksatteBehandling()
        val infotrygdFom = LocalDate.of(2021, 1, 1)
        val infotrygdTom = LocalDate.of(2021, 3, 31)
        val efFom = LocalDate.of(2021, 2, 1)
        mockTilkjentYtelse(efFom)
        mockReplika(listOf(lagInfotrygdPeriode(stønadFom = infotrygdFom, stønadTom = infotrygdTom)))
        val perioder = service.hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent)

        assertThat(perioder).hasSize(1)
        assertThat(perioder[0].datakilde).isEqualTo(Datakilde.INFOTRYGD)
        assertThat(perioder[0].stønadFom).isEqualTo(infotrygdFom)
        assertThat(perioder[0].stønadTom).isEqualTo(efFom.minusDays(1))
    }

    @Test
    internal fun `hvis en periode fra ef overlapper perioder fra infotrygd så er det perioden fra EF som har høyere presidens`() {
        mockFinnSisteIverksatteBehandling()
        val fom = YearMonth.now().atDay(1)
        val tom = YearMonth.now().atEndOfMonth()
        mockTilkjentYtelse(lagAndelTilkjentYtelse(1, fom, tom))
        mockReplika(listOf(lagInfotrygdPeriode(beløp = 2, stønadFom = fom, stønadTom = tom)))
        val perioder = service.hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent)

        assertThat(perioder).hasSize(1)
        assertThat(perioder[0].datakilde).isEqualTo(Datakilde.EF)
        assertThat(perioder[0].stønadFom).isEqualTo(fom)
        assertThat(perioder[0].stønadTom).isEqualTo(tom)
        assertThat(perioder[0].månedsbeløp).isEqualTo(1)
    }

    @Test
    internal fun `skal endre tom-datoer på overlappende perioder tvers fagsystem`() {
        val periode1fom = LocalDate.of(2021, 1, 1)
        val periode1tom = LocalDate.of(2021, 1, 31)
        val periode2fom = LocalDate.of(2021, 2, 1)
        val periode2tom = LocalDate.of(2021, 3, 31)
        val efFra = LocalDate.of(2021, 3, 1)
        val efTil = LocalDate.of(2021, 3, 31)

        mockFinnSisteIverksatteBehandling()
        mockTilkjentYtelse(lagAndelTilkjentYtelse(100, fraOgMed = efFra, tilOgMed = efTil))
        mockReplika(
            listOf(
                lagInfotrygdPeriode(stønadFom = periode1fom, stønadTom = periode1tom, beløp = 1),
                lagInfotrygdPeriode(stønadFom = periode2fom, stønadTom = periode2tom, beløp = 2),
            ).sortedByDescending { it.stønadFom },
        )
        val perioder = service.hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent)

        assertThat(perioder).hasSize(3)
        assertThat(perioder[0].stønadFom).isEqualTo(efFra)
        assertThat(perioder[0].stønadTom).isEqualTo(efTil)

        assertThat(perioder[1].stønadFom).isEqualTo(periode2fom)
        assertThat(perioder[1].stønadTom).isNotEqualTo(periode2tom)
        assertThat(perioder[1].stønadTom).isEqualTo(efFra.minusDays(1))

        assertThat(perioder[2].stønadFom).isEqualTo(periode1fom)
        assertThat(perioder[2].stønadTom).isEqualTo(periode1tom)
    }

    private fun mockReplika(overgangsstønad: List<InfotrygdPeriode>) {
        every { replikaClient.hentSammenslåttePerioder(any()) } returns
            InfotrygdPeriodeResponse(
                overgangsstønad,
                emptyList(),
                emptyList(),
            )
    }

    private fun mockFagsak(fagsak: Fagsak? = this.fagsak) {
        every { fagsakService.finnFagsak(setOf(personIdent), StønadType.OVERGANGSSTØNAD) } returns fagsak
    }

    private fun mockFinnSisteIverksatteBehandling(behandling: Behandling? = this.behandling) {
        every { behandlingService.finnSisteIverksatteBehandling(fagsak.id) } returns behandling
    }

    private fun mockTilkjentYtelse(vararg andelTilkjentYtelse: AndelTilkjentYtelse) {
        if (andelTilkjentYtelse.isEmpty()) error("Må sette startdato hvis man har en tom liste med andeler")
        every { tilkjentYtelseService.hentForBehandling(any()) } returns
            lagTilkjentYtelse(andelTilkjentYtelse.toList(), behandlingId = behandling.id)
    }

    private fun mockTilkjentYtelse(startdato: LocalDate) {
        every { tilkjentYtelseService.hentForBehandling(any()) } returns
            lagTilkjentYtelse(andelerTilkjentYtelse = emptyList(), behandlingId = behandling.id, startdato = startdato)
    }

    private fun lagVedtak(
        behandlingId: UUID = UUID.randomUUID(),
        vararg perioder: Vedtaksperiode,
    ): Vedtak =
        Vedtak(
            behandlingId = behandlingId,
            resultatType = ResultatType.INNVILGE,
            periodeBegrunnelse = null,
            inntektBegrunnelse = null,
            avslåBegrunnelse = null,
            perioder = perioder.let { PeriodeWrapper(it.toList()) },
            inntekter = null,
            saksbehandlerIdent = null,
            opphørFom = null,
            beslutterIdent = null,
        )

    private fun lagVedtaksperiode(
        fra: LocalDate = now(),
        til: LocalDate = now(),
        aktivitet: AktivitetType = AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_ARBEID,
        periodeType: VedtaksperiodeType = VedtaksperiodeType.UTVIDELSE,
    ): Vedtaksperiode =
        Vedtaksperiode(
            datoFra = fra,
            datoTil = til,
            aktivitet = aktivitet,
            periodeType = periodeType,
        )

    private fun mockHentBehandling() {
        every { behandlingService.hentBehandling(any()) } returns behandling
    }

    private fun mockFinnBarn(
        barnId: UUID,
        forventetRetur: BehandlingBarn,
    ) {
        every { barnService.hentBehandlingBarnForBarnIder(listOf(barnId)) } returns listOf(forventetRetur)
    }

    private fun mockFinnVilkårsvurderinger(vilkårsvurderinger: List<Vilkårsvurdering>) {
        every { vilkårsvurderingRepository.findByTypeAndBehandlingIdIn(any(), any()) } returns vilkårsvurderinger
    }

    private fun mockVedtak(vedtak: Vedtak) {
        every { vedtakService.hentVedtak(any()) } returns vedtak
    }

    private fun lagBehandlingBarn(barnId: UUID): BehandlingBarn =
        BehandlingBarn(
            id = barnId,
            behandlingId = behandling.id,
            personIdent = "barnIdent",
            fødselTermindato = LocalDate.now().minusMonths(7),
        )

    private fun lagVilkårsvurdering(
        barnId: UUID?,
        resultat: Vilkårsresultat = Vilkårsresultat.OPPFYLT,
    ) = Vilkårsvurdering(behandlingId = behandling.id, resultat = resultat, type = VilkårType.ALENEOMSORG, barnId = barnId, delvilkårsvurdering = DelvilkårsvurderingWrapper(emptyList()), opphavsvilkår = null)
}
