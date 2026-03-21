#!/usr/bin/env python3
"""
Sweep Self-Improving Agent
==========================
Uses `claude --print` (Claude Code CLI) — no extra API key needed.
Runs under your existing Claude Code authentication.

Phase 1 (--phase tests):   Writes JUnit 5 tests for WebSocket/matchmaking code.
Phase 2 (--phase improve): Iteratively fixes issues, validates, writes tests, proposes commits.

Usage:
    python agent/agent.py                       # both phases
    python agent/agent.py --phase tests         # generate tests only
    python agent/agent.py --phase improve       # improve code only
    python agent/agent.py --iterations 2        # limit iterations per phase
"""

import os
import re
import sys
import subprocess
import argparse
import logging
from datetime import datetime
from pathlib import Path
import platform

IS_WINDOWS = platform.system() == "Windows"

# ── Paths ─────────────────────────────────────────────────────────────────────

BASE_DIR   = Path(__file__).parent.parent   # D:\Sweep
SERVER_DIR = BASE_DIR / "Sweep-Server"
CLIENT_DIR = BASE_DIR / "Sweep-Base"
LOG_DIR    = Path(__file__).parent / "logs"
LOG_DIR.mkdir(exist_ok=True)

DEFAULT_MAX_ITER = 5

# ── Logging ───────────────────────────────────────────────────────────────────

_ts = datetime.now().strftime("%Y%m%d_%H%M%S")
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.FileHandler(LOG_DIR / f"agent_{_ts}.log", encoding="utf-8"),
        logging.StreamHandler(sys.stdout),
    ],
)
log = logging.getLogger("sweep-agent")

# ── File block protocol ────────────────────────────────────────────────────────
#
#  The agent outputs file changes using this format:
#
#    <<<FILE:relative/path/to/File.java>>>
#    <full file content>
#    <<<END_FILE>>>
#
#  The script parses these blocks and writes the files.

FILE_BLOCK_RE = re.compile(r'<<<FILE:(.+?)>>>\n(.*?)<<<END_FILE>>>', re.DOTALL)


def parse_file_blocks(text: str) -> list[tuple[str, str]]:
    return [(m.group(1).strip(), m.group(2)) for m in FILE_BLOCK_RE.finditer(text)]


# ── Claude CLI call ───────────────────────────────────────────────────────────

def call_claude(prompt: str) -> str:
    """
    Call `claude --print <prompt>` directly — no bash wrapper needed.
    claude is in the system PATH (verified at startup).
    """
    result = subprocess.run(
        ["claude", "--print", prompt],
        capture_output=True,
        text=True,
        timeout=300,
        cwd=str(BASE_DIR),
    )
    if result.returncode != 0:
        err = (result.stderr or result.stdout)[:1000]
        return f"[CLAUDE_ERROR] {err}"
    return result.stdout


# ── Helpers ───────────────────────────────────────────────────────────────────

def read_file(path: str) -> str:
    p = BASE_DIR / path
    if not p.exists():
        return f"(file not found: {path})"
    try:
        return p.read_text(encoding="utf-8", errors="ignore")
    except Exception as e:
        return f"(error reading {path}: {e})"


def read_files_for_prompt(paths: list[str], max_chars_each: int = 6000) -> str:
    """Format multiple files for inclusion in a prompt."""
    parts = []
    for path in paths:
        content = read_file(path)
        if len(content) > max_chars_each:
            content = content[:max_chars_each] + f"\n... (truncated, {len(content)} total chars)"
        parts.append(f"=== {path} ===\n{content}")
    return "\n\n".join(parts)


def list_java_files(directory: str) -> str:
    p = BASE_DIR / directory
    if not p.exists():
        return "(directory not found)"
    files = sorted(
        str(f.relative_to(BASE_DIR)).replace("\\", "/")
        for f in p.rglob("*.java")
    )
    return "\n".join(files) if files else "(no .java files found)"


def apply_file_blocks(response: str) -> list[str]:
    """Parse file blocks from response and write them. Returns list of written paths."""
    blocks = parse_file_blocks(response)
    written = []
    for path, content in blocks:
        p = BASE_DIR / path
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(content, encoding="utf-8")
        log.info(f"Written: {path}")
        written.append(path)
    return written


def _gradlew(cwd: Path) -> str:
    """Return the right gradlew executable for the current OS."""
    return "gradlew.bat" if IS_WINDOWS else "./gradlew"


def run_gradle(module: str, task: str) -> tuple[bool, str]:
    """Run a gradle task. Returns (success, output_tail)."""
    if module == "server":
        cwd  = SERVER_DIR
        args = [_gradlew(SERVER_DIR), task]
    else:
        cwd  = CLIENT_DIR
        args = [_gradlew(CLIENT_DIR), f":game-logic:{task}"]
    log.info(f"Gradle [{module}]: {' '.join(args)}")
    result = subprocess.run(
        args,
        capture_output=True,
        text=True,
        timeout=240,
        cwd=str(cwd),
        shell=IS_WINDOWS,   # needed on Windows so .bat files resolve correctly
    )
    ok  = result.returncode == 0
    out = (result.stdout + result.stderr)[-4000:]
    return ok, out


def git_diff() -> str:
    result = subprocess.run(
        ["git", "diff", "HEAD"],
        capture_output=True, text=True, cwd=str(BASE_DIR),
    )
    return result.stdout[:8000] if result.stdout.strip() else "No changes"


def git_revert():
    subprocess.run(["git", "checkout", "."], cwd=str(BASE_DIR))
    subprocess.run(["git", "clean", "-fd"], cwd=str(BASE_DIR))
    log.warning("Reverted all uncommitted changes")


def git_commit(message: str):
    subprocess.run(["git", "add", "-A"], cwd=str(BASE_DIR))
    subprocess.run(["git", "commit", "-m", message], cwd=str(BASE_DIR))
    log.info(f"Committed: {message}")


def offer_commit(phase: str, iteration: int) -> bool:
    """Show diff and ask user to commit/revert/skip. Returns True if committed."""
    diff = git_diff()
    has_changes = diff.strip() not in ("", "No changes")

    if not has_changes:
        print("\n  No file changes to commit.")
        return False

    print(f"\n{'═' * 60}")
    print("PROPOSED CHANGES (git diff):")
    print("─" * 60)
    print(diff)
    print("═" * 60)

    while True:
        choice = input(
            "\n  [y] commit   [n] revert   [c] keep but skip commit: "
        ).strip().lower()
        if choice in ("y", "n", "c"):
            break

    if choice == "y":
        default = f"agent({phase}): iteration {iteration}"
        msg = input(f"  Commit message [{default}]: ").strip() or default
        git_commit(msg)
        print("  Committed.")
        return True
    elif choice == "n":
        git_revert()
        print("  Reverted.")
    return False


# ── System instructions (embedded in each prompt) ─────────────────────────────

FILE_FORMAT_INSTRUCTIONS = """\
When you want to create or modify a file, output it using EXACTLY this format
(no extra spaces around the tags):

<<<FILE:relative/path/from/project/root/File.java>>>
<full file content goes here>
<<<END_FILE>>>

Rules:
- Always output the FULL file content, not just the changed lines.
- Use forward slashes in paths (e.g. Sweep-Server/src/test/...).
- You may output multiple FILE blocks in one response.
- Outside FILE blocks, write your explanation freely.
"""

TEST_INSTRUCTIONS = f"""\
You are a senior Java developer writing tests for the SWEEP card game (a 3-player trick/card game).

Project layout (paths relative to project root D:/Sweep):
  Server (Spring Boot 3, Java 17):
    Source: Sweep-Server/src/main/java/com/sweepgame/server/
    Tests:  Sweep-Server/src/test/java/com/sweepgame/server/   ← write here

  Shared game logic (Java 17):
    Source: Sweep-Base/game-logic/src/main/java/com/sweepgame/game/
    Tests:  Sweep-Base/game-logic/src/test/java/com/sweepgame/game/  ← write here

Test frameworks:
  Server:     JUnit 5 + Mockito (both available via spring-boot-starter-test)
  game-logic: JUnit 5 only — if you need Mockito, add to Sweep-Base/game-logic/build.gradle:
                testImplementation 'org.mockito:mockito-core:5.3.1'

Test writing rules:
  1. Prefer @ExtendWith(MockitoExtension.class) unit tests over @SpringBootTest.
  2. One test class per source class, named <ClassName>Test.java.
  3. Cover: happy path + at least one edge case per meaningful method.
  4. If you discover a real bug, mark it: // BUG FOUND: <description>  (don't fix it here).
  5. Keep tests focused and fast — no Thread.sleep, no real network calls.

{FILE_FORMAT_INSTRUCTIONS}
"""

IMPROVE_INSTRUCTIONS = f"""\
You are a senior Java developer iteratively improving the SWEEP card game.
Focus: WebSocket communication and matchmaking (both are known to have issues).

Project layout (paths relative to project root D:/Sweep):
  Server (Spring Boot 3, Java 17): Sweep-Server/src/main/java/com/sweepgame/server/
  Client (LibGDX, Java 17):        Sweep-Base/core/src/main/java/com/sweepgame/
  Shared game logic:               Sweep-Base/game-logic/src/main/java/com/sweepgame/game/

Known issues (from git log):
  - Matchmaking is "better but not good"
  - WebSocket synchronization needs fixing
  - Multiplayer UI needs fixing

Your job for THIS iteration:
  1. Read the source files provided below.
  2. Identify ONE specific, concrete, fixable bug or issue.
  3. Output a minimal, focused fix (do not refactor unrelated code).
  4. After your fix files, write a brief explanation:
       WHAT WAS THE BUG: ...
       WHAT I CHANGED:   ...
       HOW TO VERIFY:    ...
  5. Also output updated/new test files that cover exactly what you changed.

Be conservative — one focused change per response.

{FILE_FORMAT_INSTRUCTIONS}
"""

# ── Phase 1: Test Generation ───────────────────────────────────────────────────

TEST_TARGETS = [
    {
        "label": "MatchmakingService",
        "sources": [
            "Sweep-Server/src/main/java/com/sweepgame/server/service/MatchmakingService.java",
        ],
        "module": "server",
    },
    {
        "label": "GameSessionManager",
        "sources": [
            "Sweep-Server/src/main/java/com/sweepgame/server/service/GameSessionManager.java",
            "Sweep-Server/src/main/java/com/sweepgame/server/model/GameSession.java",
        ],
        "module": "server",
    },
    {
        "label": "WebSocketConfig",
        "sources": [
            "Sweep-Server/src/main/java/com/sweepgame/server/config/WebSocketConfig.java",
        ],
        "module": "server",
    },
    {
        "label": "SweepLogic (game-logic)",
        "sources": [
            "Sweep-Base/game-logic/src/main/java/com/sweepgame/game/SweepLogic.java",
            "Sweep-Base/game-logic/src/main/java/com/sweepgame/game/Card.java",
        ],
        "module": "game-logic",
    },
]


def run_tests_phase(max_iter: int) -> None:
    print(f"\n{'═' * 60}")
    print("  PHASE 1: TEST GENERATION")
    print(f"{'═' * 60}\n")

    for i, target in enumerate(TEST_TARGETS[:max_iter]):
        label  = target["label"]
        module = target["module"]
        print(f"\n{'─' * 40}")
        print(f"  [{i+1}/{min(len(TEST_TARGETS), max_iter)}] Writing tests for: {label}")
        print(f"{'─' * 40}")

        source_text = read_files_for_prompt(target["sources"])

        prompt = f"""\
{TEST_INSTRUCTIONS}

Write JUnit 5 tests for the following source file(s):

{source_text}

Output the complete test file(s) using the FILE block format above.
"""

        print("  Calling Claude...")
        response = call_claude(prompt)

        if response.startswith("[CLAUDE_ERROR]"):
            log.error(f"Claude CLI error: {response}")
            print(f"  ERROR: {response}")
            continue

        print(f"\n[Agent response for {label}]:\n{response}\n")

        written = apply_file_blocks(response)
        if not written:
            print("  No file blocks found in response — skipping gradle.")
            continue

        print(f"  Written: {written}")
        print(f"  Running gradle test [{module}]...")
        ok, out = run_gradle(module, "compileTestJava")

        if not ok:
            print(f"\n  Compile FAILED. Asking Claude to fix...\n{out[-2000:]}")

            fix_prompt = f"""\
{TEST_INSTRUCTIONS}

You wrote these test files but they fail to compile. Fix the compilation errors.

SOURCE FILES:
{source_text}

COMPILE ERROR:
{out}

Output the corrected test file(s) using the FILE block format.
"""
            fix_response = call_claude(fix_prompt)
            print(f"\n[Agent fix]:\n{fix_response}\n")
            apply_file_blocks(fix_response)

            ok, out = run_gradle(module, "compileTestJava")
            if not ok:
                print(f"  Still failing. Moving on.\n{out[-1000:]}")
                offer_commit("tests", i + 1)
                continue

        # Run the full test suite
        ok, out = run_gradle(module, "test")
        status = "PASSED" if ok else "FAILED (tests reveal bugs — expected)"
        print(f"  Test run: {status}")
        if not ok:
            print(out[-1500:])

        offer_commit("tests", i + 1)


# ── Phase 2: Improvement Loop ──────────────────────────────────────────────────

IMPROVE_SOURCES = [
    "Sweep-Server/src/main/java/com/sweepgame/server/service/MatchmakingService.java",
    "Sweep-Server/src/main/java/com/sweepgame/server/service/GameSessionManager.java",
    "Sweep-Server/src/main/java/com/sweepgame/server/config/WebSocketConfig.java",
    "Sweep-Base/core/src/main/java/com/sweepgame/game/MultiplayerMode.java",
]


def run_improve_phase(max_iter: int) -> None:
    print(f"\n{'═' * 60}")
    print("  PHASE 2: IMPROVEMENT LOOP")
    print(f"{'═' * 60}\n")

    history: list[str] = []  # track what was fixed across iterations

    for iteration in range(1, max_iter + 1):
        print(f"\n{'─' * 40}")
        print(f"  Iteration {iteration}/{max_iter}")
        print(f"{'─' * 40}")

        source_text = read_files_for_prompt(IMPROVE_SOURCES)

        # Check if tests exist to include them
        test_dir = SERVER_DIR / "src" / "test"
        test_list = list_java_files("Sweep-Server/src/test") if test_dir.exists() else "(no tests yet)"

        history_text = (
            "\n".join(f"  - {h}" for h in history) if history else "  (none yet)"
        )

        prompt = f"""\
{IMPROVE_INSTRUCTIONS}

FIXES ALREADY MADE IN PREVIOUS ITERATIONS:
{history_text}

Pick an issue that has NOT been fixed yet.

EXISTING TEST FILES:
{test_list}

SOURCE FILES:
{source_text}

Identify one bug, fix it, and output all changed files using the FILE block format.
"""

        print("  Calling Claude...")
        response = call_claude(prompt)

        if response.startswith("[CLAUDE_ERROR]"):
            log.error(f"Claude CLI error: {response}")
            print(f"  ERROR: {response}")
            break

        print(f"\n[Agent]:\n{response}\n")

        written = apply_file_blocks(response)
        if not written:
            print("  No file changes produced. Agent may need more context.")
            history.append(f"Iteration {iteration}: no changes produced")
            continue

        print(f"  Written: {written}")

        # Determine which modules were touched
        modules_touched = set()
        for path in written:
            if "Sweep-Server" in path:
                modules_touched.add("server")
            if "game-logic" in path:
                modules_touched.add("game-logic")

        if not modules_touched:
            modules_touched = {"server"}

        # Validate: compile
        all_ok = True
        compile_errors = []
        for module in modules_touched:
            ok, out = run_gradle(module, "compileJava")
            if not ok:
                all_ok = False
                compile_errors.append(f"[{module}] {out[-2000:]}")

        if not all_ok:
            print(f"\n  COMPILE FAILED — reverting.\n" + "\n".join(compile_errors))
            git_revert()
            history.append(f"Iteration {iteration}: compile failed — reverted")
        else:
            # Validate: tests
            test_ok = True
            for module in modules_touched:
                test_dir_exists = (SERVER_DIR / "src" / "test").exists() if module == "server" else \
                                  (CLIENT_DIR / "game-logic" / "src" / "test").exists()
                if test_dir_exists:
                    ok, out = run_gradle(module, "test")
                    if not ok:
                        test_ok = False
                        print(f"\n  TESTS FAILED [{module}] — reverting.\n{out[-2000:]}")
                        git_revert()
                        history.append(f"Iteration {iteration}: tests failed — reverted")
                        break

            if test_ok:
                print("  Build + tests: OK")
                # Extract what was fixed from the response
                what_line = re.search(r'WHAT WAS THE BUG:\s*(.+)', response)
                what_fixed = what_line.group(1).strip() if what_line else f"fix in {', '.join(written)}"
                history.append(f"Iteration {iteration}: {what_fixed}")

                committed = offer_commit("improve", iteration)

        # Ask to continue
        if iteration < max_iter:
            cont = input("\nRun another iteration? (y/n): ").strip().lower()
            if cont != "y":
                break


# ── Entry Point ────────────────────────────────────────────────────────────────

def main() -> None:
    parser = argparse.ArgumentParser(description="Sweep Self-Improving Agent (Claude CLI)")
    parser.add_argument(
        "--phase",
        choices=["tests", "improve", "both"],
        default="both",
        help="Phase to run (default: both)",
    )
    parser.add_argument(
        "--iterations",
        type=int,
        default=DEFAULT_MAX_ITER,
        help=f"Max iterations per phase (default: {DEFAULT_MAX_ITER})",
    )
    args = parser.parse_args()

    # Verify `claude` CLI is reachable
    check = subprocess.run(
        ["claude", "--version"],
        capture_output=True, text=True, timeout=10,
    )
    if check.returncode != 0:
        print("\nERROR: `claude` CLI not found or not working.")
        print("Make sure Claude Code is installed and you are logged in.")
        print("Test it manually:  claude --version")
        sys.exit(1)

    log.info(f"Agent start — phase: {args.phase}, max_iter: {args.iterations}")
    log.info(f"Claude version: {check.stdout.strip()}")

    phases = ["tests", "improve"] if args.phase == "both" else [args.phase]

    for phase in phases:
        if phase == "tests":
            run_tests_phase(args.iterations)
        else:
            run_improve_phase(args.iterations)

        if phase == "tests" and args.phase == "both":
            cont = input(
                "\nTests phase complete. Start improvement phase? (y/n): "
            ).strip().lower()
            if cont != "y":
                break

    log.info("Agent done.")
    print(f"\nDone. Logs saved to: {LOG_DIR}")


if __name__ == "__main__":
    main()
