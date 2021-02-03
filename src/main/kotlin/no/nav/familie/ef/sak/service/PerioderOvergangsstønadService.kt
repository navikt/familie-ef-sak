package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.exception.PdlNotFoundException
import no.nav.familie.ef.sak.integration.InfotrygdReplikaClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PerioderOvergangsstønadService(private val infotrygdReplikaClient: InfotrygdReplikaClient,
                                     private val pdlClient: PdlClient) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Henter perioder fra infotrygd for en person
     * SKal hente perioder fra ef-sak også i fremtiden
     */
    fun hentPerioder(request: PerioderOvergangsstønadRequest): PerioderOvergangsstønadResponse {
        val personIdenter = hentPersonIdenter(request)
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

    private fun hentPersonIdenter(request: PerioderOvergangsstønadRequest): Set<String> {
        return try {
            pdlClient.hentPersonidenter(request.personIdent, true).identer.map { it.ident }.toSet()
        } catch(e: PdlNotFoundException) {
            logger.warn("Finner ikke person, returnerer personIdent i request")
            setOf(request.personIdent)
        }
    }

}