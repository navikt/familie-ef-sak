package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.TestBehandlingsType.BARNETILSYN
import no.nav.familie.ef.sak.behandling.TestBehandlingsType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ef.sak.behandling.TestBehandlingsType.MIGRERING
import no.nav.familie.ef.sak.behandling.TestBehandlingsType.SKOLEPENGER
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.migrering.MigreringService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.iverksett.IverksettService
import no.nav.familie.ef.sak.journalføring.JournalpostClient
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.SøkerMedBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.vilkår.VilkårType.AKTIVITET
import no.nav.familie.ef.sak.vilkår.VilkårType.AKTIVITET_ARBEID
import no.nav.familie.ef.sak.vilkår.VilkårType.ALDER_PÅ_BARN
import no.nav.familie.ef.sak.vilkår.VilkårType.ALENEOMSORG
import no.nav.familie.ef.sak.vilkår.VilkårType.DOKUMENTASJON_AV_UTDANNING
import no.nav.familie.ef.sak.vilkår.VilkårType.DOKUMENTASJON_TILSYNSUTGIFTER
import no.nav.familie.ef.sak.vilkår.VilkårType.ER_UTDANNING_HENSIKTSMESSIG
import no.nav.familie.ef.sak.vilkår.VilkårType.FORUTGÅENDE_MEDLEMSKAP
import no.nav.familie.ef.sak.vilkår.VilkårType.INNTEKT
import no.nav.familie.ef.sak.vilkår.VilkårType.LOVLIG_OPPHOLD
import no.nav.familie.ef.sak.vilkår.VilkårType.MOR_ELLER_FAR
import no.nav.familie.ef.sak.vilkår.VilkårType.NYTT_BARN_SAMME_PARTNER
import no.nav.familie.ef.sak.vilkår.VilkårType.RETT_TIL_OVERGANGSSTØNAD
import no.nav.familie.ef.sak.vilkår.VilkårType.SAGT_OPP_ELLER_REDUSERT
import no.nav.familie.ef.sak.vilkår.VilkårType.SAMLIV
import no.nav.familie.ef.sak.vilkår.VilkårType.SIVILSTAND
import no.nav.familie.ef.sak.vilkår.VilkårType.TIDLIGERE_VEDTAKSPERIODER
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.ef.sak.vilkår.VurderingStegService
import no.nav.familie.ef.sak.vilkår.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.SvarPåVurderingerDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.VurderingDto
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.søknad.Barn
import no.nav.familie.kontrakter.ef.søknad.EnumTekstverdiMedSvarId
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@RestController
@RequestMapping("/api/test")
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class TestSaksbehandlingController(
    private val fagsakService: FagsakService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val iverksettService: IverksettService,
    private val behandlingService: BehandlingService,
    private val søknadService: SøknadService,
    private val personService: PersonService,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val barnService: BarnService,
    private val taskRepository: TaskRepository,
    private val oppgaveService: OppgaveService,
    private val journalpostClient: JournalpostClient,
    private val migreringService: MigreringService,
    private val vurderingService: VurderingService,
    private val vurderingStegService: VurderingStegService
) {

    @PostMapping("{behandlingId}/utfyll-vilkar")
    fun utfyllVilkår(@PathVariable behandlingId: UUID): Ressurs<UUID> {
        val vurderinger = vurderingService.hentAlleVurderinger(behandlingId)
        vurderinger.filter { it.vilkårType != SIVILSTAND }.forEach { vurdering ->
            val delvilkårsvurderinger = lagDelvilkår(vurdering)
            vurderingStegService.oppdaterVilkår(SvarPåVurderingerDto(vurdering.id, behandlingId, delvilkårsvurderinger))
        }
        return Ressurs.success(behandlingId)
    }

    private fun lagDelvilkår(vurdering: VilkårsvurderingDto): List<DelvilkårsvurderingDto> {
        return when (vurdering.vilkårType) {
            FORUTGÅENDE_MEDLEMSKAP -> listOf(jaMedBegrunnelse(RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN))
            LOVLIG_OPPHOLD -> listOf(
                jaMedBegrunnelse(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE)
            )
            MOR_ELLER_FAR -> listOf(jaUtenBegrunnelse(RegelId.OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN))
            SIVILSTAND -> error("yolo")
            SAMLIV -> listOf(
                jaMedBegrunnelse(RegelId.LEVER_IKKE_MED_ANNEN_FORELDER),
                jaMedBegrunnelse(RegelId.LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD)
            )
            ALENEOMSORG -> listOf(
                jaUtenBegrunnelse(RegelId.SKRIFTLIG_AVTALE_OM_DELT_BOSTED),
                neiMedBegrunnelse(RegelId.NÆRE_BOFORHOLD),
                jaMedBegrunnelse(RegelId.MER_AV_DAGLIG_OMSORG)
            )
            NYTT_BARN_SAMME_PARTNER -> listOf(neiUtenBegrunnelse(RegelId.HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER))

            SAGT_OPP_ELLER_REDUSERT -> listOf(
                neiUtenBegrunnelse(RegelId.SAGT_OPP_ELLER_REDUSERT)
            )
            AKTIVITET -> listOf(
                jaMedBegrunnelse(RegelId.FYLLER_BRUKER_AKTIVITETSPLIKT)
            )
            AKTIVITET_ARBEID -> listOf(
                delvilkår(RegelId.ER_I_ARBEID_ELLER_FORBIGÅENDE_SYKDOM, SvarId.ER_I_ARBEID, "begrunnelse")
            )
            TIDLIGERE_VEDTAKSPERIODER -> listOf(
                neiUtenBegrunnelse(RegelId.HAR_TIDLIGERE_MOTTATT_OVERGANSSTØNAD),
                neiUtenBegrunnelse(RegelId.HAR_TIDLIGERE_ANDRE_STØNADER_SOM_HAR_BETYDNING)
            )
            INNTEKT -> listOf(
                jaMedBegrunnelse(RegelId.INNTEKT_LAVERE_ENN_INNTEKTSGRENSE),
                jaMedBegrunnelse(RegelId.INNTEKT_SAMSVARER_MED_OS)
            )

            ALDER_PÅ_BARN -> listOf(
                neiUtenBegrunnelse(RegelId.HAR_ALDER_LAVERE_ENN_GRENSEVERDI)
            )
            DOKUMENTASJON_TILSYNSUTGIFTER -> listOf(
                jaMedBegrunnelse(RegelId.HAR_DOKUMENTERTE_TILSYNSUTGIFTER)
            )

            RETT_TIL_OVERGANGSSTØNAD -> listOf(
                jaMedBegrunnelse(RegelId.RETT_TIL_OVERGANGSSTØNAD)
            )

            DOKUMENTASJON_AV_UTDANNING -> listOf(
                jaMedBegrunnelse(RegelId.DOKUMENTASJON_AV_UTDANNING),
                jaMedBegrunnelse(RegelId.DOKUMENTASJON_AV_UTGIFTER_UTDANNING)
            )

            ER_UTDANNING_HENSIKTSMESSIG -> listOf(
                jaMedBegrunnelse(RegelId.SAKSBEHANDLER_VURDERING)
            )

        }
    }

    private fun neiUtenBegrunnelse(regelId: RegelId) = delvilkår(regelId, SvarId.NEI)

    private fun neiMedBegrunnelse(regelId: RegelId) = delvilkår(regelId, SvarId.NEI, "begrunnelse")

    private fun jaUtenBegrunnelse(regelId: RegelId) = delvilkår(regelId, SvarId.JA)

    private fun jaMedBegrunnelse(regelId: RegelId) = delvilkår(regelId, SvarId.JA, "begrunnelse")

    private fun delvilkår(regelId: RegelId, svar: SvarId, begrunnelse: String? = null) = DelvilkårsvurderingDto(
        Vilkårsresultat.OPPFYLT,
        listOf(VurderingDto(regelId, svar, begrunnelse))
    )

    @PostMapping(path = ["fagsak"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettFagsakForTestperson(@RequestBody testFagsakRequest: TestFagsakRequest): Ressurs<UUID> {
        val personIdent = testFagsakRequest.personIdent
        val søknadBuilder = lagSøknad(personIdent)
        val fagsak =
            fagsakService.hentEllerOpprettFagsak(personIdent, testFagsakRequest.behandlingsType.tilStønadstype())

        val behandling: Behandling = when (testFagsakRequest.behandlingsType) {
            FØRSTEGANGSBEHANDLING -> lagFørstegangsbehandling(søknadBuilder.søknadOvergangsstønad, fagsak)
            MIGRERING -> lagMigreringBehandling(fagsak)
            BARNETILSYN -> lagBarnetilsynBehandling(søknadBuilder.søknadBarnetilsyn, fagsak)
            SKOLEPENGER -> lagSkolepengerBehandling(søknadBuilder.søknadSkolepenger, fagsak)
        }

        if (!behandling.erMigrering()) {
            iverksettService.startBehandling(behandling, fagsak)
            val grunnlagsdata =
                grunnlagsdataService.opprettGrunnlagsdata(behandling.id) // opprettGrunnlagsdata håndteres i migreringservice
            barnService.opprettBarnPåBehandlingMedSøknadsdata(
                behandling.id,
                fagsak.id,
                grunnlagsdata.grunnlagsdata.barn,
                fagsak.stønadstype
            )
            behandlingshistorikkService.opprettHistorikkInnslag(
                Behandlingshistorikk(
                    behandlingId = behandling.id,
                    steg = StegType.VILKÅR
                )
            )
            val oppgaveId = oppgaveService.opprettOppgave(
                behandling.id,
                Oppgavetype.BehandleSak,
                SikkerhetContext.hentSaksbehandler(true),
                "Dummy-oppgave opprettet i ny løsning"
            )
            taskRepository.save(
                taskRepository.save(
                    BehandlingsstatistikkTask.opprettMottattTask(
                        behandlingId = behandling.id,
                        oppgaveId = oppgaveId
                    )
                )
            )
        }

        return Ressurs.success(behandling.id)
    }

    private fun lagBarnetilsynBehandling(søknadBarnetilsyn: SøknadBarnetilsyn, fagsak: Fagsak): Behandling {
        val behandling = behandlingService.opprettBehandling(
            BehandlingType.FØRSTEGANGSBEHANDLING,
            fagsak.id,
            behandlingsårsak = BehandlingÅrsak.SØKNAD
        )
        val journalposter = behandlingService.hentBehandlingsjournalposter(behandling.id)
        søknadService.lagreSøknadForBarnetilsyn(
            søknadBarnetilsyn,
            behandling.id,
            fagsak.id,
            journalposter.firstOrNull()?.journalpostId ?: "TESTJPID"
        )
        return behandling
    }

    private fun lagSkolepengerBehandling(søknadSkolepenger: SøknadSkolepenger, fagsak: Fagsak): Behandling {
        val behandling = behandlingService.opprettBehandling(
            BehandlingType.FØRSTEGANGSBEHANDLING,
            fagsak.id,
            behandlingsårsak = BehandlingÅrsak.SØKNAD
        )
        val journalposter = behandlingService.hentBehandlingsjournalposter(behandling.id)
        søknadService.lagreSøknadForSkolepenger(
            søknadSkolepenger,
            behandling.id,
            fagsak.id,
            journalposter.firstOrNull()?.journalpostId ?: "TESTJPID"
        )
        return behandling
    }

    private fun lagSøknad(personIdent: String): TestsøknadBuilder {
        val søkerMedBarn = personService.hentPersonMedBarn(personIdent)
        val barneListe: List<Barn> = mapSøkersBarn(søkerMedBarn)
        return TestsøknadBuilder.Builder()
            .setPersonalia(søkerMedBarn.søker.navn.gjeldende().visningsnavn(), søkerMedBarn.søkerIdent)
            .setBarn(barneListe)
            .setBosituasjon(
                delerDuBolig =
                EnumTekstverdiMedSvarId(
                    verdi = "Nei, jeg bor alene med barn eller jeg er gravid og bor alene",
                    svarId = "borAleneMedBarnEllerGravid"
                )
            )
            .setSivilstandsplaner(
                harPlaner = true,
                fraDato = LocalDate.of(2019, 9, 17),
                vordendeSamboerEktefelle = TestsøknadBuilder.Builder()
                    .defaultPersonMinimum(
                        navn = "Fyren som skal bli min samboer",
                        fødselsdato = LocalDate.of(1979, 9, 17)
                    )
            )
            .build()
    }

    private fun mapSøkersBarn(søkerMedBarn: SøkerMedBarn): List<Barn> {
        val barneListe: List<Barn> = søkerMedBarn.barn.map {
            TestsøknadBuilder.Builder().defaultBarn(
                navn = it.value.navn.gjeldende().visningsnavn(),
                fødselsnummer = it.key,
                harSkalHaSammeAdresse = true,
                ikkeRegistrertPåSøkersAdresseBeskrivelse = "Fordi",
                erBarnetFødt = true,
                fødselTermindato = Fødselsnummer(it.key).fødselsdato,
                annenForelder = TestsøknadBuilder.Builder().defaultAnnenForelder(
                    ikkeOppgittAnnenForelderBegrunnelse = null,
                    bosattINorge = false,
                    land = "Sverige",
                    personMinimum = TestsøknadBuilder.Builder()
                        .defaultPersonMinimum("Bob Burger", LocalDate.of(1979, 9, 17))
                ),
                samvær = TestsøknadBuilder.Builder().defaultSamvær(
                    beskrivSamværUtenBarn = "Har sjelden sett noe til han",
                    borAnnenForelderISammeHus = "ja",
                    borAnnenForelderISammeHusBeskrivelse = "Samme blokk",
                    harDereSkriftligAvtaleOmSamvær = "jaIkkeKonkreteTidspunkter",
                    harDereTidligereBoddSammen = true,
                    hvorMyeErDuSammenMedAnnenForelder = "møtesUtenom",
                    hvordanPraktiseresSamværet = "Bytter litt på innimellom",
                    nårFlyttetDereFraHverandre = LocalDate.of(2020, 12, 31),
                    skalAnnenForelderHaSamvær = "jaMerEnnVanlig",
                    spørsmålAvtaleOmDeltBosted = true
                ),
                skalBoHosSøker = "jaMenSamarbeiderIkke"
            )
        }
        return barneListe
    }

    private fun lagFørstegangsbehandling(søknad: SøknadOvergangsstønad, fagsak: Fagsak): Behandling {
        val behandling = behandlingService.opprettBehandling(
            BehandlingType.FØRSTEGANGSBEHANDLING,
            fagsak.id,
            behandlingsårsak = BehandlingÅrsak.SØKNAD
        )
        val journalposter = behandlingService.hentBehandlingsjournalposter(behandling.id)
        søknadService.lagreSøknadForOvergangsstønad(
            søknad,
            behandling.id,
            fagsak.id,
            journalposter.firstOrNull()?.journalpostId ?: "TESTJPID"
        )
        return behandling
    }

    private fun lagMigreringBehandling(fagsak: Fagsak): Behandling {
        return migreringService.opprettMigrering(
            fagsak = fagsak,
            periode = Månedsperiode(YearMonth.now(), YearMonth.now().plusMonths(1)),
            inntektsgrunnlag = 0,
            samordningsfradrag = 0
        )
    }

    private fun arkiver(fnr: String): String {
        val arkiverDokumentRequest = ArkiverDokumentRequest(
            fnr,
            false,
            listOf(
                Dokument(
                    "TEST".toByteArray(),
                    Filtype.PDFA,
                    null,
                    null,
                    Dokumenttype.OVERGANGSSTØNAD_SØKNAD
                )
            ),
            emptyList()
        )

        val saksbehandler = SikkerhetContext.hentSaksbehandler(true)
        val dokumentResponse = journalpostClient.arkiverDokument(arkiverDokumentRequest, saksbehandler)
        return dokumentResponse.journalpostId
    }
}

private fun TestBehandlingsType.tilStønadstype(): StønadType =
    when (this) {
        FØRSTEGANGSBEHANDLING, MIGRERING -> StønadType.OVERGANGSSTØNAD
        BARNETILSYN -> StønadType.BARNETILSYN
        SKOLEPENGER -> StønadType.SKOLEPENGER
    }

data class TestFagsakRequest(
    val personIdent: String,
    val behandlingsType: TestBehandlingsType = FØRSTEGANGSBEHANDLING
)

enum class TestBehandlingsType {
    FØRSTEGANGSBEHANDLING,
    MIGRERING,
    BARNETILSYN,
    SKOLEPENGER
}
