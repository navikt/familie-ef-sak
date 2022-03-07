# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Nytt vedtak som overlapper delvis med forrige vedtak

  Scenario: Nytt vedtak som delvis overlapper tidligere vedtak skal føre til en splitting og fjerning

    Gitt følgende vedtak
      | BehandlingId | Vedtaksresultat | Fra og med dato | Til og med dato | Aktivitet         |
      | 1            | INNVILGE        | 01.2021         | 03.2021         | BARN_UNDER_ETT_ÅR |
      | 2            | INNVILGE        | 02.2021         | 03.2021         | BARNET_ER_SYKT    |

    Og følgende andeler tilkjent ytelse
      | BehandlingId | Beløp | Fra og med dato | Til og med dato |
      | 1            | 1000  | 01.2021         | 03.2021         |
      | 2            | 1000  | 01.2021         | 01.2021         |
      | 2            | 2000  | 02.2021         | 03.2021         |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Fra og med dato | Til og med dato | Beløp | Aktivitet         |
      | 1            | SPLITTET     | 2                     | 01.2021         | 01.2021         | 1000  | BARN_UNDER_ETT_ÅR |
      | 1            | FJERNET      | 2                     | 02.2021         | 03.2021         | 1000  | BARN_UNDER_ETT_ÅR |
      | 2            |              |                       | 02.2021         | 03.2021         | 2000  | BARNET_ER_SYKT    |

