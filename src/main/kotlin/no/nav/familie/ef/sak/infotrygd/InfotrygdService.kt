package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadType
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSøkRequest
import org.springframework.stereotype.Service

@Service
class InfotrygdService(private val infotrygdReplikaClient: InfotrygdReplikaClient,
                       private val pdlClient: PdlClient) {

    /**
     * Forslag på sjekk om en person eksisterer i infotrygd
     */
    fun eksisterer(personIdent: String, søknadTyper: Set<SøknadType> = SøknadType.values().toSet()): Boolean {
        require(søknadTyper.isNotEmpty()) { "Forventer att søknadstyper ikke er empty" }
        val identer = pdlClient.hentPersonidenter(personIdent, true).identer()
        val stønadTyper = søknadTyper.map { it.tilStønadType() }.toSet()
        val response = infotrygdReplikaClient.hentInslagHosInfotrygd(InfotrygdSøkRequest(identer))
        val harVedtak = response.vedtak.any { stønadTyper.contains(it.stønadType) }
        val harSak = response.saker.any { stønadTyper.contains(it.stønadType) }
        return harVedtak || harSak
    }

    private fun SøknadType.tilStønadType() = StønadType.valueOf(name)
}