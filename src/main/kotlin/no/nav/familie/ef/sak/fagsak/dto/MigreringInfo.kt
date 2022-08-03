package no.nav.familie.ef.sak.fagsak.dto

import no.nav.familie.ef.sak.beregning.Beløpsperiode
import no.nav.familie.kontrakter.felles.Periode
import java.time.YearMonth

data class MigreringInfo(
    val kanMigreres: Boolean,
    val årsak: String? = null,
    val kanGåVidereTilJournalføring: Boolean = false,
    val stønadsperiode: Periode? = null,
    @Deprecated("Bruk stønadsperiode.", ReplaceWith("stønadsperiode.fomMåned")) val stønadFom: YearMonth? = stønadsperiode?.fomMåned,
    @Deprecated("Bruk stønadsperiode.", ReplaceWith("stønadsperiode.tomMåned")) val stønadTom: YearMonth? = stønadsperiode?.tomMåned,
    val inntektsgrunnlag: Int? = null,
    val samordningsfradrag: Int? = null,
    val beløpsperioder: List<Beløpsperiode>? = null
)
