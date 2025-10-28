package no.nav.familie.ef.sak.opplysninger.personopplysninger.medl

import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.springframework.stereotype.Service

@Service
class MedlService(
    private val medlClient: MedlClient,
) {
    fun hentMedlemskapsunntak(ident: String): Medlemskapsinfo = MedlemskapsinfoMapper.tilMedlemskapsInfo(medlClient.hentMedlemskapsUnntak(ident))
}
