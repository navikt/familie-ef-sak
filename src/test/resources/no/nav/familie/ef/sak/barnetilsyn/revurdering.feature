# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Revurdering av barnetilsyn

  Scenario: Revurdering uten endringer men fra samma dato skriver over tidligere rad

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter |
      | 1            | INNVILGE        | 1           | 200      |
      | 2            | INNVILGE        | 1           | 200      |

    Og følgende kontantstøtte
      | BehandlingId | Beløp |
      | 1            | 10    |
      | 2            | 10    |

    Og følgende tilleggsstønad
      | BehandlingId | Beløp |
      | 1            | 15    |
      | 2            | 15    |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Kontantstøtte | Tilleggsstønad | Antall barn | Utgifter | Beløp |
      | 1            | FJERNET      | 2                     | 10            | 15             | 1           | 200      | 107   |
      | 2            |              |                       | 10            | 15             | 1           | 200      | 107   |


  Scenario: Revurdering med endring i tillegsstønad oh kontantstøtte blir markert som erstattet, fordi det fører til likt stønadsbeløp

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter |
      | 1            | INNVILGE        | 1           | 200      |
      | 2            | INNVILGE        | 1           | 200      |

    Og følgende kontantstøtte
      | BehandlingId | Beløp |
      | 1            | 10    |
      | 2            | 2     |

    Og følgende tilleggsstønad
      | BehandlingId | Beløp |
      | 1            | 15    |
      | 2            | 20    |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Kontantstøtte | Tilleggsstønad | Antall barn | Utgifter | Beløp |
      | 1            | ERSTATTET    | 2                     | 10            | 15             | 1           | 200      | 107   |
      | 2            |              |                       | 2             | 20             | 1           | 200      | 107   |


  Scenario: Revurdering med endringer

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Fra og med dato | Til og med dato | Antall barn | Utgifter |
      | 1            | INNVILGE        | 01.2021         | 03.2021         | 1           | 200      |
      | 2            | INNVILGE        | 02.2021         | 03.2021         | 1           | 200      |

    Og følgende kontantstøtte
      | BehandlingId | Fra og med dato | Til og med dato | Beløp |
      | 1            | 01.2021         | 03.2021         | 10    |
      | 2            | 02.2021         | 03.2021         | 10    |

    Og følgende tilleggsstønad
      | BehandlingId | Fra og med dato | Til og med dato | Beløp |
      | 1            | 01.2021         | 03.2021         | 15    |
      | 2            | 02.2021         | 03.2021         | 20    |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Kontantstøtte | Tilleggsstønad | Antall barn | Utgifter | Beløp |
      | 1            | 01.2021         | 01.2021         | SPLITTET     | 2                     | 10            | 15             | 1           | 200      | 107   |
      | 1            | 02.2021         | 03.2021         | FJERNET      | 2                     | 10            | 15             | 1           | 200      | 107   |
      | 2            | 02.2021         | 03.2021         |              |                       | 10            | 20             | 1           | 200      | 102   |

