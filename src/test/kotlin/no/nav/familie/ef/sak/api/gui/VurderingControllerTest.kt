package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.journalføring.dto.UstrukturertDokumentasjonType
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataRepository
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Grunnlagsdata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Koordinater
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Vegadresse
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.ef.sak.vilkår.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.OppdaterVilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.SvarPåVurderingerDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.VurderingDto
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.iverksett.BehandlingKategori
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

internal class VurderingControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var grunnlagsdataService: GrunnlagsdataService

    @Autowired
    lateinit var grunnlagsdataRepository: GrunnlagsdataRepository

    @Autowired
    lateinit var søknadService: SøknadService

    @Autowired
    lateinit var barnService: BarnService

    @Autowired
    lateinit var vurderingService: VurderingService

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Henter vilkår og sjekker initiering av nære boforhold`() {
        val respons: ResponseEntity<Ressurs<VilkårDto>> = opprettVilkår()

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(respons.body?.data).isNotNull

        val næreBoforholdVurderingDto =
            respons.body?.data?.vurderinger?.first { it.vilkårType == VilkårType.ALENEOMSORG }
                ?.delvilkårsvurderinger?.first { it.vurderinger.first().regelId == RegelId.NÆRE_BOFORHOLD }?.vurderinger?.first()

        assertThat(næreBoforholdVurderingDto?.svar).isEqualTo(SvarId.NEI)
        assertThat(næreBoforholdVurderingDto?.begrunnelse).contains("annen forelder bor mer enn 1 km unna bruker")
    }

    @Test
    internal fun `Vilkår og grunnlagsdata skal oppdateres dersom adressen til en av de involverte er endret`() {
        // Opprett behandling og inngangsvilkår
        val behandling = opprettBehandlingMedGrunnlagsdata()
        val vurderinger = hentVilkår(behandling).body?.data?.vurderinger ?: error("Mangler vurderinger")

        // Endre grunnlagsdata - slik at dette nå er forskjellig fra ef-sak og pdl
        val grunnlagsdata = grunnlagsdataRepository.findByIdOrThrow(behandling.id)
        val endretGrunnlagsdata = endreVegadresseForGrunnlagsdata(grunnlagsdata)
        grunnlagsdataRepository.update(endretGrunnlagsdata)

        // Hent inngangsvilkår - skal nå oppdatere grunnlagsdataene og nullstille vilkårene
        val oppdaterteVurderinger = hentVilkår(behandling).body?.data?.vurderinger ?: error("Mangler nye vurderinger")
        val oppdatertGrunnlagsdata = grunnlagsdataRepository.findByIdOrThrow(behandling.id)

        assertThat(oppdatertGrunnlagsdata.data).isNotEqualTo(endretGrunnlagsdata.data)
        assertThat(oppdatertGrunnlagsdata.data).isEqualTo(grunnlagsdata.data)
        assertThat(vurderinger).isNotEqualTo(oppdaterteVurderinger)
    }

    private fun endreVegadresseForGrunnlagsdata(grunnlagsdata: Grunnlagsdata) =
        grunnlagsdata.copy(
            data =
            grunnlagsdata.data.copy(
                søker =
                grunnlagsdata.data.søker.copy(
                    bostedsadresse =
                    listOf(
                        grunnlagsdata.data.søker.bostedsadresse.first().copy(
                            vegadresse = nyVegadresse(),
                        ),
                    ),
                ),
            ),
        )

    private fun nyVegadresse() =
        Vegadresse(
            husnummer = "13",
            husbokstav = "b",
            adressenavn = "Viktors vei",
            kommunenummer = "0301",
            postnummer = "0575",
            bruksenhetsnummer = "",
            tilleggsnavn = null,
            koordinater = Koordinater(x = 601371f, y = 6629367f, z = null, kvalitet = null),
            matrikkelId = 0,
        )

    @Test
    internal fun `oppdaterVilkår - skal sjekke att behandlingId som blir sendt inn er lik den som finnes i vilkårsvurderingen`() {
        val opprettetVurdering = opprettVilkår().body?.data!!
        val fagsak = fagsakService.hentEllerOpprettFagsakMedBehandlinger("0", StønadType.OVERGANGSSTØNAD)
        val behandlingÅrsak = BehandlingÅrsak.SØKNAD
        val behandling =
            behandlingService.opprettBehandling(
                BehandlingType.FØRSTEGANGSBEHANDLING,
                fagsak.id,
                behandlingsårsak = behandlingÅrsak,
            )

        val oppdaterVilkårsvurdering =
            lagOppdaterVilkårsvurdering(opprettetVurdering, VilkårType.FORUTGÅENDE_MEDLEMSKAP)
                .copy(behandlingId = behandling.id)
        validerSjekkPåBehandlingId(oppdaterVilkårsvurdering, "vilkar")
    }

    @Test
    internal fun `nullstillVilkår skal sjekke att behandlingId som blir sendt inn er lik den som finnes i vilkårsvurderingen`() {
        val opprettetVurdering = opprettVilkår().body?.data!!

        val fagsak = fagsakService.hentEllerOpprettFagsakMedBehandlinger("0", StønadType.OVERGANGSSTØNAD)
        val behandlingÅrsak = BehandlingÅrsak.SØKNAD
        val behandling =
            behandlingService.opprettBehandling(
                BehandlingType.FØRSTEGANGSBEHANDLING,
                fagsak.id,
                behandlingsårsak = behandlingÅrsak,
            )
        val nullstillVurdering = OppdaterVilkårsvurderingDto(opprettetVurdering.vurderinger.first().id, behandling.id)

        validerSjekkPåBehandlingId(nullstillVurdering, "nullstill")
    }

    private fun validerSjekkPåBehandlingId(
        request: Any,
        path: String,
    ) {
        val respons: ResponseEntity<Ressurs<VilkårsvurderingDto>> =
            restTemplate.exchange(
                localhost("/api/vurdering/$path"),
                HttpMethod.POST,
                HttpEntity(request, headers),
            )

        assertThat(respons.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(respons.body!!.frontendFeilmelding).isEqualTo("BehandlingId er feil, her har noe gått galt")
    }

    @Test
    internal fun `skal oppdatere vurderingen for FORUTGÅENDE_MEDLEMSKAP som har ett spørsmål som vi setter til JA`() {
        val opprettetVurdering = opprettVilkår().body?.data!!
        val oppdatertVilkårsvarMedJa =
            lagOppdaterVilkårsvurdering(opprettetVurdering, VilkårType.FORUTGÅENDE_MEDLEMSKAP)
        val respons: ResponseEntity<Ressurs<VilkårsvurderingDto>> =
            restTemplate.exchange(
                localhost("/api/vurdering/vilkar"),
                HttpMethod.POST,
                HttpEntity(oppdatertVilkårsvarMedJa, headers),
            )

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(respons.body?.data?.id).isEqualTo(oppdatertVilkårsvarMedJa.id)
    }

    @Test
    internal fun `skal oppdatere vurderingen for FORUTGÅENDE_MEDLEMSKAP til EØS slik at behandlingen får EØS-kategori`() {
        val opprettetVurdering = opprettVilkår().body?.data!!
        val behandlingId = opprettetVurdering.vurderinger.first().behandlingId
        val oppdatertVilkår =
            opprettetVurdering.vurderinger.first { it.vilkårType == VilkårType.FORUTGÅENDE_MEDLEMSKAP }.let {
                svarPåVurderingerDtoForEøsMedlemskap(it)
            }
        assertThat(behandlingService.hentBehandling(behandlingId).kategori).isEqualTo(BehandlingKategori.NASJONAL)
        val respons: ResponseEntity<Ressurs<VilkårsvurderingDto>> =
            restTemplate.exchange(
                localhost("/api/vurdering/vilkar"),
                HttpMethod.POST,
                HttpEntity(oppdatertVilkår, headers),
            )

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(respons.body?.data?.id).isEqualTo(oppdatertVilkår.id)
        assertThat(behandlingService.hentBehandling(behandlingId).kategori).isEqualTo(BehandlingKategori.EØS)
    }

    @Test
    internal fun `skal nullstille vurderingen for TIDLIGERE VEDTAKSPERIODER og initiere delvilkårsvurderingene med riktig resultattype`() {
        val opprettetVurdering = opprettVilkår().body?.data!!
        val oppdatertVilkårsvarMedJa =
            OppdaterVilkårsvurderingDto(
                opprettetVurdering.vurderinger.first { it.vilkårType == VilkårType.TIDLIGERE_VEDTAKSPERIODER }.id,
                opprettetVurdering.vurderinger.first().behandlingId,
            )
        val respons: ResponseEntity<Ressurs<VilkårsvurderingDto>> =
            restTemplate.exchange(
                localhost("/api/vurdering/nullstill"),
                HttpMethod.POST,
                HttpEntity(oppdatertVilkårsvarMedJa, headers),
            )

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(
            respons.body?.data?.delvilkårsvurderinger?.first {
                it.vurderinger.first().regelId == RegelId.HAR_TIDLIGERE_ANDRE_STØNADER_SOM_HAR_BETYDNING
            }?.resultat,
        ).isEqualTo(
            Vilkårsresultat.IKKE_TATT_STILLING_TIL,
        )
        assertThat(
            respons.body?.data?.delvilkårsvurderinger?.first {
                it.vurderinger.first().regelId == RegelId.HAR_TIDLIGERE_MOTTATT_OVERGANSSTØNAD
            }?.resultat,
        ).isEqualTo(
            Vilkårsresultat.OPPFYLT,
        )
        assertThat(respons.body?.data?.id).isEqualTo(oppdatertVilkårsvarMedJa.id)
    }

    private fun lagOppdaterVilkårsvurdering(
        opprettetVurdering: VilkårDto,
        vilkårType: VilkårType,
    ): SvarPåVurderingerDto {
        return opprettetVurdering.vurderinger.first { it.vilkårType == vilkårType }.let {
            lagOppdaterVilkårsvurderingMedSvarJa(it)
        }
    }

    private fun lagOppdaterVilkårsvurderingMedSvarJa(it: VilkårsvurderingDto) =
        SvarPåVurderingerDto(
            id = it.id,
            behandlingId = it.behandlingId,
            delvilkårsvurderinger =
            it.delvilkårsvurderinger.map {
                it.copy(
                    vurderinger =
                    it.vurderinger.map { vurderingDto ->
                        vurderingDto.copy(svar = SvarId.JA, begrunnelse = "En begrunnelse")
                    },
                )
            },
        )

    private fun opprettVilkår(): ResponseEntity<Ressurs<VilkårDto>> {
        val behandling = opprettBehandlingMedGrunnlagsdata()
        return hentVilkår(behandling)
    }

    private fun hentVilkår(behandling: Behandling): ResponseEntity<Ressurs<VilkårDto>> =
        restTemplate.exchange(
            localhost("/api/vurdering/${behandling.id}/vilkar"),
            HttpMethod.GET,
            HttpEntity<Any>(headers),
        )

    private fun opprettBehandlingMedGrunnlagsdata(): Behandling {
        val søknad =
            TestsøknadBuilder.Builder()
                .setBarn(
                    listOf(
                        TestsøknadBuilder.Builder().defaultBarn("Navn navnesen", "14041385481", harSkalHaSammeAdresse = false),
                        TestsøknadBuilder.Builder().defaultBarn("Navn navnesen", "01012067050", harSkalHaSammeAdresse = false),
                    ),
                )
                .setPersonalia("Navn på forsørger", "01010172272")
                .build().søknadOvergangsstønad

        // val søknad = SøknadMedVedlegg(Testsøknad.søknadOvergangsstønad, emptyList())
        val fagsak =
            fagsakService.hentEllerOpprettFagsakMedBehandlinger(
                søknad.personalia.verdi.fødselsnummer.verdi.verdi,
                StønadType.OVERGANGSSTØNAD,
            )
        val behandlingÅrsak = BehandlingÅrsak.SØKNAD
        val behandling =
            behandlingService.opprettBehandling(
                BehandlingType.FØRSTEGANGSBEHANDLING,
                fagsak.id,
                behandlingsårsak = behandlingÅrsak,
            )
        søknadService.lagreSøknadForOvergangsstønad(søknad, behandling.id, fagsak.id, "1234")
        val grunnlagsdata = grunnlagsdataService.opprettGrunnlagsdata(behandling.id)
        barnService.opprettBarnPåBehandlingMedSøknadsdata(
            behandlingId = behandling.id,
            fagsakId = behandling.fagsakId,
            grunnlagsdataBarn = grunnlagsdata.grunnlagsdata.barn,
            stønadstype = StønadType.OVERGANGSSTØNAD,
            ustrukturertDokumentasjonType = UstrukturertDokumentasjonType.IKKE_VALGT,
            barnSomSkalFødes = listOf(),
            vilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE,
        )
        return behandling
    }

    private fun svarPåVurderingerDtoForEøsMedlemskap(it: VilkårsvurderingDto) =
        SvarPåVurderingerDto(
            id = it.id,
            behandlingId = it.behandlingId,
            delvilkårsvurderinger =
            listOf(
                DelvilkårsvurderingDto(
                    Vilkårsresultat.IKKE_OPPFYLT,
                    listOf(
                        VurderingDto(
                            RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN,
                            SvarId.NEI,
                        ),
                        VurderingDto(
                            RegelId.MEDLEMSKAP_UNNTAK,
                            SvarId.MEDLEM_MER_ENN_5_ÅR_EØS,
                            "a",
                        ),
                    ),
                ),
            ),
        )
}
