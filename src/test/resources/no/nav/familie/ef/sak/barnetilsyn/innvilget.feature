# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Enkel innvilget behandling av typen barnetilsyn

  Scenario: Innvilget barnetilsyn skal gi informasjon om antall barn, utgifter, kontantstøtte

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter |
      | 1            | INNVILGE        | 1           | 200      |

    Og følgende kontantstøtte
      | BehandlingId | Beløp |
      | 1            | 10    |

    Og følgende tilleggsstønad
      | BehandlingId | Beløp |
      | 1            | 15    |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Kontantstøtte | Tilleggsstønad | Antall barn | Utgifter | Beløp |
      | 1            |              |                       | 10            | 15             | 1           | 200      | 107   |


  Scenario: Innvilget barnetilsyn med 0 barn gir 0 i stønadsbeløp

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter |
      | 1            | INNVILGE        | 0           | 200      |

    Og følgende kontantstøtte
      | BehandlingId | Beløp |
      | 1            | 10    |

    Og følgende tilleggsstønad
      | BehandlingId | Beløp |
      | 1            | 15    |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Kontantstøtte | Tilleggsstønad | Antall barn | Utgifter | Beløp |
      | 1            |              |                       | 10            | 15             | 0           | 200      | 0     |

