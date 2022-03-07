# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Vedtak med to perioder blir erstattet med nytt vedtak

  Scenario: Vedtak med to perioder blir erstattet og fører til en erstatning og fjerning av periode

    Gitt følgende vedtak
      | BehandlingId | Vedtaksresultat | Aktivitet          | Fra og med dato | Til og med dato |
      | 1            | INNVILGE        | BARN_UNDER_ETT_ÅR  | 01.2021         | 01.2021         |
      | 1            | INNVILGE        | FORSØRGER_I_ARBEID | 02.2021         | 12.2021         |
      | 2            | INNVILGE        | BARN_UNDER_ETT_ÅR  | 01.2021         | 03.2021         |
      | 2            | INNVILGE        | FORSØRGER_I_ARBEID | 04.2021         | 01.2022         |

    Og følgende andeler tilkjent ytelse
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 01.2021         | 01.2021         |
      | 1            | 02.2021         | 12.2021         |
      | 2            | 01.2021         | 03.2021         |
      | 2            | 04.2021         | 01.2022         |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Fra og med dato | Til og med dato | Aktivitet          |
      | 1            | ERSTATTET    | 2                     | 01.2021         | 01.2021         | BARN_UNDER_ETT_ÅR  |
      | 1            | FJERNET      | 2                     | 02.2021         | 12.2021         | FORSØRGER_I_ARBEID |
      | 2            |              |                       | 01.2021         | 03.2021         | BARN_UNDER_ETT_ÅR  |
      | 2            |              |                       | 04.2021         | 01.2022         | FORSØRGER_I_ARBEID |

