# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Sanksjon av barnetilsyn

  Scenario: Sanksjon mellom 2 måneder

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter | Fra og med dato | Til og med dato | Vedtaksperiode | Aktivitet | Sanksjonsårsak        |
      | 1            | INNVILGE        | 1           | 200      | 01.2021         | 03.2021         | ORDINÆR        | I_ARBEID  |                       |
      | 2            | SANKSJONERE     | 0           | 0        | 02.2021         | 02.2021         | SANKSJON_1_MND |           | NEKTET_TILBUDT_ARBEID |

    Og følgende kontantstøtte
      | BehandlingId | Beløp | Fra og med dato | Til og med dato |
      | 1            | 10    | 01.2021         | 03.2021         |

    Og følgende tilleggsstønad
      | BehandlingId | Beløp | Fra og med dato | Til og med dato |
      | 1            | 15    | 01.2021         | 03.2021         |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Fra og med dato | Til og med dato | Vedtaksperiode | Endret i behandlingId | Kontantstøtte | Tilleggsstønad | Antall barn | Utgifter | Beløp | Aktivitet | Sanksjonsårsak        |
      | 1            | SPLITTET     | 01.2021         | 01.2021         | ORDINÆR        | 2                     | 10            | 15             | 1           | 200      | 107   | I_ARBEID  |                       |
      | 1            | FJERNET      | 02.2021         | 03.2021         | ORDINÆR        | 2                     | 10            | 15             | 1           | 200      | 107   | I_ARBEID  |                       |
      | 2            |              | 02.2021         | 02.2021         | SANKSJON_1_MND |                       | 0             | 0              | 0           | 0        | 0     |           | NEKTET_TILBUDT_ARBEID |
      | 1            |              | 03.2021         | 03.2021         | ORDINÆR        |                       | 10            | 15             | 1           | 200      | 107   | I_ARBEID  |                       |

