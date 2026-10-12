-- Record the Stripe transfer id that paid out a transaction, so a refund can reverse it.
-- Previously the PaymentIntent id was stored on the payout in the transfer-id field and the
-- real Stripe transfer id was discarded, making transfer reversal impossible.
ALTER TABLE payment_transaction
    ADD COLUMN IF NOT EXISTS stripe_transfer_id VARCHAR(100);
