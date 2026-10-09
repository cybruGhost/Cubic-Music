"""Conservative Dependabot PR maintenance.

Only patch updates to a small allowlist may be merged. Never checkout PR code,
run PR-supplied scripts, handle secrets in output, or force-fix conflicts.
Checks must have succeeded on the exact PR head SHA.
"""
import json
import os
import re
import sys
import urllib.error
import urllib.request

repo = os.environ["REPOSITORY"]
token = os.environ["GH_TOKEN"]
dry = os.environ.get("DRY_RUN", "false").lower() == "true"
base = f"https://api.github.com/repos/{repo}"
headers = {
    "Authorization": f"Bearer {token}",
    "Accept": "application/vnd.github+json",
    "X-GitHub-Api-Version": "2022-11-28",
    "User-Agent": "cubic-music-dependabot-review",
}
# A tiny, explicitly reviewed allowlist. No build toolchain, networking,
# playback, native code or Kotlin/Compose/Gradle plugin changes.
SAFE_LIBRARIES = {
    "androidx.constraintlayout:constraintlayout-compose",
    "com.google.code.gson:gson",
    "org.jetbrains:annotations",
}
EXPECTED_CHECKS = {
    "Catalog and compatibility",
    "Android debug build (Java 21)",
}

def api(path, method="GET", data=None):
    payload = None if data is None else json.dumps(data).encode()
    request = urllib.request.Request(base + path, data=payload, method=method, headers=headers)
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            return json.load(response)
    except urllib.error.HTTPError as exc:
        print(f"GitHub API {method} {path} returned HTTP {exc.code}", file=sys.stderr)
        return None

def patch_candidate(title):
    match = re.fullmatch(
        r"build\(deps\): bump (.+) from (\d+)\.(\d+)\.(\d+) to (\d+)\.(\d+)\.(\d+)",
        title,
    )
    if not match:
        return False
    name, *nums = match.groups()
    old = tuple(map(int, nums[:3]))
    new = tuple(map(int, nums[3:]))
    return name in SAFE_LIBRARIES and old[:2] == new[:2] and new[2] > old[2]

def successful_checks(sha):
    result = api(f"/commits/{sha}/check-runs?per_page=100")
    if not result or result.get("total_count", 101) > 100:
        return False
    checks = result.get("check_runs", [])
    latest = {}
    for check in checks:
        name = check.get("name")
        if name and (name not in latest or check.get("id", 0) > latest[name].get("id", 0)):
            latest[name] = check
    if not EXPECTED_CHECKS <= latest.keys():
        return False
    if any(check.get("status") != "completed" or check.get("conclusion") not in ("success", "neutral", "skipped") for check in latest.values()):
        return False
    return all(latest[name].get("conclusion") == "success" for name in EXPECTED_CHECKS)

def main():
    print("Safe Dependabot merge policy. Dry run:", dry)
    pulls = api("/pulls?state=open&per_page=100")
    if not isinstance(pulls, list):
        raise SystemExit("Could not retrieve PRs")
    for item in pulls:
        number = item["number"]
        title = item.get("title", "")
        author = item.get("user", {}).get("login", "")
        head = item.get("head", {})
        if author != "dependabot[bot]" or not patch_candidate(title):
            continue
        if head.get("repo", {}).get("full_name") != repo or item.get("base", {}).get("ref") != "main":
            continue
        if item.get("draft") or head.get("ref", "").startswith("dependabot/") is False:
            continue
        detail = api(f"/pulls/{number}")
        if not detail:
            continue
        if detail.get("mergeable") is not True or detail.get("mergeable_state") != "clean":
            print(f"PR #{number}: not clean/mergeable; manual review required")
            continue
        changed = api(f"/pulls/{number}/files?per_page=100")
        if not isinstance(changed, list) or len(changed) != 1:
            print(f"PR #{number}: multiple files or unavailable diff; manual review")
            continue
        file = changed[0]
        if (file.get("filename") != "gradle/libs.versions.toml"
            or file.get("status") != "modified"
            or file.get("deletions") != 1 or file.get("additions") != 1):
            print(f"PR #{number}: unexpected file change; manual review")
            continue
        if not successful_checks(head["sha"]):
            print(f"PR #{number}: full required checks not yet green for head; leaving open")
            continue
        if dry:
            print(f"DRY RUN: eligible PR #{number}: {title}")
            continue
        outcome = api(f"/pulls/{number}/merge", "PUT", {
            "sha": head["sha"], "merge_method": "squash",
            "commit_title": title,
        })
        if outcome and outcome.get("merged"):
            print(f"Merged PR #{number}: {title}")
        else:
            print(f"PR #{number}: merge not performed; left for review")

if __name__ == "__main__":
    main()
