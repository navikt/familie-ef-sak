# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Nytt vedtak sletter andel og en ny periode

  Scenario: Periode splittes som følge av G-omregning

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 01.2021         | 03.2021         |
      | 2            | 02.2021         | 03.2021         |

    Og følgende inntekter
      | BehandlingId | Fra og med dato |
      | 1            | 01.2021         |
      | 2            | 02.2021         |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId |
      | 1            | 01.2021         | 01.2021         | SPLITTET     | 2                     |
      | 1            | 02.2021         | 03.2021         | FJERNET      | 2                     |
      | 2            | 02.2021         | 03.2021         |              |                       |

