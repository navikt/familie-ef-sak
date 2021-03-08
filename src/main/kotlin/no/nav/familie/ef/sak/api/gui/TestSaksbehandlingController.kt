package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.BehandlingshistorikkService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.PersonService
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.kontrakter.ef.søknad.*
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/test")
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class TestSaksbehandlingController(private val fagsakService: FagsakService,
                                   private val behandlingshistorikkService: BehandlingshistorikkService,
                                   private val behandlingService: BehandlingService,
                                   private val personService: PersonService) {

    @PostMapping(path = ["fagsak"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettFagsakForTestperson(@RequestBody testFagsakRequest: TestFagsakRequest): Ressurs<UUID> {
        val fagsakDto =
                fagsakService.hentEllerOpprettFagsak(testFagsakRequest.personIdent, Stønadstype.OVERGANGSSTØNAD)
        val fagsak = fagsakService.hentFagsak(fagsakDto.id)
        val behandling = behandlingService.opprettBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, fagsak.id)
        behandlingshistorikkService.opprettHistorikkInnslag(Behandlingshistorikk(behandlingId = behandling.id,
                                                                                 steg = StegType.REGISTRERE_OPPLYSNINGER))
        val søkerMedBarn = personService.hentPersonMedRelasjoner(testFagsakRequest.personIdent)

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
                            personMinimum = TestsøknadBuilder.Builder().defaultPersonMinimum("Bob Burger", LocalDate.of(1979, 9, 17)),
                    ),
                    samvær = TestsøknadBuilder.Builder().defaultSamvær(
                            beskrivSamværUtenBarn = "Har sjelden sett noe til han",
                            borAnnenForelderISammeHus =  "ja",
                            borAnnenForelderISammeHusBeskrivelse = "Samme blokk",
                            harDereSkriftligAvtaleOmSamvær =  "jaIkkeKonkreteTidspunkter",
                            harDereTidligereBoddSammen = true,
                            hvorMyeErDuSammenMedAnnenForelder = "møtesUtenom",
                            hvordanPraktiseresSamværet =  "Bytter litt på innimellom",
                            nårFlyttetDereFraHverandre = LocalDate.of(2020, 12, 31),
                            skalAnnenForelderHaSamvær = "jaMerEnnVanlig",
                            spørsmålAvtaleOmDeltBosted = true
                    ),
                    skalBoHosSøker = "jaMenSamarbeiderIkke"
            )
        }

        val søknad: SøknadOvergangsstønad = TestsøknadBuilder.Builder()
                .setPersonalia(søkerMedBarn.søker.navn.gjeldende().visningsnavn(), søkerMedBarn.søkerIdent)
                .setBarn(barneListe)
                .setBosituasjon(delerDuBolig = EnumTekstverdiMedSvarId(verdi = "Nei, jeg bor alene med barn eller jeg er gravid og bor alene",
                                                                   svarId = "borAleneMedBarnEllerGravid"))
                .setSivilstandsplaner(harPlaner = true,
                                      fraDato = LocalDate.of(2019, 9, 17),
                                      vordendeSamboerEktefelle = TestsøknadBuilder.Builder().defaultPersonMinimum(navn = "Fyren som skal bli min samboer", fødselsdato = LocalDate.of(1979, 9, 17)),
                )
                .build().søknadOvergangsstønad


        behandlingService.lagreSøknadForOvergangsstønad(søknad,
                                                        behandling.id,
                                                        fagsak.id,
                                                        "TESTJPID")
        return Ressurs.success(behandling.id)
    }
}

data class TestFagsakRequest(val personIdent: String)
