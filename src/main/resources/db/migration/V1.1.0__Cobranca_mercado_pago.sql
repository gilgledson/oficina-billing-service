-- Cobrança do orçamento no Mercado Pago (Checkout Pro): o ID da preferência de
-- pagamento e o link que o cliente usa para pagar.
ALTER TABLE orcamento ADD COLUMN pagamento_externo_id VARCHAR(100);
ALTER TABLE orcamento ADD COLUMN link_pagamento VARCHAR(500);

-- Meios de pagamento que o Mercado Pago aceita e o balcão não tinha.
ALTER TABLE orcamento DROP CONSTRAINT check_orcamento_metodo_pagamento;
ALTER TABLE orcamento ADD CONSTRAINT check_orcamento_metodo_pagamento
    CHECK (metodo_pagamento IN ('DINHEIRO', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'PIX', 'BOLETO', 'SALDO_EM_CONTA'));
