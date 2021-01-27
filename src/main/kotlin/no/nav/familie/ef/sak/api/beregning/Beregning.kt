package no.nav.familie.ef.sak.api.beregning

import java.math.BigDecimal
import java.time.LocalDate

data class Beløpsperiode(val fraOgMedDato: LocalDate,
                         val tilDato: LocalDate,
                         val beløp: BigDecimal)

data class Grunnbeløp(val fraOgMedDato: LocalDate,
                      val tilDato: LocalDate,
                      val grunnbeløp: BigDecimal,
                      val perMnd: BigDecimal,
                      val gjennomsnittPerÅr: BigDecimal? = null)

fun finnGrunnbeløpsPerioder(fraOgMedDato: LocalDate, tilDato: LocalDate): List<Beløpsperiode> {
    return grunnbeløpsperioder
            .filter { overlapper(it, fraOgMedDato, tilDato) }
            .map { Beløpsperiode(maxOf(it.fraOgMedDato, fraOgMedDato), minOf(it.tilDato, tilDato), it.grunnbeløp) }
            .sortedBy { it.fraOgMedDato }
}

private fun overlapper(grunnbeløpsperiode: Grunnbeløp,
                       fraOgMedDato: LocalDate,
                       tilDato: LocalDate) =
        grunnbeløpsperiode.fraOgMedDato in fraOgMedDato..tilDato || fraOgMedDato in grunnbeløpsperiode.fraOgMedDato..grunnbeløpsperiode.tilDato.minusDays(1)

// TODO: Kopiert inn fra https://github.com/navikt/g - kan kanskje kalle tjenesten på sikt hvis den er tenkt å være oppdatert?
val grunnbeløpsperioder: List<Grunnbeløp> =
        listOf(
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2020-05-01"),
                        tilDato = LocalDate.MAX,
                        grunnbeløp = 101351.toBigDecimal(),
                        perMnd = 8446.toBigDecimal(),
                        gjennomsnittPerÅr = 100853.toBigDecimal()
                ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2019-05-01"),
                        tilDato = LocalDate.parse("2020-05-01"),
                        grunnbeløp = 99858.toBigDecimal(),
                        perMnd = 8322.toBigDecimal(),
                        gjennomsnittPerÅr = 98866.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2018-05-01"),
                        tilDato = LocalDate.parse("2019-05-01"),
                        grunnbeløp = 96883.toBigDecimal(),
                        perMnd = 8074.toBigDecimal(),
                        gjennomsnittPerÅr = 95800.toBigDecimal(),
                ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2017-05-01"),
                        tilDato = LocalDate.parse("2018-05-01"),
                        grunnbeløp = 93634.toBigDecimal(),
                        perMnd = 7803.toBigDecimal(),
                        gjennomsnittPerÅr = 93281.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2016-05-01"),
                        tilDato = LocalDate.parse("2017-05-01"),
                        grunnbeløp = 92576.toBigDecimal(),
                        perMnd = 7715.toBigDecimal(),
                        gjennomsnittPerÅr = 91740.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2015-05-01"),
                        tilDato = LocalDate.parse("2016-05-01"),
                        grunnbeløp = 90068.toBigDecimal(),
                        perMnd = 7506.toBigDecimal(),
                        gjennomsnittPerÅr = 89502.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2014-05-01"),
                        tilDato = LocalDate.parse("2015-05-01"),
                        grunnbeløp = 88370.toBigDecimal(),
                        perMnd = 7364.toBigDecimal(),
                        gjennomsnittPerÅr = 87328.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2013-05-01"),
                        tilDato = LocalDate.parse("2014-05-01"),
                        grunnbeløp = 85245.toBigDecimal(),
                        perMnd = 7104.toBigDecimal(),
                        gjennomsnittPerÅr = 84204.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2012-05-01"),
                        tilDato = LocalDate.parse("2013-05-01"),
                        grunnbeløp = 82122.toBigDecimal(),
                        perMnd = 6844.toBigDecimal(),
                        gjennomsnittPerÅr = 81153.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2011-05-01"),
                        tilDato = LocalDate.parse("2012-05-01"),
                        grunnbeløp = 79216.toBigDecimal(),
                        perMnd = 6601.toBigDecimal(),
                        gjennomsnittPerÅr = 78024.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2010-05-01"),
                        tilDato = LocalDate.parse("2011-05-01"),
                        grunnbeløp = 75641.toBigDecimal(),
                        perMnd = 6303.toBigDecimal(),
                        gjennomsnittPerÅr = 74721.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2009-05-01"),
                        tilDato = LocalDate.parse("2010-05-01"),
                        grunnbeløp = 72881.toBigDecimal(),
                        perMnd = 6073.toBigDecimal(),
                        gjennomsnittPerÅr = 72006.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2008-05-01"),
                        tilDato = LocalDate.parse("2009-05-01"),
                        grunnbeløp = 70256.toBigDecimal(),
                        perMnd = 5855.toBigDecimal(),
                        gjennomsnittPerÅr = 69108.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2007-05-01"),
                        tilDato = LocalDate.parse("2008-05-01"),
                        grunnbeløp = 66812.toBigDecimal(),
                        perMnd = 5568.toBigDecimal(),
                        gjennomsnittPerÅr = 65505.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2006-05-01"),
                        tilDato = LocalDate.parse("2007-05-01"),
                        grunnbeløp = 62892.toBigDecimal(),
                        perMnd = 5241.toBigDecimal(),
                        gjennomsnittPerÅr = 62161.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2005-05-01"),
                        tilDato = LocalDate.parse("2006-05-01"),
                        grunnbeløp = 60699.toBigDecimal(),
                        perMnd = 5058.toBigDecimal(),
                        gjennomsnittPerÅr = 60059.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2004-05-01"),
                        tilDato = LocalDate.parse("2005-05-01"),
                        grunnbeløp = 58778.toBigDecimal(),
                        perMnd = 4898.toBigDecimal(),
                        gjennomsnittPerÅr = 58139.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2003-05-01"),
                        tilDato = LocalDate.parse("2004-05-01"),
                        grunnbeløp = 56861.toBigDecimal(),
                        perMnd = 4738.toBigDecimal(),
                        gjennomsnittPerÅr = 55964.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2002-05-01"),
                        tilDato = LocalDate.parse("2003-05-01"),
                        grunnbeløp = 54170.toBigDecimal(),
                        perMnd = 4514.toBigDecimal(),
                        gjennomsnittPerÅr = 53233.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2001-05-01"),
                        tilDato = LocalDate.parse("2002-05-01"),
                        grunnbeløp = 51360.toBigDecimal(),
                        perMnd = 4280.toBigDecimal(),
                        gjennomsnittPerÅr = 50603.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("2000-05-01"),
                        tilDato = LocalDate.parse("2001-05-01"),
                        grunnbeløp = 49090.toBigDecimal(),
                        perMnd = 4091.toBigDecimal(),
                        gjennomsnittPerÅr = 48377.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1999-05-01"),
                        tilDato = LocalDate.parse("2000-05-01"),
                        grunnbeløp = 46950.toBigDecimal(),
                        perMnd = 3913.toBigDecimal(),
                        gjennomsnittPerÅr = 46423.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1998-05-01"),
                        tilDato = LocalDate.parse("1999-05-01"),
                        grunnbeløp = 45370.toBigDecimal(),
                        perMnd = 3781.toBigDecimal(),
                        gjennomsnittPerÅr = 44413.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1997-05-01"),
                        tilDato = LocalDate.parse("1998-05-01"),
                        grunnbeløp = 42500.toBigDecimal(),
                        perMnd = 3542.toBigDecimal(),
                        gjennomsnittPerÅr = 42000.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1996-05-01"),
                        tilDato = LocalDate.parse("1997-05-01"),
                        grunnbeløp = 41000.toBigDecimal(),
                        perMnd = 3417.toBigDecimal(),
                        gjennomsnittPerÅr = 40410.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1995-05-01"),
                        tilDato = LocalDate.parse("1996-05-01"),
                        grunnbeløp = 39230.toBigDecimal(),
                        perMnd = 3269.toBigDecimal(),
                        gjennomsnittPerÅr = 38847.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1994-05-01"),
                        tilDato = LocalDate.parse("1995-05-01"),
                        grunnbeløp = 38080.toBigDecimal(),
                        perMnd = 3173.toBigDecimal(),
                        gjennomsnittPerÅr = 37820.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1993-05-01"),
                        tilDato = LocalDate.parse("1994-05-01"),
                        grunnbeløp = 37300.toBigDecimal(),
                        perMnd = 3108.toBigDecimal(),
                        gjennomsnittPerÅr = 37033.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1992-05-01"),
                        tilDato = LocalDate.parse("1993-05-01"),
                        grunnbeløp = 36500.toBigDecimal(),
                        perMnd = 3042.toBigDecimal(),
                        gjennomsnittPerÅr = 36167.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1991-05-01"),
                        tilDato = LocalDate.parse("1992-05-01"),
                        grunnbeløp = 35500.toBigDecimal(),
                        perMnd = 2958.toBigDecimal(),
                        gjennomsnittPerÅr = 35033.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1990-12-01"),
                        tilDato = LocalDate.parse("1991-05-01"),
                        grunnbeløp = 34100.toBigDecimal(),
                        perMnd = 2842.toBigDecimal(),
                        gjennomsnittPerÅr = 33575.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1990-05-01"),
                        tilDato = LocalDate.parse("1990-12-01"),
                        grunnbeløp = 34000.toBigDecimal(),
                        perMnd = 2833.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1989-04-01"),
                        tilDato = LocalDate.parse("1990-05-01"),
                        grunnbeløp = 32700.toBigDecimal(),
                        perMnd = 2725.toBigDecimal(),
                        gjennomsnittPerÅr = 32275.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1988-04-01"),
                        tilDato = LocalDate.parse("1989-04-01"),
                        grunnbeløp = 31000.toBigDecimal(),
                        perMnd = 2583.toBigDecimal(),
                        gjennomsnittPerÅr = 30850.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1988-01-01"),
                        tilDato = LocalDate.parse("1988-04-01"),
                        grunnbeløp = 30400.toBigDecimal(),
                        perMnd = 2533.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1987-05-01"),
                        tilDato = LocalDate.parse("1988-01-01"),
                        grunnbeløp = 29900.toBigDecimal(),
                        perMnd = 2492.toBigDecimal(),
                        gjennomsnittPerÅr = 29267.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1986-05-01"),
                        tilDato = LocalDate.parse("1987-05-01"),
                        grunnbeløp = 28000.toBigDecimal(),
                        perMnd = 2333.toBigDecimal(),
                        gjennomsnittPerÅr = 27433.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1986-01-01"),
                        tilDato = LocalDate.parse("1986-05-01"),
                        grunnbeløp = 26300.toBigDecimal(),
                        perMnd = 2192.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1985-05-01"),
                        tilDato = LocalDate.parse("1986-01-01"),
                        grunnbeløp = 25900.toBigDecimal(),
                        perMnd = 2158.toBigDecimal(),
                        gjennomsnittPerÅr = 25333.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1984-05-01"),
                        tilDato = LocalDate.parse("1985-05-01"),
                        grunnbeløp = 24200.toBigDecimal(),
                        perMnd = 2017.toBigDecimal(),
                        gjennomsnittPerÅr = 23667.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1983-05-01"),
                        tilDato = LocalDate.parse("1984-05-01"),
                        grunnbeløp = 22600.toBigDecimal(),
                        perMnd = 1883.toBigDecimal(),
                        gjennomsnittPerÅr = 22333.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1983-01-01"),
                        tilDato = LocalDate.parse("1983-05-01"),
                        grunnbeløp = 21800.toBigDecimal(),
                        perMnd = 1817.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1982-05-01"),
                        tilDato = LocalDate.parse("1983-01-01"),
                        grunnbeløp = 21200.toBigDecimal(),
                        perMnd = 1767.toBigDecimal(),
                        gjennomsnittPerÅr = 20667.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1981-10-01"),
                        tilDato = LocalDate.parse("1982-05-01"),
                        grunnbeløp = 19600.toBigDecimal(),
                        perMnd = 1633.toBigDecimal(),
                        gjennomsnittPerÅr = 18658.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1981-05-01"),
                        tilDato = LocalDate.parse("1981-10-01"),
                        grunnbeløp = 19100.toBigDecimal(),
                        perMnd = 1592.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1981-01-01"),
                        tilDato = LocalDate.parse("1981-05-01"),
                        grunnbeløp = 17400.toBigDecimal(),
                        perMnd = 1450.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1980-05-01"),
                        tilDato = LocalDate.parse("1981-01-01"),
                        grunnbeløp = 16900.toBigDecimal(),
                        perMnd = 1408.toBigDecimal(),
                        gjennomsnittPerÅr = 16633.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1980-01-01"),
                        tilDato = LocalDate.parse("1980-05-01"),
                        grunnbeløp = 16100.toBigDecimal(),
                        perMnd = 1342.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1979-01-01"),
                        tilDato = LocalDate.parse("1980-01-01"),
                        grunnbeløp = 15200.toBigDecimal(),
                        perMnd = 1267.toBigDecimal(),
                        gjennomsnittPerÅr = 15200.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1978-07-01"),
                        tilDato = LocalDate.parse("1979-01-01"),
                        grunnbeløp = 14700.toBigDecimal(),
                        perMnd = 1225.toBigDecimal(),
                        gjennomsnittPerÅr = 14550.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1977-12-01"),
                        tilDato = LocalDate.parse("1978-07-01"),
                        grunnbeløp = 14400.toBigDecimal(),
                        perMnd = 1200.toBigDecimal(),
                        gjennomsnittPerÅr = 13383.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1977-05-01"),
                        tilDato = LocalDate.parse("1977-12-01"),
                        grunnbeløp = 13400.toBigDecimal(),
                        perMnd = 1117.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1977-01-01"),
                        tilDato = LocalDate.parse("1977-05-01"),
                        grunnbeløp = 13100.toBigDecimal(),
                        perMnd = 1092.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1976-05-01"),
                        tilDato = LocalDate.parse("1977-01-01"),
                        grunnbeløp = 12100.toBigDecimal(),
                        perMnd = 1008.toBigDecimal(),
                        gjennomsnittPerÅr = 12000.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1976-01-01"),
                        tilDato = LocalDate.parse("1976-05-01"),
                        grunnbeløp = 11800.toBigDecimal(),
                        perMnd = 983.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1975-05-01"),
                        tilDato = LocalDate.parse("1976-01-01"),
                        grunnbeløp = 11000.toBigDecimal(),
                        perMnd = 917.toBigDecimal(),
                        gjennomsnittPerÅr = 10800.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1975-01-01"),
                        tilDato = LocalDate.parse("1975-05-01"),
                        grunnbeløp = 10400.toBigDecimal(),
                        perMnd = 867.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1974-05-01"),
                        tilDato = LocalDate.parse("1975-01-01"),
                        grunnbeløp = 9700.toBigDecimal(),
                        perMnd = 808.toBigDecimal(),
                        gjennomsnittPerÅr = 9533.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1974-01-01"),
                        tilDato = LocalDate.parse("1974-05-01"),
                        grunnbeløp = 9200.toBigDecimal(),
                        perMnd = 767.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1973-01-01"),
                        tilDato = LocalDate.parse("1974-01-01"),
                        grunnbeløp = 8500.toBigDecimal(),
                        perMnd = 708.toBigDecimal(),
                        gjennomsnittPerÅr = 8500.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1972-01-01"),
                        tilDato = LocalDate.parse("1973-01-01"),
                        grunnbeløp = 7900.toBigDecimal(),
                        perMnd = 658.toBigDecimal(),
                        gjennomsnittPerÅr = 7900.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1971-05-01"),
                        tilDato = LocalDate.parse("1972-01-01"),
                        grunnbeløp = 7500.toBigDecimal(),
                        perMnd = 625.toBigDecimal(),
                        gjennomsnittPerÅr = 7400.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1971-01-01"),
                        tilDato = LocalDate.parse("1971-05-01"),
                        grunnbeløp = 7200.toBigDecimal(),
                        perMnd = 600.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1970-01-01"),
                        tilDato = LocalDate.parse("1971-01-01"),
                        grunnbeløp = 6800.toBigDecimal(),
                        perMnd = 567.toBigDecimal(),
                        gjennomsnittPerÅr = 6800.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1969-01-01"),
                        tilDato = LocalDate.parse("1970-01-01"),
                        grunnbeløp = 6400.toBigDecimal(),
                        perMnd = 533.toBigDecimal(),
                        gjennomsnittPerÅr = 6400.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1968-01-01"),
                        tilDato = LocalDate.parse("1969-01-01"),
                        grunnbeløp = 5900.toBigDecimal(),
                        perMnd = 492.toBigDecimal(),
                        gjennomsnittPerÅr = 5900.toBigDecimal(),

                        ),
                Grunnbeløp(
                        fraOgMedDato = LocalDate.parse("1967-01-01"),
                        tilDato = LocalDate.parse("1968-01-01"),
                        grunnbeløp = 5400.toBigDecimal(),
                        perMnd = 450.toBigDecimal(),
                        gjennomsnittPerÅr = 5400.toBigDecimal()
                )
        )
