package no.nav.familie.ef.sak.vurdering.medlemskap

import no.nav.familie.ef.sak.vurdering.medlemskap.Medlemskapshistorikk
import no.nav.familie.ef.sak.util.Periode
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class MedlemskapshistorikkTest {

    @Test
    fun `getMedlemskapsperioder uten bostedsadresser gir ett evigvarende opphold`() {
        val pdlPerson = pdlPerson()
        val medlemskapsinfoUtenUnntak = Medlemskapsinfo("3213213", emptyList(), emptyList(), emptyList())
        val forventetEvigvarendeUgyldigMedlemskap = Periode(LocalDate.MIN, LocalDate.MAX, false)

        val historikk = Medlemskapshistorikk(pdlPerson, medlemskapsinfoUtenUnntak)

        assertThat(historikk.medlemskapsperioder[0]).isEqualTo(forventetEvigvarendeUgyldigMedlemskap)
    }

    @Test
    fun `getMedlemskapsperioder uten opphold slår sammen bostedsadresser til én gyldig bosattperiode`() {
        val pdlPerson =
                pdlPerson(Pair(LocalDate.of(2002, 2, 14), null),
                          Pair(LocalDate.of(2007, 10, 12), null),
                          Pair(LocalDate.of(2015, 6, 14), null))
        val medlemskapsinfoUtenUnntak = Medlemskapsinfo("3213213", emptyList(), emptyList(), emptyList())
        val forventetUgyldigPeriodeFørFørsteMedlemskap = Periode(LocalDate.MIN, LocalDate.of(2002, 2, 13), false)
        val forventetGyldigMedlemskapsperiode = Periode(LocalDate.of(2002, 2, 14), LocalDate.MAX, true)

        val historikk = Medlemskapshistorikk(pdlPerson, medlemskapsinfoUtenUnntak)

        assertThat(historikk.medlemskapsperioder[0]).isEqualTo(forventetUgyldigPeriodeFørFørsteMedlemskap)
        assertThat(historikk.medlemskapsperioder[1]).isEqualTo(forventetGyldigMedlemskapsperiode)
    }

    @Test
    fun `getMedlemskapsperioder lager korrekt flere opphold i medlemskapsperiode med flere avvisninger i en bosattperiode`() {
        val pdlPerson = pdlPerson(Pair(LocalDate.of(2002, 2, 14), null))
        val medlemskapsinfoMedAvvistPerioder =
                Medlemskapsinfo("3213213",
                                emptyList(),
                                avvistePerioder(Pair(LocalDate.of(2008, 4, 15),
                                                     LocalDate.of(2010, 1, 16)),
                                                Pair(LocalDate.of(2010, 4, 15),
                                                     LocalDate.of(2010, 5, 16))),
                                emptyList())
        val forventetUgyldigPeriodeFørFørsteMedlemskap = Periode(LocalDate.MIN, LocalDate.of(2002, 2, 13), false)
        val forventetFørsteGyldigeMedlemskapsperiode =
                Periode(LocalDate.of(2002, 2, 14), LocalDate.of(2008, 4, 14), true)
        val forventetUgyldigMedlemskapsperiodeSomFølgeAvAvvisning =
                Periode(LocalDate.of(2008, 4, 15), LocalDate.of(2010, 1, 16), false)
        val forventetGyldigPeriode =
                Periode(LocalDate.of(2010, 1, 17), LocalDate.of(2010, 4, 14), true)
        val forventetUgyldigPeriodeSomFølgeAvEnAvvistPeriodeTil =
                Periode(LocalDate.of(2010, 4, 15), LocalDate.of(2010, 5, 16), false)
        val forventetAvsluttendeGyldigPeriode =
                Periode(LocalDate.of(2010, 5, 17), LocalDate.MAX, true)

        val historikk = Medlemskapshistorikk(pdlPerson, medlemskapsinfoMedAvvistPerioder)

        assertThat(historikk.medlemskapsperioder[0]).isEqualTo(forventetUgyldigPeriodeFørFørsteMedlemskap)
        assertThat(historikk.medlemskapsperioder[1]).isEqualTo(forventetFørsteGyldigeMedlemskapsperiode)
        assertThat(historikk.medlemskapsperioder[2]).isEqualTo(forventetUgyldigMedlemskapsperiodeSomFølgeAvAvvisning)
        assertThat(historikk.medlemskapsperioder[3]).isEqualTo(forventetGyldigPeriode)
        assertThat(historikk.medlemskapsperioder[4]).isEqualTo(forventetUgyldigPeriodeSomFølgeAvEnAvvistPeriodeTil)
        assertThat(historikk.medlemskapsperioder[5]).isEqualTo(forventetAvsluttendeGyldigPeriode)
    }

    @Test
    fun `getMedlemskapsperioder gyldig unntak i gyldig periode medfører ingen forskjell`() {
        val pdlPerson =
                pdlPerson(Pair(LocalDate.of(2002, 2, 14), null))
        val medlemskapsinfoMedGyldigePeriode =
                Medlemskapsinfo("3213213",
                                gyldigePerioder(Pair(LocalDate.of(2008, 4, 15),
                                                     LocalDate.of(2010, 1, 16))),
                                emptyList(),
                                emptyList())
        val forventetUgyldigPeriodeFørFørsteMedlemskap = Periode(LocalDate.MIN, LocalDate.of(2002, 2, 13), false)
        val forventetUendretMedlemskapsperiode = Periode(LocalDate.of(2002, 2, 14), LocalDate.MAX, true)

        val historikk = Medlemskapshistorikk(pdlPerson, medlemskapsinfoMedGyldigePeriode)

        assertThat(historikk.medlemskapsperioder[0]).isEqualTo(forventetUgyldigPeriodeFørFørsteMedlemskap)
        assertThat(historikk.medlemskapsperioder[1]).isEqualTo(forventetUendretMedlemskapsperiode)
    }

    @Test
    fun `getMedlemskapsperioder med opphold slår sammen bostedsadresser til bosattperioder med gyldig lik true og false`() {

        val pdlPerson =
                pdlPerson(Pair(LocalDate.of(2002, 2, 14), LocalDateTime.of(2007, 10, 12, 0, 0)),
                          Pair(LocalDate.of(2015, 6, 14), null))
        val medlemskapsinfoUtenUnntak = Medlemskapsinfo("3213213", emptyList(), emptyList(), emptyList())
        val forventetUgyldigPeriodeFørFørsteMedlemskap =
                Periode(LocalDate.MIN, LocalDate.of(2002, 2, 13), false)
        val forventetGyldigPeriodeSomSamsvarerMedPeriodeForBostedsadresse =
                Periode(LocalDate.of(2002, 2, 14), LocalDate.of(2007, 10, 12), true)
        val forventetOppholdIMedlemskapSomTilsvarerOppholdIBostedshistorikk =
                Periode(LocalDate.of(2007, 10, 13), LocalDate.of(2015, 6, 13), false)
        val forventetPågåendeMedlemskapsperiodeSomFølgeAvNåværendeBosted =
                Periode(LocalDate.of(2015, 6, 14), LocalDate.MAX, true)

        val historikk = Medlemskapshistorikk(pdlPerson, medlemskapsinfoUtenUnntak)

        assertThat(historikk.medlemskapsperioder[0]).isEqualTo(forventetUgyldigPeriodeFørFørsteMedlemskap)
        assertThat(historikk.medlemskapsperioder[1]).isEqualTo(forventetGyldigPeriodeSomSamsvarerMedPeriodeForBostedsadresse)
        assertThat(historikk.medlemskapsperioder[2]).isEqualTo(forventetOppholdIMedlemskapSomTilsvarerOppholdIBostedshistorikk)
        assertThat(historikk.medlemskapsperioder[3]).isEqualTo(forventetPågåendeMedlemskapsperiodeSomFølgeAvNåværendeBosted)
    }

    @Test
    fun `getMedlemskapsperioder legger til avsluttende ugyldig periode hvis siste bostedsadresse er avsluttet`() {
        val pdlPerson = pdlPerson(Pair(LocalDate.of(2002, 2, 14),
                                       LocalDateTime.of(2007, 10, 12, 0, 0)))
        val medlemskapsinfoUtenUnntak = Medlemskapsinfo("3213213", emptyList(), emptyList(), emptyList())
        val forventetUgyldigPeriodeFørFørsteMedlemskap = Periode(LocalDate.MIN, LocalDate.of(2002, 2, 13), false)
        val forventetGyldigPeriodeSomSamsvarerMedBostedsadresse =
                Periode(LocalDate.of(2002, 2, 14), LocalDate.of(2007, 10, 12), true)
        val forventetUgyldigEvigPeriodeEtterSisteBostedsadresse =
                Periode(LocalDate.of(2007, 10, 13), LocalDate.MAX, false)

        val historikk = Medlemskapshistorikk(pdlPerson, medlemskapsinfoUtenUnntak)

        assertThat(historikk.medlemskapsperioder[0]).isEqualTo(forventetUgyldigPeriodeFørFørsteMedlemskap)
        assertThat(historikk.medlemskapsperioder[1]).isEqualTo(forventetGyldigPeriodeSomSamsvarerMedBostedsadresse)
        assertThat(historikk.medlemskapsperioder[2]).isEqualTo(forventetUgyldigEvigPeriodeEtterSisteBostedsadresse)
    }

    @Test
    fun `getMedlemskapsperioder med gyldig unntak i opphold gir gyldig medlemskap`() {

        val pdlPerson = pdlPerson()
        val medlemskapsinfoMedGyldigePeriode =
                Medlemskapsinfo("3213213",
                                gyldigePerioder(Pair(LocalDate.of(2014, 1, 12),
                                                     LocalDate.of(2017, 4, 12))),
                                emptyList(),
                                emptyList())
        val forventetUgyldigPeriodeFørFørsteMedlemskap =
                Periode(LocalDate.MIN, LocalDate.of(2014, 1, 11), false)
        val forventetGyldigPeriodeSomSamsvarerMedGyldigUnntak =
                Periode(LocalDate.of(2014, 1, 12), LocalDate.of(2017, 4, 12), true)
        val forventetEvigvarendeUgyldigPeriodeEtterGyldigUnntak =
                Periode(LocalDate.of(2017, 4, 13), LocalDate.MAX, false)

        val historikk = Medlemskapshistorikk(pdlPerson, medlemskapsinfoMedGyldigePeriode)

        assertThat(historikk.medlemskapsperioder[0]).isEqualTo(forventetUgyldigPeriodeFørFørsteMedlemskap)
        assertThat(historikk.medlemskapsperioder[1]).isEqualTo(forventetGyldigPeriodeSomSamsvarerMedGyldigUnntak)
        assertThat(historikk.medlemskapsperioder[2]).isEqualTo(forventetEvigvarendeUgyldigPeriodeEtterGyldigUnntak)
    }

    @Test
    fun `getMedlemskapsperioder med uavklart unntak i opphold gir ukjent medlemskap`() {

        val pdlPerson = pdlPerson()
        val medlemskapsinfoMedUavklartePerioder =
                Medlemskapsinfo("3213213",
                                emptyList(),
                                emptyList(),
                                uavklartePerioder(Pair(LocalDate.of(2014, 1, 12),
                                                       LocalDate.of(2017, 4, 12))))
        val forventetUgyldigPeriodeFørFørsteMedlemskap =
                Periode(LocalDate.MIN, LocalDate.of(2014, 1, 11), false)
        val forventetEvigvarendeUgyldigPeriodeEtterUavklartUnntak =
                Periode(LocalDate.of(2017, 4, 13), LocalDate.MAX, false)
        val forventetUkjentPeriodeSomSamsvarerMedUavklartUnntak =
                Periode(LocalDate.of(2014, 1, 12), LocalDate.of(2017, 4, 12), null)

        val historikk = Medlemskapshistorikk(pdlPerson, medlemskapsinfoMedUavklartePerioder)

        assertThat(historikk.medlemskapsperioder[0]).isEqualTo(forventetUgyldigPeriodeFørFørsteMedlemskap)
        assertThat(historikk.medlemskapsperioder[1]).isEqualTo(forventetUkjentPeriodeSomSamsvarerMedUavklartUnntak)
        assertThat(historikk.medlemskapsperioder[2]).isEqualTo(forventetEvigvarendeUgyldigPeriodeEtterUavklartUnntak)
    }

    @Test
    fun `getMedlemskapsperioder med gyldig unntak som går over forskjellige perioder påvirker begge perioder`() {

        val pdlPerson =
                pdlPerson(Pair(LocalDate.of(2002, 2, 14), LocalDateTime.of(2007, 10, 12, 0, 0)),
                          Pair(LocalDate.of(2015, 6, 14), null))
        val medlemskapsinfoMedGyldigePeriode =
                Medlemskapsinfo("3213213",
                                gyldigePerioder(Pair(LocalDate.of(2014, 1, 12),
                                                     LocalDate.of(2017, 4, 12))),
                                emptyList(),
                                emptyList())
        val forventetUgyldigPeriodeFørFørsteMedlemskap = Periode(LocalDate.MIN, LocalDate.of(2002, 2, 13), false)
        val forventetGyldigPeriodeSomSamsvarerMedBostedsadresse =
                Periode(LocalDate.of(2002, 2, 14), LocalDate.of(2007, 10, 12), true)
        val forventetUgyldigPeriodeSomErForkortetAvGyldigUnntak =
                Periode(LocalDate.of(2007, 10, 13), LocalDate.of(2014, 1, 11), false)
        val forventetGyldigPeriodeSomErForlengetGrunnetGyldigUnntak =
                Periode(LocalDate.of(2014, 1, 12), LocalDate.MAX, true)

        val historikk = Medlemskapshistorikk(pdlPerson, medlemskapsinfoMedGyldigePeriode)

        assertThat(historikk.medlemskapsperioder[0]).isEqualTo(forventetUgyldigPeriodeFørFørsteMedlemskap)
        assertThat(historikk.medlemskapsperioder[1]).isEqualTo(forventetGyldigPeriodeSomSamsvarerMedBostedsadresse)
        assertThat(historikk.medlemskapsperioder[2]).isEqualTo(forventetUgyldigPeriodeSomErForkortetAvGyldigUnntak)
        assertThat(historikk.medlemskapsperioder[3]).isEqualTo(forventetGyldigPeriodeSomErForlengetGrunnetGyldigUnntak)
    }

}


