package no.nav.familie.ef.sak.felles.util

import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import org.slf4j.LoggerFactory

object Timer {
    private val logger = Logg.getLogger(this::class)

    fun <T> loggTid(
        metadata: String = "",
        call: () -> T,
    ): T {
        val start = System.currentTimeMillis()
        val result = call()
        val caller = Thread.currentThread().stackTrace[2]
        logger.info(
            "class=${caller.className} method=${caller.methodName} " +
                "tid=${System.currentTimeMillis() - start}ms $metadata",
        )
        return result
    }
}
