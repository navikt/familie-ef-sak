# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Opphør og sanksjon

  Scenario: Sanksjonerer der det tidligere vært opphør

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Vedtaksperiode | Opphørsdato |
      | 1            | 01.2021         | 02.2021         |                 |                |             |
      | 2            |                 |                 | OPPHØRT         |                | 02.2021     |
      | 3            | 02.2021         | 02.2021         | SANKSJONERE     | SANKSJON       |             |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Vedtaksperiode | Er opphør |
      | 1            | 01.2021         | 01.2021         | SPLITTET     | 2                     |                |           |
      | 1            | 02.2021         | 02.2021         | FJERNET      | 2                     |                |           |
      | 2            | 02.2021         | 02.2021         | ERSTATTET    | 3                     |                | Ja        |
      | 3            | 02.2021         | 02.2021         |              |                       | SANKSJON       |           |

  Scenario: Sanksjonerer før tidligere opphør

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Vedtaksperiode | Opphørsdato |
      | 1            | 01.2021         | 03.2021         |                 |                |             |
      | 2            |                 |                 | OPPHØRT         |                | 03.2021     |
      | 3            | 02.2021         | 02.2021         | SANKSJONERE     | SANKSJON       |             |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Vedtaksperiode | Er opphør |
      | 1            | 01.2021         | 01.2021         | SPLITTET     | 3                     |                |           |
      | 1            | 02.2021         | 02.2021         | FJERNET      | 3                     |                |           |
      | 3            | 02.2021         | 02.2021         |              |                       | SANKSJON       |           |
      | 1            | 03.2021         | 03.2021         | FJERNET      | 2                     |                |           |
      | 2            | 03.2021         | 03.2021         |              |                       |                | Ja        |

  Scenario: Opphører før sanksjon støttes ikke i BeregnYtelseSteg ennå

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Vedtaksperiode | Opphørsdato |
      | 1            | 01.2021         | 03.2021         |                 |                |             |
      | 2            | 03.2021         | 03.2021         | SANKSJONERE     | SANKSJON       |             |
      | 3            |                 |                 | OPPHØRT         |                | 02.2021     |

    Når beregner ytelse kaster feil med innehold Systemet støtter ikke revurdering før sanksjonsperioden


