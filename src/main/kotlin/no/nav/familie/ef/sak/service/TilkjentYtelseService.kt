package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.integration.OppdragClient
import no.nav.familie.ef.sak.mapper.tilDto
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.økonomi.tilKlassifisering
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class TilkjentYtelseService(private val oppdragClient: OppdragClient,
                            private val fagsakService: FagsakService,
                            private val tilkjentYtelseRepository: TilkjentYtelseRepository
) {

    fun hentStatus(behandling: Behandling): OppdragStatus {
        val tilkjentYtelse = hentTilkjentYtelseFraBehandlingId(behandling.id)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        val oppdragId = OppdragId(fagsystem = fagsak.stønadstype.tilKlassifisering(),
                                  personIdent = tilkjentYtelse.personident,
                                  behandlingsId = behandling.eksternId.id.toString())

        return oppdragClient.hentStatus(oppdragId)
    }

    fun hentTilkjentYtelseDto(tilkjentYtelseId: UUID): TilkjentYtelseDTO {
        val tilkjentYtelse = hentTilkjentYtelse(tilkjentYtelseId)
        return tilkjentYtelse.tilDto()
    }

    private fun hentTilkjentYtelse(tilkjentYtelseId: UUID) =
            tilkjentYtelseRepository.findByIdOrNull(tilkjentYtelseId)
            ?: error("Fant ikke tilkjent ytelse med id $tilkjentYtelseId")


    private fun hentTilkjentYtelseFraBehandlingId(behandlingId: UUID) =
            tilkjentYtelseRepository.findByBehandlingId(behandlingId)
            ?: error("Fant ikke tilkjent ytelse med behandlingsid $behandlingId")

}
