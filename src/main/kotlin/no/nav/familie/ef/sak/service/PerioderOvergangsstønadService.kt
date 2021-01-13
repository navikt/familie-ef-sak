package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.integration.InfotrygdReplikaClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.repository.domain.SøknadType
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.ef.infotrygd.StønadTreff
import no.nav.familie.kontrakter.ef.infotrygd.SøkFlereStønaderRequest
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Service

@Service
class PerioderOvergangsstønadService(private val infotrygdReplikaClient: InfotrygdReplikaClient,
                                     private val pdlClient: PdlClient) {

    /**
     * Henter perioder fra infotrygd for en person
     */
    fun hentPerioder(request: PerioderOvergangsstønadRequest): PerioderOvergangsstønadResponse {
        val identer = hentIdenter(request.ident)
        val infotrygdRequest = InfotrygdPerioderOvergangsstønadRequest(identer, request.fomDato, request.tomDato)
        val infotrygdPerioder = infotrygdReplikaClient.hentPerioderOvergangsstønad(infotrygdRequest)
        val perioder = infotrygdPerioder.perioder.map {
            PeriodeOvergangsstønad(ident = it.ident,
                                   fomDato = it.fomDato,
                                   tomDato = it.tomDato,
                                   datakilde = PeriodeOvergangsstønad.Datakilde.INFOTRYGD)
        }
        return PerioderOvergangsstønadResponse(perioder)
    }

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
    fun eksisterer(ident: String, søknadTyper: Set<SøknadType>): Map<SøknadType, StønadTreff> {
        require(søknadTyper.isNotEmpty()) { "Forventer att søknadstyper ikke er empty" }
        val identer = hentIdenter(ident)
        val søknadType = søknadTyper.map { it.tilStønadType() }.toSet()
        return infotrygdReplikaClient.eksistererPerson(SøkFlereStønaderRequest(identer, søknadType)).stønader
                .map { it.key.tilSøknadType() to it.value }.toMap()
    }

    private fun hentIdenter(ident: String): Set<String> {
        return pdlClient.hentPersonidenter(ident, true).identer.map { it.ident }.toSet()
    }

    private fun SøknadType.tilStønadType() = StønadType.valueOf(name)
    private fun StønadType.tilSøknadType() = SøknadType.valueOf(name)

}