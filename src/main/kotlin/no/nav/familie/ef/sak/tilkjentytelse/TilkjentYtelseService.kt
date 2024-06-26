package no.nav.familie.ef.sak.tilkjentytelse

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.isEqualOrAfter
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.iverksett.tilIverksettDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingTilkjentYtelseDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class TilkjentYtelseService(
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val fagsakService: FagsakService,
) {
    fun hentForBehandlingEllerNull(behandlingId: UUID): TilkjentYtelse? = tilkjentYtelseRepository.findByBehandlingId(behandlingId)

    fun hentForBehandling(behandlingId: UUID): TilkjentYtelse =
        tilkjentYtelseRepository.findByBehandlingId(behandlingId)
            ?: error("Fant ikke tilkjent ytelse med behandlingsid $behandlingId")

    fun opprettTilkjentYtelse(nyTilkjentYtelse: TilkjentYtelse): TilkjentYtelse = tilkjentYtelseRepository.insert(nyTilkjentYtelse)

    fun harLøpendeUtbetaling(behandlingId: UUID): Boolean =
        tilkjentYtelseRepository
            .findByBehandlingId(behandlingId)
            ?.let { it.andelerTilkjentYtelse.any { andel -> andel.stønadTom.isAfter(LocalDate.now()) } } ?: false

    fun finnTilkjentYtelserTilKonsistensavstemming(
        stønadstype: StønadType,
        datoForAvstemming: LocalDate,
    ): List<KonsistensavstemmingTilkjentYtelseDto> {
        val tilkjentYtelser = tilkjentYtelseRepository.finnTilkjentYtelserTilKonsistensavstemming(stønadstype, datoForAvstemming)

        return tilkjentYtelser.chunked(PdlClient.MAKS_ANTALL_IDENTER).map { mapTilDto(it, datoForAvstemming) }.flatten()
    }

    private fun mapTilDto(
        tilkjenteYtelser: List<TilkjentYtelse>,
        datoForAvstemming: LocalDate,
    ): List<KonsistensavstemmingTilkjentYtelseDto> {
        val behandlinger =
            behandlingService
                .hentBehandlinger(tilkjenteYtelser.map { it.behandlingId }.toSet())
                .associateBy { it.id }

        val fagsakerMedOppdatertPersonIdenter =
            fagsakService
                .fagsakerMedOppdatertePersonIdenter(behandlinger.map { it.value.fagsakId })
                .associateBy { it.id }

        return tilkjenteYtelser.map { tilkjentYtelse ->
            val behandling =
                behandlinger[tilkjentYtelse.behandlingId]
                    ?: error("Finner ikke behandling for behandlingId=${tilkjentYtelse.behandlingId}")
            val andelerTilkjentYtelse =
                tilkjentYtelse.andelerTilkjentYtelse
                    .filter { it.stønadTom.isEqualOrAfter(datoForAvstemming) }
                    .filter { it.beløp > 0 }
                    .map { it.tilIverksettDto() }

            val fagsakMedOppdatertPersonIdent =
                fagsakerMedOppdatertPersonIdenter[behandling.fagsakId]
                    ?: error("Finner ikke fagsak for fagsakId=${behandling.fagsakId}")

            KonsistensavstemmingTilkjentYtelseDto(
                behandlingId = tilkjentYtelse.behandlingId,
                eksternBehandlingId = behandling.eksternId,
                eksternFagsakId = fagsakMedOppdatertPersonIdent.eksternId,
                personIdent = fagsakMedOppdatertPersonIdent.hentAktivIdent(),
                andelerTilkjentYtelse = andelerTilkjentYtelse,
            )
        }
    }

    fun slettTilkjentYtelseForBehandling(behandlingId: UUID) {
        brukerfeilHvis(behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke reberegne tilkjent ytelse for en behandling som er låst for videre redigering"
        }
        tilkjentYtelseRepository.findByBehandlingId(behandlingId)?.let { tilkjentYtelseRepository.deleteById(it.id) }
    }
}
