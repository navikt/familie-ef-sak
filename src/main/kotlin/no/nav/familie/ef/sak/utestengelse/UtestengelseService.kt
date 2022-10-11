package no.nav.familie.ef.sak.utestengelse

import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UtestengelseService(
    private val utestengelseRepository: UtestengelseRepository
) {

    fun opprettUtestengelse(dto: OpprettUtestengelseDto): UtestengelseDto {
        validerFinnesIkkeOverlappendePerioder(dto)

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
            .sortedWith(compareBy({ it.fom }, { it.sporbar.opprettetTid }))

    fun slettUtestengelse(fagsakPersonId: UUID, id: UUID) {
        val utestengelse = utestengelseRepository.findByIdOrThrow(id)
        feilHvis(fagsakPersonId != utestengelse.fagsakPersonId) {
            "FagsakPersonId=$fagsakPersonId er ikke lik utestengelse sin fagsakPersonId(${utestengelse.fagsakPersonId})"
        }
        feilHvis(utestengelse.slettet) {
            "Utestengelse er allerede slettet"
        }
        utestengelseRepository.update(utestengelse.copy(slettet = true))
    }

    private fun validerFinnesIkkeOverlappendePerioder(dto: OpprettUtestengelseDto) {
        val tidligerePerioder = hentUtestengelser(dto.fagsakPersonId).map { Månedsperiode(it.fom, it.tom) }

        tidligerePerioder.firstOrNull { it.overlapper(dto.periode) }?.let {
            throw ApiFeil(
                "Ny utestengelse overlapper med en eksisterende utestengelse ${it.fom}-${it.tom}",
                HttpStatus.BAD_REQUEST
            )
        }
    }
}