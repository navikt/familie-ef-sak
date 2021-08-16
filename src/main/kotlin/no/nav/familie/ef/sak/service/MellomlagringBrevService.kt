package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.repository.MellomlagerBrevRepository
import no.nav.familie.ef.sak.repository.domain.MellomlagretBrev
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MellomlagringBrevService(private val mellomlagerBrevRepository: MellomlagerBrevRepository) {

    fun mellomLagreBrev(mellomlagretBrev: MellomlagretBrev): UUID {
        return when (mellomlagerBrevRepository.existsById(mellomlagretBrev.behandlingId)) {
            true -> mellomlagerBrevRepository.update(mellomlagretBrev)
            false -> mellomlagerBrevRepository.insert(mellomlagretBrev)
        }.behandlingId
    }

    fun hentMellomlagretBrev(behhandlingId: UUID): MellomlagretBrev? {
        return mellomlagerBrevRepository.findByIdOrNull(behhandlingId)
    }

}
