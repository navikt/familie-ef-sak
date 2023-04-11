ALTER TABLE behandling ADD COLUMN kategori varchar default 'NASJONAL';
UPDATE behandling SET kategori = 'EØS' WHERE id in ('e0904a7e-e2bf-4b0c-b678-fa8b85bb2400',
                                                    'c1b74d8e-a40a-4ac0-b949-af5cd0813420',
                                                    '4418f169-ee1e-4287-a0b6-2ce6314504ee',
                                                    '8c890738-46d1-4189-8920-809cd8a51b18',
                                                    'd93c2286-b29e-4754-87b0-0aa07dd47029',
                                                    '70ff19d9-df3e-41ea-b7d6-4ec3c737d1b9',
                                                    '1c372c08-2e9e-42d3-82de-6f527a030916')
