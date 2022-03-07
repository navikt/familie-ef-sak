# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Vedtak med to perioder blir erstattet med nytt vedtak

  Scenario: Vedtak med to perioder blir erstattet og fører til en erstatning og fjerning av periode

    Gitt følgende vedtak
      | BehandlingId | Aktivitet          | Fra og med dato | Til og med dato |
      | 1            | BARN_UNDER_ETT_ÅR  | 06.2021         | 06.2021         |
      | 1            | FORSØRGER_I_ARBEID | 07.2021         | 12.2021         |
      | 2            | BARN_UNDER_ETT_ÅR  | 06.2021         | 07.2021         |
      | 2            | FORSØRGER_I_ARBEID | 08.2021         | 01.2022         |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Fra og med dato | Til og med dato | Aktivitet          |
      | 1            | ERSTATTET    | 2                     | 06.2021         | 06.2021         | BARN_UNDER_ETT_ÅR  |
      | 1            | FJERNET      | 2                     | 07.2021         | 12.2021         | FORSØRGER_I_ARBEID |
      | 2            |              |                       | 06.2021         | 07.2021         | BARN_UNDER_ETT_ÅR  |
      | 2            |              |                       | 08.2021         | 01.2022         | FORSØRGER_I_ARBEID |

