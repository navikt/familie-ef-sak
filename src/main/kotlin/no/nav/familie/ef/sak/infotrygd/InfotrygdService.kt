package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSøkRequest
import org.springframework.stereotype.Service

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
        return infotrygdReplikaClient.hentSaker(InfotrygdSøkRequest(hentPersonIdenter(personIdent)))
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
        return InternePerioder(overgangsstønad = filtrerOgSlåSammenPerioder(perioder.overgangsstønad).map { it.tilInternPeriode() },
                               barnetilsyn = filtrerOgSlåSammenPerioder(perioder.barnetilsyn).map { it.tilInternPeriode() },
                               skolepenger = filtrerOgSlåSammenPerioder(perioder.skolepenger).map { it.tilInternPeriode() })
    }

    private fun mapPerioder(perioder: List<InfotrygdPeriode>) =
            InfotrygdStønadPerioderDto(perioder, filtrerOgSlåSammenPerioder(perioder).map { it.tilSummertInfotrygdperiodeDto() })

    private fun hentPerioderFraReplika(identer: Set<String>,
                                       stønadstyper: Set<StønadType> = StønadType.values().toSet()): InfotrygdPeriodeResponse {
        require(stønadstyper.isNotEmpty()) { "Må sende med stønadstype" }
        val request = InfotrygdPeriodeRequest(identer, stønadstyper)
        return infotrygdReplikaClient.hentPerioder(request)
    }

    private fun filtrerOgSlåSammenPerioder(perioder: List<InfotrygdPeriode>): List<InfotrygdPeriode> {
        val filtrertPerioder = InfotrygdPeriodeUtil.filtrerOgSorterPerioderFraInfotrygd(perioder)
                .filter { it.kode != InfotrygdEndringKode.ANNULERT && it.kode != InfotrygdEndringKode.UAKTUELL }
        return InfotrygdPeriodeUtil.slåSammenInfotrygdperioder(filtrertPerioder)
    }

    private fun hentPersonIdenter(personIdent: String): Set<String> {
        return pdlClient.hentPersonidenter(personIdent, true).identer()
    }

}
