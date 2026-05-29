package no.nav.familie.ef.sak.ekstern.soknad

import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.TidligereVedtaksperioderService
import no.nav.familie.ef.sak.vilkår.dto.TidligereVedtaksperioderDto
import no.nav.familie.ef.sak.vilkår.dto.tilDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EksternSøknadService(
    private val personService: PersonService,
    private val tidligereVedtaksperioderService: TidligereVedtaksperioderService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun harTidligereInnvilgetVedtak(personIdent: String): TidligereVedtakStatus =
        try {
            val folkeregisteridentifikatorer = personService.hentSøker(personIdent).folkeregisteridentifikator
            val tidligereVedtaksperioder = tidligereVedtaksperioderService.hentTidligereVedtaksperioder(folkeregisteridentifikatorer)

            tidligereVedtaksperioder.tilDto().tilTidligereVedtakStatus()
        } catch (e: Exception) {
            logger.warn("Feil ved sjekk av tidligere innvilget vedtak", e)
            TidligereVedtakStatus.VET_IKKE
        }
}

private fun TidligereVedtaksperioderDto.tilTidligereVedtakStatus(): TidligereVedtakStatus {
    val harFraInfotrygd = infotrygd?.harTidligereInnvilgetVedtak() ?: false
    val harFraSak = sak?.harTidligereInnvilgetVedtak() ?: false

    if (harFraInfotrygd || harFraSak) return TidligereVedtakStatus.JA
    if (historiskPensjon == null) return TidligereVedtakStatus.VET_IKKE
    if (historiskPensjon) return TidligereVedtakStatus.JA

    return TidligereVedtakStatus.NEI
}
