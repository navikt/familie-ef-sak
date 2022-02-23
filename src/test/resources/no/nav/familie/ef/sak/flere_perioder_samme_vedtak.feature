# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Vedtak med to perioder blir erstattet med nytt vedtak

  Scenario: Vedtak med to perioder blir erstattet og fører til en erstatning og fjerning av periode

    Gitt følgende vedtak
      | BehandlingId | Vedtaksresultat | Aktivitet         | Fra og med dato | Til og med dato |
      | 1            | INNVILGE        | BARN_UNDER_ETT_ÅR | 01.01.2021      | 31.12.2021      |
      | 2            | INNVILGE        | BARN_UNDER_ETT_ÅR | 01.01.2021      | 31.01.2022      |

    Og følgende andeler tilkjent ytelse
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 01.01.2021      | 31.01.2021      |
      | 1            | 01.02.2021      | 31.12.2021      |
      | 2            | 01.01.2021      | 31.03.2021      |
      | 2            | 01.04.2021      | 31.01.2022      |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Fra og med dato | Til og med dato |
      | 1            | ERSTATTET    | 2                     | 01.01.2021      | 31.01.2021      |
      | 1            | FJERNET      | 2                     | 01.02.2021      | 31.12.2021      |
      | 2            |              |                       | 01.01.2021      | 31.03.2021      |
      | 2            |              |                       | 01.04.2021      | 31.01.2022      |

