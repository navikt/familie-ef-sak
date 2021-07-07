package no.nav.familie.ef.sak.service

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.repository.MellomlagerBrevRepository
import no.nav.familie.ef.sak.repository.VedtaksbrevRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.Fil
import no.nav.familie.ef.sak.repository.domain.MellomlagretBrev
import no.nav.familie.ef.sak.repository.domain.Vedtaksbrev
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.vedtaksbrev.BrevClient
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MellomlagringBrevService(private val mellomlagerBrevRepository: MellomlagerBrevRepository) {

    fun mellomLagreBrev(mellomlagretBrev: MellomlagretBrev): UUID {
        return mellomlagerBrevRepository.insert(mellomlagretBrev).behandlingId
    }

    fun hentMellomlagretBrev(behhandlingId: UUID): MellomlagretBrev {
        return mellomlagerBrevRepository.findByIdOrThrow(behhandlingId)
    }

}
