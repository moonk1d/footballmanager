# Branch Naming Convention Guide

Use descriptive branch names prefixed appropriately to indicate the type of work being done. This ensures clarity, consistency, and better collaboration across the team.

## Branch Types and Descriptions

### `feat/...` (Feature Branch)
- **Purpose**: Used for developing new features or functionalities.
- **Example**: `feat/team-roster-management`
- **Description**: Represents work on a new feature that adds value to the project, such as implementing a new module, API, or user-facing functionality.

### `fix/...` (Bug Fix Branch)
- **Purpose**: Used for fixing bugs or issues in the codebase.
- **Example**: `fix/incorrect-standings-calculation`
- **Description**: Represents work to resolve a defect or issue reported in the project, such as fixing incorrect calculations, broken functionality, or runtime errors.

### `chore/...` (Maintenance Branch)
- **Purpose**: Used for routine tasks or updates that do not affect the core functionality.
- **Example**: `chore/update-spring-boot`
- **Description**: Represents work on non-functional changes, such as updating dependencies, improving documentation, or performing code cleanup.

### `refactor/...` (Refactoring Branch)
- **Purpose**: Used for restructuring or improving existing code without changing its behavior.
- **Example**: `refactor/user-service-logic`
- **Description**: Represents work to improve code quality, readability, or maintainability, such as reorganizing logic, reducing complexity, or optimizing performance.

## Why Use These Naming Conventions?
- **Clarity**: Makes it easy to understand the purpose of a branch at a glance.
- **Consistency**: Ensures uniformity across the team, improving collaboration.
- **Automation**: Facilitates integration with CI/CD workflows and branch protection rules.