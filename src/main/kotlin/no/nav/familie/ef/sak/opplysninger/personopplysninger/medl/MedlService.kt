package no.nav.familie.ef.sak.opplysninger.personopplysninger.medl

import no.nav.familie.ef.sak.felles.util.FnrUtil
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException

@Service
class MedlService(
    private val medlClient: MedlClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentMedlemskapsunntak(ident: String): Medlemskapsinfo = MedlemskapsinfoMapper.tilMedlemskapsInfo(hentMedlemskapsunntakFraMedl(ident))

    /**
     * MEDL slår opp personen i pip-api, som ikke kjenner til NPID, og svarer da med 404.
     * For personer som kun har NPID behandles dette som at det ikke finnes noen unntak i MEDL.
     */
    private fun hentMedlemskapsunntakFraMedl(ident: String): List<Medlemskapsunntak> =
        try {
            medlClient.hentMedlemskapsUnntak(ident)
        } catch (e: HttpClientErrorException.NotFound) {
            if (!FnrUtil.erNpid(ident)) throw e
            logger.warn("Fant ikke person med NPID i MEDL, fortsetter uten medlemskapsunntak")
            secureLogger.warn("Fant ikke person med NPID=$ident i MEDL, fortsetter uten medlemskapsunntak")
            emptyList()
        }
}
