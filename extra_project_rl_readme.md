# Reinforcement Project Git Notes

This file documents how the extra reinforcement learning project should live inside `smartairbase` without polluting the main Git history.

## What should be committed

These files are normally safe to keep in Git:

- `reinforcement_project/*.py`
- `reinforcement_project/*.yml`
- `reinforcement_project/requirements.txt`
- `reinforcement_project/environment.yml`
- `reinforcement_project/README.md`
- `reinforcement_project/game.ipynb` if the notebook is part of the project deliverable
- `reinforcement_project/generated_missions_100.json` only if this is a canonical input dataset we want everyone to use

## What should not be committed

These are generated or machine-specific and should stay out of the repo:

- `reinforcement_project/.venv/`
- `reinforcement_project/__pycache__/`
- `reinforcement_project/.fleet_web_sessions/`
- `reinforcement_project/logs/`
- `reinforcement_project/artifacts/`
- `reinforcement_project/models/` by default
- `reinforcement_project/frontend/.next/`
- `reinforcement_project/frontend/node_modules/`
- `events.out.tfevents.*`

Most of the files currently showing up in `git status` fall into this ignore list:

- `.fleet_web_sessions/*.json`
- `__pycache__/*.pyc`
- `logs/.../events.out.tfevents.*`
- `models/.../evaluations.npz`

## When to use Git LFS

Do not use LFS for normal source files. Use it only for large binary artifacts that we intentionally want to version, such as:

- trained model checkpoints like `.pt`, `.pth`, `.ckpt`, `.onnx`
- binary evaluation bundles like `.npz`

Recommended rule:

1. Ignore training outputs by default.
2. Only keep a curated model artifact when there is a real reason to version it.
3. If we keep one, store it with Git LFS instead of regular Git.

The root `.gitattributes` now includes LFS rules for common RL artifact formats under `reinforcement_project/models/` and `reinforcement_project/artifacts/`.

If Git LFS is not set up yet:

```bash
git lfs install
git lfs pull
```

If you intentionally want to commit an ignored model artifact:

```bash
git add -f reinforcement_project/models/path/to/artifact.npz
```

## Cleanup for the current staged files

After updating `.gitignore`, unstage the generated files that should disappear from the commit:

```bash
git restore --staged reinforcement_project/.fleet_web_sessions
git restore --staged reinforcement_project/__pycache__
git restore --staged reinforcement_project/logs
git restore --staged reinforcement_project/models
```

Then check status again:

```bash
git status
```

At that point, only the real project files should remain staged.

## Important note about `reinforcement_project/frontend`

`reinforcement_project/frontend` currently contains its own `.git` directory, and the root repository is tracking it as a gitlink. That means it is acting like a nested Git repository instead of a normal folder.

Choose one approach and stick to it:

1. Keep it separate.
   Commit and manage the frontend in its own repo or as a real submodule.
2. Fold it into this repo.
   Only do this if you no longer need separate frontend Git history.

If you want option 2, do it carefully after preserving anything important from the nested repo. The safe sequence is:

```bash
git rm --cached reinforcement_project/frontend
rm -rf reinforcement_project/frontend/.git
git add reinforcement_project/frontend
```

Only run the `rm -rf reinforcement_project/frontend/.git` step if you are sure the nested repo history is no longer needed locally.

## Suggested commit scope

For the first clean commit of the RL project, prefer:

- Python source files
- config files
- environment files
- the RL README
- the frontend gitlink or the frontend folder, but not both approaches mixed together

Avoid committing training outputs, caches, logs, and machine-local session files.
