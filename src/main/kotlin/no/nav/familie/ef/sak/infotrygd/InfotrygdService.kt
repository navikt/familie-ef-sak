package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSøkRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class InfotrygdService(private val infotrygdReplikaClient: InfotrygdReplikaClient,
                       private val pdlClient: PdlClient) {

    private val logger = LoggerFactory.getLogger(javaClass)

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

    fun hentPerioderForOvergangsstønad(personIdent: String): List<InfotrygdPeriode> {
        return hentPerioder(personIdent, setOf(StønadType.OVERGANGSSTØNAD)).overgangsstønad
    }

    fun hentPerioder(personIdent: String): InfotrygdPeriodeResponse {
        return hentPerioder(personIdent, emptySet())
    }

    private fun hentPerioder(personIdent: String,
                             stønadstyper: Set<StønadType>): InfotrygdPeriodeResponse {
        val identer = hentPersonIdenter(personIdent)
        val request = InfotrygdPeriodeRequest(identer, stønadstyper)
        return infotrygdReplikaClient.hentPerioder(request)
    }

    private fun hentPersonIdenter(personIdent: String): Set<String> {
        return pdlClient.hentPersonidenter(personIdent, true).identer()
    }

}
