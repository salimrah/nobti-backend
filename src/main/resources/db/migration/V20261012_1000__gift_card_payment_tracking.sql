-- Track the payment transaction that funds each gift card.
--
-- Gift cards may only be issued when backed by a SUCCEEDED payment transaction
-- (enforced in GiftCardApplicationService). This closes the free-money path where a
-- gift card could be minted from a client-supplied amount with no payment.
--
-- The column is nullable so previously issued cards remain valid; the partial unique
-- index guarantees a single payment cannot be reused to mint more than one gift card.
ALTER TABLE gift_card
    ADD COLUMN IF NOT EXISTS payment_transaction_id UUID;

ALTER TABLE gift_card
    ADD CONSTRAINT fk_gift_card_payment_transaction
        FOREIGN KEY (payment_transaction_id) REFERENCES payment_transaction(id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_gift_card_payment_transaction
    ON gift_card(payment_transaction_id)
    WHERE payment_transaction_id IS NOT NULL;
