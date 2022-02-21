# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Vedtak opphører

  Scenario: Periode fjernes som følge av opphør

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 01.01.2021      | 31.01.2021      |
      | 2            | 01.01.2021      |                 |

    Og følgende andeler tilkjent ytelse
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 01.01.2021      | 31.01.2021      |

    Og følgende tilkjent ytelse uten andel
      | BehandlingId |
      | 2            |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId |
      | 1            | 01.01.2021      | 31.01.2021      | FJERNET      | 2                     |

