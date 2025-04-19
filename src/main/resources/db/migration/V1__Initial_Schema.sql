-- PostgreSQL Schema for Football Tournament Management V1

-- Drop existing types and tables in reverse order of creation (optional, for clean slate)
DROP INDEX IF EXISTS idx_match_stats_match_id;
DROP INDEX IF EXISTS idx_match_stats_player_id;
DROP INDEX IF EXISTS idx_match_stats_team_id;
DROP TABLE IF EXISTS match_stats;

DROP INDEX IF EXISTS idx_matches_league_id;
DROP INDEX IF EXISTS idx_matches_home_team_id;
DROP INDEX IF EXISTS idx_matches_away_team_id;
DROP TABLE IF EXISTS matches;

DROP INDEX IF EXISTS idx_tlt_league_id;
DROP INDEX IF EXISTS idx_tlt_team_id;
DROP TABLE IF EXISTS tournament_league_teams;

DROP INDEX IF EXISTS idx_leagues_tournament_id;
DROP TABLE IF EXISTS leagues;

DROP TABLE IF EXISTS tournaments;

DROP INDEX IF EXISTS idx_join_requests_user_id;
DROP INDEX IF EXISTS idx_join_requests_team_id;
DROP TABLE IF EXISTS join_requests;

DROP INDEX IF EXISTS idx_player_team_assignments_user_id;
DROP INDEX IF EXISTS idx_player_team_assignments_team_id;
DROP TABLE IF EXISTS player_team_assignments;

DROP INDEX IF EXISTS idx_team_managers_user_id;
DROP INDEX IF EXISTS idx_team_managers_team_id;
DROP TABLE IF EXISTS team_managers;

DROP INDEX IF EXISTS idx_teams_name;
DROP TABLE IF EXISTS teams;

DROP INDEX IF EXISTS idx_user_roles_user_id;
DROP INDEX IF EXISTS idx_user_roles_role_id;
DROP TABLE IF EXISTS user_roles;

DROP TABLE IF EXISTS "roles";

DROP INDEX IF EXISTS idx_users_email;
DROP TABLE IF EXISTS users;

DROP TYPE IF EXISTS match_stat_type;
DROP TYPE IF EXISTS promotion_relegation_status;
DROP TYPE IF EXISTS match_status;
DROP TYPE IF EXISTS tournament_status;
DROP TYPE IF EXISTS join_request_status;

-- Define ENUM Types (Custom data types for specific fields)
CREATE TYPE join_request_status AS ENUM ('pending', 'approved', 'rejected', 'cancelled');
CREATE TYPE tournament_status AS ENUM ('Setup', 'Active', 'Completed');
CREATE TYPE match_status AS ENUM ('Scheduled', 'Completed', 'Postponed', 'Cancelled');
CREATE TYPE promotion_relegation_status AS ENUM ('Promoted', 'Relegated', 'Stay');
CREATE TYPE match_stat_type AS ENUM ('Goal', 'Assist', 'Yellow Card', 'Red Card');

-- Create Tables

CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    date_of_birth DATE NULL,
    playing_position VARCHAR(100) NULL,
    profile_picture_url VARCHAR(512) NULL,
    contact_number VARCHAR(50) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP -- Consider using a trigger to auto-update this
);
CREATE INDEX idx_users_email ON users(email);

CREATE TABLE "roles" (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) UNIQUE NOT NULL -- e.g., 'Administrator', 'Team Manager'
);

CREATE TABLE user_roles (
    user_role_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES "roles"(role_id) ON DELETE RESTRICT,
    UNIQUE (user_id, role_id) -- A user has a specific role only once
);
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

CREATE TABLE teams (
    team_id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    logo_url VARCHAR(512) NULL,
    description TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP -- Consider using a trigger
);
CREATE INDEX idx_teams_name ON teams(name);

CREATE TABLE team_managers (
    team_manager_id SERIAL PRIMARY KEY,
    user_id INTEGER UNIQUE NOT NULL REFERENCES users(user_id) ON DELETE CASCADE, -- User manages only one team
    team_id INTEGER UNIQUE NOT NULL REFERENCES teams(team_id) ON DELETE CASCADE, -- Team has only one manager
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_team_managers_user_id ON team_managers(user_id);
CREATE INDEX idx_team_managers_team_id ON team_managers(team_id);

CREATE TABLE player_team_assignments (
    assignment_id SERIAL PRIMARY KEY,
    user_id INTEGER UNIQUE NOT NULL REFERENCES users(user_id) ON DELETE CASCADE, -- Player assigned to max one team
    team_id INTEGER NOT NULL REFERENCES teams(team_id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_player_team_assignments_user_id ON player_team_assignments(user_id);
CREATE INDEX idx_player_team_assignments_team_id ON player_team_assignments(team_id);

CREATE TABLE join_requests (
    request_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    team_id INTEGER NOT NULL REFERENCES teams(team_id) ON DELETE CASCADE,
    status join_request_status NOT NULL DEFAULT 'pending',
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMPTZ NULL,
    UNIQUE (user_id, team_id, status) -- Optional: Prevent duplicate *pending* requests? Consider implications.
);
CREATE INDEX idx_join_requests_user_id ON join_requests(user_id);
CREATE INDEX idx_join_requests_team_id ON join_requests(team_id);

CREATE TABLE tournaments (
    tournament_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    year_period VARCHAR(100) NOT NULL, -- e.g., '2024 Summer', '2025'
    status tournament_status NOT NULL DEFAULT 'Setup',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP -- Consider using a trigger
);

CREATE TABLE leagues (
    league_id SERIAL PRIMARY KEY,
    tournament_id INTEGER NOT NULL REFERENCES tournaments(tournament_id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL, -- e.g., 'Premier League', 'Division 1'
    level INTEGER NULL, -- Optional: For ordering leagues, e.g., 1 is highest
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tournament_id, name) -- League names should be unique within a tournament
);
CREATE INDEX idx_leagues_tournament_id ON leagues(tournament_id);

CREATE TABLE tournament_league_teams (
    tlt_id SERIAL PRIMARY KEY,
    league_id INTEGER NOT NULL REFERENCES leagues(league_id) ON DELETE CASCADE,
    team_id INTEGER NOT NULL REFERENCES teams(team_id) ON DELETE RESTRICT, -- Prevent deleting team if participating
    final_rank INTEGER NULL, -- Filled when league completes
    promotion_status promotion_relegation_status NULL, -- Filled when league completes
    UNIQUE (league_id, team_id) -- Team can only be in one league per tournament
);
CREATE INDEX idx_tlt_league_id ON tournament_league_teams(league_id);
CREATE INDEX idx_tlt_team_id ON tournament_league_teams(team_id);

CREATE TABLE matches (
    match_id SERIAL PRIMARY KEY,
    league_id INTEGER NOT NULL REFERENCES leagues(league_id) ON DELETE CASCADE,
    home_team_id INTEGER NOT NULL REFERENCES teams(team_id) ON DELETE RESTRICT,
    away_team_id INTEGER NOT NULL REFERENCES teams(team_id) ON DELETE RESTRICT,
    match_date_time TIMESTAMPTZ NOT NULL,
    location VARCHAR(255) NULL,
    status match_status NOT NULL DEFAULT 'Scheduled',
    home_score INTEGER NULL CHECK (home_score >= 0),
    away_score INTEGER NULL CHECK (away_score >= 0),
    recorded_by_admin_id INTEGER NULL REFERENCES users(user_id) ON DELETE SET NULL, -- Admin who recorded result
    recorded_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP -- Consider using a trigger
);
CREATE INDEX idx_matches_league_id ON matches(league_id);
CREATE INDEX idx_matches_home_team_id ON matches(home_team_id);
CREATE INDEX idx_matches_away_team_id ON matches(away_team_id);

CREATE TABLE match_stats (
    stat_id SERIAL PRIMARY KEY,
    match_id INTEGER NOT NULL REFERENCES matches(match_id) ON DELETE CASCADE,
    player_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT, -- Player involved
    team_id INTEGER NOT NULL REFERENCES teams(team_id) ON DELETE RESTRICT, -- Team player belonged to in this match
    stat_type match_stat_type NOT NULL, -- 'Goal', 'Assist', 'Yellow Card', 'Red Card'
    minute_of_event INTEGER NULL, -- Optional: Time the event occurred
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_match_stats_match_id ON match_stats(match_id);
CREATE INDEX idx_match_stats_player_id ON match_stats(player_id);
CREATE INDEX idx_match_stats_team_id ON match_stats(team_id);


-- Optional: Add basic roles if needed
INSERT INTO "roles" (role_name) VALUES ('Administrator'), ('Team Manager') ON CONFLICT (role_name) DO NOTHING;


-- Note on updated_at:
-- For automatically updating the `updated_at` columns, you would typically create a trigger function in PostgreSQL.
-- Example Trigger Function (Apply to relevant tables like users, teams, tournaments, matches):
/*
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_timestamp
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

-- Repeat CREATE TRIGGER for teams, tournaments, matches etc.
*/

-- End of Schema Script