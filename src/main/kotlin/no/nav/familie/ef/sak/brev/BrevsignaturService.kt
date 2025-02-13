package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.brev.dto.SignaturDto
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.VedtakErUtenBeslutter
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND
import org.springframework.stereotype.Service

@Service
class BrevsignaturService(
    val personopplysningerService: PersonopplysningerService,
    val vedtakService: VedtakService,
) {
    fun lagSignaturMedEnhet(
        fagsak: Fagsak,
        vedtakErUtenBeslutter: Boolean,
    ): SignaturDto = lagSignaturDto(fagsak.hentAktivIdent(), vedtakErUtenBeslutter)

    fun lagSignaturMedEnhet(
        saksbehandling: Saksbehandling,
        vedtakErUtenBeslutter: VedtakErUtenBeslutter,
    ): SignaturDto = lagSignaturDto(saksbehandling.ident, vedtakErUtenBeslutter.value)

    private fun lagSignaturDto(
        ident: String,
        vedtakErUtenBeslutter: Boolean,
    ): SignaturDto {
        val harStrengtFortroligAdresse: Boolean =
            personopplysningerService
                .hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(ident)
                .let { it == STRENGT_FORTROLIG || it == STRENGT_FORTROLIG_UTLAND }

        return if (harStrengtFortroligAdresse) {
            SignaturDto(NAV_ANONYM_NAVN, ENHET_VIKAFOSSEN, true)
        } else {
            SignaturDto(SikkerhetContext.hentSaksbehandlerNavn(true), ENHET_NAY, vedtakErUtenBeslutter)
        }
    }

    companion object {
        val NAV_ANONYM_NAVN = "Nav anonym"
        val ENHET_VIKAFOSSEN = "Nav Vikafossen"
        val ENHET_NAY = "Nav Arbeid og ytelser"
    }
}
