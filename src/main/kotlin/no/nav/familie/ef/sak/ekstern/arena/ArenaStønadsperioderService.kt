package no.nav.familie.ef.sak.ekstern.arena

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.ekstern.arena.ArenaPeriodeUtil.mapOgFiltrer
import no.nav.familie.ef.sak.ekstern.arena.ArenaPeriodeUtil.slåSammenPerioder
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infrastruktur.exception.PdlNotFoundException
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderArenaRequest
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Denne tjenesten henter perioder for andre typer stønader fra EF også
 * Arena kaller idag en endepunkt hos infotrygd som returnerer perioder fra alle stønader, men det var sagt i starten att de
 * skulle hente perioder for overgangsstønad fra oss.
 */
@Service
class ArenaStønadsperioderService(private val infotrygdReplikaClient: InfotrygdReplikaClient,
                                  private val fagsakService: FagsakService,
                                  private val behandlingService: BehandlingService,
                                  private val tilkjentYtelseService: TilkjentYtelseService,
                                  private val personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient,
                                  private val pdlClient: PdlClient) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Henter perioder fra infotrygd for en person
     * Skal hente perioder fra ef-sak også i fremtiden
     */
    fun hentPerioder(request: PerioderOvergangsstønadRequest): PerioderOvergangsstønadResponse {
        return runBlocking {
            val asyncResponse = async { personopplysningerIntegrasjonerClient.hentInfotrygdPerioder(request) }

            //val responseFraReplika = hentReplikaPerioder(request) // TODO ta i bruk når replikaen virker i prod. Husk å rydde i PersonopplysningerInt
            val perioderFraInfotrygd = asyncResponse.await()
            val perioderFraEf = hentPerioderFraEf(request)
            PerioderOvergangsstønadResponse(perioderFraInfotrygd.perioder + perioderFraEf)
        }
    }

    fun hentReplikaPerioder(request: PerioderOvergangsstønadRequest): List<PeriodeOvergangsstønad> {
        val personIdenter = hentPersonIdenter(request)
        return hentPerioderFraReplika(personIdenter, request)
    }

    private fun hentPerioderFraEf(request: PerioderOvergangsstønadRequest): List<PeriodeOvergangsstønad> {
        // det er slik Arena ønsker det, det er samme i appen infotrygd-enslig-forsoerger
        val fom = request.fomDato ?: LocalDate.now()
        val tom = request.tomDato ?: LocalDate.now()
        val personIdenter = hentPersonIdenter(request)
        return Stønadstype.values()
                .mapNotNull { fagsakService.finnFagsak(personIdenter, it)}
                .mapNotNull { behandlingService.finnSisteIverksatteBehandling(it.id) }
                .mapNotNull { if (it.type != BehandlingType.TEKNISK_OPPHØR) it else null }
                .map { tilkjentYtelseService.hentForBehandling(it.id) }
                .flatMap { it.andelerTilkjentYtelse }
                .filter { it.stønadFom <= tom && it.stønadTom >= fom }
                .let { mapOgFiltrer(it) }
                .let { slåSammenPerioder(it) }
    }

    private fun hentPerioderFraReplika(personIdenter: Set<String>,
                                       request: PerioderOvergangsstønadRequest): List<PeriodeOvergangsstønad> {
        val infotrygdRequest = InfotrygdPerioderArenaRequest(personIdenter, request.fomDato, request.tomDato)
        val infotrygdPerioder = infotrygdReplikaClient.hentPerioderArena(infotrygdRequest)
        val perioder = mapOgFiltrer(infotrygdPerioder)
        return slåSammenPerioder(perioder)
    }

    private fun hentPersonIdenter(request: PerioderOvergangsstønadRequest): Set<String> {
        return try {
            pdlClient.hentPersonidenter(request.personIdent, true).identer()
        } catch (e: PdlNotFoundException) {
            logger.warn("Finner ikke person, returnerer personIdent i request")
            setOf(request.personIdent)
        }
    }

}