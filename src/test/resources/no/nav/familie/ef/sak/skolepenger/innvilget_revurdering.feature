# language: no
# encoding: UTF-8

Egenskap: Skolepenger med revurdering

  Scenario: Revurdert med ett nytt beløp i den samme måneden som tidligere

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Id utgift | Dato faktura | Utgifter | Beløp |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 1         | 08.2021      | 200      | 200   |
      | 2            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 1         | 08.2021      | 200      | 200   |
      | 2            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 2         | 08.2021      | 300      | 300   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 08.2021         | 08.2021         | 200   | 1               |

    Så forvent følgende andeler lagret for behandling med id: 2
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 08.2021         | 08.2021         | 500   | 2               |

  Scenario: Revurdert med ett nytt beløp i en annen måned

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Id utgift | Dato faktura | Utgifter | Beløp |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 1         | 08.2021      | 200      | 200   |
      | 2            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 1         | 08.2021      | 200      | 200   |
      | 2            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 2         | 10.2021      | 300      | 300   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 08.2021         | 08.2021         | 200   | 1               |

    Så forvent følgende andeler lagret for behandling med id: 2
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 08.2021         | 08.2021         | 200   | 2               |
      | 10.2021         | 10.2021         | 300   | 2               |

