-- One payment transaction per Stripe PaymentIntent. Prevents duplicate payment rows for the same
-- intent (e.g. under webhook retries/races). Partial index so the many NULL rows are excluded.
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_stripe_pi
    ON payment_transaction(stripe_payment_intent_id)
    WHERE stripe_payment_intent_id IS NOT NULL;
