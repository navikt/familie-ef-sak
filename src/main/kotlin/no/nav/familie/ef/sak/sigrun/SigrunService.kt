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
    val person = inntekt.filterNot { listOf(næringKodeverdiBeregnetSkatt, svalbardPersoninntektNaering, svalbardSumAllePersoninntekter).contains(it.tekniskNavn) }.sumOf { it.verdi.toInt() }

    val næringSvalbard = inntekt.filter { it.tekniskNavn == svalbardPersoninntektNaering }.sumOf { it.verdi.toInt() }
    val personSvalbard = inntekt.filter { it.tekniskNavn == svalbardSumAllePersoninntekter }.sumOf { it.verdi.toInt() }

    val svalbard = if (næringSvalbard != 0 || personSvalbard > 0) SvalbardPensjonsgivendeInntekt(næring = næringSvalbard, person = personSvalbard) else null

    return PensjonsgivendeInntektVisning(inntektsaar, næring, person, svalbard = svalbard)
}

data class PensjonsgivendeInntektVisning(
    val inntektsår: Int,
    var næring: Int,
    var person: Int,
    val svalbard: SvalbardPensjonsgivendeInntekt? = null,
)

data class SvalbardPensjonsgivendeInntekt(
    val næring: Int,
    val person: Int,
)
