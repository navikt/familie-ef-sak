package no.nav.familie.ef.sak.beregning.skolepenger

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.beregning.skolepenger.SkolepengerMaksbeløp.maksbeløp
import no.nav.familie.ef.sak.felles.util.DatoFormat.YEAR_MONTH_FORMAT_NORSK
import no.nav.familie.ef.sak.felles.util.Skoleår
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.SkolepengerUtgiftDto
import no.nav.familie.ef.sak.vedtak.dto.SkoleårsperiodeSkolepengerDto
import no.nav.familie.ef.sak.vedtak.dto.tilDto
import no.nav.familie.kontrakter.felles.harOverlappende
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Skoleår 2021 = 21/22
 */
@Service
class BeregningSkolepengerService(
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
) {
    private val logger = Logg.getLogger(this::class)

    fun beregnYtelse(
        utgiftsperioder: List<SkoleårsperiodeSkolepengerDto>,
        behandlingId: UUID,
        erOpphør: Boolean = false,
    ): BeregningSkolepengerResponse {
        val forrigePerioder = hentPerioderFraForrigeVedtak(behandlingId)
        return beregnYtelse(utgiftsperioder, forrigePerioder, erOpphør)
    }

    private fun hentPerioderFraForrigeVedtak(behandlingId: UUID): List<SkoleårsperiodeSkolepengerDto> =
        behandlingService.hentSaksbehandling(behandlingId).forrigeBehandlingId?.let { forrigeBehandlingId ->
            hentPerioder(forrigeBehandlingId)
        } ?: emptyList()

    private fun hentPerioder(forrigeBehandlingId: UUID): List<SkoleårsperiodeSkolepengerDto> {
        val vedtak = vedtakService.hentVedtak(forrigeBehandlingId)
        feilHvis(vedtak.skolepenger == null) {
            "Vedtak for forrigeBehandlingId=$forrigeBehandlingId mangler skolepenger"
        }
        return vedtak.skolepenger.skoleårsperioder.map { it.tilDto() }
    }

    private fun beregnYtelse(
        perioder: List<SkoleårsperiodeSkolepengerDto>,
        forrigePerioder: List<SkoleårsperiodeSkolepengerDto>,
        erOpphør: Boolean,
    ): BeregningSkolepengerResponse {
        validerGyldigePerioder(perioder, erOpphør)
        validerFornuftigeBeløp(perioder)
        validerSkoleår(perioder)
        validerForrigePerioder(perioder, forrigePerioder, erOpphør)

        val beregnedePerioder = beregnSkoleårsperioder(perioder)
        return BeregningSkolepengerResponse(beregnedePerioder)
    }

    private fun beregnSkoleårsperioder(
        perioder: List<SkoleårsperiodeSkolepengerDto>,
    ): List<BeløpsperiodeSkolepenger> =
        perioder
            .flatMap { skoleårsperiode -> skoleårsperiode.utgiftsperioder }
            .groupBy { it.årMånedFra }
            .toSortedMap()
            .map { (key, value) ->
                BeløpsperiodeSkolepenger(
                    årMånedFra = key,
                    beløp = value.sumOf { it.stønad },
                )
            }

    private fun validerFornuftigeBeløp(skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>) {
        brukerfeilHvis(skoleårsperioder.any { periode -> periode.utgiftsperioder.any { it.stønad < 0 } }) {
            "Stønad kan ikke være lavere enn 0kr"
        }

        skoleårsperioder.forEach { skoleårsperiode ->
            validerStudiebelastning(skoleårsperiode)
            validerUnderMaksBeløp(skoleårsperiode)
        }
    }

    private fun validerStudiebelastning(skoleårsperiode: SkoleårsperiodeSkolepengerDto) {
        brukerfeilHvis(skoleårsperiode.perioder.any { it.studiebelastning < 50 || it.studiebelastning > 100 }) {
            "Studiebelastning må være mellom 50-100%"
        }
    }

    private fun validerUnderMaksBeløp(skoleårsperiode: SkoleårsperiodeSkolepengerDto) {
        val førstePeriode = skoleårsperiode.perioder.first()
        val skoleår = førstePeriode.skoleår
        val maksbeløp = maksbeløp(førstePeriode.studietype, skoleår)
        brukerfeilHvis(skoleårsperiode.utgiftsperioder.sumOf { it.stønad } > maksbeløp) {
            "Stønad for skoleåret $skoleår overstiger makssats $maksbeløp"
        }
    }

    private fun validerGyldigePerioder(
        skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>,
        erOpphør: Boolean,
    ) {
        feilHvis(!erOpphør && skoleårsperioder.isEmpty()) {
            "Mangler skoleår"
        }
        feilHvis(skoleårsperioder.any { it.perioder.isEmpty() }) {
            "Mangler skoleårsperioder"
        }
        feilHvis(skoleårsperioder.any { it.utgiftsperioder.isEmpty() }) {
            "Mangler utgiftsperioder"
        }
        val utgiftsIder = skoleårsperioder.flatMap { it.utgiftsperioder.map { it.id } }
        feilHvis(utgiftsIder.size != utgiftsIder.toSet().size) {
            "Det finnes duplikat av ider på utgifter $skoleårsperioder"
        }
    }

    private fun validerSkoleår(perioder: List<SkoleårsperiodeSkolepengerDto>) {
        val tidligereSkoleår = mutableSetOf<Skoleår>()
        perioder.forEach { skoleårsperiode ->
            val skoleår = skoleårsperiode.perioder.first().skoleår
            val periodeUtenforSkoleår = skoleårsperiode.perioder.find { skoleår != it.skoleår }
            brukerfeilHvis(periodeUtenforSkoleår != null) {
                val fra = periodeUtenforSkoleår?.årMånedFra?.format(YEAR_MONTH_FORMAT_NORSK)
                val til = periodeUtenforSkoleår?.årMånedTil?.format(YEAR_MONTH_FORMAT_NORSK)
                "Periode $fra-$til er definert utenfor skoleåret $skoleår"
            }
            brukerfeilHvisIkke(tidligereSkoleår.add(skoleår)) {
                "Skoleåret $skoleår kan ikke legges inn flere ganger"
            }
            brukerfeilHvis(skoleårsperiode.perioder.map { it.periode }.harOverlappende()) {
                "Skoleår $skoleår inneholder overlappende perioder"
            }
            val studietype = skoleårsperiode.perioder.first().studietype
            feilHvis(skoleårsperiode.perioder.any { it.studietype != studietype }) {
                "Skoleår $skoleår inneholder ulike studietyper"
            }
        }
    }

    private fun validerForrigePerioder(
        perioder: List<SkoleårsperiodeSkolepengerDto>,
        forrigePerioder: List<SkoleårsperiodeSkolepengerDto>,
        erOpphør: Boolean,
    ) {
        if (forrigePerioder.isEmpty()) return
        val tidligereUtgiftIder =
            forrigePerioder
                .flatMap { periode ->
                    periode.utgiftsperioder.map { it.id to it }
                }.toMap()
        if (erOpphør) {
            validerIngenNyePerioderFinnes(perioder, forrigePerioder)
            validerNoeErFjernet(perioder, forrigePerioder)
        } else {
            validerForrigePerioderFortsattFinnes(perioder, tidligereUtgiftIder)
            validerForrigePerioderErUendrede(perioder, tidligereUtgiftIder)
        }
    }

    private fun validerIngenNyePerioderFinnes(
        perioder: List<SkoleårsperiodeSkolepengerDto>,
        forrigePerioder: List<SkoleårsperiodeSkolepengerDto>,
    ) {
        val forrigePerioderPerSkoleår = forrigePerioder.associateBy { it.perioder.first().skoleår }
        val nyePerioderPerSkoleår = perioder.associateBy { it.perioder.first().skoleår }
        feilHvisIkke(forrigePerioderPerSkoleår.keys.containsAll(nyePerioderPerSkoleår.keys)) {
            "Det finnes nye skoleårsperioder"
        }
        nyePerioderPerSkoleår.entries.forEach { (skoleår, skoleårsperiode) ->
            val forrigePeriodeForSkoleår = forrigePerioderPerSkoleår[skoleår] ?: return
            feilHvis(forrigePeriodeForSkoleår.perioder.size < skoleårsperiode.perioder.size) {
                "En ny periode for skoleår=$skoleår er lagt til"
            }
            feilHvis(forrigePeriodeForSkoleår.utgiftsperioder.size < skoleårsperiode.utgiftsperioder.size) {
                "En ny utgiftsperiode for skoleår=$skoleår er lagt til"
            }
            feilHvis(
                forrigePeriodeForSkoleår.utgiftsperioder.size == skoleårsperiode.utgiftsperioder.size &&
                    forrigePeriodeForSkoleår.utgiftsperioder != skoleårsperiode.utgiftsperioder,
            ) {
                "Utgiftsperioder for $skoleår er endrede"
            }
        }
    }

    private fun validerNoeErFjernet(
        perioder: List<SkoleårsperiodeSkolepengerDto>,
        forrigePerioder: List<SkoleårsperiodeSkolepengerDto>,
    ) {
        if (forrigePerioder.size != perioder.size) {
            return
        }
        val forrigePerioderPerSkoleår = forrigePerioder.associateBy { it.perioder.first().skoleår }
        perioder.associateBy { it.perioder.first().skoleår }.entries.forEach { (skoleår, skoleårsperiode) ->
            val forrigePeriodeForSkoleår = forrigePerioderPerSkoleår[skoleår] ?: return
            if (forrigePeriodeForSkoleår.perioder != skoleårsperiode.perioder) {
                return
            }
            if (forrigePeriodeForSkoleår.utgiftsperioder.size != skoleårsperiode.utgiftsperioder.size) {
                return
            }
        }
        logger.warn("Finner ikke noe som er endret mellom forrigePerioder=$forrigePerioder og nyePerioder=$perioder")
        throw ApiFeil("Periodene er uendrede, finner ikke noe å opphøre", HttpStatus.BAD_REQUEST)
    }

    private fun validerForrigePerioderErUendrede(
        skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>,
        tidligereUtgiftIder: Map<UUID, SkolepengerUtgiftDto>,
    ) {
        skoleårsperioder.forEach { skoleårsperiode ->
            val skoleår = skoleårsperiode.perioder.first().skoleår
            val endretUtgift =
                skoleårsperiode.utgiftsperioder.find { utgift ->
                    val tidligereUtgift = tidligereUtgiftIder[utgift.id]
                    tidligereUtgift != null && tidligereUtgift != utgift
                }
            feilHvis(endretUtgift != null) {
                "Utgiftsperiode er endret for skoleår=$skoleår id=${endretUtgift?.id} er endret" +
                    "ny=$endretUtgift tidligere=${tidligereUtgiftIder[endretUtgift?.id]}"
            }
        }
    }

    private fun validerForrigePerioderFortsattFinnes(
        skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>,
        tidligereUtgiftIder: Map<UUID, SkolepengerUtgiftDto>,
    ) {
        val nyeIder = skoleårsperioder.flatMap { periode -> periode.utgiftsperioder.map { it.id } }.toSet()
        val manglende = tidligereUtgiftIder.entries.filterNot { nyeIder.contains(it.key) }
        brukerfeilHvis(manglende.isNotEmpty()) {
            val manglendePerioder =
                manglende.joinToString(", \n") { (_, utgiftsperiode) ->
                    "fakturadato=${utgiftsperiode.årMånedFra} " +
                        "stønad=${utgiftsperiode.stønad}"
                }
            "Mangler utgiftsperioder fra forrige vedtak \n$manglendePerioder"
        }
    }
}
