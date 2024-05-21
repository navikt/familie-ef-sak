package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.domain.MellomlagretBrev
import no.nav.familie.ef.sak.brev.domain.MellomlagretFrittståendeSanitybrev
import no.nav.familie.ef.sak.brev.dto.MellomlagretBrevResponse
import no.nav.familie.ef.sak.brev.dto.MellomlagretBrevSanity
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class MellomlagringBrevService(
    private val mellomlagerBrevRepository: MellomlagerBrevRepository,
    private val mellomlagerFrittståendeSanitybrevRepository: MellomlagerFrittståendeSanitybrevRepository,
) {
    fun mellomLagreBrev(
        behandlingId: UUID,
        brevverdier: String,
        brevmal: String,
        sanityVersjon: String,
    ): UUID {
        slettMellomlagringHvisFinnes(behandlingId)
        val mellomlagretBrev =
            MellomlagretBrev(
                behandlingId,
                brevverdier,
                brevmal,
                sanityVersjon,
                LocalDate.now(),
            )
        return mellomlagerBrevRepository.insert(mellomlagretBrev).behandlingId
    }

    fun mellomLagreFrittståendeSanitybrev(
        fagsakId: UUID,
        brevverdier: String,
        brevmal: String,
    ): UUID {
        slettMellomlagretFrittståendeBrev(fagsakId, SikkerhetContext.hentSaksbehandler())
        val mellomlagretBrev =
            MellomlagretFrittståendeSanitybrev(
                fagsakId = fagsakId,
                brevverdier = brevverdier,
                brevmal = brevmal,
            )
        return mellomlagerFrittståendeSanitybrevRepository.insert(mellomlagretBrev).fagsakId
    }

    fun hentMellomlagretFrittståendeSanitybrev(fagsakId: UUID): MellomlagretBrevResponse? =
        mellomlagerFrittståendeSanitybrevRepository.findByFagsakIdAndSaksbehandlerIdent(
            fagsakId,
            SikkerhetContext.hentSaksbehandler(),
        )?.let { MellomlagretBrevSanity(brevverdier = it.brevverdier, brevmal = it.brevmal) }

    fun hentOgValiderMellomlagretBrev(
        behhandlingId: UUID,
        sanityVersjon: String,
    ): MellomlagretBrevResponse? =
        mellomlagerBrevRepository.findByIdOrNull(behhandlingId)?.let {
            if (sanityVersjon == it.sanityVersjon) {
                return MellomlagretBrevSanity(
                    brevverdier = it.brevverdier,
                    brevmal = it.brevmal,
                )
            }
            return null
        }

    fun slettMellomlagringHvisFinnes(behandlingId: UUID) {
        mellomlagerBrevRepository.deleteById(behandlingId)
    }

    fun slettMellomlagretFrittståendeBrev(
        fagsakId: UUID,
        saksbehandlerIdent: String,
    ) {
        mellomlagerFrittståendeSanitybrevRepository.findByFagsakIdAndSaksbehandlerIdent(fagsakId, saksbehandlerIdent)
            ?.let { mellomlagerFrittståendeSanitybrevRepository.deleteById(it.id) }
    }
}
