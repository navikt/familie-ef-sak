package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.repository.domain.Fil
import org.springframework.stereotype.Service
import java.util.*

@Service
class BlankettService(val blankettRepository: BlankettRepository) {

    fun oppdaterBlankett(behandlingId: UUID, pdf: ByteArray) {
        val blankett = Blankett(behandlingId, Fil(pdf))
        blankettRepository.update(blankett)
    }

    fun lagreTomBlankett(behandlingId: UUID) {
        val blankett = Blankett(behandlingId, Fil(byteArrayOf()))
        blankettRepository.insert(blankett)
    }

}