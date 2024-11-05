package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Adressebeskyttelse
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse as PdlAdressebeskyttelse

/**
 * @param antallTotalt antall totalt av de man har tilgang til
 * @param antallKontrollert antall kontrollert av de man har tilgang til
 * @param antallManglerKontrollUtenTilgang antall av de som man ikke har tilgang til som ikke er kontrollerte
 */
data class UttrekkArbeidssøkereDto(
    val årMåned: YearMonth,
    val antallTotalt: Int,
    val antallKontrollert: Int,
    val antallManglerKontrollUtenTilgang: Int,
    val arbeidssøkere: List<UttrekkArbeidssøkerDto>,
)

data class UttrekkArbeidssøkerDto(
    val id: UUID,
    val fagsakId: UUID,
    val behandlingIdForVedtak: UUID,
    val personIdent: String,
    val navn: String,
    val registrertArbeidssøker: Boolean?,
    val adressebeskyttelse: Adressebeskyttelse?,
    val kontrollert: Boolean,
    val kontrollertTid: LocalDateTime?,
    val kontrollertAv: String?,
)

fun UttrekkArbeidssøkere.tilDto(
    personIdent: String,
    navn: String,
    adressebeskyttelse: PdlAdressebeskyttelse?,
) = UttrekkArbeidssøkerDto(
    id = this.id,
    fagsakId = this.fagsakId,
    behandlingIdForVedtak = this.vedtakId,
    personIdent = personIdent,
    navn = navn,
    registrertArbeidssøker = this.registrertArbeidssøker,
    adressebeskyttelse = adressebeskyttelse?.let { Adressebeskyttelse.valueOf(it.gradering.name) },
    kontrollert = this.kontrollert,
    kontrollertTid = this.kontrollertTid,
    kontrollertAv = this.kontrollertAv,
)
