package no.nav.familie.ef.sak.sigrun

import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.sigrun.ekstern.BeregnetSkatt
import no.nav.familie.ef.sak.sigrun.ekstern.SigrunClient
import no.nav.familie.ef.sak.sigrun.ekstern.SummertSkattegrunnlag
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.UUID

@Service
class SigrunService(val sigrunClient: SigrunClient, val fagsakPersonService: FagsakPersonService) {

    val forrigeÅr = YearMonth.now().year - 1
    val listInntektsår = listOf(forrigeÅr, forrigeÅr - 1, forrigeÅr - 2)

    fun hentInntektSisteTreÅr(fagsakPersonId: UUID): List<PensjonsgivendeInntektVisning> {
        val aktivIdent = fagsakPersonService.hentAktivIdent(fagsakPersonId)

        val beregnetSkattegrunnlagSisteTreÅr = listInntektsår.map { sigrunClient.hentBeregnetSkatt(aktivIdent, it).mapTilPensjonsgivendeInntektVisning(it) }
        /* Venter med kall mot beregnet skattegrunnlag - trenger blant annet eget filter
        val inntektFraSvalbard = hentInntektFraSvalbardSisteTreÅr(aktivIdent)
        beregnetSkattegrunnlagSisteTreÅr.forEach {
            it.verdi += inntektFraSvalbard.find { svalbard -> svalbard.inntektsaar == it.inntektsaar }?.verdi ?: 0
        }
         */

        return beregnetSkattegrunnlagSisteTreÅr
    }

    private fun hentInntektFraSvalbardSisteTreÅr(aktivIdent: String): List<PensjonsgivendeInntektVisning> {
        val inntektFraSvalbardSisteTreÅr = listInntektsår.map {
            sigrunClient.hentSummertSkattegrunnlag(aktivIdent, it).mapSvalbardGrunnlagTilPensjonsgivendeInntektVisning(it)
        }

        return inntektFraSvalbardSisteTreÅr
    }
}

val næringKodeverdiBeregnetSkatt = "personinntektNaering"
val skatteoppgjørsdatoKodeverdi = "skatteoppgjoersdato"

private fun List<BeregnetSkatt>.mapTilPensjonsgivendeInntektVisning(inntektsaar: Int): PensjonsgivendeInntektVisning {
    // Skatteoppgjørsdato er en del av response, og bruker samme felter som inntekt. Verdi er datoen for skatteoppgjøret og teknisk navn er "skatteoppgjoersdato".
    // Dette filtreres vekk for at summeringen av inntekt skal gå bra, i tillegg til at det ikke er sett behov for å vise skatteoppgjørsdato i frontend.
    val inntekt = this.filter { it.tekniskNavn != skatteoppgjørsdatoKodeverdi }

    val næring = inntekt.filter { it.tekniskNavn == næringKodeverdiBeregnetSkatt }.sumOf { it.verdi.toInt() }
    val person = inntekt.filter { it.tekniskNavn != næringKodeverdiBeregnetSkatt }.sumOf { it.verdi.toInt() }
    return PensjonsgivendeInntektVisning(inntektsaar, næring, person)
}

private fun SummertSkattegrunnlag.mapSvalbardGrunnlagTilPensjonsgivendeInntektVisning(inntektsaar: Int): PensjonsgivendeInntektVisning {
    val inntekt = this.svalbardGrunnlag.filter { it.tekniskNavn != skatteoppgjørsdatoKodeverdi }
    // Andre kodeverdier enn i bregnet skatt, må avklares hva som skal brukes
    return PensjonsgivendeInntektVisning(inntektsaar, 0, 0)
}

data class PensjonsgivendeInntektVisning(
    val inntektsaar: Int,
    var næring: Int,
    var person: Int
)
