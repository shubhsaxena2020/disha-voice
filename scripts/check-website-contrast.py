#!/usr/bin/env python3
"""WCAG 2.2 AA contrast audit for the Disha Voice website.

Unlike a regex over the stylesheet, this resolves the *rendered* result: it
parses ``website/index.html`` into a DOM, matches every CSS rule against
every element with a real selector matcher, resolves the cascade by
specificity and source order, and walks up the ancestor chain to find the
background a piece of text actually sits on.

That distinction matters. A stylesheet-only audit cannot tell that
``.safety-side p`` never applies to ``.flow-step p``, and it cannot know that
``.safety-content h2`` is overridden to a light colour on a dark band. Both
are exactly the cases where a text-only audit produces false results.

Alpha is composited: a translucent background is blended with the effective
background behind it, as a browser would.

Thresholds (WCAG 2.2 SC 1.4.3):
  * normal text  (under 24px, or under 18.66px when bold) -> 4.5:1
  * large text   (24px+, or 18.66px+ bold)               -> 3:1

Elements carrying ``aria-hidden="true"`` are excluded, because they are
removed from the accessibility tree and are not exposed to assistive
technology. Excluded elements are listed in the report so the exclusion is
visible rather than silent.

Usage:
    python scripts/check-website-contrast.py
    python scripts/check-website-contrast.py --list-excluded

Exit code 0 = every rendered text pair passes. Exit code 1 = a failure.
"""

from __future__ import annotations

import argparse
import re
import sys
from html.parser import HTMLParser
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HTML_PATH = ROOT / "website" / "index.html"
CSS_PATH = ROOT / "website" / "styles.css"


# ==========================================================================
# Colour maths
# ==========================================================================


def _srgb_to_linear(channel: float) -> float:
    if channel <= 0.03928:
        return channel / 12.92
    return ((channel + 0.055) / 1.055) ** 2.4


def relative_luminance(rgb: tuple[float, float, float]) -> float:
    r, g, b = (_srgb_to_linear(c / 255) for c in rgb)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def contrast_ratio(fg: tuple[float, float, float], bg: tuple[float, float, float]) -> float:
    l1 = relative_luminance(fg)
    l2 = relative_luminance(bg)
    lighter, darker = max(l1, l2), min(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)


NAMED_COLORS = {
    "white": (255, 255, 255),
    "black": (0, 0, 0),
    "red": (255, 0, 0),
    "green": (0, 128, 0),
    "blue": (0, 0, 255),
    "gray": (128, 128, 128),
    "grey": (128, 128, 128),
    "silver": (192, 192, 192),
    "navy": (0, 0, 128),
    "teal": (0, 128, 128),
    "olive": (128, 128, 0),
    "purple": (128, 0, 128),
    "maroon": (128, 0, 0),
    "lime": (0, 255, 0),
    "aqua": (0, 255, 255),
    "fuchsia": (255, 0, 255),
    "yellow": (255, 255, 0),
    "orange": (255, 165, 0),
    "transparent": (0, 0, 0),
}


def parse_color(value: str) -> tuple[float, float, float, float] | None:
    """Parse a CSS colour into (r, g, b, alpha) with 0-255 channels."""
    if not value:
        return None
    v = value.strip().lower()
    if v in ("transparent", "none", "initial", "unset", "inherit", "currentcolor"):
        return None
    if v in NAMED_COLORS:
        r, g, b = NAMED_COLORS[v]
        return (float(r), float(g), float(b), 0.0 if v == "transparent" else 1.0)
    m = re.match(r"^#([0-9a-f]{3})$", v)
    if m:
        h = m.group(1)
        return tuple(float(int(c * 2, 16)) for c in h) + (1.0,)  # type: ignore[return-value]
    m = re.match(r"^#([0-9a-f]{6})$", v)
    if m:
        h = m.group(1)
        return tuple(float(int(h[i : i + 2], 16)) for i in (0, 2, 4)) + (1.0,)  # type: ignore[return-value]
    m = re.match(r"^rgba?\(([^)]+)\)$", v)
    if m:
        parts = [p.strip() for p in m.group(1).split(",")]
        if len(parts) >= 3:

            def chan(p: str) -> float:
                if p.endswith("%"):
                    return float(p[:-1]) * 255 / 100
                return float(p)

            r, g, b = (chan(p) for p in parts[:3])
            a = float(parts[3]) if len(parts) > 3 else 1.0
            return (r, g, b, a)
    return None


def composite(fg: tuple[float, float, float, float], bg: tuple[float, float, float]) -> tuple[float, float, float]:
    """Blend a translucent colour over an opaque background."""
    a = fg[3]
    return (
        fg[0] * a + bg[0] * (1 - a),
        fg[1] * a + bg[1] * (1 - a),
        fg[2] * a + bg[2] * (1 - a),
    )


# ==========================================================================
# Minimal DOM
# ==========================================================================


class Element:
    __slots__ = ("tag", "attrs", "children", "parent", "text_parts", "index")

    def __init__(self, tag: str, attrs: dict[str, str], parent: "Element | None"):
        self.tag = tag
        self.attrs = attrs
        self.children: list["Element"] = []
        self.parent = parent
        self.text_parts: list[str] = []
        self.index = 0

    @property
    def classes(self) -> set[str]:
        return set(self.attrs.get("class", "").split())

    @property
    def element_id(self) -> str:
        return self.attrs.get("id", "")

    def ancestors(self):
        node = self.parent
        while node is not None:
            yield node
            node = node.parent

    def has_direct_text(self) -> bool:
        return any(part.strip() for part in self.text_parts)

    def aria_hidden(self) -> bool:
        return self.attrs.get("aria-hidden", "").lower() == "true"

    def is_hidden(self) -> bool:
        if "hidden" in self.attrs:
            return True
        style = self.attrs.get("style", "").replace(" ", "")
        return "display:none" in style or "visibility:hidden" in style

    def describe(self) -> str:
        cls = ".".join(sorted(self.classes))
        base = self.tag + (f".{cls}" if cls else "")
        if self.element_id:
            base += f"#{self.element_id}"
        return base


VOID_TAGS = {
    "area", "base", "br", "col", "embed", "hr", "img", "input",
    "link", "meta", "param", "source", "track", "wbr",
}


class DOMBuilder(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.root = Element("#document", {}, None)
        self.current = self.root
        self._counter = 0

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attrd = {k: (v or "") for k, v in attrs}
        el = Element(tag, attrd, self.current)
        self._counter += 1
        el.index = self._counter
        self.current.children.append(el)
        if tag not in VOID_TAGS:
            self.current = el

    def handle_startendtag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attrd = {k: (v or "") for k, v in attrs}
        el = Element(tag, attrd, self.current)
        self._counter += 1
        el.index = self._counter
        self.current.children.append(el)

    def handle_endtag(self, tag: str) -> None:
        node = self.current
        while node is not None and node.tag != tag:
            node = node.parent
        if node is not None and node.parent is not None:
            self.current = node.parent

    def handle_data(self, data: str) -> None:
        self.current.text_parts.append(data)


# ==========================================================================
# CSS parsing
# ==========================================================================


class Rule:
    __slots__ = ("selector", "props", "order", "condition", "specificity")

    def __init__(self, selector: str, props: dict[str, str], order: int, condition: str):
        self.selector = selector
        self.props = props
        self.order = order
        self.condition = condition
        self.specificity = compute_specificity(selector)


VAR_RE = re.compile(r"var\(\s*(--[a-zA-Z0-9_-]+)\s*(?:,\s*([^)]*))?\)")


def resolve_vars(value: str, custom: dict[str, str]) -> str:
    """Resolve ``var(--name)`` references against the custom property table.

    The stylesheet uses custom properties for its palette, so a value like
    ``color:var(--green)`` has to be looked up or the element is silently
    skipped. That gap hid a real defect: a green link on the dark safety
    band, at 1.94:1.
    """
    if "var(" not in value:
        return value
    resolved = value
    for _ in range(8):  # bounded: custom properties may reference each other
        if "var(" not in resolved:
            break
        replaced = False

        def repl(m: re.Match) -> str:
            nonlocal replaced
            name = m.group(1)
            if name in custom:
                replaced = True
                return custom[name]
            fallback = m.group(2)
            return fallback.strip() if fallback else m.group(0)

        new_value = VAR_RE.sub(repl, resolved)
        resolved = new_value
        if not replaced:
            break
    return resolved


def collect_custom_properties(rules: list[Rule]) -> dict[str, str]:
    """Build the custom property table from :root (and any other rule)."""
    custom: dict[str, str] = {}
    for rule in rules:
        if rule.condition:
            continue
        for prop, value in rule.props.items():
            if prop.startswith("--"):
                custom[prop] = value
    # resolve references among custom properties
    for _ in range(8):
        for name in list(custom):
            custom[name] = resolve_vars(custom[name], custom)
    return custom


def parse_css(css: str) -> list[Rule]:
    """Parse CSS into rules, tracking @media conditions.

    A brace-aware scanner is used because the stylesheet is minified and
    contains nested at-rules; a flat regex would mangle them.
    """
    rules: list[Rule] = []
    i = 0
    n = len(css)
    order = 0
    conditions: list[str] = []

    def parse_block(conds: list[str]) -> None:
        nonlocal i, order
        while i < n:
            while i < n and css[i].isspace():
                i += 1
            if i >= n:
                return
            if css[i] == "}":
                i += 1
                return
            start = i
            while i < n and css[i] not in "{;":
                i += 1
            if i >= n:
                return
            prelude = css[start:i].strip()
            if css[i] == ";":
                i += 1
                continue
            i += 1
            if prelude.startswith("@"):
                depth = 1
                block_start = i
                while i < n and depth:
                    if css[i] == "{":
                        depth += 1
                    elif css[i] == "}":
                        depth -= 1
                    i += 1
                inner = css[block_start : i - 1]
                if prelude.lower().startswith("@media"):
                    conditions.append(" ".join(prelude.split()))
                    parse_block(conditions)
                    conditions.pop()
                else:
                    parse_block(conds)
                continue
            depth = 1
            body_start = i
            while i < n and depth:
                if css[i] == "{":
                    depth += 1
                elif css[i] == "}":
                    depth -= 1
                if depth == 0:
                    break
                i += 1
            body = css[body_start:i]
            i += 1
            props: dict[str, str] = {}
            for decl in body.split(";"):
                if ":" not in decl:
                    continue
                key, value = decl.split(":", 1)
                props[key.strip().lower()] = value.strip()
            if props:
                cond = " ".join(conds)
                for sel in split_selector_list(prelude):
                    rules.append(Rule(sel, props, order, cond))
                    order += 1

    parse_block(conditions)
    return rules


def split_selector_list(prelude: str) -> list[str]:
    """Split a selector list on top-level commas."""
    out: list[str] = []
    depth = 0
    buf = ""
    for ch in prelude:
        if ch in "([":
            depth += 1
        elif ch in ")]":
            depth -= 1
        if ch == "," and depth == 0:
            out.append(buf.strip())
            buf = ""
        else:
            buf += ch
    if buf.strip():
        out.append(buf.strip())
    return [s for s in out if s]


# ==========================================================================
# Selector matching
# ==========================================================================

COMPOUND_RE = re.compile(
    r"""
    (?P<tag>^[a-zA-Z][a-zA-Z0-9-]*|^\*)
  | \.(?P<cls>[a-zA-Z0-9_-]+)
  | \#(?P<id>[a-zA-Z0-9_-]+)
  | \[(?P<attr>[^\]]+)\]
  | :(?P<pseudo>[a-zA-Z-]+)(?:\((?P<pargs>[^)]*)\))?
    """,
    re.VERBOSE,
)


def parse_compound(text: str) -> dict:
    """Parse one compound selector into its simple selectors."""
    parts = {"tag": None, "classes": set(), "id": None, "attrs": [], "pseudos": []}
    pos = 0
    text = text.strip()
    while pos < len(text):
        m = COMPOUND_RE.match(text, pos)
        if not m:
            pos += 1
            continue
        pos = m.end()
        if m.group("tag"):
            parts["tag"] = None if m.group("tag") == "*" else m.group("tag").lower()
        elif m.group("cls"):
            parts["classes"].add(m.group("cls"))
        elif m.group("id"):
            parts["id"] = m.group("id")
        elif m.group("attr") is not None:
            parts["attrs"].append(m.group("attr"))
        elif m.group("pseudo"):
            parts["pseudos"].append((m.group("pseudo").lower(), m.group("pargs")))
    return parts


def split_selector(selector: str) -> list[tuple[str, dict]]:
    """Split a selector into (combinator, compound) pairs.

    The first compound's combinator is ''. Combinators are ' ' (descendant),
    '>', '+' and '~'.
    """
    tokens: list[tuple[str, str]] = []
    buf = ""
    depth = 0
    combinator = ""
    for ch in selector:
        if ch in "([":
            depth += 1
        elif ch in ")]":
            depth -= 1
        if depth == 0 and ch in " >+~":
            if buf.strip():
                tokens.append((combinator, buf.strip()))
                buf = ""
            if ch != " ":
                combinator = ch
            elif combinator not in (">", "+", "~"):
                combinator = " "
            continue
        buf += ch
    if buf.strip():
        tokens.append((combinator, buf.strip()))

    result: list[tuple[str, dict]] = [(comb, parse_compound(text)) for comb, text in tokens]
    if not result:
        return []
    result[0] = ("", result[0][1])
    return result


def compute_specificity(selector: str) -> tuple[int, int, int]:
    ids = classes = types = 0
    for _, compound in split_selector(selector):
        ids += 1 if compound["id"] else 0
        classes += len(compound["classes"]) + len(compound["attrs"])
        classes += sum(
            1
            for p, _ in compound["pseudos"]
            if p not in ("before", "after", "first-line", "first-letter")
        )
        types += 1 if compound["tag"] else 0
    return (ids, classes, types)


def match_compound(el: Element, compound: dict) -> bool:
    if compound["tag"] and el.tag.lower() != compound["tag"]:
        return False
    if compound["id"] and el.element_id != compound["id"]:
        return False
    if not compound["classes"].issubset(el.classes):
        return False
    for attr_spec in compound["attrs"]:
        if "=" not in attr_spec:
            continue
        name, _, value = attr_spec.partition("=")
        name = name.strip()
        value = value.strip().strip("\"'")
        if name.lower() == "class":
            if value not in el.classes:
                return False
        elif el.attrs.get(name) != value:
            return False
    for pseudo, args in compound["pseudos"]:
        if pseudo == "not":
            if args is None:
                return False
            for inner in split_selector_list(args):
                if match_compound(el, parse_compound(inner)):
                    return False
        elif pseudo == "first-child":
            if el.parent is None or el.parent.children[0] is not el:
                return False
        elif pseudo == "last-child":
            if el.parent is None or el.parent.children[-1] is not el:
                return False
        elif pseudo in ("first-of-type", "last-of-type"):
            if el.parent is None:
                return False
            same = [c for c in el.parent.children if c.tag == el.tag]
            if not same:
                return False
            if pseudo == "first-of-type" and same[0] is not el:
                return False
            if pseudo == "last-of-type" and same[-1] is not el:
                return False
        elif pseudo in (
            "hover", "focus", "active", "visited", "focus-visible",
            "focus-within", "target", "checked", "disabled",
        ):
            # state-dependent: assume not active for the default rendering
            return False
        elif pseudo in (
            "before", "after", "first-line", "first-letter",
            "marker", "placeholder", "selection",
        ):
            continue
    return True


def selector_matches(selector: str, el: Element) -> bool:
    """True if the selector matches the element, using the real ancestor chain."""
    parts = split_selector(selector)
    if not parts:
        return False

    def match_from(idx: int, node: Element) -> bool:
        comb, compound = parts[idx]
        if not match_compound(node, compound):
            return False
        if idx == 0:
            return True
        prev_comb = parts[idx][0]
        if prev_comb == ">":
            return node.parent is not None and match_from(idx - 1, node.parent)
        if prev_comb == "+":
            sib = node.parent
            if sib is None:
                return False
            idx_sib = sib.children.index(node)
            if idx_sib == 0:
                return False
            return match_from(idx - 1, sib.children[idx_sib - 1])
        if prev_comb == "~":
            sib = node.parent
            if sib is None:
                return False
            idx_sib = sib.children.index(node)
            for j in range(idx_sib - 1, -1, -1):
                if match_from(idx - 1, sib.children[j]):
                    return True
            return False
        for anc in node.ancestors():
            if match_from(idx - 1, anc):
                return True
        return False

    return match_from(len(parts) - 1, el)


# ==========================================================================
# Cascade resolution
# ==========================================================================


def resolve_properties(rules: list[Rule], el: Element, condition: str,
                      custom: dict[str, str] | None = None) -> dict[str, str]:
    """Resolve the computed properties for an element under one condition.

    ``condition`` is "" for the base cascade, or a media-query string. Rules
    whose condition is "" always apply; rules in the named condition also
    apply, at their own specificity and source order.
    """
    custom = custom or {}
    applicable = [
        r
        for r in rules
        if (r.condition == "" or r.condition == condition)
        and selector_matches(r.selector, el)
    ]
    # sort by specificity then source order; later wins
    applicable.sort(key=lambda r: (r.specificity, r.order))
    computed: dict[str, str] = {}
    for rule in applicable:
        for prop, value in rule.props.items():
            value = resolve_vars(value, custom)
            if prop in ("background", "background-color"):
                computed["background-color"] = value
            elif prop == "color":
                computed["color"] = value
            elif prop == "font-size":
                computed["font-size"] = value
            elif prop == "font-weight":
                computed["font-weight"] = value
            elif prop == "font":
                # shorthand: font:700 14px var(--display) or font:9px var(--mono)
                m = re.match(r"^(?:(\d{3})|bold)\s+([\d.]+px|[\d.]+rem)", value)
                if m:
                    if m.group(1):
                        computed["font-weight"] = m.group(1)
                    computed["font-size"] = m.group(2)
                else:
                    m2 = re.match(r"^([\d.]+)(px|rem)\b", value)
                    if m2:
                        computed["font-size"] = m2.group(1) + m2.group(2)
            elif prop == "display":
                computed["display"] = value
            elif prop == "visibility":
                computed["visibility"] = value
    return computed


PAGE_BACKGROUND = (244.0, 244.0, 237.0)


def effective_background(rules: list[Rule], el: Element, condition: str,
                        custom: dict[str, str] | None = None) -> tuple[float, float, float]:
    """Walk up the ancestor chain compositing backgrounds, as a browser would."""
    layers: list[tuple[float, float, float, float]] = []
    node: Element | None = el
    while node is not None:
        computed = resolve_properties(rules, node, condition, custom)
        raw = computed.get("background-color")
        parsed = parse_color(raw) if raw else None
        if parsed is None:
            # a gradient or image background: treat this layer as transparent
            node = node.parent
            continue
        layers.append(parsed)
        if parsed[3] >= 1.0:
            break
        node = node.parent
    if not layers:
        return PAGE_BACKGROUND
    # composite from the bottom-most layer upward
    result = PAGE_BACKGROUND
    for layer in reversed(layers):
        result = composite(layer, result)
    return result


def is_large_text(size_px: float, weight: str) -> bool:
    """WCAG large text: >=24px, or >=18.66px when bold."""
    bold = weight in ("bold", "600", "700", "800", "900")
    if size_px >= 24:
        return True
    return bold and size_px >= 18.66


def resolve_font(rules: list[Rule], el: Element, condition: str,
                 custom: dict[str, str] | None = None,
                 inherited_size: float = 16.0,
                 inherited_weight: str = "400") -> tuple[float, str]:
    """Resolve font-size and font-weight, inheriting when not declared.

    Inheritance follows the real ancestor chain, because a bare ``<small>``
    inside a styled parent inherits the parent's size.
    """
    computed = resolve_properties(rules, el, condition, custom)
    size = inherited_size
    raw_size = computed.get("font-size")
    if raw_size:
        m = re.match(r"^([\d.]+)(px|rem|em|%)", raw_size)
        if m:
            value = float(m.group(1))
            unit = m.group(2)
            if unit == "px":
                size = value
            elif unit == "rem":
                size = value * 16
            elif unit == "em":
                size = value * inherited_size
            elif unit == "%":
                size = inherited_size * value / 100
    weight = computed.get("font-weight", inherited_weight)
    return size, weight


def inherited_font(rules: list[Rule], el: Element, condition: str,
                   custom: dict[str, str] | None = None) -> tuple[float, str]:
    """Compute the font size/weight an element inherits from its parent."""
    parent = el.parent
    if parent is None or parent.tag == "#document":
        return 16.0, "400"
    psize, pweight = resolve_font(rules, parent, condition, custom)
    return psize, pweight


def resolve_color(rules: list[Rule], el: Element, condition: str,
                  custom: dict[str, str] | None = None) -> tuple[float, float, float, float] | None:
    """Resolve the effective text colour, following CSS inheritance.

    ``color`` is an inherited property: an element with no matching colour
    rule takes its parent's colour. Without this, a bare ``<span>`` inside a
    styled parent is skipped entirely - which is how a 4.42:1 failure on
    "Current stage" went unnoticed in the first version of this auditor.
    """
    node: Element | None = el
    while node is not None:
        computed = resolve_properties(rules, node, condition, custom)
        raw = computed.get("color")
        if raw:
            parsed = parse_color(raw)
            if parsed is not None:
                return parsed
        node = node.parent
    return None


# ==========================================================================
# Audit
# ==========================================================================


def collect_conditions(rules: list[Rule]) -> list[str]:
    conds = {""}
    for r in rules:
        if r.condition:
            conds.add(r.condition)
    return sorted(conds)


def iter_elements(el: Element):
    for child in el.children:
        yield child
        yield from iter_elements(child)


def audit(list_excluded: bool = False) -> int:
    if not HTML_PATH.exists() or not CSS_PATH.exists():
        print("ERROR: website/index.html or website/styles.css not found", file=sys.stderr)
        return 2

    html = HTML_PATH.read_text(encoding="utf-8")
    css = CSS_PATH.read_text(encoding="utf-8")

    builder = DOMBuilder()
    builder.feed(html)
    root = builder.root

    rules = parse_css(css)
    custom = collect_custom_properties(rules)
    conditions = collect_conditions(rules)

    failures: list[str] = []
    checked = 0
    excluded: list[str] = []

    for condition in conditions:
        label = f"@{condition}" if condition else "base"
        for el in iter_elements(root):
            if not el.has_direct_text():
                continue
            desc = el.describe()

            if el.aria_hidden() or any(a.aria_hidden() for a in el.ancestors()):
                excluded.append(f"{desc} (aria-hidden)")
                continue
            if el.is_hidden():
                excluded.append(f"{desc} (hidden)")
                continue

            computed = resolve_properties(rules, el, condition, custom)
            if computed.get("display") == "none" or computed.get("visibility") == "hidden":
                excluded.append(f"{desc} (not rendered)")
                continue

            color_raw = computed.get("color")
            fg_parsed = None
            if color_raw:
                fg_parsed = parse_color(color_raw)
            if fg_parsed is None:
                # colour is inherited: fall back to the nearest ancestor's
                fg_parsed = resolve_color(rules, el, condition, custom)
            if fg_parsed is None:
                continue

            bg = effective_background(rules, el, condition, custom)
            if fg_parsed[3] < 1.0:
                fg_rgb = composite(fg_parsed, bg)
            else:
                fg_rgb = (fg_parsed[0], fg_parsed[1], fg_parsed[2])

            inh_size, inh_weight = inherited_font(rules, el, condition, custom)
            size, weight = resolve_font(rules, el, condition, custom, inh_size, inh_weight)
            large = is_large_text(size, weight)
            required = 3.0 if large else 4.5
            ratio = contrast_ratio(fg_rgb, bg)
            checked += 1

            if ratio < required:
                failures.append(
                    f"[{label}] {desc}: #{int(fg_rgb[0]):02x}{int(fg_rgb[1]):02x}{int(fg_rgb[2]):02x} "
                    f"on #{int(bg[0]):02x}{int(bg[1]):02x}{int(bg[2]):02x} "
                    f"= {ratio:.2f}:1 (needs {required}:1, "
                    f"{'large' if large else 'normal'} text, {size:.0f}px)"
                )

    print(f"Checked {checked} rendered text/background pairs from {HTML_PATH.relative_to(ROOT)}")
    if list_excluded:
        print(f"\nExcluded {len(excluded)} element(s) (aria-hidden, hidden, or not rendered):")
        for e in sorted(set(excluded)):
            print(f"  - {e}")

    if failures:
        print(f"\n{len(failures)} pair(s) below WCAG 2.2 AA:\n")
        for f in failures:
            print(f"  FAIL  {f}")
        return 1

    print("All rendered text pairs meet WCAG 2.2 AA contrast thresholds.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--list-excluded", action="store_true",
                        help="print the elements that were excluded and why")
    args = parser.parse_args()
    return audit(list_excluded=args.list_excluded)


if __name__ == "__main__":
    sys.exit(main())
