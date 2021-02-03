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

        val barnNavnOgFnr = søkerMedBarn.barn.map { NavnOgFnr(it.value.navn.gjeldende().visningsnavn(), it.key) }

        val søknad: SøknadOvergangsstønad = TestsøknadBuilder.Builder()
                .setPersonalia(søkerMedBarn.søker.navn.gjeldende().visningsnavn(), søkerMedBarn.søkerIdent)
                .setBarn(
                        navn = barnNavnOgFnr[0].navn,
                        fødselsnummer = Fødselsnummer(barnNavnOgFnr[0].fødselsnummer),
                        harSkalHaSammeAdresse = true,
                        ikkeRegistrertPåSøkersAdresseBeskrivelse = "Fordi",
                        erBarnetFødt = true,
                        fødselTermindato = Fødselsnummer(barnNavnOgFnr[0].fødselsnummer).fødselsdato,
                        annenForelder = AnnenForelder(
                                ikkeOppgittAnnenForelderBegrunnelse = null,
                                bosattNorge = Søknadsfelt("Bosatt i norge", false),
                                land = Søknadsfelt("Land", "Sverige"),
                                person = Søknadsfelt("annenForelder", TestsøknadBuilder.Builder().defaultPersonMinimum("Bob Burger", LocalDate.of(1979, 9, 17))),
                        ),
                        samvær = Samvær(
                                avtaleOmDeltBosted = null,
                                beskrivSamværUtenBarn = Søknadsfelt("Beskrivelse av samvær", "Har sjelden sett noe til han"),
                                borAnnenForelderISammeHus = Søknadsfelt("Samme hus", "Jepp", null, "ja"),
                                borAnnenForelderISammeHusBeskrivelse = Søknadsfelt("Samme hus beskrivelse", "Samme blokk"),
                                erklæringOmSamlivsbrudd = null,
                                harDereSkriftligAvtaleOmSamvær = Søknadsfelt("Avtale", "Jepps", null, "jaIkkeKonkreteTidspunkter"),
                                harDereTidligereBoddSammen = Søknadsfelt("Bodd sammen", true),
                                hvorMyeErDuSammenMedAnnenForelder = Søknadsfelt("Hvor mye sammen", "Mye", null, "møtesUtenom"),
                                hvordanPraktiseresSamværet = Søknadsfelt("Hvordan praktiseres", "Bytter litt på innimellom"),
                                nårFlyttetDereFraHverandre = Søknadsfelt("Når fraflytting", LocalDate.of(2020, 12, 31)),
                                samværsavtale = null,
                                skalAnnenForelderHaSamvær = Søknadsfelt("Skal annen forelder ha samvær", "Jepps", null, "jaMerEnnVanlig"),
                                skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke = null,
                                spørsmålAvtaleOmDeltBosted = Søknadsfelt("Delt bosted", true)

                        )
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
