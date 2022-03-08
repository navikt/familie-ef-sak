package no.nav.familie.ef.sak.testutil

import no.nav.familie.util.FnrGenerator
import java.time.LocalDate

object FnrGeneratorUtil {

    fun genererFnr(localDate: LocalDate) =
            FnrGenerator.generer(localDate.year, localDate.month.value, localDate.dayOfMonth, false)
}
