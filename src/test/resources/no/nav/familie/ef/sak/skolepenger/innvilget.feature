# language: no
# encoding: UTF-8

Egenskap: Skolepenger

  Scenario: Innvilget skolepenger

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Dato faktura | Utgifter | Beløp |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 01.2021         | 03.2021         | 100              | 01.2021      | 200      | 200   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 01.2021         | 01.2021         | 200   | 1               |


  Scenario: Innvilget skolepenger, 2 beløp i det samme skoleåret

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Id utgift | Dato faktura | Utgifter | Beløp |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 12.2021         | 100              | 1         | 08.2021      | 200      | 200   |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 01.2022         | 06.2022         | 100              | 2         | 01.2022      | 300      | 300   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 08.2021         | 08.2021         | 200   | 1               |
      | 01.2022         | 01.2022         | 300   | 1               |

  Scenario: Innvilget skolepenger, 2 beløp i samme måned summereres til en rad i andeler

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Id utgift | Dato faktura | Utgifter | Beløp |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 08.2021         | 100              | 1         | 08.2021      | 200      | 200   |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 09.2021         | 06.2022         | 100              | 2         | 08.2021      | 300      | 300   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 08.2021         | 08.2021         | 500   | 1               |

  Scenario: Innvilget skolepenger, 2 ulike skoleår som betales ut samme dato

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Id utgift | Dato faktura | Utgifter | Beløp |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 1         | 08.2021      | 200      | 200   |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2022         | 06.2023         | 100              | 2         | 08.2021      | 300      | 300   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 08.2021         | 08.2021         | 500   | 1               |

  Scenario: 0-beløp skal håndteres

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Id utgift | Dato faktura | Utgifter | Beløp |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 1         | 08.2021      | 200      | 200   |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2022         | 06.2023         | 100              | 2         | 08.2021      | 300      | 0     |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 08.2021         | 08.2021         | 200   | 1               |

  Scenario: 0-beløp i en annen måned skal ikke generere en andel

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Id utgift | Dato faktura | Utgifter | Beløp |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 1         | 08.2021      | 200      | 200   |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2022         | 06.2023         | 100              | 2         | 10.2021      | 300      | 0     |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 08.2021         | 08.2021         | 200   | 1               |

