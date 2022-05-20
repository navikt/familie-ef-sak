package no.nav.familie.ef.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable
import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.cucumber.domeneparser.IdTIlUUIDHolder.behandlingIdTilUUID
import no.nav.familie.ef.sak.cucumber.domeneparser.IdTIlUUIDHolder.tilkjentYtelseIdNummerTilUUID
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelseType
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.BarnetilsynWrapper
import no.nav.familie.ef.sak.vedtak.domain.Barnetilsynperiode
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.KontantstøtteWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeMedBeløp
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.TilleggsstønadWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.ef.sak.vedtak.historikk.HistorikkEndring
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.felles.ef.StønadType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.AbstractMap
import java.util.UUID

object VedtakDomeneParser {

    fun mapVedtakOvergangsstønad(dataTable: DataTable): List<Vedtak> {
        return mapVedtak(dataTable) { vedtak, rader ->
            val perioder = mapPerioderForOvergangsstønad(rader)
            vedtak.copy(perioder = PeriodeWrapper(perioder),
                        inntekter = InntektWrapper(lagDefaultInntektsperiode(perioder)))
        }
    }

    fun mapAktivitetForBarnetilsyn(dataTable: DataTable): Map<UUID, SvarId?> {
        return dataTable.forHverBehandling { _, rader ->
            val rad = rader.first()
            val behandlingId = behandlingIdTilUUID[parseInt(VedtakDomenebegrep.BEHANDLING_ID, rad)]!!
            val arbeidAktivitet = parseArbeidAktivitet(rad)
            behandlingId to arbeidAktivitet
        }.toMap()
    }

    fun mapVedtakForBarnetilsyn(dataTable: DataTable): List<Vedtak> {
        return mapVedtak(dataTable) { vedtak, rader ->
            val perioder = when (vedtak.resultatType) {
                ResultatType.INNVILGE -> mapPerioderForBarnetilsyn(rader)
                ResultatType.SANKSJONERE -> {
                    val perioderForBarnetilsyn = mapPerioderForBarnetilsyn(rader)
                    validerSanksjon(perioderForBarnetilsyn)
                    perioderForBarnetilsyn
                }
                else -> emptyList()
            }
            vedtak.copy(barnetilsyn = BarnetilsynWrapper(perioder, null),
                        kontantstøtte = KontantstøtteWrapper(emptyList()), // overskreves i egen "Gitt"
                        tilleggsstønad = TilleggsstønadWrapper(false, emptyList(), null)) // overskreves i egen "Gitt"
        }
    }

    private fun mapVedtak(
            dataTable: DataTable,
            vedtakDecorator: (vedtak: Vedtak, rader: List<Map<String, String>>) -> Vedtak = { vedtak, _ -> vedtak }
    ): List<Vedtak> {
        return dataTable.forHverBehandling { _, rader ->
            val rad = rader.first()
            val resultatType = parseResultatType(rad) ?: ResultatType.INNVILGE
            vedtakDecorator.invoke(mapVedtakDomene(rad, resultatType), rader)
        }
    }

    private fun <T> DataTable.forHverBehandling(mapper: (behandlingId: UUID, rader: List<Map<String, String>>) -> T) =
            this.asMaps().groupBy {
                behandlingIdTilUUID[parseInt(VedtakDomenebegrep.BEHANDLING_ID, it)]!!
            }.map { (behandlingId, rader) -> mapper(behandlingId, rader) }

    private fun mapVedtakDomene(rad: Map<String, String>,
                                resultatType: ResultatType) =
            Vedtak(behandlingId = behandlingIdTilUUID[parseInt(VedtakDomenebegrep.BEHANDLING_ID, rad)]!!,
                   resultatType = resultatType,
                   opphørFom = parseValgfriÅrMåned(VedtakDomenebegrep.OPPHØRSDATO, rad)?.atDay(1),
                   sanksjonsårsak = if (resultatType == ResultatType.SANKSJONERE) Sanksjonsårsak.NEKTET_TILBUDT_ARBEID else null,
                   internBegrunnelse = if (resultatType == ResultatType.SANKSJONERE) "Ok" else null)

    private fun validerSanksjon(perioder: List<Barnetilsynperiode>) {
        feilHvisIkke(perioder.size == 1) {
            "Antall rader for sanksjonering må være 1, per behandlingId"
        }
        val periode = perioder.single()
        feilHvis(YearMonth.from(periode.datoFra) != YearMonth.from(periode.datoTil)) {
            "Sanksjon strekker seg ikke 1 måned: ${periode.datoFra} - ${periode.datoTil}"
        }
    }

    private fun mapPerioderForOvergangsstønad(rader: List<Map<String, String>>): List<Vedtaksperiode> {
        return rader.map { rad ->
            Vedtaksperiode(
                    datoFra = parseFraOgMed(rad),
                    datoTil = parseTilOgMed(rad),
                    aktivitet = parseAktivitetType(rad) ?: AktivitetType.BARN_UNDER_ETT_ÅR,
                    periodeType = parseVedtaksperiodeType(rad) ?: VedtaksperiodeType.HOVEDPERIODE
            )
        }
    }

    private fun mapPerioderForBarnetilsyn(rader: List<Map<String, String>>): List<Barnetilsynperiode> {
        return rader.map { rad ->
            Barnetilsynperiode(
                    datoFra = parseFraOgMed(rad),
                    datoTil = parseTilOgMed(rad),
                    utgifter = parseValgfriInt(VedtakDomenebegrep.UTGIFTER, rad) ?: 0,
                    barn = parseValgfriInt(VedtakDomenebegrep.ANTALL_BARN, rad)?.let { IntRange(1, it).map { UUID.randomUUID() } }
                           ?: emptyList()
            )
        }
    }

    fun mapOgSettPeriodeMedBeløp(vedtak: List<Vedtak>,
                                 dataTable: DataTable,
                                 oppdaterVedtak: (Vedtak, List<PeriodeMedBeløp>) -> Vedtak): List<Vedtak> {
        val beløpsperioder = dataTable.forHverBehandling { behandlingId, rader ->
            behandlingId to rader.map { rad ->
                PeriodeMedBeløp(
                        datoFra = parseFraOgMed(rad),
                        datoTil = parseTilOgMed(rad),
                        beløp = parseValgfriInt(VedtakDomenebegrep.BELØP, rad) ?: 0
                )
            }
        }.toMap()
        return vedtak.map {
            val perioder = beløpsperioder[it.behandlingId] ?: emptyList()
            oppdaterVedtak(it, perioder)
        }
    }

    private fun lagDefaultInntektsperiode(perioder: List<Vedtaksperiode>) =
            perioder.firstOrNull()?.let {
                listOf(Inntektsperiode(it.datoFra,
                                       LocalDate.MAX,
                                       BigDecimal.ZERO,
                                       BigDecimal.ZERO))
            } ?: emptyList()

    fun mapInntekter(dataTable: DataTable): Map<UUID, InntektWrapper> {
        return dataTable.forHverBehandling { behandlingId, rader ->
            val inntektsperioder = rader.fold(mutableListOf<Inntektsperiode>()) { acc, rad ->
                val datoFra = parseFraOgMed(rad)
                acc.removeLastOrNull()?.copy(sluttDato = datoFra.minusDays(1))?.let { acc.add(it) }
                acc.add(Inntektsperiode(datoFra,
                                        LocalDate.MAX,
                                        BigDecimal(parseValgfriInt(VedtakDomenebegrep.INNTEKT, rad) ?: 0),
                                        BigDecimal(parseValgfriInt(VedtakDomenebegrep.SAMORDNINGSFRADRAG, rad) ?: 0)))
                acc
            }
            behandlingId to InntektWrapper(inntektsperioder)
        }.toMap()
    }

    fun mapBehandlingForHistorikkEndring(dataTable: DataTable, stønadstype: StønadType): List<ForventetHistorikk> {
        return dataTable.asMaps().map {
            BehandlingForHistorikkEndringMapper().mapRad(it, stønadstype)
        }
    }

    class ForventetHistorikk(
            val behandlingId: UUID,
            val historikkEndring: HistorikkEndring?,
            val stønadFra: LocalDate,
            val stønadTil: LocalDate,
            val inntekt: Int?,
            val beløp: Int?,
            val aktivitetType: AktivitetType?,
            val kontantstøtte: Int?,
            val tilleggsstønad: Int?,
            val antallBarn: Int?,
            val utgifter: Int?,
            val arbeidAktivitet: SvarId?,
            val erSanksjon: Boolean?,
            val sanksjonsårsak: Sanksjonsårsak?,
    )

    private fun parseFraOgMed(rad: Map<String, String>) =
            parseValgfriÅrMåned(VedtakDomenebegrep.FRA_OG_MED_DATO, rad)?.atDay(1) ?: LocalDate.now()

    private fun parseTilOgMed(rad: Map<String, String>) =
            parseValgfriÅrMåned(VedtakDomenebegrep.TIL_OG_MED_DATO, rad)?.atEndOfMonth() ?: LocalDate.now()

    class BehandlingForHistorikkEndringMapper {

        fun mapRad(rad: Map<String, String>, stønadstype: StønadType): ForventetHistorikk {
            val aktivitetType = parseAktivitetType(rad)
                                ?: if (stønadstype == StønadType.OVERGANGSSTØNAD) AktivitetType.BARN_UNDER_ETT_ÅR else null
            return ForventetHistorikk(
                    behandlingId = behandlingIdTilUUID[parseInt(VedtakDomenebegrep.BEHANDLING_ID, rad)]!!,
                    historikkEndring = parseEndringType(rad)?.let { endringType ->
                        HistorikkEndring(
                                type = endringType,
                                behandlingId = behandlingIdTilUUID[parseInt(VedtakDomenebegrep.ENDRET_I_BEHANDLING_ID, rad)]!!,
                                vedtakstidspunkt = LocalDateTime.now()
                        )
                    },
                    stønadFra = parseValgfriÅrMåned(VedtakDomenebegrep.FRA_OG_MED_DATO, rad)?.atDay(1) ?: YearMonth.now()
                            .atDay(1),
                    stønadTil = parseValgfriÅrMåned(VedtakDomenebegrep.TIL_OG_MED_DATO, rad)?.atEndOfMonth() ?: YearMonth.now()
                            .atEndOfMonth(),
                    inntekt = parseValgfriInt(VedtakDomenebegrep.INNTEKT, rad),
                    beløp = parseValgfriInt(VedtakDomenebegrep.BELØP, rad),
                    aktivitetType = aktivitetType,
                    kontantstøtte = parseValgfriInt(VedtakDomenebegrep.KONTANTSTØTTE, rad),
                    tilleggsstønad = parseValgfriInt(VedtakDomenebegrep.TILLEGGSSTØNAD, rad),
                    antallBarn = parseValgfriInt(VedtakDomenebegrep.ANTALL_BARN, rad),
                    utgifter = parseValgfriInt(VedtakDomenebegrep.UTGIFTER, rad),
                    arbeidAktivitet = parseArbeidAktivitet(rad),
                    erSanksjon = parseValgfriBoolean(VedtakDomenebegrep.ER_SANKSJON, rad),
                    sanksjonsårsak = parseSanksjonsårsak(rad)
            )
        }
    }

}

enum class VedtakDomenebegrep(val nøkkel: String) : Domenenøkkel {
    RESULTAT_TYPE("Vedtaksresultat"),
    TILKJENT_YTELSE_ID("Tilkjent ytelse Id"),
    PERSONIDENT("Personnummer"),
    INNTEKT("Inntekt"),
    INNTEKTSREDUKSJON("Inntektsreduksjon"),
    SAMORDNINGSFRADRAG("Samordningsfradrag"),
    BELØP("Beløp"),
    BELØP_MELLOM("Beløp mellom"),
    FRA_OG_MED_DATO("Fra og med dato"),
    TIL_OG_MED_DATO("Til og med dato"),
    AKTIVITET_TYPE("Aktivitet"),
    ARBEID_AKTIVITET("Arbeid aktivitet"), //Inngangsvilkår i barnetilsyn
    VEDTAKSPERIODE_TYPE("Vedtaksperiode"),
    BEHANDLING_ID("BehandlingId"),
    ENDRET_I_BEHANDLING_ID("Endret i behandlingId"),
    KILDE_BEHANDLING_ID("Kildebehandling"),
    ENDRING_TYPE("Endringstype"),
    OPPHØRSDATO("Opphørsdato"),
    UTGIFTER("Utgifter"),
    ANTALL_BARN("Antall barn"),
    TILLEGGSSTØNAD("Tilleggsstønad"),
    KONTANTSTØTTE("Kontantstøtte"),
    ER_SANKSJON("Er sanksjon"),
    SANKSJONSÅRSAK("Sanksjonsårsak"),
    STUDIETYPE("Studietype"),
    STUDIEBELASTNING("Studiebelastning"),
    ;

    override fun nøkkel(): String {
        return nøkkel
    }
}
