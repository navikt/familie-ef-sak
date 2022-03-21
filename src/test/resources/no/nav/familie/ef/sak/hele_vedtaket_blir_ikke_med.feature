# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Nytt vedtak som overlapper delvis med forrige vedtak

  Scenario: Nytt vedtak som delvis overlapper tidligere vedtak skal føre til en splitting og fjerning

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Aktivitet         |
      | 1            | 01.2021         | 03.2021         | BARN_UNDER_ETT_ÅR |
      | 2            | 02.2021         | 03.2021         | BARNET_ER_SYKT    |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt |
      | 1            | 01.2021         | 200000  |
      | 2            | 02.2021         | 300000  |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Fra og med dato | Til og med dato | Beløp | Aktivitet         |
      | 1            | SPLITTET     | 2                     | 01.2021         | 01.2021         | 13403 | BARN_UNDER_ETT_ÅR |
      | 1            | FJERNET      | 2                     | 02.2021         | 03.2021         | 13403 | BARN_UNDER_ETT_ÅR |
      | 2            |              |                       | 02.2021         | 03.2021         | 9653  | BARNET_ER_SYKT    |

