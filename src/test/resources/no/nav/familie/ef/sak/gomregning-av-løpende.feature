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
      | 1            | 01.2021         | 10000   | 0                  |

    Og beregner ytelse med G

    Og siste grunnbeløp endres til år 2021 med beløp 106399

    Når Utfør g-omregning

#    Så skal vi ha to perioder - før og etter dato: mai.2021
#
#    Så skal inntekt være justert til: 10900???
#
#    Så skal nytt beløp være: 10900???




