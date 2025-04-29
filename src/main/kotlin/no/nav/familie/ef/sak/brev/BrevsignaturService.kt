package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.dto.SignaturDto
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.vedtak.domain.VedtakErUtenBeslutter
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BrevsignaturService(
    private val personopplysningerService: PersonopplysningerService,
    private val oppgaveClient: OppgaveClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun lagSaksbehandlerSignatur(
        personIdent: String,
        vedtakErUtenBeslutter: VedtakErUtenBeslutter,
    ): SignaturDto {
        val harStrengtFortroligAdresse: Boolean =
            personopplysningerService
                .hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(personIdent)
                .let { it == STRENGT_FORTROLIG || it == STRENGT_FORTROLIG_UTLAND }

        if (harStrengtFortroligAdresse) {
            return SignaturDto(NAV_ANONYM_NAVN, ENHET_VIKAFOSSEN, true)
        }

        val saksbehandler = hentSaksbehandlerInfo(SikkerhetContext.hentSaksbehandler())
        val signaturEnhet = utledSignaturEnhet(saksbehandler.enhetsnavn)

        return SignaturDto(SikkerhetContext.hentSaksbehandlerNavn(true), signaturEnhet, vedtakErUtenBeslutter.value)
    }

    fun lagSaksbehandlerSignatur(
        personIdent: String,
        vedtakErUtenBeslutter: VedtakErUtenBeslutter,
        saksbehandlerNavn: String,
        saksbehandlerIdent: String,
    ) = SignaturDto(saksbehandlerNavn, utledSignaturEnhet(hentSaksbehandlerInfo(saksbehandlerIdent).enhetsnavn), vedtakErUtenBeslutter.value)

    fun lagBeslutterSignatur(
        personIdent: String,
        vedtakErUtenBeslutter: VedtakErUtenBeslutter,
    ): SignaturDto {
        val harStrengtFortroligAdresse: Boolean =
            personopplysningerService
                .hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(personIdent)
                .let { it == STRENGT_FORTROLIG || it == STRENGT_FORTROLIG_UTLAND }

        if (harStrengtFortroligAdresse) {
            return SignaturDto(NAV_ANONYM_NAVN, ENHET_VIKAFOSSEN, true)
        }

        val saksbehandler = hentSaksbehandlerInfo(SikkerhetContext.hentSaksbehandler())
        val signaturNavn = if (vedtakErUtenBeslutter.value) "" else SikkerhetContext.hentSaksbehandlerNavn(true)
        val signaturEnhet = if (vedtakErUtenBeslutter.value) "" else utledSignaturEnhet(saksbehandler.enhetsnavn)

        return SignaturDto(signaturNavn, signaturEnhet, vedtakErUtenBeslutter.value)
    }

    private fun hentSaksbehandlerInfo(navIdent: String) = oppgaveClient.hentSaksbehandlerInfo(navIdent)

    private fun utledSignaturEnhet(enhetsnavn: String) =
        when (enhetsnavn) {
            "NAV ARBEID OG YTELSER SKIEN" -> "Nav arbeid og ytelser Skien"
            "NAV ARBEID OG YTELSER MØRE OG ROMSDAL" -> "Nav arbeid og ytelser Møre og Romsdal"
            "NAV ARBEID OG YTELSER SØRLANDET" -> "Nav arbeid og ytelser Sørlandet"
            else -> loggAdvarselOgReturnerEnhetsnavn(enhetsnavn)
        }

    private fun loggAdvarselOgReturnerEnhetsnavn(enhetsnavn: String): String {
        logger.warn("En saksbehandler med enhet $enhetsnavn har signert et brev. Vurder om vi må legge til dette enhetsnavnet for korrekt visning i brevsignaturen.")
        return ENHET_NAY
    }

    companion object {
        val NAV_ANONYM_NAVN = "Nav anonym"
        val ENHET_VIKAFOSSEN = "Nav Vikafossen"
        val ENHET_NAY = "Nav arbeid og ytelser"
    }
}
