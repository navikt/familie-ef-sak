package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSøkRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class InfotrygdService(private val infotrygdReplikaClient: InfotrygdReplikaClient,
                       private val pdlClient: PdlClient) {

    /**
     * Forslag på sjekk om en person eksisterer i infotrygd
     */
    fun eksisterer(personIdent: String, stønadTyper: Set<StønadType> = StønadType.values().toSet()): Boolean {
        require(stønadTyper.isNotEmpty()) { "Forventer att stønadTyper ikke er empty" }
        val identer = hentPersonIdenter(personIdent)
        val response = infotrygdReplikaClient.hentInslagHosInfotrygd(InfotrygdSøkRequest(identer))

        val harVedtak = response.vedtak.any { stønadTyper.contains(it.stønadType) }
        val harSak = response.saker.any { stønadTyper.contains(it.stønadType) }
        return harVedtak || harSak
    }

    fun hentDtoPerioder(personIdent: String): InfotrygdPerioderDto {
        val perioder = hentPerioderFraReplika(personIdent)
        return InfotrygdPerioderDto(
                overgangsstønad = mapPerioder(perioder.overgangsstønad),
                barnetilsyn = mapPerioder(perioder.barnetilsyn),
                skolepenger = mapPerioder(perioder.skolepenger)
        )
    }

    fun hentSaker(personIdent: String): InfotrygdSakResponse {
        val response = infotrygdReplikaClient.hentSaker(InfotrygdSøkRequest(hentPersonIdenter(personIdent)))
        return response.copy(saker = response.saker
                .sortedWith(compareByDescending<InfotrygdSak, LocalDate?>(nullsLast()) { it.vedtaksdato }
                                    .thenByDescending(nullsLast()) { it.mottattDato }))
    }

    /**
     * Returnerer perioder uten å slå de sammen, brukes når man eks kun ønsker å se om det finnes innslag i infotrygd fra før
     */
    fun hentPerioderFraReplika(personIdent: String): InfotrygdPeriodeResponse {
        val personIdenter = hentPersonIdenter(personIdent)
        return hentPerioderFraReplika(personIdenter)
    }

    /**
     * Filtrerer og slår sammen perioder fra infotrygd for å få en bedre totalbilde om hva som er gjeldende
     */
    fun hentSammenslåttePerioderSomInternPerioder(personIdenter: Set<String>): InternePerioder {
        val perioder = hentPerioderFraReplika(personIdenter)
        return InternePerioder(overgangsstønad = slåSammenPerioder(perioder.overgangsstønad).map { it.tilInternPeriode() },
                               barnetilsyn = slåSammenPerioder(perioder.barnetilsyn).map { it.tilInternPeriode() },
                               skolepenger = slåSammenPerioder(perioder.skolepenger).map { it.tilInternPeriode() })
    }

    private fun mapPerioder(perioder: List<InfotrygdPeriode>) =
            InfotrygdStønadPerioderDto(perioder.filter { it.kode != InfotrygdEndringKode.ANNULERT },
                                       slåSammenPerioder(perioder).map { it.tilSummertInfotrygdperiodeDto() })

    private fun hentPerioderFraReplika(identer: Set<String>,
                                       stønadstyper: Set<StønadType> = StønadType.values().toSet()): InfotrygdPeriodeResponse {
        require(stønadstyper.isNotEmpty()) { "Må sende med stønadstype" }
        val request = InfotrygdPeriodeRequest(identer, stønadstyper)
        return infotrygdReplikaClient.hentPerioder(request)
    }

    private fun slåSammenPerioder(perioder: List<InfotrygdPeriode>): List<InfotrygdPeriode> {
        return InfotrygdPeriodeUtil.slåSammenInfotrygdperioder(perioder)
    }

    private fun hentPersonIdenter(personIdent: String): Set<String> {
        return pdlClient.hentPersonidenter(personIdent, true).identer()
    }

}
