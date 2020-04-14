package no.nav.familie.ef.sak.vurdering

import no.nav.familie.ef.sak.integration.dto.pdl.Bostedsadresse
import no.nav.familie.ef.sak.integration.dto.pdl.Foedsel
import no.nav.familie.ef.sak.integration.dto.pdl.Folkeregistermetadata
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPerson
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import no.nav.familie.kontrakter.felles.medlemskap.PeriodeInfo
import no.nav.familie.kontrakter.felles.medlemskap.PeriodeStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class MedlemskapshistorikkTest {

    @Test
    fun `getMedlemskapsperioder uten opphold slår sammen bostedsadresser til  én gyldig bosattperiode`() {

        val pdlPerson =
                pdlPerson(Pair(LocalDate.of(2002, 2, 14), null),
                          Pair(LocalDate.of(2007, 10, 12), null),
                          Pair(LocalDate.of(2015, 6, 14), null))
        val medlemskapsinfo = Medlemskapsinfo("3213213", emptyList(), emptyList(), emptyList())

        val historikk = Medlemskapshistorikk(pdlPerson, medlemskapsinfo)

        assertThat(historikk.medlemskapsperioder[0]).isEqualTo(Periode(LocalDate.MIN, LocalDate.of(2002, 2, 13), false))
        assertThat(historikk.medlemskapsperioder[1]).isEqualTo(Periode(LocalDate.of(2002, 2, 14), LocalDate.MAX, true))
    }

    @Test
    fun `getMedlemskapsperioder legger til avvist medlemskap som opphold`() {

        val pdlPerson =
                pdlPerson(Pair(LocalDate.of(2002, 2, 14), null),
                          Pair(LocalDate.of(2007, 10, 12), null),
                          Pair(LocalDate.of(2015, 6, 14), null))
        val medlemskapsinfo = Medlemskapsinfo("3213213",
                                              emptyList(),
                                              avvistePerioder(Pair(LocalDate.of(2008, 4, 15),
                                                                   LocalDate.of(2010, 1, 16))),
                                              emptyList())

        val historikk = Medlemskapshistorikk(pdlPerson, medlemskapsinfo)

        assertThat(historikk.medlemskapsperioder[0]).isEqualTo(Periode(LocalDate.MIN, LocalDate.of(2002, 2, 13), false))
        assertThat(historikk.medlemskapsperioder[1]).isEqualTo(Periode(LocalDate.of(2002, 2, 14),
                                                                       LocalDate.of(2008, 4, 14),
                                                                       true))
        assertThat(historikk.medlemskapsperioder[2]).isEqualTo(Periode(LocalDate.of(2008, 4, 15),
                                                                       LocalDate.of(2010, 1, 16),
                                                                       false))
        assertThat(historikk.medlemskapsperioder[3]).isEqualTo(Periode(LocalDate.of(2010, 1, 17),
                                                                       LocalDate.MAX,
                                                                       true))
    }

    @Test
    fun `getMedlemskapsperioder gylidg unntak i gyldig periode medfører ingen forskjell`() {

        val pdlPerson =
                pdlPerson(Pair(LocalDate.of(2002, 2, 14), null),
                          Pair(LocalDate.of(2007, 10, 12), null),
                          Pair(LocalDate.of(2015, 6, 14), null))
        val medlemskapsinfo = Medlemskapsinfo("3213213",
                                              gyldigePerioder(Pair(LocalDate.of(2008, 4, 15),
                                                                   LocalDate.of(2010, 1, 16))),
                                              emptyList(),
                                              emptyList())

        val historikk = Medlemskapshistorikk(pdlPerson, medlemskapsinfo)

        assertThat(historikk.medlemskapsperioder[0]).isEqualTo(Periode(LocalDate.MIN, LocalDate.of(2002, 2, 13), false))
        assertThat(historikk.medlemskapsperioder[1]).isEqualTo(Periode(LocalDate.of(2002, 2, 14), LocalDate.MAX, true))
    }

    @Test
    fun `getMedlemskapsperioder med opphold slår sammen bostedsadresser til bosattperioder med gylidg lik true og false`() {

        val pdlPerson =
                pdlPerson(Pair(LocalDate.of(2002, 2, 14), LocalDateTime.of(2007, 10, 12, 0, 0)),
                          Pair(LocalDate.of(2015, 6, 14), null))
        val medlemskapsinfo = Medlemskapsinfo("3213213", emptyList(), emptyList(), emptyList())

        val historikk = Medlemskapshistorikk(pdlPerson, medlemskapsinfo)

        assertThat(historikk.medlemskapsperioder[0]).isEqualTo(Periode(LocalDate.MIN, LocalDate.of(2002, 2, 13), false))
        assertThat(historikk.medlemskapsperioder[1]).isEqualTo(Periode(LocalDate.of(2002, 2, 14),
                                                                       LocalDate.of(2007, 10, 12),
                                                                       true))
        assertThat(historikk.medlemskapsperioder[2]).isEqualTo(Periode(LocalDate.of(2007, 10, 13),
                                                                       LocalDate.of(2015, 6, 13),
                                                                       false))
        assertThat(historikk.medlemskapsperioder[3]).isEqualTo(Periode(LocalDate.of(2015, 6, 14),
                                                                       LocalDate.MAX,
                                                                       true))
    }

    @Test
    fun `getMedlemskapsperioder legger til avsluttende ugyldig periode hvis siste bostedsadresse er avsluttet`() {

        val pdlPerson =
                pdlPerson(Pair(LocalDate.of(2002, 2, 14), null),
                          Pair(LocalDate.of(2006, 6, 14), LocalDateTime.of(2007, 10, 12, 0, 0)))
        val medlemskapsinfo = Medlemskapsinfo("3213213", emptyList(), emptyList(), emptyList())

        val historikk = Medlemskapshistorikk(pdlPerson, medlemskapsinfo)

        assertThat(historikk.medlemskapsperioder[0]).isEqualTo(Periode(LocalDate.MIN, LocalDate.of(2002, 2, 13), false))
        assertThat(historikk.medlemskapsperioder[1]).isEqualTo(Periode(LocalDate.of(2002, 2, 14),
                                                                       LocalDate.of(2007, 10, 12),
                                                                       true))
        assertThat(historikk.medlemskapsperioder[2]).isEqualTo(Periode(LocalDate.of(2007, 10, 13),
                                                                       LocalDate.MAX,
                                                                       false))
    }

    @Test
    fun `getMedlemskapsperioder uten bostedsadresser gir ett evigvarende opphold`() {

        val pdlPerson = pdlPerson()
        val medlemskapsinfo = Medlemskapsinfo("3213213", emptyList(), emptyList(), emptyList())

        val historikk = Medlemskapshistorikk(pdlPerson, medlemskapsinfo)

        assertThat(historikk.medlemskapsperioder[0]).isEqualTo(Periode(LocalDate.MIN,
                                                                       LocalDate.MAX,
                                                                       false))
    }


    private fun pdlPerson(vararg perioder: Pair<LocalDate, LocalDateTime?>) = object : PdlPerson {
        override val foedsel: List<Foedsel> = listOf(Foedsel(null, null, null, null, null))
        override val bostedsadresse: List<Bostedsadresse> = perioder.map {
            Bostedsadresse(it.first, null, Folkeregistermetadata(null, it.second), null, null)
        }
    }

    private fun avvistePerioder(vararg perioder: Pair<LocalDate, LocalDate>) = perioder.map {
        PeriodeInfo(PeriodeStatus.AVST, null, it.first, it.second, true, "", null)
    }

    private fun gyldigePerioder(vararg perioder: Pair<LocalDate, LocalDate>) = perioder.map {
        PeriodeInfo(PeriodeStatus.GYLD, null, it.first, it.second, true, "", null)
    }

    private fun uavklartePerioder(vararg perioder: Pair<LocalDate, LocalDate>) = perioder.map {
        PeriodeInfo(PeriodeStatus.UAVK, null, it.first, it.second, true, "", null)
    }


}