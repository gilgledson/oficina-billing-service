-- ID, no Mercado Pago, do pagamento que quitou o orçamento. Serve para
-- distinguir a notificação repetida do mesmo pagamento de um segundo pagamento.
ALTER TABLE orcamento ADD COLUMN pagamento_id VARCHAR(100);
