package no.nav.familie.ef.sak.iverksett

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.domain.ÅrsakRevurdering
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.ef.sak.behandling.revurdering.ÅrsakRevurderingsRepository
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.ef.sak.brev.BrevmottakereRepository
import no.nav.familie.ef.sak.brev.domain.MottakerRolle
import no.nav.familie.ef.sak.felles.util.DatoUtil
import no.nav.familie.ef.sak.felles.util.opprettGrunnlagsdata
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.oppfølgingsoppgave.OppfølgingsoppgaveService
import no.nav.familie.ef.sak.opplysninger.mapper.BarnMatcher
import no.nav.familie.ef.sak.opplysninger.mapper.MatchetBehandlingBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.repository.barnMedIdent
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.repository.søker
import no.nav.familie.ef.sak.repository.tilkjentYtelse
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.repository.vedtaksperiode
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekreving
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType.HOVEDPERIODE
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType.MIDLERTIDIG_OPPHØR
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType.SANKSJON
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.kontrakter.ef.felles.AvslagÅrsak
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.felles.Opplysningskilde
import no.nav.familie.kontrakter.ef.felles.Revurderingsårsak
import no.nav.familie.kontrakter.ef.felles.Vedtaksresultat
import no.nav.familie.kontrakter.ef.iverksett.BehandlingKategori
import no.nav.familie.kontrakter.ef.iverksett.BehandlingsdetaljerDto
import no.nav.familie.kontrakter.ef.iverksett.Brevmottaker
import no.nav.familie.kontrakter.ef.iverksett.FagsakdetaljerDto
import no.nav.familie.kontrakter.ef.iverksett.Grunnbeløp
import no.nav.familie.kontrakter.ef.iverksett.IverksettBarnetilsynDto
import no.nav.familie.kontrakter.ef.iverksett.IverksettDto
import no.nav.familie.kontrakter.ef.iverksett.IverksettOvergangsstønadDto
import no.nav.familie.kontrakter.ef.iverksett.IverksettSkolepengerDto
import no.nav.familie.kontrakter.ef.iverksett.SøkerDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksdetaljerBarnetilsynDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksdetaljerDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksdetaljerOvergangsstønadDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksdetaljerSkolepengerDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksperiodeOvergangsstønadDto
import no.nav.familie.kontrakter.ef.iverksett.VilkårsvurderingDto
import no.nav.familie.kontrakter.ef.iverksett.ÅrsakRevurderingDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.util.Optional
import java.util.UUID
import no.nav.familie.kontrakter.ef.felles.BehandlingType as BehandlingTypeIverksett
import no.nav.familie.kontrakter.ef.felles.RegelId as RegelIdIverksett
import no.nav.familie.kontrakter.ef.felles.VilkårType as VilkårTypeIverksett
import no.nav.familie.kontrakter.ef.felles.Vilkårsresultat as VilkårsresultatIverksett
import no.nav.familie.kontrakter.ef.iverksett.AktivitetType as AktivitetTypeIverksett
import no.nav.familie.kontrakter.ef.iverksett.SkolepengerStudietype as SkolepengerStudietypeIverksett
import no.nav.familie.kontrakter.ef.iverksett.SvarId as SvarIdIverksett
import no.nav.familie.kontrakter.ef.iverksett.VedtaksperiodeType as VedtaksperiodeTypeIverksett

internal class IverksettingDtoMapperTest {
    private val tilbakekrevingService = mockk<TilbakekrevingService>(relaxed = true)
    private val simuleringService = mockk<SimuleringService>()
    private val vedtakService = mockk<VedtakService>()
    private val behandlingshistorikkService = mockk<BehandlingshistorikkService>()
    private val barnService = mockk<BarnService>()
    private val grunnlagsdataService = mockk<GrunnlagsdataService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val brevmottakereRepository = mockk<BrevmottakereRepository>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>(relaxed = true)
    private val barnMatcher = mockk<BarnMatcher>()
    private val årsakRevurderingsRepository = mockk<ÅrsakRevurderingsRepository>()
    private val oppfølgingsoppgaveService = mockk<OppfølgingsoppgaveService>()
    private val behandlingRepository = mockk<BehandlingRepository>()

    private val iverksettingDtoMapper =
        IverksettingDtoMapper(
            arbeidsfordelingService = arbeidsfordelingService,
            behandlingshistorikkService = behandlingshistorikkService,
            grunnlagsdataService = grunnlagsdataService,
            simuleringService = simuleringService,
            barnService = barnService,
            tilbakekrevingService = tilbakekrevingService,
            tilkjentYtelseService = tilkjentYtelseService,
            vedtakService = vedtakService,
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            brevmottakereRepository = brevmottakereRepository,
            årsakRevurderingsRepository = årsakRevurderingsRepository,
            oppfølgingsoppgaveService = oppfølgingsoppgaveService,
            behandlingRepository = behandlingRepository,
        )

    private val fagsak = fagsak(fagsakpersoner(setOf("1")))
    private val forrigeBehandlingId = UUID.fromString("73144d90-d238-41d2-833b-fc719dae23cc")
    private val forrigeEksternBehandlingId = 22L
    private val forrigeBehandling = behandling(fagsak = fagsak, id = forrigeBehandlingId, eksternId = forrigeEksternBehandlingId)
    private val behandling = behandling(fagsak = fagsak, forrigeBehandlingId = forrigeBehandlingId)
    private val saksbehandling = saksbehandling(fagsak, behandling)

    @BeforeEach
    internal fun setUp() {
        every { vedtakService.hentVedtak(behandling.id) } returns Vedtak(behandling.id, ResultatType.INNVILGE)
        val behandlingshistorikk =
            Behandlingshistorikk(
                behandlingId = behandling.id,
                opprettetAv = "opprettetAv",
                steg = StegType.SEND_TIL_BESLUTTER,
            )
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any()) } returns behandlingshistorikk
        every { brevmottakereRepository.findByIdOrNull(any()) } returns null
        every { årsakRevurderingsRepository.findByIdOrNull(any()) } returns
            ÅrsakRevurdering(
                behandlingId = saksbehandling.id,
                opplysningskilde = Opplysningskilde.MELDING_MODIA,
                årsak = Revurderingsårsak.ENDRING_INNTEKT,
                beskrivelse = "beskrivelse",
            )
        every { oppfølgingsoppgaveService.hentOppgaverForOpprettelseEllerNull(any()) } returns null
        every { behandlingRepository.findById(any()) } returns Optional.of(forrigeBehandling)
    }

    @Test
    internal fun `Skal mappe tilbakekreving med varseltekst og feilutbetaling`() {
        val forventetVarseltekst = "forventetVarseltekst"
        val simuleringsoppsummering =
            Simuleringsoppsummering(
                perioder = emptyList(),
                fomDatoNestePeriode = null,
                etterbetaling = BigDecimal.ZERO,
                feilutbetaling = BigDecimal.TEN,
                fom = null,
                tomDatoNestePeriode = null,
                forfallsdatoNestePeriode = null,
                tidSimuleringHentet = null,
                tomSisteUtbetaling = null,
            )

        every {
            tilbakekrevingService.hentTilbakekreving(behandlingId = behandling.id)
        } returns
            Tilbakekreving(
                behandlingId = behandling.id,
                valg = Tilbakekrevingsvalg.OPPRETT_MED_VARSEL,
                varseltekst = forventetVarseltekst,
                begrunnelse = "ingen",
            )
        every {
            simuleringService.hentLagretSimuleringsoppsummering(behandlingId = behandling.id)
        } returns simuleringsoppsummering.copy(feilutbetaling = BigDecimal.TEN)

        val tilbakekreving = iverksettingDtoMapper.mapTilbakekreving(behandling.id)
        assertThat(tilbakekreving?.tilbakekrevingMedVarsel?.varseltekst).isEqualTo(forventetVarseltekst)
        assertThat(tilbakekreving?.tilbakekrevingMedVarsel?.sumFeilutbetaling).isEqualTo(BigDecimal.TEN)
    }

    @Test
    internal fun `tilDto - skal kunne mappe person uten barn`() {
        every { barnService.finnBarnPåBehandling(any()) } returns emptyList()
        every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns
            GrunnlagsdataMedMetadata(opprettGrunnlagsdata(), LocalDateTime.now())
        every { tilkjentYtelseService.hentForBehandling(any()) } returns
            tilkjentYtelse(
                UUID.randomUUID(),
                personIdent = "132",
                stønadsår = LocalDate.now().year,
                grunnbeløpsmåned = Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed,
            )
        every { vilkårsvurderingRepository.findByBehandlingId(any()) } returns mockk(relaxed = true)

        iverksettingDtoMapper.tilDto(saksbehandling, "bes")

        verify(exactly = 1) { grunnlagsdataService.hentGrunnlagsdata(any()) }
        verify(exactly = 1) { arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(any()) }
    }

    @Test
    internal fun `map overgangsstønad til IverksettDto - sjekk alle felter`() {
        val behandlingId = mockReturnerObjekterMedAlleFelterFylt()

        val saksbehandling = saksbehandling()
        val iverksettDto = iverksettingDtoMapper.tilDto(saksbehandling, "beslutter")

        assertAlleFelter(iverksettDto as IverksettOvergangsstønadDto, behandlingId)
    }

    @Test
    internal fun `map barnetilsyn til IverksettDto - sjekk alle felter`() {
        val behandlingId = mockReturnerObjekterMedAlleFelterFylt()

        val saksbehandling = saksbehandling(stønadType = StønadType.BARNETILSYN)
        val iverksettDto = iverksettingDtoMapper.tilDto(saksbehandling, "beslutter")

        assertAlleFelter(iverksettDto as IverksettBarnetilsynDto, behandlingId)
    }

    @Test
    internal fun `map skolepenger til IverksettDto - sjekk alle felter`() {
        val behandlingId = mockReturnerObjekterMedAlleFelterFylt()

        val saksbehandling = saksbehandling(stønadType = StønadType.SKOLEPENGER)
        val iverksettDto = iverksettingDtoMapper.tilDto(saksbehandling, "beslutter")

        assertAlleFelter(iverksettDto as IverksettSkolepengerDto, behandlingId)
    }

    @Test
    internal fun `skal mappe avslag og årsak til avslag`() {
        mockReturnerObjekterMedAlleFelterFylt()

        val saksbehandling = saksbehandling(resultat = BehandlingResultat.AVSLÅTT)
        val avslåttVedtak =
            Vedtak(
                behandlingId = behandling.id,
                resultatType = ResultatType.AVSLÅ,
                avslåÅrsak = AvslagÅrsak.MINDRE_INNTEKTSENDRINGER,
            )

        every { vedtakService.hentVedtak(any()) } returns avslåttVedtak
        val iverksettDto = iverksettingDtoMapper.tilDto(saksbehandling, "beslutter")
        assertThat(iverksettDto.vedtak.avslagÅrsak).isEqualTo(avslåttVedtak.avslåÅrsak)
    }

    @Test
    internal fun `skal sende med sanksjonsperioder for innvilget vedtak`() {
        val dato = LocalDate.of(2021, 1, 1)
        mockReturnerObjekterMedAlleFelterFylt()

        val saksbehandling = saksbehandling(resultat = BehandlingResultat.INNVILGET)
        val perioder =
            listOf(
                vedtaksperiode(startDato = dato, sluttDato = dato, vedtaksperiodeType = HOVEDPERIODE),
                vedtaksperiode(startDato = dato, sluttDato = dato, vedtaksperiodeType = SANKSJON),
            )
        val innvilgetVedtak = vedtak(behandling.id, perioder = PeriodeWrapper(perioder))

        every { vedtakService.hentVedtak(any()) } returns innvilgetVedtak
        val iverksettDto = iverksettingDtoMapper.tilDto(saksbehandling, "beslutter")
        val periodetyper =
            iverksettDto.vedtak.vedtaksperioder
                .map { it as VedtaksperiodeOvergangsstønadDto }
                .map { it.periodeType }
        val hovedperiode = no.nav.familie.kontrakter.ef.iverksett.VedtaksperiodeType.HOVEDPERIODE
        val sanksjon = no.nav.familie.kontrakter.ef.iverksett.VedtaksperiodeType.SANKSJON
        assertThat(periodetyper).containsExactly(hovedperiode, sanksjon)
    }

    @Test
    internal fun `skal ikke sende med midlertidig opphør med sanksjonsperioder for innvilget vedtak`() {
        val dato = LocalDate.of(2021, 1, 1)
        mockReturnerObjekterMedAlleFelterFylt()

        val saksbehandling = saksbehandling(resultat = BehandlingResultat.INNVILGET)
        val perioder =
            listOf(
                vedtaksperiode(startDato = dato, sluttDato = dato, vedtaksperiodeType = HOVEDPERIODE),
                vedtaksperiode(startDato = dato, sluttDato = dato, vedtaksperiodeType = MIDLERTIDIG_OPPHØR),
                vedtaksperiode(startDato = dato, sluttDato = dato, vedtaksperiodeType = HOVEDPERIODE),
            )
        val innvilgetVedtak = vedtak(behandling.id, perioder = PeriodeWrapper(perioder))

        every { vedtakService.hentVedtak(any()) } returns innvilgetVedtak
        val iverksettDto = iverksettingDtoMapper.tilDto(saksbehandling, "beslutter")
        val periodetyper =
            iverksettDto.vedtak.vedtaksperioder
                .map { it as VedtaksperiodeOvergangsstønadDto }
                .map { it.periodeType }
        val hovedperiode = no.nav.familie.kontrakter.ef.iverksett.VedtaksperiodeType.HOVEDPERIODE
        assertThat(periodetyper).containsExactly(hovedperiode, hovedperiode)
    }

    @Test
    internal fun `skal kunne mappe alle enums`() {
        BehandlingType.values().forEach { BehandlingTypeIverksett.valueOf(it.name) }

        RegelId.values().forEach { RegelIdIverksett.valueOf(it.name) }
        SvarId.values().forEach { SvarIdIverksett.valueOf(it.name) }
        VilkårType.values().forEach { VilkårTypeIverksett.valueOf(it.name) }
        Vilkårsresultat.values().forEach { VilkårsresultatIverksett.valueOf(it.name) }

        AktivitetType.values().forEach { AktivitetTypeIverksett.valueOf(it.name) }
        VedtaksperiodeType
            .values()
            .filter { it != MIDLERTIDIG_OPPHØR }
            .forEach { VedtaksperiodeTypeIverksett.valueOf(it.name) }

        SkolepengerStudietype.values().forEach { SkolepengerStudietypeIverksett.valueOf(it.name) }
    }

    private fun assertAlleFelter(
        iverksettDto: IverksettOvergangsstønadDto,
        behandlingId: UUID?,
    ) {
        assertAlleFelterIverksettDto(iverksettDto, behandlingId, StønadType.OVERGANGSSTØNAD)
        assertVedtaksperiode(iverksettDto.vedtak)
        assertGrunnbeløp(iverksettDto)
    }

    private fun assertGrunnbeløp(iverksettDto: IverksettOvergangsstønadDto) {
        assertThat(iverksettDto.vedtak.grunnbeløp).isEqualTo(
            Grunnbeløp(
                Grunnbeløpsperioder.nyesteGrunnbeløp.periode,
                Grunnbeløpsperioder.nyesteGrunnbeløp.grunnbeløp,
            ),
        )
    }

    private fun assertAlleFelter(
        iverksettDto: IverksettBarnetilsynDto,
        behandlingId: UUID?,
    ) {
        assertAlleFelterIverksettDto(iverksettDto, behandlingId, StønadType.BARNETILSYN)
        assertVedtaksperiode(iverksettDto.vedtak)
    }

    private fun assertAlleFelter(
        iverksettDto: IverksettSkolepengerDto,
        behandlingId: UUID?,
    ) {
        assertAlleFelterIverksettDto(iverksettDto, behandlingId, StønadType.SKOLEPENGER)
        assertVedtaksperiode(iverksettDto.vedtak)
    }

    private fun assertAlleFelterIverksettDto(
        iverksettDto: IverksettDto,
        behandlingId: UUID?,
        stønadType: StønadType,
    ) {
        val behandling = iverksettDto.behandling
        assertFagsak(iverksettDto.fagsak, stønadType)
        assertSøker(iverksettDto.søker)
        assertBehandling(behandling, behandlingId)
        assertVilkårsvurdering(behandling.vilkårsvurderinger)
        assertVedtak(iverksettDto.vedtak)
    }

    private fun assertFagsak(
        fagsak: FagsakdetaljerDto,
        stønadType: StønadType,
    ) {
        assertThat(fagsak.eksternId).isEqualTo(4)
        assertThat(fagsak.fagsakId).isEqualTo(UUID.fromString("65811679-17ed-4c3c-b1ab-c1678acdfa7b"))
        assertThat(fagsak.stønadstype).isEqualTo(stønadType)
    }

    private fun assertBehandling(
        behandling: BehandlingsdetaljerDto,
        behandlingId: UUID?,
    ) {
        assertThat(behandling.behandlingId).isEqualTo(behandlingId)
        assertThat(behandling.behandlingType.name).isEqualTo(BehandlingType.FØRSTEGANGSBEHANDLING.name)
        assertThat(behandling.behandlingÅrsak).isEqualTo(BehandlingÅrsak.SØKNAD)
        assertThat(behandling.forrigeBehandlingId).isEqualTo(forrigeBehandlingId)
        assertThat(behandling.forrigeBehandlingEksternId).isEqualTo(forrigeEksternBehandlingId)
        assertThat(behandling.eksternId).isEqualTo(1)
        assertThat(behandling.aktivitetspliktInntrefferDato).isNull() // Ikke i bruk?
        assertThat(behandling.kravMottatt).isEqualTo(LocalDate.of(2022, 3, 1))
        assertThat(behandling.årsakRevurdering!!)
            .isEqualTo(ÅrsakRevurderingDto(Opplysningskilde.MELDING_MODIA, Revurderingsårsak.ENDRING_INNTEKT))
        assertThat(behandling.vilkårsvurderinger.size).isEqualTo(1)
    }

    private fun assertSøker(søker: SøkerDto) {
        assertThat(søker.personIdent).isEqualTo("3")
        assertThat(søker.barn.size).isEqualTo(1)
        assertThat(søker.barn.first().personIdent).isEqualTo("123")
        assertThat(søker.barn.first().termindato).isEqualTo(LocalDate.of(2022, 3, 25))
        assertThat(søker.tilhørendeEnhet).isEqualTo("4489")
        assertThat(søker.adressebeskyttelse?.name).isEqualTo(ADRESSEBESKYTTELSEGRADERING.UGRADERT.name)
    }

    private fun assertVedtak(vedtak: VedtaksdetaljerDto) {
        assertThat(vedtak.beslutterId).isEqualTo("beslutter")
        assertThat(vedtak.brevmottakere).hasSize(2)

        val brevmottaker = vedtak.brevmottakere[0]
        assertThat(brevmottaker.ident).isEqualTo("personIdent")
        assertThat(brevmottaker.navn).isEqualTo("fornavn etternavn")
        assertThat(brevmottaker.identType.name).isEqualTo(Brevmottaker.IdentType.PERSONIDENT.name)
        assertThat(brevmottaker.mottakerRolle.name).isEqualTo(MottakerRolle.BRUKER.name)

        val brevmottaker2 = vedtak.brevmottakere[1]
        assertThat(brevmottaker2.ident).isEqualTo("organisasjonsnummer")
        assertThat(brevmottaker2.navn).isEqualTo("organisasjonsnavn")
        assertThat(brevmottaker2.identType.name).isEqualTo(Brevmottaker.IdentType.ORGANISASJONSNUMMER.name)
        assertThat(brevmottaker2.mottakerRolle.name).isEqualTo(MottakerRolle.BRUKER.name)

        assertThat(vedtak.opphørÅrsak).isNull() // Burde ha verdi i test?
        assertThat(vedtak.resultat).isEqualTo(Vedtaksresultat.INNVILGET)
        assertThat(vedtak.saksbehandlerId).isEqualTo("opprettetAv")

        val tilbakekrevingMedVarsel = vedtak.tilbakekreving?.tilbakekrevingMedVarsel
        assertThat(tilbakekrevingMedVarsel?.fellesperioder).hasSize(1)
        assertThat(tilbakekrevingMedVarsel?.fellesperioder?.first()?.fom).isEqualTo(YearMonth.of(2022, 3))
        assertThat(tilbakekrevingMedVarsel?.fellesperioder?.first()?.tom).isEqualTo(YearMonth.of(2022, 3))
        assertThat(tilbakekrevingMedVarsel?.varseltekst).isEqualTo("varseltekst")
        assertThat(tilbakekrevingMedVarsel?.sumFeilutbetaling).isEqualTo(BigDecimal("1000.0"))
        assertThat(vedtak.tilbakekreving?.tilbakekrevingsvalg)
            .isEqualTo(no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)

        assertThat(vedtak.tilkjentYtelse?.startmåned).isEqualTo(YearMonth.of(2022, 4))
        // opphørsdato ikke i bruk?

        assertThat(vedtak.tilkjentYtelse?.andelerTilkjentYtelse).hasSize(1)
        // assertThat(iverksettDto.vedtak.vedtakstidspunkt) - sjekker ikke denne da det er LocalDate.now()
    }

    private fun assertVilkårsvurdering(vilkårsvurderinger: List<VilkårsvurderingDto>) {
        assertThat(vilkårsvurderinger).hasSize(1)
        val vilkårsvurdering = vilkårsvurderinger[0]

        val delvilkårsvurderinger = vilkårsvurdering.delvilkårsvurderinger
        assertThat(delvilkårsvurderinger).hasSize(1)

        val delvilkårsvurdering = delvilkårsvurderinger[0]
        assertThat(vilkårsvurdering.resultat.name).isEqualTo(Vilkårsresultat.OPPFYLT.name)
        assertThat(vilkårsvurdering.vilkårType.name).isEqualTo(VilkårType.FORUTGÅENDE_MEDLEMSKAP.name)
        assertThat(delvilkårsvurderinger).hasSize(1)

        assertThat(delvilkårsvurdering.resultat.name).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL.name)
        val vurderinger = delvilkårsvurdering.vurderinger
        assertThat(vurderinger).hasSize(1)

        val vurdering = vurderinger[0]
        assertThat(vurdering.regelId.name).isEqualTo(RegelId.MEDLEMSKAP_UNNTAK.name)
        assertThat(vurdering.svar?.name).isEqualTo(SvarIdIverksett.JA.name)
        assertThat(vurdering.begrunnelse).isEqualTo("begrunnelse")
    }

    private fun assertVedtaksperiode(vedtak: VedtaksdetaljerOvergangsstønadDto) {
        assertThat(vedtak.vedtaksperioder).hasSize(1)
        val vedtaksperiode = vedtak.vedtaksperioder[0]
        assertThat(vedtaksperiode.fraOgMed).isEqualTo(LocalDate.of(2022, 3, 27))
        assertThat(vedtaksperiode.tilOgMed).isEqualTo(LocalDate.of(2022, 3, 28))
        assertThat(vedtaksperiode.aktivitet.name).isEqualTo(AktivitetType.BARN_UNDER_ETT_ÅR.name)
        assertThat(vedtaksperiode.periodeType.name).isEqualTo(HOVEDPERIODE.name)
    }

    private fun assertVedtaksperiode(vedtak: VedtaksdetaljerBarnetilsynDto) {
        assertThat(vedtak.vedtaksperioder).hasSize(1)
        val vedtaksperiode = vedtak.vedtaksperioder[0]
        assertThat(vedtaksperiode.fraOgMed).isEqualTo(LocalDate.of(2022, 3, 1))
        assertThat(vedtaksperiode.tilOgMed).isEqualTo(LocalDate.of(2022, 3, 31))
        assertThat(vedtaksperiode.antallBarn).isEqualTo(1)
        assertThat(vedtaksperiode.utgifter).isEqualTo(10)

        assertThat(vedtak.kontantstøtte).hasSize(1)
        val kontantstøtte = vedtak.kontantstøtte[0]
        assertThat(kontantstøtte.fraOgMed).isEqualTo(LocalDate.of(2021, 3, 1))
        assertThat(kontantstøtte.tilOgMed).isEqualTo(LocalDate.of(2021, 3, 31))
        assertThat(kontantstøtte.beløp).isEqualTo(1)
        assertThat(vedtak.tilleggsstønad).hasSize(1)

        val tilleggsstønad = vedtak.tilleggsstønad[0]
        assertThat(tilleggsstønad.fraOgMed).isEqualTo(LocalDate.of(2021, 4, 1))
        assertThat(tilleggsstønad.tilOgMed).isEqualTo(LocalDate.of(2021, 4, 30))
        assertThat(tilleggsstønad.beløp).isEqualTo(2)
    }

    private fun assertVedtaksperiode(vedtak: VedtaksdetaljerSkolepengerDto) {
        assertThat(vedtak.vedtaksperioder).hasSize(1)
        val vedtaksperiode = vedtak.vedtaksperioder[0]

        assertThat(vedtaksperiode.perioder).hasSize(1)
        assertThat(vedtaksperiode.perioder[0].studietype).isEqualTo(SkolepengerStudietypeIverksett.HØGSKOLE_UNIVERSITET)
        assertThat(vedtaksperiode.perioder[0].fraOgMed).isEqualTo(LocalDate.of(2021, 3, 1))
        assertThat(vedtaksperiode.perioder[0].tilOgMed).isEqualTo(LocalDate.of(2021, 3, 31))
        assertThat(vedtaksperiode.perioder[0].studiebelastning).isEqualTo(50)

        assertThat(vedtaksperiode.utgiftsperioder).hasSize(1)
        assertThat(vedtaksperiode.utgiftsperioder[0].utgiftsdato).isEqualTo(LocalDate.of(2021, 2, 1))
        assertThat(vedtaksperiode.utgiftsperioder[0].stønad).isEqualTo(150)
    }

    private fun mockReturnerObjekterMedAlleFelterFylt(): UUID? {
        val grunnlagsdata =
            opprettGrunnlagsdata().copy(
                søker = søker(),
                barn = listOf(barnMedIdent(fnr = "123", navn = "fornavn etternavn")),
            )
        every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns
            GrunnlagsdataMedMetadata(grunnlagsdata, LocalDateTime.parse("2022-03-25T05:51:31.439"))
        every { arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(any()) } returns "4489"
        val behandlingId = UUID.fromString("73144d90-d238-41d2-833b-fc719dae23cb")

        val behandlingBarn = objectMapper.readValue<BehandlingBarn>(behandlingBarnJson)
        every { barnService.finnBarnPåBehandling(any()) } returns listOf(behandlingBarn)
        every { barnMatcher.kobleBehandlingBarnOgRegisterBarn(any(), any()) } returns
            listOf(
                MatchetBehandlingBarn(
                    fødselsnummer = "1234",
                    barn = barnMedIdent(fnr = "1234", "fornavn etternavn"),
                    behandlingBarn = behandlingBarn,
                ),
            )
        every { vilkårsvurderingRepository.findByBehandlingId(any()) } returns
            listOf(
                objectMapper.readValue(
                    vilkårsvurderingJson,
                ),
            )
        every { vedtakService.hentVedtak(any()) } returns objectMapper.readValue(vedtakJson)
        every { brevmottakereRepository.findByIdOrNull(any()) } returns objectMapper.readValue(brevmottakereJson)
        every { tilbakekrevingService.hentTilbakekreving(any()) } returns objectMapper.readValue(tilbakekrevingJson)
        every { simuleringService.hentLagretSimuleringsoppsummering(any()) } returns
            objectMapper.readValue(
                simuleringsoppsummeringJson,
            )
        every { tilkjentYtelseService.hentForBehandling(any()) } returns objectMapper.readValue(tilkjentYtelseJson)
        return behandlingId
    }

    @Nested
    inner class ValideringGrunnbeløp {
        @Test
        fun `skal feile ved iverksetting med utdatert grunnbeløp`() {
            val inneværendeÅr = LocalDate.now().year

            every { tilkjentYtelseService.hentForBehandling(saksbehandling.id) } returns
                tilkjentYtelse(
                    behandlingId = UUID.randomUUID(),
                    personIdent = "132",
                    grunnbeløpsmåned = YearMonth.of(inneværendeÅr - 2, Month.MAY),
                )

            mockkObject(DatoUtil)
            every { DatoUtil.dagensDatoMedTid() } returns LocalDateTime.of(inneværendeÅr, 1, 1, 0, 0)
            assertThrows<ApiFeil> { iverksettingDtoMapper.tilDto(saksbehandling, "bes") }
            unmockkObject(DatoUtil)
        }

        @Test
        fun `skal ikke feile ved iverksetting med nyeste grunnbeløp`() {
            val inneværendeÅr = LocalDate.now().year

            every { barnService.finnBarnPåBehandling(any()) } returns emptyList()
            every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns
                GrunnlagsdataMedMetadata(opprettGrunnlagsdata(), LocalDateTime.now())
            every { vilkårsvurderingRepository.findByBehandlingId(any()) } returns mockk(relaxed = true)

            every { tilkjentYtelseService.hentForBehandling(saksbehandling.id) } returns
                tilkjentYtelse(
                    behandlingId = UUID.randomUUID(),
                    personIdent = "132",
                    grunnbeløpsmåned = Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed,
                )

            mockkObject(DatoUtil)
            every { DatoUtil.dagensDatoMedTid() } returns LocalDateTime.of(inneværendeÅr, 1, 1, 0, 0)

            iverksettingDtoMapper.tilDto(saksbehandling, "bes")

            unmockkObject(DatoUtil)
        }

        @Test
        fun `skal ikke feile ved iverksetting av utdatert G dersom vedtaksresultat er OPPHØRT`() {
            /*
             * Ved et opphør settes grunnbeløpsmåned til samme grunnbeløpsmåned som tilkjent ytelse fra forrige vedtak.
             * Derfor vil opphør ikke nødvendigvis ha nyeste grunnbeløpsmåned. Dette går fint fordi det ikke gjøres noen beregning der grunnbeløp brukes.
             * */

            val inneværendeÅr = LocalDate.now().year

            val opphørtSaksbehandling = saksbehandling.copy(resultat = BehandlingResultat.OPPHØRT)

            every { vedtakService.hentVedtak(behandling.id) } returns Vedtak(behandling.id, ResultatType.OPPHØRT)
            every { barnService.finnBarnPåBehandling(any()) } returns emptyList()
            every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns
                GrunnlagsdataMedMetadata(opprettGrunnlagsdata(), LocalDateTime.now())
            every { vilkårsvurderingRepository.findByBehandlingId(any()) } returns mockk(relaxed = true)

            every { tilkjentYtelseService.hentForBehandling(saksbehandling.id) } returns
                tilkjentYtelse(
                    behandlingId = UUID.randomUUID(),
                    personIdent = "132",
                    grunnbeløpsmåned = YearMonth.of(inneværendeÅr - 3, Month.MAY),
                )

            mockkObject(DatoUtil)
            every { DatoUtil.dagensDatoMedTid() } returns LocalDateTime.of(inneværendeÅr, 1, 1, 0, 0)

            iverksettingDtoMapper.tilDto(opphørtSaksbehandling, "bes")

            unmockkObject(DatoUtil)
        }
    }

    private fun saksbehandling(stønadType: StønadType = StønadType.OVERGANGSSTØNAD) =
        Saksbehandling(
            id = UUID.fromString("73144d90-d238-41d2-833b-fc719dae23cb"),
            eksternId = 1,
            forrigeBehandlingId = UUID.fromString("73144d90-d238-41d2-833b-fc719dae23cc"),
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            status = BehandlingStatus.OPPRETTET,
            steg = StegType.VILKÅR,
            kategori = BehandlingKategori.NASJONAL,
            årsak = BehandlingÅrsak.SØKNAD,
            kravMottatt = LocalDate.of(2022, 3, 1),
            resultat = BehandlingResultat.IKKE_SATT,
            henlagtÅrsak = HenlagtÅrsak.FEILREGISTRERT,
            ident = "3",
            fagsakId = UUID.fromString("65811679-17ed-4c3c-b1ab-c1678acdfa7b"),
            eksternFagsakId = 4,
            stønadstype = stønadType,
            migrert = false,
            opprettetAv = "z094239",
            opprettetTid = LocalDateTime.parse("2022-03-02T05:36:39.553"),
            endretTid = LocalDateTime.parse("2022-03-03T05:36:39.556"),
            vedtakstidspunkt = LocalDateTime.parse("2022-03-03T05:36:39.556"),
        )

    private val behandlingBarnJson =
        """
        {
            "id": "73144d90-d238-41d2-833b-fc719dae23cd",
            "behandlingId": "73144d90-d238-41d2-833b-fc719dae23cb",
            "søknadBarnId": "73144d90-d238-41d2-833b-fc719dae23ce",
            "personIdent": "123",
            "navn": "fornavn etternavn",
            "fødselTermindato": "2022-03-25"
        }
        """.trimIndent()

    private val vilkårsvurderingJson =
        """
        {
            "id": "73144d90-d238-41d2-833b-fc719dae23aa",
            "behandlingId": "73144d90-d238-41d2-833b-fc719dae23cb",
            "resultat": "OPPFYLT",
            "type": "FORUTGÅENDE_MEDLEMSKAP",
            "barnId": "73144d90-d238-41d2-833b-fc719dae23ab",
            "delvilkårsvurdering": {
                "delvilkårsvurderinger": [{
                    "resultat": "IKKE_TATT_STILLING_TIL",
                    "vurderinger": [{
                        "regelId": "MEDLEMSKAP_UNNTAK",
                        "svar": "JA",
                        "begrunnelse": "begrunnelse"
                    }]
                }]
            }
        }
        """.trimIndent()

    private val brevmottakereJson =
        """
        {
            "behandlingId": "73144d90-d238-41d2-833b-fc719dae23cb",
            "personer": {
                "personer": [{
                    "personIdent": "personIdent",
                    "navn": "fornavn etternavn",
                    "mottakerRolle": "BRUKER"
                }]
            },
            "organisasjoner": {
                "organisasjoner": [{
                    "organisasjonsnummer": "organisasjonsnummer",
                    "navnHosOrganisasjon": "organisasjonsnavn",
                    "mottakerRolle": "BRUKER"
                }]
            }
        }
        """.trimIndent()

    private val vedtakSkolepengerJson = """
        {
           "skoleårsperioder": [
             {
               "perioder": [
                 {
                   "studietype": "HØGSKOLE_UNIVERSITET",
                   "datoFra": "2021-03-01",
                   "datoTil": "2021-03-31",
                   "studiebelastning": 50
                 }
               ],
               "utgiftsperioder": [
                 {
                   "id": "61516ce4-ae65-456b-a4d0-751dd3451bb6",
                   "utgiftsdato": "2021-02-01",
                   "utgifter": 200,
                   "stønad": 150
                 }
               ]
             }
           ]
          }
    """

    private val vedtakJson =
        """
        {
            "behandlingId": "73144d90-d238-41d2-833b-fc719dae23cb",
            "resultatType": "INNVILGE",
            "periodeBegrunnelse": "periodeBegrunnelse",
            "inntektBegrunnelse": "inntektBegrunnelse",
            "avslåBegrunnelse": "avslåBegrunnelse",
            "avslåÅrsak": "VILKÅR_IKKE_OPPFYLT",
            "perioder": {
                "perioder": [{
                    "datoFra": "2022-03-27",
                    "datoTil": "2022-03-28",
                    "aktivitet": "BARN_UNDER_ETT_ÅR",
                    "periodeType": "HOVEDPERIODE"
                }] 
            },
            "barnetilsyn": {
                "perioder": [{
                    "datoFra": "2022-03-27",
                    "datoTil": "2022-03-28",
                    "utgifter": 10,
                    "barn": ["d1d105bb-a573-4870-932e-21def0226cfa"],
                    "periodetype": "ORDINÆR"
                }] 
            },
            "kontantstøtte": {
                "perioder": [{
                    "datoFra": "2021-03-27",
                    "datoTil": "2021-03-28",
                    "beløp": 1
                }] 
            },
            "tilleggsstønad": {
                "perioder": [{
                    "datoFra": "2021-04-27",
                    "datoTil": "2021-04-28",
                    "beløp": 2
                }] 
            },
            "skolepenger": $vedtakSkolepengerJson,
            "samordningsfradragType": "UFØRETRYGD",
            "saksbehandlerIdent": "saksbehandlerIdent",
            "opphørFom": "2022-03",
            "beslutterIdent": "beslutter",
            "sanksjonsårsak": "SAGT_OPP_STILLING",
            "internBegrunnelse": "internBegrunnelse"
          }
        """.trimIndent()

    private val tilbakekrevingJson =
        """
        {
            "behandlingId": "73144d90-d238-41d2-833b-fc719dae23cb",
            "valg": "OPPRETT_MED_VARSEL",
            "varseltekst": "varseltekst",
            "begrunnelse": "begrunnelse"
        }
        """.trimIndent()

    private val simuleringsoppsummeringJson =
        """
        {
            "etterbetaling": "100.0",
            "feilutbetaling": "1000.0",
            "fom": "2022-03-30",
            "perioder": [{
                "feilutbetaling": "1000.0",
                "fom": "2022-03-30",
                "forfallsdato": "2022-04-01",
                "nyttBeløp": "2000.0",
                "resultat": "3000.0",
                "tidligereUtbetalt": "4000.0",
                "tom": "2022-03-31"
            }]
        }
        """.trimIndent()

    private val tilkjentYtelseJson =
        """
        {
            "id": "73144d90-d238-41d2-833b-fc719dae23cf",
            "behandlingId": "73144d90-d238-41d2-833b-fc719dae23cb",
            "personident": "personIdent",
            "startdato": "2022-04-07",
            "andelerTilkjentYtelse": [{
                "beløp": 200,
                "stønadFom": "2022-04-02",
                "stønadTom": "2022-04-03",
                "personIdent": "personIdent",
                "inntekt": 300000,
                "inntektsreduksjon": "3000",
                "samordningsfradrag": "1000",
                "kildeBehandlingId": "73144d90-d238-41d2-833b-fc719dae23af"
            }]
        }
        """.trimIndent()
}
