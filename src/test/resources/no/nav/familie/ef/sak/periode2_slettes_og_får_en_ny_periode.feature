# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Nytt vedtak sletter andel og en ny periode

  Scenario: Endring i vedtaksperiode fører til sletting og en ny periode

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 01.2021         | 02.2021         |
      | 2            | 01.2021         | 03.2021         |

    Og følgende andeler tilkjent ytelse
      | BehandlingId | Fra og med dato | Til og med dato | Endret i behandlingId |
      | 1            | 01.2021         | 01.2021         |                       |
      | 1            | 02.2021         | 02.2021         |                       |
      | 2            | 01.2021         | 01.2021         | 1                     |
      | 2            | 03.2021         | 03.2021         |                       |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId |
      | 1            | 01.2021         | 01.2021         |              |                       |
      | 1            | 02.2021         | 02.2021         | FJERNET      | 2                     |
      | 2            | 03.2021         | 03.2021         |              |                       |

