# language: no
# encoding: UTF-8

Egenskap: G-omregning

  Scenario: Enkel behandling med 1 periode g-omregnes

    Gitt siste grunnbeløp endres til år 2020 med beløp 101351

    Og følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Aktivitet         | Vedtaksperiode |
      | 1            | 01.2021         | 12.2021         | INNVILGE        | BARN_UNDER_ETT_ÅR | HOVEDPERIODE   |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt | Samordningsfradrag |
      | 1            | 01.2021         | 120000  | 0                  |

    Og beregner ytelse med G

    Og siste grunnbeløp endres til år 2021 med beløp 106399

    Når Utfør g-omregning

    Så forvent følgende andeler for g-omregnet tilkjent ytelse
      | Fra og med dato | Til og med dato | Kildebehandling | Inntekt | Beløp |
      | 01.2021         | 04.2021         | 1               | 120000  | 16403 |
      | 05.2021         | 12.2021         | 1               | 125000  | 17257 |