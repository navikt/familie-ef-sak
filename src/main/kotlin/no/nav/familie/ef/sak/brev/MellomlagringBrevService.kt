package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.domain.Fritekstbrev
import no.nav.familie.ef.sak.brev.domain.MellomlagretBrev
import no.nav.familie.ef.sak.brev.domain.MellomlagretFritekstbrev
import no.nav.familie.ef.sak.brev.domain.MellomlagretFrittståendeBrev
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevDto
import no.nav.familie.ef.sak.brev.dto.MellomlagretBrevFritekst
import no.nav.familie.ef.sak.brev.dto.MellomlagretBrevResponse
import no.nav.familie.ef.sak.brev.dto.MellomlagretBrevSanity
import no.nav.familie.ef.sak.brev.dto.VedtaksbrevFritekstDto
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class MellomlagringBrevService(private val mellomlagerBrevRepository: MellomlagerBrevRepository,
                               private val mellomlagerFritekstbrevRepository: MellomlagerFritekstbrevRepository,
                               private val mellomlagerFrittståendeBrevRepository: MellomlagerFrittståendeBrevRepository) {

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

    fun mellomlagreFrittståendeBrev(mellomlagretBrev: FrittståendeBrevDto): UUID {
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandler(true)
        slettMellomlagretFrittståendeBrev(mellomlagretBrev, saksbehandlerIdent)
        val mellomlagretFrittståendeBrev = MellomlagretFrittståendeBrev(fagsakId = mellomlagretBrev.fagsakId,
                                                                        brev =
                                                                        Fritekstbrev(overskrift = mellomlagretBrev.overskrift,
                                                                                     avsnitt = mellomlagretBrev.avsnitt),
                                                                        brevType =
                                                                        mellomlagretBrev.brevType,
                                                                        saksbehandlerIdent = saksbehandlerIdent)
        return mellomlagerFrittståendeBrevRepository.insert(mellomlagretFrittståendeBrev).fagsakId
    }

    fun hentMellomlagretFrittståendeBrev(fagsakId: UUID): FrittståendeBrevDto? {
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandler(true)
        mellomlagerFrittståendeBrevRepository.findByFagsakIdAndSaksbehandlerIdent(fagsakId, saksbehandlerIdent)?.let {
            return FrittståendeBrevDto(it.brev.overskrift,
                                       it.brev.avsnitt,
                                       fagsakId,
                                       it.brevType)
        }
        return null
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

    private fun slettMellomlagretFrittståendeBrev(mellomlagretBrev: FrittståendeBrevDto, saksbehandlerIdent: String) {
        mellomlagerFrittståendeBrevRepository.findByFagsakIdAndSaksbehandlerIdent(mellomlagretBrev.fagsakId,
                                                                                  saksbehandlerIdent)
                ?.let { mellomlagerFrittståendeBrevRepository.deleteById(it.id) }
    }
}