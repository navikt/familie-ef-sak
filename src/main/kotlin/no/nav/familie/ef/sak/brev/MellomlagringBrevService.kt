package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.dto.MellomlagretBrevResponse
import no.nav.familie.ef.sak.brev.dto.MellomlagretBrevSanity
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
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
        // Bruker upsert fremfor delete+insert for å unngå duplicate key ved samtidige mellomlagringer
        mellomlagerBrevRepository.upsert(
            behandlingId,
            brevverdier,
            brevmal,
            sanityVersjon,
            LocalDate.now(),
        )
        return behandlingId
    }

    fun mellomLagreFrittståendeSanitybrev(
        fagsakId: UUID,
        brevverdier: String,
        brevmal: String,
    ): UUID {
        // Bruker upsert fremfor delete+insert for å unngå duplicate key ved samtidige mellomlagringer
        mellomlagerFrittståendeSanitybrevRepository.upsert(
            UUID.randomUUID(),
            fagsakId,
            brevverdier,
            brevmal,
            SikkerhetContext.hentSaksbehandler(),
            LocalDateTime.now(),
        )
        return fagsakId
    }

    fun hentMellomlagretFrittståendeSanitybrev(fagsakId: UUID): MellomlagretBrevResponse? =
        mellomlagerFrittståendeSanitybrevRepository
            .findByFagsakIdAndSaksbehandlerIdent(
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
        mellomlagerFrittståendeSanitybrevRepository
            .findByFagsakIdAndSaksbehandlerIdent(fagsakId, saksbehandlerIdent)
            ?.let { mellomlagerFrittståendeSanitybrevRepository.deleteById(it.id) }
    }
}
