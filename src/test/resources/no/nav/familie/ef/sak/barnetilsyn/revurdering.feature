# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Revurdering av barnetilsyn

  Scenario: Endret aktivitet på første måned i revurderingen vises i andelshistorikken 2

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Fra og med dato | Til og med dato | Antall barn | Utgifter | Vedtaksperiode | Aktivitet          |
      | 1            | INNVILGE        | 02.2024         | 02.2024         | 1           | 1114     | ORDINÆR        | I_ARBEID           |
      | 1            | INNVILGE        | 03.2024         | 12.2024         | 1           | 1113     | ORDINÆR        | I_ARBEID           |
      | 2            | INNVILGE        | 03.2024         | 03.2024         | 1           | 1213     | ORDINÆR        | FORBIGÅENDE_SYKDOM |
      | 2            | INNVILGE        | 04.2024         | 12.2024         | 1           | 1113     | ORDINÆR        | I_ARBEID           |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype  | Endret i behandlingId | Vedtaksperiode | Aktivitet          |
      | 1            | 02.2024         | 02.2024         |               |                       | ORDINÆR        | I_ARBEID           |
      | 1            | 03.2024         | 12.2024         | ERSTATTET     | 2                     | ORDINÆR        | I_ARBEID           |
      | 2            | 03.2024         | 03.2024         |               |                       | ORDINÆR        | FORBIGÅENDE_SYKDOM |
      | 2            | 04.2024         | 12.2024         |               |                       | ORDINÆR        | I_ARBEID           |

  Scenario: Endret aktivitet på første måned i revurderingen vises i andelshistorikken

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Fra og med dato | Til og med dato | Antall barn | Utgifter | Vedtaksperiode | Aktivitet          |
      | 1            | INNVILGE        | 02.2024         | 02.2024         | 1           | 1114     | ORDINÆR        | I_ARBEID           |
      | 1            | INNVILGE        | 03.2024         | 12.2024         | 1           | 1113     | ORDINÆR        | I_ARBEID           |
      | 2            | INNVILGE        | 03.2024         | 03.2024         | 1           | 1113     | ORDINÆR        | FORBIGÅENDE_SYKDOM |
      | 2            | INNVILGE        | 04.2024         | 12.2024         | 1           | 1113     | ORDINÆR        | I_ARBEID           |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype  | Endret i behandlingId | Vedtaksperiode | Aktivitet          |
      | 1            | 02.2024         | 02.2024         |               |                       | ORDINÆR        | I_ARBEID           |
      | 1            | 03.2024         | 12.2024         | ERSTATTET       | 2                      | ORDINÆR        | I_ARBEID |
      | 2            | 03.2024         | 03.2024         |               |                       | ORDINÆR        | FORBIGÅENDE_SYKDOM           |
      | 2            | 04.2024         | 12.2024         |               |                       | ORDINÆR        | I_ARBEID           |

  Scenario: Revurdering uten endringer men fra samma dato skriver over tidligere rad

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter | Vedtaksperiode | Aktivitet |
      | 1            | INNVILGE        | 1           | 200      | ORDINÆR        | I_ARBEID  |
      | 2            | INNVILGE        | 1           | 200      | ORDINÆR        | FORBIGÅENDE_SYKDOM  |

    Og følgende kontantstøtte
      | BehandlingId | Beløp |
      | 1            | 10    |
      | 2            | 10    |

    Og følgende tilleggsstønad
      | BehandlingId | Beløp |
      | 1            | 15    |
      | 2            | 15    |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Kontantstøtte | Tilleggsstønad | Antall barn | Utgifter | Beløp |  Aktivitet |
      | 1            | ERSTATTET    | 2                     | 10            | 15             | 1           | 200      | 107   | I_ARBEID  |
      | 2            |              |                       | 10            | 15             | 1           | 200      | 107   | FORBIGÅENDE_SYKDOM  |


  Scenario: Revurdering uten endringer men fra samma dato skriver over tidligere rad (orginaltest)

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter | Vedtaksperiode |
      | 1            | INNVILGE        | 1           | 200      | ORDINÆR        |
      | 2            | INNVILGE        | 1           | 200      | ORDINÆR        |

    Og følgende kontantstøtte
      | BehandlingId | Beløp |
      | 1            | 10    |
      | 2            | 10    |

    Og følgende tilleggsstønad
      | BehandlingId | Beløp |
      | 1            | 15    |
      | 2            | 15    |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Kontantstøtte | Tilleggsstønad | Antall barn | Utgifter | Beløp |
      | 1            | FJERNET      | 2                     | 10            | 15             | 1           | 200      | 107   |
      | 2            |              |                       | 10            | 15             | 1           | 200      | 107   |



  Scenario: Revurdering med endring i tillegsstønad oh kontantstøtte blir markert som erstattet, fordi det fører til likt stønadsbeløp

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter | Vedtaksperiode | Aktivitet |
      | 1            | INNVILGE        | 1           | 200      | ORDINÆR        | I_ARBEID  |
      | 2            | INNVILGE        | 1           | 200      | ORDINÆR        | I_ARBEID  |

    Og følgende kontantstøtte
      | BehandlingId | Beløp |
      | 1            | 10    |
      | 2            | 2     |

    Og følgende tilleggsstønad
      | BehandlingId | Beløp |
      | 1            | 15    |
      | 2            | 20    |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Kontantstøtte | Tilleggsstønad | Antall barn | Utgifter | Beløp |
      | 1            | ERSTATTET    | 2                     | 10            | 15             | 1           | 200      | 107   |
      | 2            |              |                       | 2             | 20             | 1           | 200      | 107   |


  Scenario: Revurdering med endringer

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Fra og med dato | Til og med dato | Antall barn | Utgifter | Vedtaksperiode | Aktivitet |
      | 1            | INNVILGE        | 01.2021         | 03.2021         | 1           | 200      | ORDINÆR        | I_ARBEID  |
      | 2            | INNVILGE        | 02.2021         | 03.2021         | 1           | 200      | ORDINÆR        | I_ARBEID  |

    Og følgende kontantstøtte
      | BehandlingId | Fra og med dato | Til og med dato | Beløp |
      | 1            | 01.2021         | 03.2021         | 10    |
      | 2            | 02.2021         | 03.2021         | 10    |

    Og følgende tilleggsstønad
      | BehandlingId | Fra og med dato | Til og med dato | Beløp |
      | 1            | 01.2021         | 03.2021         | 15    |
      | 2            | 02.2021         | 03.2021         | 20    |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Kontantstøtte | Tilleggsstønad | Antall barn | Utgifter | Beløp |
      | 1            | 01.2021         | 01.2021         | SPLITTET     | 2                     | 10            | 15             | 1           | 200      | 107   |
      | 1            | 02.2021         | 03.2021         | FJERNET      | 2                     | 10            | 15             | 1           | 200      | 107   |
      | 2            | 02.2021         | 03.2021         |              |                       | 10            | 20             | 1           | 200      | 102   |
