package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.repository.domain.Fil
import org.springframework.stereotype.Service
import java.util.*

@Service
class BlankettService(val blankettRepository: BlankettRepository) {

    fun lagreBlankett(behandlingId : UUID, pdf : Fil)  {
        val blankett = Blankett(behandlingId, pdf)
        blankettRepository.insert(blankett)
    }
}