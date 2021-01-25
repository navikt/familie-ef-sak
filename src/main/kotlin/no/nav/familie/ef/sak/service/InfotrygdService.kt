package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.integration.InfotrygdReplikaClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.repository.domain.SøknadType
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.StønadTreff
import no.nav.familie.kontrakter.ef.infotrygd.SøkFlereStønaderRequest
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Service

@Service
class InfotrygdService(private val infotrygdReplikaClient: InfotrygdReplikaClient,
                       private val pdlClient: PdlClient) {


    fun eksisterer(ident: String, søknadType: SøknadType): StønadTreff {
        val response = eksisterer(ident, setOf(søknadType))
        val stønadTreff = response[søknadType]
        requireNotNull(stønadTreff) {
            "Forventer att man skal få treff på $søknadType men fikk ${
                objectMapper.writeValueAsString(response)
            }"
        }
        return stønadTreff
    }

    /**
     * Finner om en person eksisterer i infotrygd og om det finnes noen aktiv sak for personen.
     */
    fun eksisterer(personIdent: String, søknadTyper: Set<SøknadType>): Map<SøknadType, StønadTreff> {
        require(søknadTyper.isNotEmpty()) { "Forventer att søknadstyper ikke er empty" }
        val identer = pdlClient.hentPersonidenter(personIdent, true).identer.map { it.ident }.toSet()
        val søknadType = søknadTyper.map { it.tilStønadType() }.toSet()
        return infotrygdReplikaClient.eksistererPerson(SøkFlereStønaderRequest(identer, søknadType)).stønader
                .map { it.key.tilSøknadType() to it.value }.toMap()
    }

    private fun SøknadType.tilStønadType() = StønadType.valueOf(name)
    private fun StønadType.tilSøknadType() = SøknadType.valueOf(name)
}