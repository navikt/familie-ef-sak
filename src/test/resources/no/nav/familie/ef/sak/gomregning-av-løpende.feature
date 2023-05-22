# language: no
# encoding: UTF-8

Egenskap: G-omregning

  Scenario: Enkel behandling med 1 periode g-omregnes

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 01.2021         | 12.2021         |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt |
      | 1            | 01.2021         | 120000  |

    Og beregner ytelse med G for år 2020 med beløp 101351

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Kildebehandling | Inntekt | Beløp |
      | 01.2021         | 12.2021         | 1               | 120000  | 16403 |

    Når utfør g-omregning for år 2021 med beløp 106399

    Så forvent følgende andeler for g-omregnet tilkjent ytelse
      | Fra og med dato | Til og med dato | Inntekt | Beløp |
      | 01.2021         | 04.2021         | 120000  | 16403 |
      | 05.2021         | 12.2021         | 125000  | 17257 |