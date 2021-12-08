package no.nav.familie.ef.sak.felles.util

import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.MDC

object MDCUtils {

    fun getCallId() = MDC.get(MDCConstants.MDC_CALL_ID) ?: error("Finner ikke CALL_ID i MDC")
}