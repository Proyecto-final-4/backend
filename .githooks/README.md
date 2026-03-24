# Git Hooks

This project includes local Git hooks in `.githooks`.

## Included checks

- `commit-msg`: enforces Conventional Commit prefixes (`feat`, `fix`, `chore`, etc.).
- `pre-commit`: runs formatter/linter checks (`spotless:check` and `checkstyle:check`) and blocks commits that appear to include an OpenAI API key.

## Enable hooks

Run this once in the repository:

```bash
git config core.hooksPath .githooks
```

On Unix-like systems, ensure scripts are executable:

```bash
chmod +x .githooks/commit-msg .githooks/pre-commit
```
