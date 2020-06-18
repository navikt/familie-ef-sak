package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.repository.VedleggRepository
import no.nav.familie.ef.sak.repository.domain.Vedlegg
import no.nav.familie.ef.sak.validering.Sakstilgang
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.*

@Service
class VedleggService(private val vedleggRepository: VedleggRepository,
                     private val sakstilgang: Sakstilgang) {

    fun hentVedlegg(id: UUID): Vedlegg {
        val vedlegg = vedleggRepository.findByIdOrNull(id)
                      ?: throw Feil("Ugyldig Primærnøkkel: $id", httpStatus = HttpStatus.BAD_REQUEST)
        sakstilgang.validerTilgang(vedlegg.sakId)
        return vedlegg
    }

}
