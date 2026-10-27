-- Create role reference table and normalize user_role.role_type → user_role.role_id FK.
-- Also backfill USER role for existing users without one.

CREATE TABLE IF NOT EXISTS role (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

INSERT INTO role (id, name, description) VALUES
    (gen_random_uuid(), 'USER',      'Regular customer'),
    (gen_random_uuid(), 'PROVIDER',  'Service provider'),
    (gen_random_uuid(), 'DELIVERER', 'Delivery personnel'),
    (gen_random_uuid(), 'ADMIN',     'System administrator');

ALTER TABLE user_role ADD COLUMN IF NOT EXISTS role_id UUID;

UPDATE user_role ur
SET role_id = r.id
FROM role r
WHERE r.name = ur.role_type;

ALTER TABLE user_role ALTER COLUMN role_id SET NOT NULL;
ALTER TABLE user_role ADD CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role(id);
ALTER TABLE user_role DROP COLUMN IF EXISTS role_type;

CREATE INDEX IF NOT EXISTS idx_user_role_role ON user_role(role_id);

-- Backfill USER role for existing users who don't have one
INSERT INTO user_role (id, user_id, role_id, active, activated_at)
SELECT gen_random_uuid(), u.id, r.id, TRUE, NOW()
FROM users u
CROSS JOIN (SELECT id FROM role WHERE name = 'USER') r
WHERE NOT EXISTS (
    SELECT 1 FROM user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);
