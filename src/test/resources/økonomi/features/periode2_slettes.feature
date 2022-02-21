# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Nytt vedtak sletter andel

  Scenario: Endring i vedtaksperiode fører til sletting

    Gitt følgende vedtak
      | BehandlingId | Vedtaksresultat | Aktivitet         | Fra og med dato | Til og med dato |
      | 1            | INNVILGE        | BARN_UNDER_ETT_ÅR | 01.01.2021      | 28.02.2021      |
      | 2            | INNVILGE        | BARN_UNDER_ETT_ÅR | 01.01.2021      | 31.01.2021      |

    Og følgende andeler tilkjent ytelse
      | BehandlingId | Fra og med dato | Til og med dato | Endret i behandlingId |
      | 1            | 01.01.2021      | 31.01.2021      |                       |
      | 1            | 01.02.2021      | 28.02.2021      |                       |
      | 2            | 01.01.2021      | 31.01.2021      | 1                     |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Fra og med dato | Til og med dato |
      | 1            |              |                       | 01.01.2021      | 31.01.2021      |
      | 1            | FJERNET      | 2                     | 01.02.2021      | 28.02.2021      |

