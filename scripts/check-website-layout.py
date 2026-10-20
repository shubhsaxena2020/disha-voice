#!/usr/bin/env python3
"""Static layout regression checks for the Disha Voice website.

These are the checks that a pure contrast auditor cannot make, but that a
browser can. They run in a real browser via Playwright when it is available,
and are skipped with a clear notice when it is not - they never silently
pass.

Checks performed at every tested viewport:

  1. exactly one ``<h1>``
  2. heading levels do not skip (h1 -> h2 -> h3 ...)
  3. all four ARIA landmarks are present
  4. the skip link resolves to a real element
  5. no positive ``tabindex``
  6. every link and button has an accessible name
  7. every image has an ``alt`` attribute
  8. no horizontal overflow
  9. no console errors
 10. every in-page anchor resolves

Usage:
    python scripts/check-website-layout.py

Exit code 0 = all checks pass (or were skipped for a stated reason).
Exit code 1 = a check failed.
"""

from __future__ import annotations

import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WEBSITE = ROOT / "website"

VIEWPORTS = [
    ("mobile", 390, 844),
    ("tablet", 900, 1000),
    ("desktop", 1440, 900),
]

# The audit script below is executed inside the page.
PAGE_AUDIT = r"""
() => {
  const problems = [];
  const note = (msg) => problems.push(msg);

  // 1. exactly one h1
  const h1s = document.querySelectorAll('h1');
  if (h1s.length !== 1) note(`expected exactly 1 <h1>, found ${h1s.length}`);

  // 2. heading order must not skip a level
  const headings = Array.from(document.querySelectorAll('h1,h2,h3,h4,h5,h6'));
  let previous = 0;
  for (const h of headings) {
    const level = Number(h.tagName[1]);
    if (previous && level > previous + 1) {
      note(`heading level skipped: h${previous} -> h${level} at "${h.textContent.trim().slice(0, 40)}"`);
    }
    previous = level;
  }

  // 3. landmarks
  for (const [label, selector] of [
    ['banner', 'header'],
    ['main', 'main'],
    ['contentinfo', 'footer'],
    ['navigation', 'nav'],
  ]) {
    if (!document.querySelector(selector)) note(`missing landmark: ${label} (${selector})`);
  }

  // 4. skip link target
  const skip = document.querySelector('.skip-link');
  if (skip) {
    const id = (skip.getAttribute('href') || '').replace('#', '');
    if (!id || !document.getElementById(id)) note(`skip link target missing: ${skip.getAttribute('href')}`);
  } else {
    note('no skip link found');
  }

  // 5. no positive tabindex
  document.querySelectorAll('[tabindex]').forEach(el => {
    const value = Number(el.getAttribute('tabindex'));
    if (value > 0) note(`positive tabindex ${value} on ${el.tagName.toLowerCase()}`);
  });

  // 6. accessible names for links and buttons
  const accessibleName = (el) => {
    const aria = el.getAttribute('aria-label');
    if (aria && aria.trim()) return aria.trim();
    const labelledby = el.getAttribute('aria-labelledby');
    if (labelledby) {
      const text = labelledby.split(/\s+/).map(id => {
        const t = document.getElementById(id);
        return t ? t.textContent.trim() : '';
      }).join(' ').trim();
      if (text) return text;
    }
    const text = (el.textContent || '').trim();
    if (text) return text;
    const title = el.getAttribute('title');
    if (title && title.trim()) return title.trim();
    return '';
  };
  document.querySelectorAll('a, button').forEach(el => {
    if (!accessibleName(el)) {
      note(`no accessible name on <${el.tagName.toLowerCase()}> href="${el.getAttribute('href') ?? ''}"`);
    }
  });

  // 7. images need an alt attribute
  document.querySelectorAll('img').forEach(el => {
    if (!el.hasAttribute('alt')) note('img without alt attribute');
  });

  // 8. no horizontal overflow
  const doc = document.documentElement;
  if (doc.scrollWidth > doc.clientWidth + 1) {
    note(`horizontal overflow: scrollWidth ${doc.scrollWidth} > clientWidth ${doc.clientWidth}`);
    document.querySelectorAll('body *').forEach(el => {
      const r = el.getBoundingClientRect();
      if (r.right > doc.clientWidth + 0.5 || r.left < -0.5) {
        note(`  overflowing element: <${el.tagName.toLowerCase()}${el.className ? ' class="' + el.className + '"' : ''}> left=${Math.round(r.left)} right=${Math.round(r.right)}`);
      }
    });
  }

  // 10. in-page anchors resolve
  document.querySelectorAll('a[href^="#"]').forEach(el => {
    const href = el.getAttribute('href');
    if (!href || href === '#') return;
    const id = href.slice(1);
    if (!document.getElementById(id)) note(`in-page anchor does not resolve: ${href}`);
  });

  return problems;
}
"""


def find_playwright() -> str | None:
    """Return a usable Playwright command, or None if there is none.

    The Python API is preferred when it is importable, because it needs no
    subprocess and no npx download. The npx CLI is the fallback for
    environments that only have Node.
    """
    try:
        import playwright  # noqa: F401

        return "python"
    except ImportError:
        pass
    if shutil.which("playwright"):
        return "playwright"
    try:
        result = subprocess.run(
            ["npx", "--yes", "playwright", "--version"],
            capture_output=True, text=True, timeout=120, cwd=str(ROOT),
        )
        if result.returncode == 0:
            return "npx playwright"
    except (subprocess.TimeoutExpired, FileNotFoundError, OSError):
        pass
    return None


def launch_browser(p):
    """Launch a browser, preferring one that is actually installed.

    Playwright's bundled Chromium is only present if
    ``playwright install chromium`` was run. On CI the system Google Chrome
    is pre-installed, so that is used instead. The order is: bundled
    Chromium, then the system Chrome channel, then any chromium-family
    browser on PATH.

    Returns a launched browser, or raises the last error.
    """
    attempts = []

    def _launch(**kwargs):
        return p.chromium.launch(**kwargs)

    # 1. the bundled Chromium
    try:
        return _launch()
    except Exception as exc:  # noqa: BLE001
        attempts.append(f"bundled chromium: {exc}")

    # 2. the system Chrome / Chromium channel
    for channel in ("chrome", "chromium", "msedge"):
        try:
            return _launch(channel=channel)
        except Exception as exc:  # noqa: BLE001
            attempts.append(f"channel {channel}: {exc}")

    raise RuntimeError(
        "No usable browser was found. Tried:\n  " + "\n  ".join(attempts)
    )


def run_with_python_api() -> dict:
    """Run the audit with the Playwright Python API."""
    from playwright.sync_api import sync_playwright

    results: dict[str, list[str]] = {}
    with sync_playwright() as p:
        browser = launch_browser(p)
        for name, width, height in VIEWPORTS:
            ctx = browser.new_context(viewport={"width": width, "height": height})
            page = ctx.new_page()
            errors: list[str] = []
            page.on("console", lambda m: errors.append(m.text) if m.type == "error" else None)
            page.on("pageerror", lambda e: errors.append(str(e)))
            page.goto((WEBSITE / "index.html").as_uri(), wait_until="load")
            problems = page.evaluate(PAGE_AUDIT)
            if errors:
                problems = problems + ["console error: " + e for e in errors]
            results[name] = problems
            ctx.close()
        browser.close()
    return results


def main() -> int:
    if not (WEBSITE / "index.html").exists():
        print("ERROR: website/index.html not found", file=sys.stderr)
        return 2

    command = find_playwright()
    if command is None:
        print("SKIPPED: Playwright is not available in this environment.")
        print("The layout checks could not run here. They are enforced in CI, where")
        print("Playwright is installed. This is NOT a pass - it is an unrun check.")
        return 0

    print(f"Using {command}")

    if command == "python":
        try:
            payload = run_with_python_api()
        except Exception as exc:  # noqa: BLE001 - report, do not crash
            print(f"Layout check could not run: {exc}", file=sys.stderr)
            return 2
    else:
        with tempfile.TemporaryDirectory() as tmp:
            script = Path(tmp) / "layout_check.py"
            script.write_text(
                "import sys, json\n"
                "from playwright.sync_api import sync_playwright\n"
                f"AUDIT = {PAGE_AUDIT!r}\n"
                "def launch(p):\n"
                "    try:\n"
                "        return p.chromium.launch()\n"
                "    except Exception:\n"
                "        for channel in ('chrome', 'chromium', 'msedge'):\n"
                "            try:\n"
                "                return p.chromium.launch(channel=channel)\n"
                "            except Exception:\n"
                "                pass\n"
                "    raise RuntimeError('no usable browser found')\n"
                "results = {}\n"
                "with sync_playwright() as p:\n"
                "    browser = launch(p)\n"
                "    for name, width, height in " + repr(VIEWPORTS) + ":\n"
                "        ctx = browser.new_context(viewport={'width': width, 'height': height})\n"
                "        page = ctx.new_page()\n"
                "        errors = []\n"
                "        page.on('console', lambda m: errors.append(m.text) if m.type == 'error' else None)\n"
                "        page.on('pageerror', lambda e: errors.append(str(e)))\n"
                "        page.goto('file://" + str(WEBSITE / 'index.html').replace('\\', '/') + "', wait_until='load')\n"
                "        problems = page.evaluate(AUDIT)\n"
                "        if errors:\n"
                "            problems = problems + ['console error: ' + e for e in errors]\n"
                "        results[name] = problems\n"
                "        ctx.close()\n"
                "    browser.close()\n"
                "print(json.dumps(results, indent=1))\n",
                encoding="utf-8",
            )

            result = subprocess.run(
                f"{command} run --with-deps python {script}" if command.startswith("npx") else f"{command} run python {script}",
                capture_output=True, text=True, timeout=600, cwd=str(ROOT),
            )

        if result.returncode != 0:
            print("Layout check could not run:", file=sys.stderr)
            print(result.stdout[-4000:], file=sys.stderr)
            print(result.stderr[-4000:], file=sys.stderr)
            return 2

        try:
            payload = json.loads(result.stdout[result.stdout.index("{") : result.stdout.rindex("}") + 1])
        except (ValueError, json.JSONDecodeError):
            print("Could not parse the layout check output:", file=sys.stderr)
            print(result.stdout[-4000:], file=sys.stderr)
            return 2

    failed = False
    for name, problems in payload.items():
        if problems:
            failed = True
            print(f"\nFAIL [{name}] {len(problems)} problem(s):")
            for p in problems:
                print(f"  - {p}")
        else:
            print(f"PASS [{name}] all layout checks")

    if failed:
        return 1
    print("\nAll viewport layout checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
