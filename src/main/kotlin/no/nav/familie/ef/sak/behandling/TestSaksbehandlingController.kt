package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.journalføring.JournalpostClient
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.SøkerMedBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.søknad.Barn
import no.nav.familie.kontrakter.ef.søknad.EnumTekstverdiMedSvarId
import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@RestController
@RequestMapping("/api/test")
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class TestSaksbehandlingController(private val fagsakService: FagsakService,
                                   private val behandlingshistorikkService: BehandlingshistorikkService,
                                   private val behandlingService: BehandlingService,
                                   private val søknadService: SøknadService,
                                   private val personService: PersonService,
                                   private val grunnlagsdataService: GrunnlagsdataService,
                                   private val taskRepository: TaskRepository,
                                   private val oppgaveService: OppgaveService,
                                   private val journalpostClient: JournalpostClient,
                                   private val migreringService: MigreringService) {

    @PostMapping(path = ["fagsak"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettFagsakForTestperson(@RequestBody testFagsakRequest: TestFagsakRequest): Ressurs<UUID> {
        val fagsak = fagsakService.hentEllerOpprettFagsak(testFagsakRequest.personIdent, Stønadstype.OVERGANGSSTØNAD)
        val søknad: SøknadOvergangsstønad = lagSøknad(testFagsakRequest.personIdent)
        val behandling: Behandling = when (testFagsakRequest.behandlingsType) {
            TestBehandlingsType.FØRSTEGANGSBEHANDLING -> lagFørstegangsbehandling(fagsak, søknad)
            TestBehandlingsType.BLANKETT -> lagBlankettBehandling(fagsak, testFagsakRequest.personIdent, søknad)
            TestBehandlingsType.MIGRERING -> lagMigreringBehandling(fagsak)
        }


        if (!behandling.erMigrering()) { // opprettGrunnlagsdata håndteres i migreringservice
            grunnlagsdataService.opprettGrunnlagsdata(behandling.id)
        }
        behandlingshistorikkService.opprettHistorikkInnslag(Behandlingshistorikk(behandlingId = behandling.id,
                                                                                 steg = StegType.VILKÅR))
        val oppgaveId = oppgaveService.opprettOppgave(behandling.id,
                                                      Oppgavetype.BehandleSak,
                                                      SikkerhetContext.hentSaksbehandler(true),
                                                      "Dummy-oppgave opprettet i ny løsning")
        taskRepository.save(taskRepository.save(BehandlingsstatistikkTask.opprettMottattTask(behandlingId = behandling.id,
                                                                                             oppgaveId = oppgaveId)))

        return Ressurs.success(behandling.id)
    }

    private fun lagSøknad(personIdent: String): SøknadOvergangsstønad {
        val søkerMedBarn = personService.hentPersonMedBarn(personIdent)
        val barneListe: List<Barn> = mapSøkersBarn(søkerMedBarn)
        return TestsøknadBuilder.Builder()
                .setPersonalia(søkerMedBarn.søker.navn.gjeldende().visningsnavn(), søkerMedBarn.søkerIdent)
                .setBarn(barneListe)
                .setBosituasjon(delerDuBolig =
                                EnumTekstverdiMedSvarId(verdi = "Nei, jeg bor alene med barn eller jeg er gravid og bor alene",
                                                        svarId = "borAleneMedBarnEllerGravid"))
                .setSivilstandsplaner(
                        harPlaner = true,
                        fraDato = LocalDate.of(2019, 9, 17),
                        vordendeSamboerEktefelle = TestsøknadBuilder.Builder()
                                .defaultPersonMinimum(navn = "Fyren som skal bli min samboer",
                                                      fødselsdato = LocalDate.of(1979, 9, 17)),
                )
                .build().søknadOvergangsstønad
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
                                    .defaultPersonMinimum("Bob Burger", LocalDate.of(1979, 9, 17)),
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

    private fun lagFørstegangsbehandling(fagsak: Fagsak, søknad: SøknadOvergangsstønad): Behandling {
        val behandling = behandlingService.opprettBehandling(BehandlingType.FØRSTEGANGSBEHANDLING,
                                                             fagsak.id,
                                                             behandlingsårsak = BehandlingÅrsak.SØKNAD)
        val journalposter = behandlingService.hentBehandlingsjournalposter(behandling.id)
        søknadService.lagreSøknadForOvergangsstønad(søknad,
                                                    behandling.id,
                                                    fagsak.id,
                                                    journalposter.firstOrNull()?.journalpostId ?: "TESTJPID")
        return behandling
    }

    private fun lagBlankettBehandling(fagsak: Fagsak, fnr: String, søknad: SøknadOvergangsstønad): Behandling {
        val journalpostId = arkiver(fnr)
        val journalpost = journalpostClient.hentJournalpost(journalpostId)
        return behandlingService.opprettBehandlingForBlankett(BehandlingType.BLANKETT, fagsak.id, søknad, journalpost)
    }


    private fun lagMigreringBehandling(fagsak: Fagsak): Behandling {
        return migreringService.opprettMigrering(fagsak = fagsak,
                                                 fra = YearMonth.now(),
                                                 til = YearMonth.now().plusMonths(1),
                                                 forventetInntekt = BigDecimal.ZERO,
                                                 samordningsfradrag = BigDecimal.ZERO)
    }

    private fun arkiver(fnr: String): String {
        val arkiverDokumentRequest = ArkiverDokumentRequest(fnr,
                                                            false,
                                                            listOf(Dokument("TEST".toByteArray(),
                                                                            Filtype.PDFA, null, null,
                                                                            Dokumenttype.OVERGANGSSTØNAD_SØKNAD)),
                                                            emptyList())

        val saksbehandler = SikkerhetContext.hentSaksbehandler(true)
        val dokumentResponse = journalpostClient.arkiverDokument(arkiverDokumentRequest, saksbehandler)
        return dokumentResponse.journalpostId
    }
}

data class TestFagsakRequest(val personIdent: String,
                             val behandlingsType: TestBehandlingsType = TestBehandlingsType.FØRSTEGANGSBEHANDLING)

enum class TestBehandlingsType {
    FØRSTEGANGSBEHANDLING,
    BLANKETT,
    MIGRERING
}