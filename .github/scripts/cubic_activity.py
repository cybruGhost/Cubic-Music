"""Record one transparent, source-related activity note per scheduled run.

Only reads filenames of tracked, allowlisted public source paths; never reads
file contents, configuration values, GitHub secrets, or backup repositories.
This is automated bookkeeping, not evidence of code changes or bug fixes.
"""
from __future__ import annotations

import os
import random
import re
import subprocess
import time
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

CAP_PER_DAY = 200
LOCAL_TZ = ZoneInfo("Africa/Nairobi")
LOG_DIR = Path("automation/engineering-activity")
SAFE_ROOTS = ("composeApp/src/", "extensions/", "modules/innertube/")
SAFE_SUFFIXES = {".kt", ".kts", ".xml"}
EXCLUDED_PARTS = ("secret", "credential", "private", "token", "password", "keystore", "authkey")

TOPICS = (
    (("lyric", "lrclib", "karaoke"), "lyrics"),
    (("crossfade", "player", "playback"), "playback"),
    (("innertube", "invidious", "piped", "stream"), "streaming"),
    (("download", "offline"), "offline playback"),
    (("playlist", "queue"), "playlists and queue"),
    (("audio", "quality", "bitrate"), "audio quality"),
    (("search", "suggestion"), "search and discovery"),
    (("setting", "preference"), "settings"),
    (("screen", "theme", "ui", "navigation"), "interface"),
    (("test",), "testing"),
)

def eligible(path: str) -> bool:
    item = Path(path)
    return (
        path.startswith(SAFE_ROOTS)
        and item.suffix.lower() in SAFE_SUFFIXES
        and all(part and not part.startswith(".") for part in item.parts)
        and not any(blocked in path.lower() for blocked in EXCLUDED_PARTS)
        and bool(re.fullmatch(r"[A-Za-z0-9_./-]+", path))
    )

def classify(path: str) -> str:
    name = path.lower()
    for terms, label in TOPICS:
        if any(term in name for term in terms):
            return label
    return "Android codebase"

def output(name: str, value: str) -> None:
    destination = os.getenv("GITHUB_OUTPUT")
    if destination:
        with open(destination, "a", encoding="utf-8") as handle:
            handle.write(f"{name}={value}\n")

def main() -> None:
    preview = os.getenv("PREVIEW_ONLY", "false").lower() == "true"
    now = datetime.now(LOCAL_TZ)
    day = now.strftime("%Y-%m-%d")
    note_path = LOG_DIR / f"{day}.md"

    existing = note_path.read_text(encoding="utf-8") if note_path.exists() else ""
    count = sum(row.startswith("- ") for row in existing.splitlines())
    output("commit_ready", "false")
    print(f"Cubic Music activity: {day} EAT, {count}/{CAP_PER_DAY} recorded")
    if count >= CAP_PER_DAY:
        print("Daily cap reached; no commit created.")
        return

    filenames = subprocess.check_output(["git", "ls-files", "-z", "--", *SAFE_ROOTS])
    candidates = [path for path in filenames.decode("utf-8").split("\0") if eligible(path)]
    if not candidates:
        raise RuntimeError("No allowlisted source references found; refusing to make an empty commit")

    # Avoid reusing the same source reference until all candidates were recorded.
    used = set(re.findall(r"reference `([^`]+)`", existing))
    options = [path for path in candidates if path not in used] or candidates
    selected = random.SystemRandom().choice(options)
    topic = classify(selected)
    subjects = (
        f"chore(auto): index {topic} source reference",
        f"docs(auto): catalogue {topic} codebase reference",
        f"chore(auto): record {topic} source inventory",
        f"docs(auto): track {topic} repository reference",
    )
    subject = random.SystemRandom().choice(subjects)

    if preview:
        print(f"PREVIEW ONLY — would record one {topic} source reference. No changes.")
        return

    # Add a little timing variation without creating burst commits.
    time.sleep(random.SystemRandom().randint(0, 25))
    timestamp = datetime.now(LOCAL_TZ).strftime("%Y-%m-%d %H:%M:%S %Z")
    LOG_DIR.mkdir(parents=True, exist_ok=True)
    if not note_path.exists():
        note_path.write_text(
            f"# Cubic Music automated source inventory — {day} (Africa/Nairobi)\n\n"
            "Automated reference notes only. They do not represent fixes, releases, or source edits.\n\n",
            encoding="utf-8",
        )
    with note_path.open("a", encoding="utf-8") as handle:
        handle.write(
            f"- {timestamp} | {topic} | reference `{selected}` | "
            "automated inventory, no source changes\n"
        )
    output("subject", subject)
    output("commit_ready", "true")
    print(f"Prepared entry {count + 1}/{CAP_PER_DAY}: {subject}")

if __name__ == "__main__":
    main()
