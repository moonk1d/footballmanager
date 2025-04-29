-- Remove roles inserted in V1 that are no longer needed
DELETE FROM roles WHERE role_name = 'Administrator';
DELETE FROM roles WHERE role_name = 'Team Manager';