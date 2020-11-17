package no.nav.familie.ef.sak.økonomi

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.Sporbar
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseMedMetaData
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.junit.jupiter.api.Assertions
import java.math.BigDecimal
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private const val behandlingEksternId = 0L
private const val fagsakEksternId = 1L

enum class TestOppdragType {
    Input,
    Output,
    Oppdrag
}

data class TestOppdrag(val type: TestOppdragType,
                       val fnr: String,
                       val ytelse: String,
                       val linjeId: Long? = null,
                       val forrigeLinjeId: Long? = null,
                       val status110: String? = null,
                       val erEndring: Boolean? = null,
                       val opphørsdato: LocalDate?,
                       val beløp: Int? = null,
                       val startPeriode: LocalDate? = null,
                       val sluttPeriode: LocalDate? = null) {

    fun tilAndelTilkjentYtelse(): AndelTilkjentYtelse? {

        return if (beløp != null && startPeriode != null && sluttPeriode != null)
            AndelTilkjentYtelse(beløp = this.beløp,
                                stønadFom = startPeriode,
                                stønadTom = sluttPeriode,
                                personIdent = fnr,
                                periodeId = linjeId,
                                forrigePeriodeId = forrigeLinjeId)
        else if (TestOppdragType.Output == type && beløp == null && startPeriode == null && sluttPeriode == null)
            AndelTilkjentYtelse.nullAndel(KjedeId(ytelse, fnr),
                                          PeriodeId(linjeId!!, forrigeLinjeId))
        else
            null
    }

    fun tilUtbetalingsperiode(): Utbetalingsperiode? {

        return if (startPeriode != null && sluttPeriode != null && linjeId != null)
            Utbetalingsperiode(erEndringPåEksisterendePeriode = erEndring ?: false,
                               opphør = opphørsdato?.let { Opphør(it) },
                               periodeId = linjeId,
                               forrigePeriodeId = forrigeLinjeId,
                               datoForVedtak = LocalDate.now(),
                               klassifisering = ytelse,
                               vedtakdatoFom = startPeriode,
                               vedtakdatoTom = sluttPeriode,
                               sats = beløp?.toBigDecimal() ?: BigDecimal.ZERO,
                               satsType = Utbetalingsperiode.SatsType.MND,
                               utbetalesTil = fnr,
                               behandlingId = 1)
        else
            null
    }
}

class TestOppdragGroup {

    private val andelerTilkjentYtelseInn: MutableList<AndelTilkjentYtelse> = mutableListOf()
    private val andelerTilkjentYtelseUt: MutableList<AndelTilkjentYtelse> = mutableListOf()
    private val utbetalingsperioder: MutableList<Utbetalingsperiode> = mutableListOf()
    private val sporbar = Sporbar()
    private var oppdragKode110: Utbetalingsoppdrag.KodeEndring = Utbetalingsoppdrag.KodeEndring.NY
    private var personIdent: String? = null

    fun add(to: TestOppdrag) {
        when (to.type) {
            TestOppdragType.Input -> {
                personIdent = to.fnr
                to.tilAndelTilkjentYtelse()?.also { andelerTilkjentYtelseInn.add(it) }
            }
            TestOppdragType.Oppdrag -> {
                oppdragKode110 = Utbetalingsoppdrag.KodeEndring.valueOf(to.status110!!)
                to.tilUtbetalingsperiode()?.also { utbetalingsperioder.add(it) }
            }
            TestOppdragType.Output -> {
                to.tilAndelTilkjentYtelse()?.also { andelerTilkjentYtelseUt.add(it) }
            }
        }
    }

    val input: TilkjentYtelse by lazy {
        TilkjentYtelse(behandlingId = behandling(fagsak = fagsak()).id,
                       personident = personIdent!!,
                       andelerTilkjentYtelse = andelerTilkjentYtelseInn,
                // Ikke påkrevd, men exception ellers
                       vedtaksdato = LocalDate.now(),
                       sporbar = sporbar)
    }

    val output: TilkjentYtelse by lazy {
        val utbetalingsoppdrag =
                Utbetalingsoppdrag(kodeEndring = oppdragKode110,
                                   fagSystem = "EFOG",
                                   saksnummer = fagsakEksternId.toString(),
                                   aktoer = personIdent!!,
                                   saksbehandlerId = sporbar.endret.endretAv,
                                   avstemmingTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS),
                                   utbetalingsperiode = utbetalingsperioder.map { it.copy(behandlingId = behandlingEksternId) })

        TilkjentYtelse(id = input.id,
                       behandlingId = input.behandlingId,
                       personident = personIdent!!,
                       andelerTilkjentYtelse = andelerTilkjentYtelseUt,
                       utbetalingsoppdrag = utbetalingsoppdrag,
                       vedtaksdato = input.vedtaksdato,
                       stønadFom = andelerTilkjentYtelseUt.filter { it.stønadFom != NULL_DATO }.minOfOrNull { it.stønadFom },
                       stønadTom = andelerTilkjentYtelseInn.filter { it.stønadTom != NULL_DATO }.maxOfOrNull { it.stønadTom },
                       sporbar = sporbar)

    }
}

object TestOppdragParser {

    private const val KEY_TYPE = "Type"
    private const val KEY_FNR = "Fnr"
    private const val KEY_YTELSE = "Ytelse"
    private const val KEY_LINJE_ID = "LID"
    private const val KEY_FORRIGE_LINJE_ID = "Pre-LID"
    private const val KEY_STATUS_OPPDRAG = "Status oppdrag"
    private const val KEY_ER_ENDRING = "Er endring"

    private val RESERVERED_KEYS =
            listOf(KEY_TYPE, KEY_FNR, KEY_YTELSE, KEY_LINJE_ID, KEY_FORRIGE_LINJE_ID, KEY_STATUS_OPPDRAG, KEY_ER_ENDRING)

    private fun parse(url: URL): List<TestOppdrag> {
        val fileContent = url.openStream()!!
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(fileContent)

        return rows.map { row ->
            val datoKeysMedBeløp = row.keys
                    .filter { key -> !RESERVERED_KEYS.contains(key) }
                    .filter { datoKey -> (row[datoKey])?.trim('x')?.toIntOrNull() != null }
                    .sorted()

            val opphørYearMonth = row.keys
                    .filter { key -> !RESERVERED_KEYS.contains(key) }
                    .sorted()
                    .firstOrNull { datoKey -> (row[datoKey])?.contains('x') ?: false }
                    ?.let { YearMonth.parse(it) }

            val firstYearMonth = datoKeysMedBeløp.firstOrNull()?.let { YearMonth.parse(it) }
            val lastYearMonth = datoKeysMedBeløp.lastOrNull()?.let { YearMonth.parse(it) }
            val beløp = datoKeysMedBeløp.firstOrNull()?.let { row[it]?.trim('x') }?.toIntOrNull()

            TestOppdrag(type = row[KEY_TYPE]?.let { TestOppdragType.valueOf(it) }!!,
                        fnr = row.getValue(KEY_FNR),
                        ytelse = row.getValue(KEY_YTELSE),
                        linjeId = row[KEY_LINJE_ID]?.let { emptyAsNull(it) }?.let { Integer.parseInt(it).toLong() },
                        forrigeLinjeId = row[KEY_FORRIGE_LINJE_ID]
                                ?.let { emptyAsNull(it) }
                                ?.let { Integer.parseInt(it).toLong() },
                        status110 = row[KEY_STATUS_OPPDRAG]?.let { emptyAsNull(it) },
                        erEndring = row[KEY_ER_ENDRING]?.let { it.toBoolean() },
                        beløp = beløp,
                        opphørsdato = opphørYearMonth?.atDay(1),
                        startPeriode = firstYearMonth?.atDay(1),
                        sluttPeriode = lastYearMonth?.atEndOfMonth())
        }

    }

    fun parseToTestOppdragGroup(url: URL): List<TestOppdragGroup> {

        val result: MutableList<TestOppdragGroup> = mutableListOf()

        var newGroup = true

        parse(url).forEach { to ->
            when (to.type) {
                TestOppdragType.Input -> {
                    if (newGroup) {
                        result.add(TestOppdragGroup())
                        newGroup = false
                    }
                }
                else -> {
                    newGroup = true
                }
            }
            result.last().add(to)
        }

        return result
    }

    private fun emptyAsNull(s: String): String? =
            if (s.isEmpty()) null else s


}

object TestOppdragRunner {

    fun run(url: URL) {
        val grupper = TestOppdragParser.parseToTestOppdragGroup(url)

        var forrigeTilkjentYtelse: TilkjentYtelse? = null

        grupper.forEachIndexed { indeks, gruppe ->
            val faktisk = lagTilkjentYtelseMedUtbetalingsoppdrag(gruppe.input, forrigeTilkjentYtelse)
            Assertions.assertEquals(gruppe.output, faktisk, "Feiler for gruppe med indeks $indeks")
            forrigeTilkjentYtelse = faktisk
        }
    }

    private fun lagTilkjentYtelseMedUtbetalingsoppdrag(nyTilkjentYtelse: TilkjentYtelse,
                                                       forrigeTilkjentYtelse: TilkjentYtelse? = null) =
            UtbetalingsoppdragGenerator
                    .lagTilkjentYtelseMedUtbetalingsoppdrag(TilkjentYtelseMedMetaData(tilkjentYtelse = nyTilkjentYtelse,
                                                                                      stønadstype =  Stønadstype.OVERGANGSSTØNAD,
                                                                                      eksternBehandlingId = behandlingEksternId,
                                                                                      eksternFagsakId = fagsakEksternId),
                                                            forrigeTilkjentYtelse)

}
