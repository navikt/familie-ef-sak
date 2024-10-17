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
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.SøkerMedBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.ef.sak.vilkår.VurderingStegService
import no.nav.familie.ef.sak.vilkår.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.SvarPåVurderingerDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.VurderingDto
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.SvarRegel
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.vilkårsreglerForStønad
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.søknad.Barn
import no.nav.familie.kontrakter.ef.søknad.EnumTekstverdiMedSvarId
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
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
    private val taskService: TaskService,
    private val oppgaveService: OppgaveService,
    private val migreringService: MigreringService,
    private val vurderingService: VurderingService,
    private val vurderingStegService: VurderingStegService,
) {
    @PostMapping("{behandlingId}/utfyll-vilkar")
    fun utfyllVilkår(
        @PathVariable behandlingId: UUID,
    ): Ressurs<UUID> {
        val vurderinger = vurderingService.hentAlleVurderinger(behandlingId)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val regler = vilkårsreglerForStønad(saksbehandling.stønadstype).associateBy { it.vilkårType }

        vurderinger.forEach { vurdering ->
            val delvilkår = lagDelvilkår(regler, vurdering)
            vurderingStegService.oppdaterVilkår(SvarPåVurderingerDto(vurdering.id, behandlingId, delvilkår))
        }
        return Ressurs.success(behandlingId)
    }

    private fun lagDelvilkår(
        regler: Map<VilkårType, Vilkårsregel>,
        vurdering: VilkårsvurderingDto,
    ): List<DelvilkårsvurderingDto> {
        val vilkårsregel = regler.getValue(vurdering.vilkårType)
        if (vurdering.vilkårType == VilkårType.ER_UTDANNING_HENSIKTSMESSIG) {
            return listOf(delvilkårErUtdanningHensiktsmessig())
        }
        return vurdering.delvilkårsvurderinger.map { delvilkår ->
            val hovedregel = delvilkår.hovedregel()
            val regelSteg = vilkårsregel.regler.getValue(hovedregel)
            regelSteg.svarMapping
                .mapNotNull { (svarId, svarRegel) ->
                    lagOppfyltVilkår(delvilkår, svarRegel, svarId)
                }.firstOrNull()
                ?: error("Finner ikke oppfylt svar for vilkårstype=${vurdering.vilkårType} hovedregel=$hovedregel")
        }
    }

    private fun lagOppfyltVilkår(
        delvilkår: DelvilkårsvurderingDto,
        svarRegel: SvarRegel,
        svarId: SvarId,
    ) = when (svarRegel) {
        SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
        SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
        SluttSvarRegel.OPPFYLT,
        ->
            delvilkår(
                delvilkår.hovedregel(),
                svarId,
                if (svarRegel == SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE) "begrunnelse" else null,
            )
        else -> null
    }

    private fun delvilkår(
        regelId: RegelId,
        svar: SvarId,
        begrunnelse: String? = null,
    ) = DelvilkårsvurderingDto(
        Vilkårsresultat.OPPFYLT,
        listOf(VurderingDto(regelId, svar, begrunnelse)),
    )

    private fun delvilkårErUtdanningHensiktsmessig() =
        DelvilkårsvurderingDto(
            Vilkårsresultat.OPPFYLT,
            listOf(
                VurderingDto(RegelId.NAVKONTOR_VURDERING, SvarId.JA),
                VurderingDto(RegelId.SAKSBEHANDLER_VURDERING, SvarId.JA, "begrunnelse"),
            ),
        )

    @Transactional
    @PostMapping(path = ["fagsak"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettFagsakForTestperson(
        @RequestBody testFagsakRequest: TestFagsakRequest,
    ): Ressurs<UUID> {
        val personIdent = testFagsakRequest.personIdent
        val søknadBuilder = lagSøknad(personIdent)
        val fagsak =
            fagsakService.hentEllerOpprettFagsak(personIdent, testFagsakRequest.behandlingsType.tilStønadstype())

        val behandling: Behandling =
            when (testFagsakRequest.behandlingsType) {
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
                fagsak.stønadstype,
            )
            behandlingshistorikkService.opprettHistorikkInnslag(
                Behandlingshistorikk(
                    behandlingId = behandling.id,
                    steg = StegType.VILKÅR,
                ),
            )
            val oppgaveId =
                oppgaveService.opprettOppgave(
                    behandlingId = behandling.id,
                    oppgavetype = Oppgavetype.BehandleSak,
                    tilordnetNavIdent = SikkerhetContext.hentSaksbehandler(),
                    beskrivelse = "Dummy-oppgave opprettet",
                )
            taskService.save(
                taskService.save(
                    BehandlingsstatistikkTask.opprettMottattTask(
                        behandlingId = behandling.id,
                        oppgaveId = oppgaveId,
                    ),
                ),
            )
        }

        return Ressurs.success(behandling.id)
    }

    private fun lagBarnetilsynBehandling(
        søknadBarnetilsyn: SøknadBarnetilsyn,
        fagsak: Fagsak,
    ): Behandling {
        val behandling =
            behandlingService.opprettBehandling(
                BehandlingType.FØRSTEGANGSBEHANDLING,
                fagsak.id,
                behandlingsårsak = BehandlingÅrsak.SØKNAD,
            )
        val journalposter = behandlingService.hentBehandlingsjournalposter(behandling.id)
        søknadService.lagreSøknadForBarnetilsyn(
            søknadBarnetilsyn,
            behandling.id,
            fagsak.id,
            journalposter.firstOrNull()?.journalpostId ?: "TESTJPID",
        )
        return behandling
    }

    private fun lagSkolepengerBehandling(
        søknadSkolepenger: SøknadSkolepenger,
        fagsak: Fagsak,
    ): Behandling {
        val behandling =
            behandlingService.opprettBehandling(
                BehandlingType.FØRSTEGANGSBEHANDLING,
                fagsak.id,
                behandlingsårsak = BehandlingÅrsak.SØKNAD,
            )
        val journalposter = behandlingService.hentBehandlingsjournalposter(behandling.id)
        søknadService.lagreSøknadForSkolepenger(
            søknadSkolepenger,
            behandling.id,
            fagsak.id,
            journalposter.firstOrNull()?.journalpostId ?: "TESTJPID",
        )
        return behandling
    }

    private fun lagSøknad(personIdent: String): TestsøknadBuilder {
        val søkerMedBarn = personService.hentPersonMedBarn(personIdent)
        val barneListe: List<Barn> = mapSøkersBarn(søkerMedBarn)
        return TestsøknadBuilder
            .Builder()
            .setPersonalia(
                søkerMedBarn.søker.navn
                    .gjeldende()
                    .visningsnavn(),
                søkerMedBarn.søkerIdent,
            ).setBarn(barneListe)
            .setBosituasjon(
                delerDuBolig =
                    EnumTekstverdiMedSvarId(
                        verdi = "Nei, jeg bor alene med barn eller jeg er gravid og bor alene",
                        svarId = "borAleneMedBarnEllerGravid",
                    ),
            ).setSivilstandsplaner(
                harPlaner = true,
                fraDato = LocalDate.of(2019, 9, 17),
                vordendeSamboerEktefelle =
                    TestsøknadBuilder
                        .Builder()
                        .defaultPersonMinimum(
                            navn = "Fyren som skal bli min samboer",
                            fødselsdato = LocalDate.of(1979, 9, 17),
                        ),
            ).build()
    }

    private fun mapSøkersBarn(søkerMedBarn: SøkerMedBarn): List<Barn> {
        val barneListe: List<Barn> =
            søkerMedBarn.barn
                .filter {
                    it.value.fødselsdato
                        .first()
                        .erUnder18År()
                }.map {
                    TestsøknadBuilder.Builder().defaultBarn(
                        navn =
                            it.value.navn
                                .gjeldende()
                                .visningsnavn(),
                        fødselsnummer = it.key,
                        harSkalHaSammeAdresse = true,
                        ikkeRegistrertPåSøkersAdresseBeskrivelse = "Fordi",
                        erBarnetFødt = true,
                        fødselTermindato =
                            it.value.fødselsdato
                                .first()
                                .fødselsdato ?: LocalDate.now(),
                        annenForelder =
                            TestsøknadBuilder.Builder().defaultAnnenForelder(
                                ikkeOppgittAnnenForelderBegrunnelse = null,
                                bosattINorge = false,
                                land = "Sverige",
                                personMinimum =
                                    TestsøknadBuilder
                                        .Builder()
                                        .defaultPersonMinimum("Bob Burger", LocalDate.of(1979, 9, 17)),
                            ),
                        samvær =
                            TestsøknadBuilder.Builder().defaultSamvær(
                                beskrivSamværUtenBarn = "Har sjelden sett noe til han",
                                borAnnenForelderISammeHus = "ja",
                                borAnnenForelderISammeHusBeskrivelse = "Samme blokk",
                                harDereSkriftligAvtaleOmSamvær = "jaIkkeKonkreteTidspunkter",
                                harDereTidligereBoddSammen = true,
                                hvorMyeErDuSammenMedAnnenForelder = "møtesUtenom",
                                hvordanPraktiseresSamværet = "Bytter litt på innimellom",
                                nårFlyttetDereFraHverandre = LocalDate.of(2020, 12, 31),
                                skalAnnenForelderHaSamvær = "jaMerEnnVanlig",
                                spørsmålAvtaleOmDeltBosted = true,
                            ),
                        skalBoHosSøker = "jaMenSamarbeiderIkke",
                    )
                }
        return barneListe
    }

    private fun lagFørstegangsbehandling(
        søknad: SøknadOvergangsstønad,
        fagsak: Fagsak,
    ): Behandling {
        val behandling =
            behandlingService.opprettBehandling(
                BehandlingType.FØRSTEGANGSBEHANDLING,
                fagsak.id,
                behandlingsårsak = BehandlingÅrsak.SØKNAD,
            )
        val journalposter = behandlingService.hentBehandlingsjournalposter(behandling.id)
        søknadService.lagreSøknadForOvergangsstønad(
            søknad,
            behandling.id,
            fagsak.id,
            journalposter.firstOrNull()?.journalpostId ?: "TESTJPID",
        )
        return behandling
    }

    private fun lagMigreringBehandling(fagsak: Fagsak): Behandling =
        migreringService.opprettMigrering(
            fagsak = fagsak,
            periode = Månedsperiode(YearMonth.now(), YearMonth.now().plusMonths(1)),
            inntektsgrunnlag = 0,
            samordningsfradrag = 0,
            ignorerFeilISimulering = true,
        )
}

private fun TestBehandlingsType.tilStønadstype(): StønadType =
    when (this) {
        FØRSTEGANGSBEHANDLING, MIGRERING -> StønadType.OVERGANGSSTØNAD
        BARNETILSYN -> StønadType.BARNETILSYN
        SKOLEPENGER -> StønadType.SKOLEPENGER
    }

data class TestFagsakRequest(
    val personIdent: String,
    val behandlingsType: TestBehandlingsType = FØRSTEGANGSBEHANDLING,
)

enum class TestBehandlingsType {
    FØRSTEGANGSBEHANDLING,
    MIGRERING,
    BARNETILSYN,
    SKOLEPENGER,
}
