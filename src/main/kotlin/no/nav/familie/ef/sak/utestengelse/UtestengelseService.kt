package no.nav.familie.ef.sak.utestengelse

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UtestengelseService(
    private val utestengelseRepository: UtestengelseRepository
) {

    fun opprettUtestengelse(dto: OpprettUtestengelseDto): UtestengelseDto {
        // TODO burde vi validere noe her att det ikke finnes noe utestengelse for den perioden?
        return utestengelseRepository.insert(
            Utestengelse(
                fagsakPersonId = dto.fagsakPersonId,
                fom = dto.periode.fomDato,
                tom = dto.periode.tomDato
            )
        ).tilDto()
    }

    fun hentUtestengelser(fagsakPersonId: UUID): List<Utestengelse> =
        utestengelseRepository.findAllByFagsakPersonId(fagsakPersonId)
            .sortedBy { it.sporbar.opprettetTid }

    fun slettUtestengelse(fagsakPersonId: UUID, id: UUID) {
        val utestengelse = utestengelseRepository.findByIdOrThrow(id)
        feilHvis(fagsakPersonId != utestengelse.fagsakPersonId) {
            "FagsakPersonId=$fagsakPersonId matcher ikke utestengelse(${utestengelse.fagsakPersonId})"
        }
        feilHvis(utestengelse.slettet) {
            "Utestengelse er allerede slettet"
        }
        utestengelseRepository.update(utestengelse.copy(slettet = true))
    }
}