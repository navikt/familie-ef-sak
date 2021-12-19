package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Brevmottakere
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BrevmottakereService(val behandlingRepository: BehandlingRepository) {


    fun lagreBrevmottakere(behandlingId: UUID, brevmottakereDto: BrevmottakereDto): UUID {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)

        val brevmottakere = Brevmottakere(personer = brevmottakereDto.personer, organisasjoner = brevmottakereDto.organisasjoner)

        return behandlingRepository.update(behandling.copy(brevmottakere = brevmottakere)).id
    }

    fun hentBrevmottakere(behandlingId: UUID): BrevmottakereDto? {
        return behandlingRepository.findByIdOrThrow(behandlingId).brevmottakere?.let {
            BrevmottakereDto(personer = it.personer, organisasjoner = it.organisasjoner)
        }
    }

}
