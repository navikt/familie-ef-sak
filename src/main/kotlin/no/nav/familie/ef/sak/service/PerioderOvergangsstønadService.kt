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

    fun hentPerioder(request: PerioderOvergangsstønadRequest): PerioderOvergangsstønadResponse {
        val identer = pdlClient.hentPersonidenter(request.ident, true).identer.map { it.ident }.toSet()
        val perioder = hentPerioderFraInfotrygd(identer, request)
        return PerioderOvergangsstønadResponse(perioder)
    }

    private fun hentPerioderFraInfotrygd(identer: Set<String>,
                                         request: PerioderOvergangsstønadRequest): List<PeriodeOvergangsstønad> {
        val infotrygdRequest = InfotrygdPerioderOvergangsstønadRequest(identer, request.fomDato, request.tomDato)
        val infotrygdPerioder = infotrygdReplikaClient.hentPerioderOvergangsstønad(infotrygdRequest)
        return infotrygdPerioder.perioder.map {
            PeriodeOvergangsstønad(ident = it.ident,
                                   fomDato = it.fomDato,
                                   tomDato = it.tomDato,
                                   fullOvergangsstønad = false, //TODO gjøres i egen PR?
                                   datakilde = PeriodeOvergangsstønad.Datakilde.INFOTRYGD)
        }
    }
}