package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
class UttrekkArbeidssøkerService(
        private val uttrekkArbeidssøkerRepository: UttrekkArbeidssøkerRepository,
) {

    fun forrigeMåned(): () -> YearMonth = { YearMonth.now().minusMonths(1) }

    @Transactional
    fun opprettUttrekkArbeidssøkere(årMåned: YearMonth = forrigeMåned().invoke()) {
        hentArbeidssøkere(årMåned).forEach {
            uttrekkArbeidssøkerRepository.insert(UttrekkArbeidssøkere(fagsakId = it.fagsakId,
                                                                      vedtakId = it.behandlingIdForVedtak,
                                                                      årMåned = årMåned))
        }
    }

    fun settSjekket(id: UUID, sjekket: Boolean) {
        uttrekkArbeidssøkerRepository.update(uttrekkArbeidssøkerRepository.findByIdOrThrow(id).copy(sjekket = sjekket))
    }

    fun hentUttrekkArbeidssøkere(årMåned: YearMonth = forrigeMåned().invoke()): UttrekkArbeidssøkereDto {
        val arbeidssøkere = uttrekkArbeidssøkerRepository.findAllByÅrMåned(årMåned)
        return UttrekkArbeidssøkereDto(årMåned = årMåned,
                                       antallTotalt = arbeidssøkere.size,
                                       antallSjekket = arbeidssøkere.count { it.sjekket },
                                       arbeidssøkere = arbeidssøkere.map(UttrekkArbeidssøkere::tilDto))
    }

    fun hentArbeidssøkere(årMåned: YearMonth = forrigeMåned().invoke()): List<ArbeidsssøkereTilUttrekk> {
        val startdato = årMåned.atDay(1)
        val sluttdato = årMåned.atEndOfMonth()
        val arbeidssøkere = uttrekkArbeidssøkerRepository.hentArbeidssøkere(startdato, sluttdato)
        return arbeidssøkere.filter { harPeriodeSomArbeidssøker(it, startdato, sluttdato) }
    }

    private fun harPeriodeSomArbeidssøker(it: ArbeidsssøkereTilUttrekk,
                                          startdato: LocalDate,
                                          sluttdato: LocalDate) =
            it.perioder.perioder.any {
                it.datoFra <= startdato
                && it.datoTil >= sluttdato
                && erArbeidssøker(it)
            }

    private fun erArbeidssøker(it: Vedtaksperiode) =
            (it.aktivitet == AktivitetType.FORSØRGER_REELL_ARBEIDSSØKER
             || it.aktivitet == AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER)

}
