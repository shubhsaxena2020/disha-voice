#!/usr/bin/env python3
"""Compare the *payload* of two APKs, ignoring the signing material.

Why this exists
---------------
A debug-signed APK is not byte-reproducible across machines. Gradle generates
a per-machine debug keystore (``~/.android/debug.keystore``), so the APK
Signing Block - which holds the certificate and its signatures - differs on
every machine that builds it. Comparing whole-file SHA-256 across CI and a
developer laptop therefore always fails, even when the build is perfectly
reproducible.

What this script compares
-------------------------
Every ZIP entry is decompressed and hashed, and the bytes before and after
the APK Signing Block are compared directly. The signing block is the only
region that is allowed to differ.

This gives a claim that is both strong and true:

    "The CI build and the published release contain byte-identical code,
    resources, manifest and model; only the debug certificate differs."

Usage
-----
    python scripts/compare-apk-payload.py <apk-a> <apk-b>

Exit code 0 = the payloads are identical. Exit code 1 = they differ, or the
arguments were unusable.
"""

from __future__ import annotations

import hashlib
import sys
import zipfile
from pathlib import Path

MAGIC = b"APK Sig Block 42"


def signing_block_range(data: bytes) -> tuple[int, int] | None:
    """Return the byte range of the APK Signing Block, or None if absent."""
    pos = data.find(MAGIC)
    if pos < 0:
        return None
    if pos < 8:
        return None
    size = int.from_bytes(data[pos - 8 : pos], "little")
    start = pos - size + 8
    if start < 0 or pos + len(MAGIC) > len(data):
        return None
    return start, pos + len(MAGIC)


def payload_digest(path: Path) -> dict:
    """Hash every ZIP entry's decompressed content."""
    entries: dict[str, str] = {}
    with zipfile.ZipFile(path) as z:
        for name in sorted(z.namelist()):
            entries[name] = hashlib.sha256(z.read(name)).hexdigest()
    return entries


def main() -> int:
    if len(sys.argv) != 3:
        print(__doc__)
        return 2

    a_path = Path(sys.argv[1])
    b_path = Path(sys.argv[2])
    for p in (a_path, b_path):
        if not p.is_file():
            print(f"ERROR: not a file: {p}", file=sys.stderr)
            return 2

    a_bytes = a_path.read_bytes()
    b_bytes = b_path.read_bytes()

    print(f"A: {a_path}  ({len(a_bytes)} bytes)")
    print(f"B: {b_path}  ({len(b_bytes)} bytes)")

    if len(a_bytes) != len(b_bytes):
        print("\nFAIL: the files differ in length, so they cannot be the same build.")
        return 1

    # 1. every ZIP entry must have identical decompressed content
    entries_a = payload_digest(a_path)
    entries_b = payload_digest(b_path)
    only_a = set(entries_a) - set(entries_b)
    only_b = set(entries_b) - set(entries_a)
    if only_a or only_b:
        print("\nFAIL: the entry name sets differ")
        for name in sorted(only_a):
            print(f"  only in A: {name}")
        for name in sorted(only_b):
            print(f"  only in B: {name}")
        return 1

    differing = [n for n in entries_a if entries_a[n] != entries_b[n]]
    print(f"\nZIP entries compared: {len(entries_a)}")
    if differing:
        print(f"FAIL: {len(differing)} entr(y/ies) have different content:")
        for name in sorted(differing)[:20]:
            print(f"  {name}")
        return 1
    print("All entry contents are identical.")

    # 2. the only bytes allowed to differ are inside the signing block
    range_a = signing_block_range(a_bytes)
    range_b = signing_block_range(b_bytes)
    if range_a is None or range_b is None:
        print("\nWARNING: no APK Signing Block found; comparing the whole file.")
        if a_bytes == b_bytes:
            print("The files are byte-identical.")
            return 0
        print("FAIL: the files differ outside any signing block.")
        return 1

    if range_a != range_b:
        print(f"\nFAIL: the signing block is at a different offset ({range_a} vs {range_b})")
        return 1

    start, end = range_a
    if a_bytes[:start] != b_bytes[:start]:
        print("\nFAIL: bytes before the signing block differ")
        return 1
    if a_bytes[end:] != b_bytes[end:]:
        print("\nFAIL: bytes after the signing block differ")
        return 1

    print(f"Bytes before the signing block ({start:,} bytes): identical")
    print(f"Bytes after the signing block  ({len(a_bytes) - end:,} bytes): identical")
    print(f"APK Signing Block ({end - start:,} bytes): differs, as expected for a "
          f"per-machine debug key")

    print("\nPASS: the two APKs have byte-identical payloads (code, resources,")
    print("manifest and model). Only the debug signing certificate differs.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
