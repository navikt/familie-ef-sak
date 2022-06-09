package no.nav.familie.ef.sak.beregning.skolepenger

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.felles.dto.harOverlappende
import no.nav.familie.ef.sak.felles.util.skoleår
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.SkolepengerUtgiftDto
import no.nav.familie.ef.sak.vedtak.dto.SkoleårsperiodeSkolepengerDto
import no.nav.familie.ef.sak.vedtak.dto.tilDto
import org.springframework.stereotype.Service
import java.time.Year
import java.util.UUID

private val maksbeløpPerSkoleår = 68_000

@Service
class BeregningSkolepengerService(
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService
) {

    fun beregnYtelse(
        utgiftsperioder: List<SkoleårsperiodeSkolepengerDto>,
        behandlingId: UUID
    ): BeregningSkolepengerResponse {
        val forrigePerioder = hentPerioderFraForrigeVedtak(behandlingId)
        return beregnYtelse(utgiftsperioder, forrigePerioder)
    }

    private fun hentPerioderFraForrigeVedtak(behandlingId: UUID): List<SkoleårsperiodeSkolepengerDto> {
        return behandlingService.hentSaksbehandling(behandlingId).forrigeBehandlingId?.let { forrigeBehandlingId ->
            hentPerioder(forrigeBehandlingId)
        } ?: emptyList()
    }

    private fun hentPerioder(forrigeBehandlingId: UUID): List<SkoleårsperiodeSkolepengerDto> {
        val vedtak = vedtakService.hentVedtak(forrigeBehandlingId)
        feilHvis(vedtak.skolepenger == null) {
            "Vedtak for forrigeBehandlingId=$forrigeBehandlingId mangler skolepenger"
        }
        return vedtak.skolepenger.skoleårsperioder.map { it.tilDto() }
    }

    private fun beregnYtelse(
        perioder: List<SkoleårsperiodeSkolepengerDto>,
        forrigePerioder: List<SkoleårsperiodeSkolepengerDto>
    ): BeregningSkolepengerResponse {
        validerGyldigePerioder(perioder)
        validerFornuftigeBeløp(perioder)
        validerSkoleår(perioder)
        validerForrigePerioder(perioder, forrigePerioder)

        val perioder = beregnSkoleårsperioder(perioder)
        return BeregningSkolepengerResponse(perioder)
    }

    private fun beregnSkoleårsperioder(
        perioder: List<SkoleårsperiodeSkolepengerDto>
    ): List<BeløpsperiodeSkolepenger> {
        return perioder
            .flatMap { skoleårsperiode -> skoleårsperiode.utgiftsperioder }
            .groupBy { it.årMånedFra }
            .toSortedMap()
            .map {
                BeløpsperiodeSkolepenger(
                    årMånedFra = it.key,
                    utgifter = it.value.sumOf { it.utgifter },
                    beløp = it.value.sumOf { it.stønad })
            }
    }

    private fun validerFornuftigeBeløp(skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>) {
        brukerfeilHvis(skoleårsperioder.any { periode -> periode.utgiftsperioder.any { it.utgifter < 1 } }) {
            "Utgifter må være høyere enn 0kr"
        }
        brukerfeilHvis(skoleårsperioder.any { periode -> periode.utgiftsperioder.any { it.stønad < 0 } }) {
            "Stønad kan ikke være lavere enn 0kr"
        }
        brukerfeilHvis(skoleårsperioder.any { periode -> periode.utgiftsperioder.any { it.stønad > it.utgifter } }) {
            "Stønad kan ikke være høyere enn utgifter"
        }

        skoleårsperioder.forEach { skoleårsperiode ->
            validerStudiebelastning(skoleårsperiode)
            validerUnderMaksBeløp(skoleårsperiode)
        }
    }

    private fun validerStudiebelastning(skoleårsperiode: SkoleårsperiodeSkolepengerDto) {
        brukerfeilHvis(skoleårsperiode.perioder.any { it.studiebelastning < 1 }) { "Studiebelastning må være over 0" }
        brukerfeilHvis(skoleårsperiode.perioder.any { it.studiebelastning > 100 }) { "Studiebelastning må være under eller lik 100" }
    }

    private fun validerUnderMaksBeløp(skoleårsperiode: SkoleårsperiodeSkolepengerDto) {
        val skoleår = skoleårsperiode.perioder.first().årMånedFra.skoleår()
        brukerfeilHvis(skoleårsperiode.utgiftsperioder.sumOf { it.stønad } > maksbeløpPerSkoleår) {
            "Stønad for skoleåret $skoleår er høyere enn $maksbeløpPerSkoleår"
        }
    }

    private fun validerGyldigePerioder(skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>) {
        feilHvis(skoleårsperioder.isEmpty()) {
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
        val tidligereSkoleår = mutableSetOf<Year>()
        perioder.forEach { skoleårsperiode ->
            val skoleår = skoleårsperiode.perioder.first().årMånedFra.skoleår()
            brukerfeilHvisIkke(skoleårsperiode.perioder.all {
                val fraSkoleår = it.årMånedFra.skoleår()
                skoleår == fraSkoleår && fraSkoleår == it.årMånedTil.skoleår()
            }) {
                "Alle perioder i et skoleår må være det samme skoleåret"
            }
            brukerfeilHvisIkke(tidligereSkoleår.add(skoleår)) {
                "Skoleåret $skoleår er definiert flere ganger"
            }
            brukerfeilHvis(skoleårsperiode.perioder.map { it.tilPeriode() }.harOverlappende()) {
                "Skoleår $skoleår inneholder overlappende perioder"
            }
            val studietype = skoleårsperiode.perioder.first().studietype
            brukerfeilHvisIkke(skoleårsperiode.perioder.all { it.studietype == studietype }) {
                "Skoleår $skoleår inneholder ulike studietyper"
            }
        }
    }

    private fun validerForrigePerioder(
        perioder: List<SkoleårsperiodeSkolepengerDto>,
        forrigePerioder: List<SkoleårsperiodeSkolepengerDto>
    ) {
        val tidligereUtgiftIder = forrigePerioder.flatMap { periode ->
            periode.utgiftsperioder.map { it.id to it }
        }.toMap()
        validerForrigePerioderFortsattFinnes(perioder, tidligereUtgiftIder)
        validerForrigePerioderErUendrede(perioder, tidligereUtgiftIder)
    }

    private fun validerForrigePerioderErUendrede(
        skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>,
        tidligereUtgiftIder: Map<UUID, SkolepengerUtgiftDto>
    ) {
        skoleårsperioder.forEach { skoleårsperiode ->
            val skoleår = skoleårsperiode.perioder.first().årMånedFra.skoleår()
            val endretUtgift = skoleårsperiode.utgiftsperioder.find { utgift ->
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
        tidligereUtgiftIder: Map<UUID, SkolepengerUtgiftDto>
    ) {
        val nyeIder = skoleårsperioder.flatMap { periode -> periode.utgiftsperioder.map { it.id } }.toSet()
        val manglende = tidligereUtgiftIder.entries.filterNot { nyeIder.contains(it.key) }
        brukerfeilHvis(manglende.isNotEmpty()) {
            val manglendePerioder = manglende.joinToString(", \n") { (_, utgiftsperiode) ->
                "fakturadato=${utgiftsperiode.årMånedFra} " +
                    "utgifter=${utgiftsperiode.utgifter} " +
                    "stønad=${utgiftsperiode.stønad}"
            }
            "Mangler utgiftsperioder fra forrige vedtak \n$manglendePerioder"
        }
    }
}
