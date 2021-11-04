package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.domain.Fritekstbrev
import no.nav.familie.ef.sak.brev.domain.MellomlagretBrev
import no.nav.familie.ef.sak.brev.domain.MellomlagretFritekstbrev
import no.nav.familie.ef.sak.brev.dto.MellomlagretBrevFritekst
import no.nav.familie.ef.sak.brev.dto.MellomlagretBrevResponse
import no.nav.familie.ef.sak.brev.dto.MellomlagretBrevSanity
import no.nav.familie.ef.sak.brev.dto.VedtaksbrevFritekstDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class MellomlagringBrevService(private val mellomlagerBrevRepository: MellomlagerBrevRepository,
                               private val mellomlagerFritekstbrevRepository: MellomlagerFritekstbrevRepository) {

    fun mellomLagreBrev(behandlingId: UUID, brevverdier: String, brevmal: String, sanityVersjon: String): UUID {
        slettMellomlagringHvisFinnes(behandlingId)
        val mellomlagretBrev = MellomlagretBrev(behandlingId,
                                                brevverdier,
                                                brevmal,
                                                sanityVersjon,
                                                LocalDate.now())
        return mellomlagerBrevRepository.insert(mellomlagretBrev).behandlingId
    }

    fun mellomlagreFritekstbrev(mellomlagretBrev: VedtaksbrevFritekstDto): UUID {
        slettMellomlagringHvisFinnes(mellomlagretBrev.behandlingId)
        val mellomlagretFritekstbrev = MellomlagretFritekstbrev(mellomlagretBrev.behandlingId,
                                                                Fritekstbrev(overskrift = mellomlagretBrev.overskrift,
                                                                             avsnitt = mellomlagretBrev.avsnitt))

        return mellomlagerFritekstbrevRepository.insert(mellomlagretFritekstbrev).behandlingId
    }

    fun hentOgValiderMellomlagretBrev(behhandlingId: UUID, sanityVersjon: String): MellomlagretBrevResponse? {
        mellomlagerBrevRepository.findByIdOrNull(behhandlingId)?.let {
            if (sanityVersjon == it.sanityVersjon) {
                return MellomlagretBrevSanity(brevverdier = it.brevverdier,
                                              brevmal = it.brevmal)
            }
            return null
        }
        mellomlagerFritekstbrevRepository.findByIdOrNull(behhandlingId)?.let {
            return MellomlagretBrevFritekst(brev = it.brev)
        }
        return null
    }

    fun slettMellomlagringHvisFinnes(behandlingId: UUID) {
        mellomlagerBrevRepository.deleteById(behandlingId)
        mellomlagerFritekstbrevRepository.deleteById(behandlingId)
    }

}