# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Nytt vedtak sletter andel og en ny periode

  Scenario: Endring i vedtaksperiode fører til sletting og en ny periode

    Gitt følgende vedtak
      | BehandlingId | Vedtaksresultat | Fra og med dato | Til og med dato |
      | 1            | INNVILGE        | 01.2021         | 02.2021         |
      | 2            | INNVILGE        | 02.2021         | 03.2021         |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt |
      | 1            | 01.2021         | 100000  |
      | 1            | 02.2021         | 100000  |
      | 2            | 02.2021         | 200000  |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId |
      | 1            | 01.2021         | 01.2021         |              |                       |
      | 1            | 02.2021         | 02.2021         | ERSTATTET    | 2                     |
      | 2            | 02.2021         | 03.2021         |              |                       |

