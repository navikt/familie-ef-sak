package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.BrevmottakerUtil.validerUnikeBrevmottakere
import no.nav.familie.ef.sak.brev.domain.Brevmottakere
import no.nav.familie.ef.sak.brev.domain.BrevmottakereFrittståendeBrev
import no.nav.familie.ef.sak.brev.domain.OrganisasjonerWrapper
import no.nav.familie.ef.sak.brev.domain.PersonerWrapper
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BrevmottakereService(
    val brevmottakereRepository: BrevmottakereRepository,
    val frittståendeBrevmottakereRepository: FrittståendeBrevmottakereRepository,
) {

    fun lagreBrevmottakere(behandlingId: UUID, brevmottakereDto: BrevmottakereDto): UUID {
        validerAntallBrevmottakere(brevmottakereDto)
        validerUnikeBrevmottakere(brevmottakereDto)

        val brevmottakere = Brevmottakere(
            behandlingId,
            PersonerWrapper(brevmottakereDto.personer),
            OrganisasjonerWrapper(brevmottakereDto.organisasjoner),
        )

        return when (brevmottakereRepository.existsById(behandlingId)) {
            true ->
                brevmottakereRepository.update(brevmottakere)

            false ->
                brevmottakereRepository.insert(brevmottakere)
        }.behandlingId
    }

    fun hentBrevmottakere(behandlingId: UUID): BrevmottakereDto? {
        return brevmottakereRepository.findByIdOrNull(behandlingId)?.let {
            BrevmottakereDto(personer = it.personer.personer, organisasjoner = it.organisasjoner.organisasjoner)
        }
    }

    fun hentBrevnottakereForFagsak(fagsakId: UUID): BrevmottakereDto? {
        return frittståendeBrevmottakereRepository.findByFagsakIdAndSaksbehandlerIdent(
            fagsakId,
            SikkerhetContext.hentSaksbehandler(),
        )?.let { BrevmottakereDto(personer = it.personer.personer, organisasjoner = it.organisasjoner.organisasjoner) }
    }

    fun lagreBrevmottakereForFagsak(fagsakId: UUID, brevmottakereDto: BrevmottakereDto): UUID {
        validerAntallBrevmottakere(brevmottakereDto)
        validerUnikeBrevmottakere(brevmottakereDto)

        val eksistrendeBrevmottakere =
            frittståendeBrevmottakereRepository.findByFagsakIdAndSaksbehandlerIdent(
                fagsakId,
                SikkerhetContext.hentSaksbehandler(),
            )

        return if ((eksistrendeBrevmottakere != null)) {
            oppdaterBrevmottakere(eksistrendeBrevmottakere, fagsakId, brevmottakereDto)
        } else {
            opprettBrevmottakere(fagsakId, brevmottakereDto)
        }.fagsakId
    }

    fun slettBrevmottakereForFagsakOgSaksbehandlerHvisFinnes(fagsakId: UUID, saksbehandlerIdent: String) =
        frittståendeBrevmottakereRepository.findByFagsakIdAndSaksbehandlerIdent(fagsakId, saksbehandlerIdent)?.let {
            frittståendeBrevmottakereRepository.deleteById(it.id)
        }

    private fun opprettBrevmottakere(
        fagsakId: UUID,
        brevmottakereDto: BrevmottakereDto,
    ) = frittståendeBrevmottakereRepository.insert(
        BrevmottakereFrittståendeBrev(
            fagsakId = fagsakId,
            personer = PersonerWrapper(brevmottakereDto.personer),
            organisasjoner = OrganisasjonerWrapper(brevmottakereDto.organisasjoner),
        ),
    )

    private fun oppdaterBrevmottakere(
        eksistrendeBrevmottakere: BrevmottakereFrittståendeBrev,
        fagsakId: UUID,
        brevmottakereDto: BrevmottakereDto,
    ) = frittståendeBrevmottakereRepository.update(
        BrevmottakereFrittståendeBrev(
            id = eksistrendeBrevmottakere.id,
            fagsakId = fagsakId,
            personer = PersonerWrapper(brevmottakereDto.personer),
            organisasjoner = OrganisasjonerWrapper(brevmottakereDto.organisasjoner),
        ),
    )

    private fun validerAntallBrevmottakere(brevmottakere: BrevmottakereDto) {
        val antallPersonmottakere = brevmottakere.personer.size
        val antallOrganisasjonMottakere = brevmottakere.organisasjoner.size
        val antallMottakere = antallPersonmottakere + antallOrganisasjonMottakere
        brukerfeilHvis(antallMottakere == 0) {
            "Vedtaksbrevet må ha minst 1 mottaker"
        }
        brukerfeilHvis(antallMottakere > 2) {
            "Vedtaksbrevet kan ikke ha mer enn 2 mottakere"
        }
    }
}
