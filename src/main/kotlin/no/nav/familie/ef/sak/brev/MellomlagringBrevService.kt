package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.BrevmottakerUtil.validerUnikeBrevmottakere
import no.nav.familie.ef.sak.brev.domain.Fritekstbrev
import no.nav.familie.ef.sak.brev.domain.FrittståendeBrevmottakere
import no.nav.familie.ef.sak.brev.domain.MellomlagretBrev
import no.nav.familie.ef.sak.brev.domain.MellomlagretFritekstbrev
import no.nav.familie.ef.sak.brev.domain.MellomlagretFrittståendeBrev
import no.nav.familie.ef.sak.brev.domain.MellomlagretFrittståendeSanitybrev
import no.nav.familie.ef.sak.brev.dto.FritekstBrevDto
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevDto
import no.nav.familie.ef.sak.brev.dto.MellomlagretBrevFritekst
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
    private val mellomlagerFritekstbrevRepository: MellomlagerFritekstbrevRepository,
    private val mellomlagerFrittståendeBrevRepository: MellomlagerFrittståendeBrevRepository,
    private val mellomlagerFrittståendeSanitybrevRepository: MellomlagerFrittståendeSanitybrevRepository,
) {

    fun mellomLagreBrev(behandlingId: UUID, brevverdier: String, brevmal: String, sanityVersjon: String): UUID {
        slettMellomlagringHvisFinnes(behandlingId)
        val mellomlagretBrev = MellomlagretBrev(
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
        val mellomlagretBrev = MellomlagretFrittståendeSanitybrev(
            fagsakId = fagsakId,
            brevverdier = brevverdier,
            brevmal = brevmal,
        )
        return mellomlagerFrittståendeSanitybrevRepository.insert(mellomlagretBrev).fagsakId
    }

    @Deprecated("Skal slettes")
    fun mellomlagreFritekstbrev(mellomlagretBrev: FritekstBrevDto): UUID {
        slettMellomlagringHvisFinnes(mellomlagretBrev.behandlingId)
        val mellomlagretFritekstbrev = MellomlagretFritekstbrev(
            mellomlagretBrev.behandlingId,
            Fritekstbrev(
                overskrift = mellomlagretBrev.overskrift,
                avsnitt = mellomlagretBrev.avsnitt,
            ),
            brevType = mellomlagretBrev.brevType,
        )

        return mellomlagerFritekstbrevRepository.insert(mellomlagretFritekstbrev).behandlingId
    }

    @Deprecated("Skal slettes")
    fun mellomlagreFrittståendeBrev(mellomlagretBrev: FrittståendeBrevDto): UUID {
        mellomlagretBrev.mottakere?.let { validerUnikeBrevmottakere(it) }
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandler()
        slettMellomlagretFrittståendeBrev(mellomlagretBrev.fagsakId, saksbehandlerIdent)
        val mellomlagretFrittståendeBrev = MellomlagretFrittståendeBrev(
            fagsakId = mellomlagretBrev.fagsakId,
            brev =
            Fritekstbrev(
                overskrift = mellomlagretBrev.overskrift,
                avsnitt = mellomlagretBrev.avsnitt,
            ),
            brevType =
            mellomlagretBrev.brevType,
            saksbehandlerIdent = saksbehandlerIdent,
            mottakere = mellomlagretBrev.mottakere?.let { FrittståendeBrevmottakere(it.personer, it.organisasjoner) },
        )
        return mellomlagerFrittståendeBrevRepository.insert(mellomlagretFrittståendeBrev).fagsakId
    }

    @Deprecated("Skal slettes")
    fun hentMellomlagretFrittståendeBrev(fagsakId: UUID): FrittståendeBrevDto? {
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandler()
        return mellomlagerFrittståendeBrevRepository.findByFagsakIdAndSaksbehandlerIdent(fagsakId, saksbehandlerIdent)?.let {
            FrittståendeBrevDto(
                it.brev.overskrift,
                it.brev.avsnitt,
                fagsakId,
                it.brevType,
                it.mottakere?.let { mottakere -> BrevmottakereDto(mottakere.personer, mottakere.organisasjoner) },
            )
        }
    }

    fun hentMellomlagretFrittståendeSanitybrev(fagsakId: UUID): MellomlagretBrevResponse? =
        mellomlagerFrittståendeSanitybrevRepository.findByFagsakIdAndSaksbehandlerIdent(
            fagsakId,
            SikkerhetContext.hentSaksbehandler(),
        )?.let { MellomlagretBrevSanity(brevverdier = it.brevverdier, brevmal = it.brevmal) }

    fun hentOgValiderMellomlagretBrev(behhandlingId: UUID, sanityVersjon: String): MellomlagretBrevResponse? {
        mellomlagerBrevRepository.findByIdOrNull(behhandlingId)?.let {
            if (sanityVersjon == it.sanityVersjon) {
                return MellomlagretBrevSanity(
                    brevverdier = it.brevverdier,
                    brevmal = it.brevmal,
                )
            }
            return null
        }
        return mellomlagerFritekstbrevRepository.findByIdOrNull(behhandlingId)?.let {
            MellomlagretBrevFritekst(brev = it.brev, brevType = it.brevType)
        }
    }

    fun slettMellomlagringHvisFinnes(behandlingId: UUID) {
        mellomlagerBrevRepository.deleteById(behandlingId)
        mellomlagerFritekstbrevRepository.deleteById(behandlingId)
    }

    fun slettMellomlagretFrittståendeBrev(fagsakId: UUID, saksbehandlerIdent: String) {
        mellomlagerFrittståendeBrevRepository.findByFagsakIdAndSaksbehandlerIdent(fagsakId, saksbehandlerIdent)
            ?.let { mellomlagerFrittståendeBrevRepository.deleteById(it.id) }

        mellomlagerFrittståendeSanitybrevRepository.findByFagsakIdAndSaksbehandlerIdent(fagsakId, saksbehandlerIdent)
            ?.let { mellomlagerFrittståendeSanitybrevRepository.deleteById(it.id) }
    }
}
