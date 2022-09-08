# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Behandling 2 blir oppdelt i 2 då den overlapper mai (G-omregning), behandling 1 sin første periode

  Scenario: Prodfeil 3

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 05.2022         | 04.2025         |
      | 2            | 02.2022         | 01.2025         |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt |
      | 1            | 05.2022         | 0       |
      | 2            | 02.2022         | 0       |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId |
      | 2            | 02.2022         | 04.2025         |              |                       |
      | 1            | 05.2025         | 04.2025         | FJERNET      | 2                     |
      | 2            | 05.2022         | 01.2025         |              |                       |

  Scenario: Prodfeil 2

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 05.2022         | 04.2025         |
      | 2            | 02.2022         | 01.2025         |
      | 3            | 07.2022         | 04.2025         |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt |
      | 1            | 05.2022         | 0       |
      | 2            | 02.2022         | 0       |
      | 3            | 07.2022         | 167000  |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId |
      | 2            | 02.2022         | 04.2022         |              |                       |
      | 1            | 05.2022         | 04.2025         | FJERNET      | 2                     |
      | 2            | 05.2022         | 06.2022         | SPLITTET     | 3                     |
      | 2            | 07.2022         | 01.2025         | FJERNET      | 3                     |
      | 3            | 07.2022         | 04.2025         |              |                       |

#      | 2            | 02.2022         | 04.2022         |              |                       |
#      | 1            | 05.2022         | 06.2022         | SPLITTET     | 3                     |
#      | 1            | 07.2022         | 01.2025         | FJERNET      | 3                     |
#      | 1            | 02.2025         | 04.2025         | FJERNET      | 2                     |
#      | 3            | 07.2022         | 04.2025         |              |                       |

  Scenario: Prodfeil

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 05.2022         | 04.2025         |
      | 2            | 05.2022         | 04.2025         |
      | 3            | 02.2022         | 01.2025         |
      | 4            | 07.2022         | 04.2025         |
      | 5            | 09.2022         | 01.2025         |
      | 6            | 10.2022         | 01.2025         |


    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt |
      | 1            | 05.2022         | 0       |
      | 2            | 05.2022         | 0       |
      | 3            | 02.2022         | 0       |
      | 4            | 07.2022         | 167000  |
      | 5            | 09.2022         | 167000  |
      | 6            | 10.2022         | 583000  |


    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId |
      | 3            | 02.2022         | 04.2022         |              |                       |
      | 1            | 05.2022         | 04.2025         | FJERNET      | 2                     |
      | 2            | 05.2022         | 04.2025         | FJERNET      | 3                     |
      | 3            | 05.2022         | 06.2022         | SPLITTET     | 4                     |
      | 3            | 07.2022         | 01.2025         | FJERNET      | 4                     |
      | 4            | 07.2022         | 08.2022         | SPLITTET     | 5                     |
      | 4            | 09.2022         | 04.2025         | FJERNET      | 5                     |
      | 5            | 09.2022         | 09.2022         | SPLITTET     | 6                     |
      | 5            | 10.2022         | 01.2025         | FJERNET      | 6                     |
      | 6            | 10.2022         | 01.2025         |              |                       |

#      | 3            | 02.2022         | 04.2022         |              |                       |
#      | 1            | 05.2022         | 04.2025         | FJERNET      | 2                     |
#      | 2            | 05.2022         | 06.2022         | SPLITTET     | 4                     |
#      | 2            | 07.2022         | 01.2025         | FJERNET      | 4                     |
#      | 4            | 09.2022         | 04.2025         | FJERNET      | 5                     |
#      | 5            | 09.2022         | 09.2022         | SPLITTET     | 6                     |
#      | 5            | 10.2022         | 01.2025         | FJERNET      | 6                     |
#      | 6            | 10.2022         | 01.2025         |              |                       |
#      | 2            | 02.2025         | 04.2025         | FJERNET      | 3                     |
#      | 4            | 07.2022         | 08.2022         | SPLITTET     | 5                     |
