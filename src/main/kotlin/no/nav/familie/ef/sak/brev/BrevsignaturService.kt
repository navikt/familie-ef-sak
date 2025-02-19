package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.dto.SignaturDto
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.vedtak.domain.VedtakErUtenBeslutter
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND
import org.springframework.stereotype.Service

@Service
class BrevsignaturService(
    private val personopplysningerService: PersonopplysningerService,
) {
    fun lagSaksbehandlerSignatur(
        personIdent: String,
        vedtakErUtenBeslutter: VedtakErUtenBeslutter,
    ): SignaturDto {
        val harStrengtFortroligAdresse: Boolean =
            personopplysningerService
                .hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(personIdent)
                .let { it == STRENGT_FORTROLIG || it == STRENGT_FORTROLIG_UTLAND }

        return if (harStrengtFortroligAdresse) {
            SignaturDto(NAV_ANONYM_NAVN, ENHET_VIKAFOSSEN, true)
        } else {
            SignaturDto(SikkerhetContext.hentSaksbehandlerNavn(true), ENHET_NAY, vedtakErUtenBeslutter.value)
        }
    }

    fun lagBeslutterSignatur(
        personIdent: String,
        vedtakErUtenBeslutter: VedtakErUtenBeslutter,
    ): SignaturDto {
        val harStrengtFortroligAdresse: Boolean =
            personopplysningerService
                .hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(personIdent)
                .let { it == STRENGT_FORTROLIG || it == STRENGT_FORTROLIG_UTLAND }

        val signaturNavn = if (vedtakErUtenBeslutter.value) "" else SikkerhetContext.hentSaksbehandlerNavn(true)
        val signaturEnhet = if (vedtakErUtenBeslutter.value) "" else ENHET_NAY

        return if (harStrengtFortroligAdresse) {
            SignaturDto(NAV_ANONYM_NAVN, ENHET_VIKAFOSSEN, true)
        } else {
            SignaturDto(signaturNavn, signaturEnhet, vedtakErUtenBeslutter.value)
        }
    }

    companion object {
        val NAV_ANONYM_NAVN = "Nav anonym"
        val ENHET_VIKAFOSSEN = "Nav Vikafossen"
        val ENHET_NAY = "Nav Arbeid og ytelser"
    }
}
