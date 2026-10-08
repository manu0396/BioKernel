---
name: engram-progress
description: Keep project progress in Engram persistent memory. Use at session start, after every meaningful step (decision, fix, milestone, blocker), and before ending the session. Triggers on "save progress", "log this to engram", "remember this", "update project memory", or any multi-step task in this repo.
---

# Engram Progress

This repo uses Engram (`.engram/` + `engram` CLI + `engram` MCP server) to keep a
durable, git-shared log of project progress. Follow this workflow so any agent or
teammate can pick up where the last session left off.

## Session start

1. Run `engram sync --import` (CLI) to load committed `.engram/chunks/` into the
   local DB — `mem_context` only reads the local DB, so pulled chunks are
   invisible until imported.
2. Call `mem_context` (MCP) filtered to this project to recall prior decisions,
   open blockers, and recent work before planning.

## During work — save progress proactively

Call `mem_save` immediately after any of these (do not wait to be asked):

- **decision** — architecture/tech choices, scope changes, rejected alternatives
- **bug** — root causes found, fixes applied, regressions
- **discovery** — non-obvious behavior, conventions, gotchas in the codebase
- **task** — completed milestones, phase transitions, pending follow-ups
- **blocker** — anything requiring the user or an external dependency

Write memories so they are useful standalone: what changed, where (file/module
names, repo-relative paths — never absolute paths), and why. Keep each memory to
one fact or decision; prefer several small saves over one big dump.

Suggested anchors for this project: release phase (see `PLANS.md`/`VERSION`),
module touched (`apps/`, `services/`, `agents/`, `shared/`, `ops/`), and
verification results (`./gradlew test`, detekt/ktlint gates).

## Conflict review

If `mem_save` returns `judgment_required: true`, iterate `candidates[]` and call
`mem_judge` once per entry using that entry's own `judgment_id`. Ask the user
when confidence < 0.7 or the relation is `supersedes`/`conflicts_with` on an
architecture/policy/decision memory; otherwise resolve with `related`,
`compatible`, `scoped`, or `not_conflict`.

## Session end — before saying "done"

1. Call `mem_session_summary` with: goal, what was completed, what remains,
   blockers, and next steps.
2. Run `engram sync` to export new memories into `.engram/chunks/`.
3. Commit `.engram/` with the code changes (or a `chore:` commit) so teammates
   receive the context via `git pull`.

## Rules

- Do not store secrets, tokens, or credentials in memories.
- Use repo-relative paths; absolute paths break portability across machines.
- Memory bookkeeping is internal — it must never replace the user-facing answer
  or be used as a substitute for tests, commits, or docs.
