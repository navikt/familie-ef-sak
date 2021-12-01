package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Adressebeskyttelse
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse as PdlAdressebeskyttelse

data class UttrekkArbeidssøkereDto(
        val årMåned: YearMonth,
        val antallTotalt: Int,
        val antallKontrollert: Int,
        val arbeidssøkere: List<UttrekkArbeidssøkerDto>
)

data class UttrekkArbeidssøkerDto(
        val id: UUID,
        val fagsakId: UUID,
        val behandlingIdForVedtak: UUID,
        val personIdent: String,
        val navn: String,
        val adressebeskyttelse: Adressebeskyttelse?,
        val kontrollert: Boolean,
        val kontrollertTid: LocalDateTime?,
        val kontrollertAv: String?
)

fun UttrekkArbeidssøkere.tilDto(personIdent: String, navn: String, adressebeskyttelse: PdlAdressebeskyttelse?) =
        UttrekkArbeidssøkerDto(id = this.id,
                               fagsakId = this.fagsakId,
                               behandlingIdForVedtak = this.vedtakId,
                               personIdent = personIdent,
                               navn = navn,
                               adressebeskyttelse = adressebeskyttelse?.let { Adressebeskyttelse.valueOf(it.gradering.name) },
                               kontrollert = this.kontrollert,
                               kontrollertTid = this.kontrollertTid,
                               kontrollertAv = this.kontrollertAv)
