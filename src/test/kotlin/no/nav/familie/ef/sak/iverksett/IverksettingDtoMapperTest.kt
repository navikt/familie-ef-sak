package no.nav.familie.ef.sak.iverksett

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.brev.BrevmottakereRepository
import no.nav.familie.ef.sak.brev.domain.MottakerRolle
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.opprettGrunnlagsdata
import no.nav.familie.ef.sak.opplysninger.mapper.BarnMatcher
import no.nav.familie.ef.sak.opplysninger.mapper.MatchetBehandlingBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.repository.barnMedIdent
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.repository.søker
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekreving
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.felles.Vedtaksresultat
import no.nav.familie.kontrakter.ef.iverksett.Brevmottaker
import no.nav.familie.kontrakter.ef.iverksett.IverksettDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class IverksettingDtoMapperTest {

    private val tilbakekrevingService = mockk<TilbakekrevingService>(relaxed = true)
    private val simuleringService = mockk<SimuleringService>()
    private val fagsakService = mockk<FagsakService>()
    private val vedtakService = mockk<VedtakService>()
    private val behandlingshistorikkService = mockk<BehandlingshistorikkService>()
    private val barnService = mockk<BarnService>()
    private val grunnlagsdataService = mockk<GrunnlagsdataService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val brevmottakereRepository = mockk<BrevmottakereRepository>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>(relaxed = true)
    private val barnMatcher = mockk<BarnMatcher>()

    private val iverksettingDtoMapper =
            IverksettingDtoMapper(arbeidsfordelingService = arbeidsfordelingService,
                                  behandlingshistorikkService = behandlingshistorikkService,
                                  fagsakService = fagsakService,
                                  grunnlagsdataService = grunnlagsdataService,
                                  simuleringService = simuleringService,
                                  barnService = barnService,
                                  tilbakekrevingService = tilbakekrevingService,
                                  tilkjentYtelseService = tilkjentYtelseService,
                                  vedtakService = vedtakService,
                                  vilkårsvurderingRepository = mockk(relaxed = true),
                                  brevmottakereRepository = brevmottakereRepository)

    private val fagsak = fagsak(fagsakpersoner(setOf("1")))
    private val behandling = behandling(fagsak)
    private val saksbehandling = saksbehandling(fagsak, behandling)

    @BeforeEach
    internal fun setUp() {
        every { fagsakService.hentFagsakForBehandling(behandling.id) } returns fagsak
        every { vedtakService.hentVedtak(behandling.id) } returns Vedtak(behandling.id, ResultatType.INNVILGE)
        val behandlingshistorikk =
                Behandlingshistorikk(behandlingId = behandling.id, opprettetAv = "", steg = StegType.SEND_TIL_BESLUTTER)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any()) } returns behandlingshistorikk
        every { brevmottakereRepository.findByIdOrNull(any()) } returns null
    }

    @Test
    internal fun `Skal mappe tilbakekreving med varseltekst og feilutbetaling`() {
        val forventetVarseltekst = "forventetVarseltekst"
        val simuleringsoppsummering = Simuleringsoppsummering(
                perioder = emptyList(),
                fomDatoNestePeriode = null,
                etterbetaling = BigDecimal.ZERO,
                feilutbetaling = BigDecimal.TEN,
                fom = null,
                tomDatoNestePeriode = null,
                forfallsdatoNestePeriode = null,
                tidSimuleringHentet = null,
                tomSisteUtbetaling = null
        )

        every {
            tilbakekrevingService.hentTilbakekreving(behandlingId = behandling.id)
        } returns Tilbakekreving(behandlingId = behandling.id,
                                 valg = Tilbakekrevingsvalg.OPPRETT_MED_VARSEL,
                                 varseltekst = forventetVarseltekst,
                                 begrunnelse = "ingen")
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
        every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns GrunnlagsdataMedMetadata(opprettGrunnlagsdata(),
                                                                                                 false,
                                                                                                 LocalDateTime.now())
        every { tilkjentYtelseService.hentForBehandling(any()) } returns mockk(relaxed = true)
        iverksettingDtoMapper.tilDto(saksbehandling, "bes")

        verify(exactly = 1) { grunnlagsdataService.hentGrunnlagsdata(any()) }
        verify(exactly = 1) { arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(any()) }
    }

    @Test
    internal fun `map fra Saksbehandling til IverksettDto - sjekk alle felter`() {

        val behandlingId = mockReturnerObjekterMedAlleFelterFylt()

        val saksbehandlingFraJson = objectMapper.readValue<Saksbehandling>(saksbehandlingJson)
        val iverksettDto = iverksettingDtoMapper.tilDto(saksbehandlingFraJson, "beslutter")

        assertAlleFelterIverksettDto(iverksettDto, behandlingId)
    }

    private fun assertAlleFelterIverksettDto(iverksettDto: IverksettDto, behandlingId: UUID?) {
        assertThat(iverksettDto.behandling.behandlingId).isEqualTo(behandlingId)
        assertThat(iverksettDto.behandling.behandlingType.name).isEqualTo(BehandlingType.FØRSTEGANGSBEHANDLING.name)
        assertThat(iverksettDto.behandling.behandlingÅrsak).isEqualTo(BehandlingÅrsak.SØKNAD)
        assertThat(iverksettDto.behandling.forrigeBehandlingId).isEqualTo(UUID.fromString("73144d90-d238-41d2-833b-fc719dae23cc"))
        assertThat(iverksettDto.behandling.eksternId).isEqualTo(1)
        assertThat(iverksettDto.behandling.aktivitetspliktInntrefferDato).isNull() // Ikke i bruk?
        assertThat(iverksettDto.behandling.kravMottatt).isEqualTo(LocalDate.of(2022, 3, 1))
        assertThat(iverksettDto.behandling.vilkårsvurderinger.size).isEqualTo(0) //Burde ha verdi i test

        assertThat(iverksettDto.fagsak.eksternId).isEqualTo(4)
        assertThat(iverksettDto.fagsak.fagsakId).isEqualTo(UUID.fromString("65811679-17ed-4c3c-b1ab-c1678acdfa7b"))
        assertThat(iverksettDto.fagsak.stønadstype).isEqualTo(StønadType.OVERGANGSSTØNAD)

        assertThat(iverksettDto.søker.personIdent).isEqualTo("3")
        assertThat(iverksettDto.søker.barn.size).isEqualTo(1)
        assertThat(iverksettDto.søker.barn.first().personIdent).isEqualTo("123")
        assertThat(iverksettDto.søker.barn.first().termindato).isEqualTo(LocalDate.of(2022, 3, 25))
        assertThat(iverksettDto.søker.tilhørendeEnhet).isEqualTo("4489")
        assertThat(iverksettDto.søker.adressebeskyttelse?.name).isEqualTo(ADRESSEBESKYTTELSEGRADERING.UGRADERT.name)

        assertThat(iverksettDto.vedtak.beslutterId).isEqualTo("beslutter")
        assertThat(iverksettDto.vedtak.brevmottakere.size).isEqualTo(2)
        assertThat(iverksettDto.vedtak.brevmottakere[0].ident).isEqualTo("personIdent")
        assertThat(iverksettDto.vedtak.brevmottakere[0].navn).isEqualTo("fornavn etternavn")
        assertThat(iverksettDto.vedtak.brevmottakere[0].identType.name).isEqualTo(Brevmottaker.IdentType.PERSONIDENT.name)
        assertThat(iverksettDto.vedtak.brevmottakere[0].mottakerRolle.name).isEqualTo(MottakerRolle.BRUKER.name)

        assertThat(iverksettDto.vedtak.brevmottakere[1].ident).isEqualTo("organisasjonsnummer")
        assertThat(iverksettDto.vedtak.brevmottakere[1].navn).isEqualTo("organisasjonsnavn")
        assertThat(iverksettDto.vedtak.brevmottakere[1].identType.name).isEqualTo(Brevmottaker.IdentType.ORGANISASJONSNUMMER.name)
        assertThat(iverksettDto.vedtak.brevmottakere[1].mottakerRolle.name).isEqualTo(MottakerRolle.BRUKER.name)

        assertThat(iverksettDto.vedtak.opphørÅrsak).isNull() // Burde ha verdi i test?
        assertThat(iverksettDto.vedtak.resultat).isEqualTo(Vedtaksresultat.INNVILGET)
        assertThat(iverksettDto.vedtak.saksbehandlerId).isEqualTo("") // Burde ha verdi i test

        assertThat(iverksettDto.vedtak.tilbakekreving?.tilbakekrevingMedVarsel?.perioder?.size).isEqualTo(1)
        assertThat(iverksettDto.vedtak.tilbakekreving?.tilbakekrevingMedVarsel?.perioder?.first()?.fom).isEqualTo(LocalDate.of(
                2022,
                3,
                30))
        assertThat(iverksettDto.vedtak.tilbakekreving?.tilbakekrevingMedVarsel?.perioder?.first()?.tom).isEqualTo(LocalDate.of(
                2022,
                3,
                31))
        assertThat(iverksettDto.vedtak.tilbakekreving?.tilbakekrevingMedVarsel?.varseltekst).isEqualTo("varseltekst")
        assertThat(iverksettDto.vedtak.tilbakekreving?.tilbakekrevingMedVarsel?.sumFeilutbetaling).isEqualTo(BigDecimal("1000.0"))
        assertThat(iverksettDto.vedtak.tilbakekreving?.tilbakekrevingsvalg?.name).isEqualTo(no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL.name)


        assertThat(iverksettDto.vedtak.tilkjentYtelse?.startdato).isEqualTo(LocalDate.of(2022, 4, 7))
        //opphørsdato ikke i bruk?

        assertThat(iverksettDto.vedtak.tilkjentYtelse?.andelerTilkjentYtelse?.size).isEqualTo(1)

        assertThat(iverksettDto.vedtak.vedtaksperioder.size).isEqualTo(1)
        assertThat(iverksettDto.vedtak.vedtaksperioder.first().fraOgMed).isEqualTo(LocalDate.of(2022, 3, 27))
        assertThat(iverksettDto.vedtak.vedtaksperioder.first().tilOgMed).isEqualTo(LocalDate.of(2022, 3, 28))
        assertThat(iverksettDto.vedtak.vedtaksperioder.first().aktivitet.name).isEqualTo(AktivitetType.BARN_UNDER_ETT_ÅR.name)
        assertThat(iverksettDto.vedtak.vedtaksperioder.first().periodeType.name).isEqualTo(VedtaksperiodeType.HOVEDPERIODE.name)
        //assertThat(iverksettDto.vedtak.vedtakstidspunkt) - sjekker ikke denne da det er LocalDate.now()
    }

    private fun mockReturnerObjekterMedAlleFelterFylt(): UUID? {
        val grunnlagsdata =
                opprettGrunnlagsdata().copy(søker = søker(), barn = listOf(barnMedIdent(fnr = "123", navn = "fornavn etternavn")))
        every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns GrunnlagsdataMedMetadata(grunnlagsdata,
                                                                                                 false,
                                                                                                 LocalDateTime.parse("2022-03-25T05:51:31.439"))
        every { arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(any()) } returns "4489"
        val behandlingId = UUID.fromString("73144d90-d238-41d2-833b-fc719dae23cb")

        val behandlingBarn = objectMapper.readValue<BehandlingBarn>(behandlingBarnJson)
        every { barnService.finnBarnPåBehandling(any()) } returns listOf(behandlingBarn)
        every { barnMatcher.kobleBehandlingBarnOgRegisterBarn(any(), any()) } returns listOf(
                MatchetBehandlingBarn(
                        fødselsnummer = "1234",
                        barn = BarnMedIdent(
                                listOf(),
                                listOf(),
                                listOf(),
                                listOf(),
                                listOf(),
                                listOf(),
                                Navn("fornavn", null, "etternavn", Metadata(false)),
                                "123"
                        ),
                        behandlingBarn = behandlingBarn
                )
        )

        every { vedtakService.hentVedtak(any()) } returns objectMapper.readValue(vedtakJson)
        every { brevmottakereRepository.findByIdOrNull(any()) } returns objectMapper.readValue(brevmottakereJson)
        every { tilbakekrevingService.hentTilbakekreving(any()) } returns objectMapper.readValue(tilbakekrevingJson)
        every { simuleringService.hentLagretSimuleringsoppsummering(any()) } returns objectMapper.readValue(
                simuleringsoppsummeringJson)
        every { tilkjentYtelseService.hentForBehandling(any()) } returns objectMapper.readValue(tilkjentYtelseJson)
        return behandlingId
    }


    private val saksbehandlingJson = """
        {
          "id": "73144d90-d238-41d2-833b-fc719dae23cb",
          "eksternId": 1,
          "forrigeBehandlingId": "73144d90-d238-41d2-833b-fc719dae23cc",
          "type": "FØRSTEGANGSBEHANDLING",
          "status": "OPPRETTET",
          "steg": "VILKÅR",
          "årsak": "SØKNAD",
          "kravMottatt": "2022-03-01T05:36:39.553",
          "resultat": "IKKE_SATT",
          "henlagtÅrsak": "FEILREGISTRERT",
          "ident": "3",
          "fagsakId": "65811679-17ed-4c3c-b1ab-c1678acdfa7b",
          "eksternFagsakId": 4,
          "stønadstype": "OVERGANGSSTØNAD",
          "migrert": false,
          "opprettetTid": "2022-03-02T05:36:39.553",
          "endretTid": "2022-03-03T05:36:39.556"
        }
    """.trimIndent()

    private val behandlingBarnJson = """
        {
            "id": "73144d90-d238-41d2-833b-fc719dae23cd",
            "behandlingId": "73144d90-d238-41d2-833b-fc719dae23cb",
            "søknadBarnId": "73144d90-d238-41d2-833b-fc719dae23ce",
            "personIdent": "123",
            "navn": "fornavn etternavn",
            "fødselTermindato": "2022-03-25"
        }
    """.trimIndent()

    private val brevmottakereJson = """
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

    private val vedtakJson = """
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
            "samordningsfradragType": "UFØRETRYGD",
            "saksbehandlerIdent": "saksbehandlerIdent",
            "opphørFom": "2022-03-27",
            "beslutterIdent": "beslutter",
            "sanksjonsårsak": "SAGT_OPP_STILLING",
            "internBegrunnelse": "internBegrunnelse"
          }
    """.trimIndent()

    private val tilbakekrevingJson = """
        {
            "behandlingId": "73144d90-d238-41d2-833b-fc719dae23cb",
            "valg": "OPPRETT_MED_VARSEL",
            "varseltekst": "varseltekst",
            "begrunnelse": "begrunnelse"
        }
    """.trimIndent()

    private val simuleringsoppsummeringJson = """
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

    private val tilkjentYtelseJson = """
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

