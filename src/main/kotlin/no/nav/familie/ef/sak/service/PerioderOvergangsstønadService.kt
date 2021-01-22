package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.integration.InfotrygdReplikaClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import org.springframework.stereotype.Service

@Service
class PerioderOvergangsstønadService(private val infotrygdReplikaClient: InfotrygdReplikaClient,
                                     private val pdlClient: PdlClient) {

    /**
     * Henter perioder fra infotrygd for en person
     * SKal hente perioder fra ef-sak også i fremtiden
     */
    fun hentPerioder(request: PerioderOvergangsstønadRequest): PerioderOvergangsstønadResponse {
        val personIdenter = pdlClient.hentPersonidenter(request.personIdent, true).identer.map { it.ident }.toSet()
        val infotrygdRequest = InfotrygdPerioderOvergangsstønadRequest(personIdenter, request.fomDato, request.tomDato)
        val infotrygdPerioder = infotrygdReplikaClient.hentPerioderOvergangsstønad(infotrygdRequest)
        val perioder = infotrygdPerioder.perioder.map {
            PeriodeOvergangsstønad(personIdent = it.personIdent,
                                   fomDato = it.fomDato,
                                   tomDato = it.tomDato,
                                   datakilde = PeriodeOvergangsstønad.Datakilde.INFOTRYGD)
        }
        return PerioderOvergangsstønadResponse(perioder)
    }

}