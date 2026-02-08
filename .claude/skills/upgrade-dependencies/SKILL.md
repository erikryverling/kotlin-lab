---
name: upgrade-dependencies
description: Upgrades the project dependencies
---

- Analyze libs.versions.toml and gradle-wrapper.properties
- Upgrade all dependencies in libs.versions.toml and gradle-wrapper.properties
- Always prefer stable version of dependencies, if possible
- Fix any compilation errors
- Verify the changes by running ./gradlew test
- Summerize all changes in libs.versions.toml and gradle-wrapper.properties and let me approve them
- When approved create a commit for the changes named "Bump dependencies"
- Push the changes
