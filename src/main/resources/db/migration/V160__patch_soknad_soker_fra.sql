-- Patch soknadskjema: sett riktig soker_fra dato for behandlinger med status "opprettet" etter mappingfeil i prod
UPDATE soknadsskjema SET soker_fra = '2025-02-01' WHERE id = '2d261a3f-4075-4826-b838-4ed57191cef8' AND soker_fra = '2025-03-01';
UPDATE soknadsskjema SET soker_fra = '2026-01-01' WHERE id = '4aeb8325-68b7-4820-aabe-4aa3397ac287' AND soker_fra = '2026-02-01';
UPDATE soknadsskjema SET soker_fra = '2025-02-01' WHERE id = 'dfeba27e-3068-4c31-9fbd-1a41f46a0afb' AND soker_fra = '2025-03-01';
UPDATE soknadsskjema SET soker_fra = '2026-07-01' WHERE id = '25fffb6f-4b88-4038-99ef-6094742a0e34' AND soker_fra = '2026-08-01';
UPDATE soknadsskjema SET soker_fra = '2026-07-01' WHERE id = '883ec603-3974-4beb-9612-e7a144ac8297' AND soker_fra = '2026-08-01';
UPDATE soknadsskjema SET soker_fra = '2026-02-01' WHERE id = '6a663289-32ba-4742-96a2-b61b92edad43' AND soker_fra = '2026-03-01';
UPDATE soknadsskjema SET soker_fra = '2026-03-01' WHERE id = '580be7eb-fdb4-40c8-8181-1891305c1549' AND soker_fra = '2026-04-01';
UPDATE soknadsskjema SET soker_fra = '2026-05-01' WHERE id = 'a2e5c29b-1678-4913-9ea4-b95ec9efd4d8' AND soker_fra = '2026-06-01';
UPDATE soknadsskjema SET soker_fra = '2025-10-01' WHERE id = '84786177-ed94-45d9-ad34-38e52237dc3b' AND soker_fra = '2025-11-01';
UPDATE soknadsskjema SET soker_fra = '2025-10-01' WHERE id = '5cdac62e-58b4-412b-93ef-8374735e1229' AND soker_fra = '2025-11-01';
UPDATE soknadsskjema SET soker_fra = '2025-06-01' WHERE id = '305b7d96-571c-4911-8a8f-2e1ead700ce3' AND soker_fra = '2025-07-01';
UPDATE soknadsskjema SET soker_fra = '2026-02-01' WHERE id = '00931dcf-eb2b-4120-9345-65b5b0b8ccfa' AND soker_fra = '2026-03-01';
UPDATE soknadsskjema SET soker_fra = '2026-02-01' WHERE id = 'e342afec-2277-409a-97c0-c5a1fa5f9cd4' AND soker_fra = '2026-03-01';
UPDATE soknadsskjema SET soker_fra = '2026-04-01' WHERE id = '87b4d70d-3fc6-42b4-bd71-a263aac6bf01' AND soker_fra = '2026-05-01';
UPDATE soknadsskjema SET soker_fra = '2026-03-01' WHERE id = '57b97c66-db42-4505-89d2-eb055d3b7c31' AND soker_fra = '2026-04-01';