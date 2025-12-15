package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.infotrygd.InfotrygdUtils.KLAGETYPER
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResultat
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSøkRequest
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class InfotrygdService(
    private val infotrygdReplikaClient: InfotrygdReplikaClient,
    private val personService: PersonService,
) {
    private val logger = Logg.getLogger(this::class)

    /**
     * Forslag på sjekk om en person eksisterer i infotrygd
     */
    fun eksisterer(
        personIdent: String,
        stønadTyper: Set<StønadType> = StønadType.values().toSet(),
    ): Boolean {
        require(stønadTyper.isNotEmpty()) { "Forventer att stønadTyper ikke er empty" }
        val identer = hentPersonIdenter(personIdent)
        val response = infotrygdReplikaClient.hentInslagHosInfotrygd(InfotrygdSøkRequest(identer))

        val harVedtak = response.vedtak.any { stønadTyper.contains(it.stønadType) }
        val harSak = response.saker.any { stønadTyper.contains(it.stønadType) }
        return harVedtak || harSak
    }

    fun hentÅpneSaker(): InfotrygdReplikaClient.ÅpnesakerRapport = infotrygdReplikaClient.hentÅpneSaker()

    fun hentDtoPerioder(personIdent: String): InfotrygdPerioderDto {
        val perioder = hentPerioderFraReplika(personIdent)
        val sammenSlåttePerioder = hentSammenslåttePerioderFraReplika(personIdent)
        return InfotrygdPerioderDto(
            overgangsstønad = mapPerioder(perioder.overgangsstønad, sammenSlåttePerioder.overgangsstønad),
            barnetilsyn = mapPerioder(perioder.barnetilsyn, sammenSlåttePerioder.barnetilsyn),
            skolepenger = mapPerioder(perioder.skolepenger, sammenSlåttePerioder.skolepenger),
        )
    }

    fun hentSaker(personIdent: String): InfotrygdSakResponse {
        val response = infotrygdReplikaClient.hentSaker(InfotrygdSøkRequest(hentPersonIdenter(personIdent)))
        return response.copy(
            saker =
                response.saker
                    .sortedWith(
                        compareByDescending<InfotrygdSak, LocalDate?>(nullsLast()) { it.vedtaksdato }
                            .thenByDescending(nullsLast()) { it.mottattDato },
                    ),
        )
    }

    fun hentÅpneKlagesaker(personIdent: String): List<InfotrygdSak> =
        hentSaker(personIdent).saker.filter {
            it.resultat == InfotrygdSakResultat.ÅPEN_SAK && KLAGETYPER.contains(it.type)
        }

    /**
     * Returnerer perioder uten å slå de sammen, brukes når man eks kun ønsker å se om det finnes innslag i infotrygd fra før
     */
    fun hentPerioderFraReplika(personIdent: String): InfotrygdPeriodeResponse {
        val personIdenter = hentPersonIdenter(personIdent)
        return hentPerioderFraReplika(personIdenter)
    }

    private fun hentSammenslåttePerioderFraReplika(personIdent: String): InfotrygdPeriodeResponse {
        val personIdenter = hentPersonIdenter(personIdent)
        feilHvis(personIdenter.isEmpty()) {
            logger.warn("Finner ikke $personIdent i pdl, kan følgelig ikke hente perioder fra replika")
            "Det finnes ingen identer i pdl for oppslaget"
        }
        return hentSammenslåttePerioderFraReplika(personIdenter)
    }

    fun hentSammenslåtteBarnetilsynPerioderFraReplika(
        personIdent: String,
    ): List<InfotrygdPeriode> {
        val personIdenter = hentPersonIdenter(personIdent)
        val perioder = hentSammenslåttePerioderFraReplika(personIdenter, setOf(StønadType.BARNETILSYN))
        return perioder.barnetilsyn
    }

    /**
     * Filtrerer og slår sammen perioder fra infotrygd for å få en bedre totalbilde om hva som er gjeldende
     */
    fun hentSammenslåttePerioderSomInternPerioder(personIdenter: Set<String>): InternePerioder {
        val perioder = hentSammenslåttePerioderFraReplika(personIdenter)
        return InternePerioder(
            overgangsstønad = perioder.overgangsstønad.map { it.tilInternPeriode() },
            barnetilsyn = perioder.barnetilsyn.map { it.tilInternPeriode() },
            skolepenger = perioder.skolepenger.map { it.tilInternPeriode() },
        )
    }

    private fun mapPerioder(
        perioder: List<InfotrygdPeriode>,
        sammenSlåttePerioder: List<InfotrygdPeriode>,
    ) = InfotrygdStønadPerioderDto(
        perioder.filter { it.kode != InfotrygdEndringKode.ANNULERT },
        sammenSlåttePerioder.map { it.tilSummertInfotrygdperiodeDto() },
    )

    /**
     * ! Denne må brukes med alle identer for en person, bruk [hentPerioderFraReplika] hvis man skal hente på en ident
     */
    fun hentPerioderFraReplika(
        identer: Set<String>,
        stønadstyper: Set<StønadType> = StønadType.values().toSet(),
    ): InfotrygdPeriodeResponse {
        require(stønadstyper.isNotEmpty()) { "Må sende med stønadstype" }
        val request = InfotrygdPeriodeRequest(identer, stønadstyper)
        return infotrygdReplikaClient.hentPerioder(request)
    }

    private fun hentSammenslåttePerioderFraReplika(
        identer: Set<String>,
        stønadstyper: Set<StønadType> = StønadType.values().toSet(),
    ): InfotrygdPeriodeResponse {
        require(stønadstyper.isNotEmpty()) { "Må sende med stønadstype" }
        val request = InfotrygdPeriodeRequest(identer, stønadstyper)
        return infotrygdReplikaClient.hentSammenslåttePerioder(request)
    }

    private fun hentPersonIdenter(personIdent: String): Set<String> = personService.hentPersonIdenter(personIdent).identer()
}
