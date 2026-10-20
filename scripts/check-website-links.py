"""Local replica of the GitHub Pages link check in .github/workflows/website.yml.

Runs the same allowlist assembly and link validation so a broken link is caught
on the machine instead of in CI.
"""
import posixpath
import re
import shutil
import sys
from html.parser import HTMLParser
from pathlib import Path, PurePosixPath
from urllib.parse import unquote, urlsplit

root = Path(__file__).resolve().parents[1]
output = root / "_site_check"
if output.exists():
    shutil.rmtree(output)

files = {
    "website/index.html": "index.html",
    "website/styles.css": "styles.css",
    "docs/privacy/privacy-policy.md": "docs/privacy/privacy-policy.md",
    "docs/DEMO_GUIDE.md": "docs/DEMO_GUIDE.md",
    "docs/safety/SAFETY.md": "docs/safety/SAFETY.md",
    "README.md": "README.md",
    "PROJECT_STATUS.md": "PROJECT_STATUS.md",
    "CHANGELOG.md": "CHANGELOG.md",
    "LICENSE": "LICENSE",
    "RESEARCH/competitor-landscape.md": "docs/research/competitor-landscape.md",
    "RESEARCH/assistive-ux-requirements.md": "docs/research/assistive-ux-requirements.md",
    "RESEARCH/skeptical-review-research.md": "docs/research/skeptical-review-research.md",
    "RESEARCH/safety-and-claims-review.md": "docs/research/safety-and-claims-review.md",
    "RESEARCH/source-register.md": "docs/research/source-register.md",
    "RESEARCH/threat-model.md": "docs/research/threat-model.md",
    "RESEARCH/speech-hindi-audit.md": "docs/research/speech-hindi-audit.md",
    "RESEARCH/model-and-data-audit.md": "docs/research/model-and-data-audit.md",
    "RESEARCH/differentiation-hypothesis.md": "docs/research/differentiation-hypothesis.md",
    "RESEARCH/open-source-license-audit.md": "docs/research/open-source-license-audit.md",
    "RESEARCH/benchmark-plan.md": "docs/research/benchmark-plan.md",
    "RESEARCH/feature-comparison.csv": "docs/research/feature-comparison.csv",
    "docs/RELEASE_NOTES_v0.1.1-prototype.md": "docs/RELEASE_NOTES_v0.1.1-prototype.md",
    "TESTING/field-evaluation-protocol.md": "docs/research/field-evaluation-protocol.md",
}
for source, destination in files.items():
    source_path = root / source
    if not source_path.is_file():
        raise SystemExit(f"Required website input is missing: {source}")
    destination_path = output / destination
    destination_path.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source_path, destination_path)

published = set(files.values())


class Links(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.hrefs = []
        self.ids = set()

    def handle_starttag(self, tag, attrs):
        attributes = dict(attrs)
        if "id" in attributes:
            self.ids.add(attributes["id"])
        if tag == "a" and attributes.get("href"):
            self.hrefs.append(attributes["href"])


html_path = output / "index.html"
parser = Links()
parser.feed(html_path.read_text(encoding="utf-8"))
parser.close()
errors = []


def check_link(link, from_output):
    parsed = urlsplit(link.strip())
    if parsed.scheme or parsed.netloc:
        return
    target_path = unquote(parsed.path)
    if not target_path:
        target = from_output
    else:
        target = posixpath.normpath(
            posixpath.join(PurePosixPath(from_output).parent.as_posix(), target_path)
        )
    if target == ".":
        target = "index.html"
    if target.startswith("../") or target not in published:
        errors.append(f"{from_output}: local link is not in the published allowlist: {link}")
        return
    if parsed.fragment and target == "index.html" and parsed.fragment not in parser.ids:
        errors.append(f"{from_output}: missing in-page anchor #{parsed.fragment}")


for href in parser.hrefs:
    check_link(href, "index.html")

markdown_path = output / "docs/privacy/privacy-policy.md"
markdown = markdown_path.read_text(encoding="utf-8")
for match in re.finditer(r"\[[^\]]*\]\(([^)]+)\)", markdown):
    check_link(match.group(1).strip().strip("<>"), "docs/privacy/privacy-policy.md")

css = (output / "styles.css").read_text(encoding="utf-8")
for match in re.finditer(r"url\(([^)]+)\)", css, flags=re.IGNORECASE):
    check_link(match.group(1).strip().strip("\"'"), "styles.css")

if errors:
    print("Website link check failed:")
    for e in errors:
        print(" -", e)
    sys.exit(1)
print(f"Built and checked {len(published)} allowlisted static files (link check passed).")
shutil.rmtree(output)
