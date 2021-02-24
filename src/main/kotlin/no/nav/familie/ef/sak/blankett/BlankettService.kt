package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.repository.domain.Fil
import org.springframework.stereotype.Service
import java.util.*

@Service
class BlankettService(val blankettRepository: BlankettRepository) {

    fun lagreEllerOppdaterBlankett(behandlingId: UUID, pdf: ByteArray) {
        val blankett = Blankett(behandlingId, Fil(pdf))
        if (!blankettRepository.existsById(behandlingId)) {
            blankettRepository.insert(blankett)
        }
        else {
            blankettRepository.update(blankett)
        }
    }
}