-- Seed initial roles into the roles table
-- Using ON CONFLICT DO NOTHING makes the script safe to re-run if roles already exist.

INSERT INTO roles (role_name) VALUES ('ROLE_USER') ON CONFLICT (role_name) DO NOTHING;
INSERT INTO roles (role_name) VALUES ('ROLE_TEAM_MANAGER') ON CONFLICT (role_name) DO NOTHING;
INSERT INTO roles (role_name) VALUES ('ROLE_ADMINISTRATOR') ON CONFLICT (role_name) DO NOTHING;
