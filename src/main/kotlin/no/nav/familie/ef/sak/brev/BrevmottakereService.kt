package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.domain.Brevmottakere
import no.nav.familie.ef.sak.brev.domain.OrganisasjonerWrapper
import no.nav.familie.ef.sak.brev.domain.PersonerWrapper
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BrevmottakereService(val brevmottakereRepository: BrevmottakereRepository) {

    fun lagreBrevmottakere(behandlingId: UUID, brevmottakereDto: BrevmottakereDto): UUID {
        validerAntallBrevmottakere(brevmottakereDto)
        validerUnikeBrevmottakere(brevmottakereDto)

        val brevmottakere = Brevmottakere(behandlingId,
                                          PersonerWrapper(brevmottakereDto.personer),
                                          OrganisasjonerWrapper(brevmottakereDto.organisasjoner))

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

    private fun validerUnikeBrevmottakere(brevmottakereDto: BrevmottakereDto) {
        val personmottakerIdenter = brevmottakereDto.personer.map { it.personIdent }
        brukerfeilHvisIkke(personmottakerIdenter.distinct().size == personmottakerIdenter.size) {
            "En person kan bare legges til en gang som brevmottaker"
        }

        val organisasjonsmottakerIdenter = brevmottakereDto.organisasjoner.map { it.organisasjonsnummer }
        brukerfeilHvisIkke(organisasjonsmottakerIdenter.distinct().size == organisasjonsmottakerIdenter.size) {
            "En organisasjon kan bare legges til en gang som brevmottaker"
        }
    }
}
