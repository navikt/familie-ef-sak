package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.repository.domain.Fil
import org.springframework.stereotype.Service
import java.util.*

@Service
class BlankettService(val blankettRepository: BlankettRepository) {

    fun lagreBlankett(behandlingId: UUID, pdf: ByteArray) {
        val blankett = Blankett(behandlingId, Fil(pdf))
        blankettRepository.insert(blankett)
    }
}