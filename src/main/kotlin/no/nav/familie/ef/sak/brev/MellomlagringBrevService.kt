package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.domain.MellomlagretBrev
import no.nav.familie.ef.sak.brev.dto.MellomlagretBrevResponseDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class MellomlagringBrevService(private val mellomlagerBrevRepository: MellomlagerBrevRepository) {

    fun mellomLagreBrev(behandlingId: UUID, brevverdier: String, brevmal: String, sanityVersjon: String): UUID {

        return when (val mellomlagretBrev = mellomlagerBrevRepository.findByIdOrNull(behandlingId)) {
            null -> mellomlagerBrevRepository.insert(MellomlagretBrev(behandlingId,
                                                                      brevverdier,
                                                                      brevmal,
                                                                      sanityVersjon,
                                                                      LocalDate.now())).behandlingId
            else ->
                mellomlagerBrevRepository.update(mellomlagretBrev.copy(brevverdier = brevverdier,
                                                                       brevmal = brevmal,
                                                                       sanityVersjon = sanityVersjon)).behandlingId
        }

    }

    fun hentOgValiderMellomlagretBrev(behhandlingId: UUID, sanityVersjon: String): MellomlagretBrevResponseDto? {
        val mellomlagretBrev = mellomlagerBrevRepository.findByIdOrNull(behhandlingId)
        if (sanityVersjon == mellomlagretBrev?.sanityVersjon) {
            return MellomlagretBrevResponseDto(brevverdier = mellomlagretBrev.brevverdier, brevmal = mellomlagretBrev.brevmal)
        }
        return null
    }

    fun slettMellomlagringHvisFinnes(behandlingId: UUID) = mellomlagerBrevRepository.deleteById(behandlingId)

}