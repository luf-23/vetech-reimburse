# -*- coding: utf-8 -*-
"""Remove comments from frontend src (.ts, .vue, .css)."""
from __future__ import annotations

import re
from pathlib import Path

SRC = Path(__file__).resolve().parent.parent / "src"


def strip_line_comments(text: str) -> str:
    out: list[str] = []
    for line in text.split("\n"):
        cleaned: list[str] = []
        in_str: str | None = None
        i = 0
        while i < len(line):
            ch = line[i]
            if in_str:
                cleaned.append(ch)
                if ch == in_str and (i == 0 or line[i - 1] != "\\"):
                    in_str = None
                i += 1
                continue
            if ch in ("'", '"', "`"):
                in_str = ch
                cleaned.append(ch)
                i += 1
                continue
            if ch == "/" and i + 1 < len(line) and line[i + 1] == "/":
                break
            cleaned.append(ch)
            i += 1
        out.append("".join(cleaned).rstrip())
    return "\n".join(out)


def strip_js_ts(text: str) -> str:
    text = re.sub(r"/\*[\s\S]*?\*/", "", text)
    text = strip_line_comments(text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text


def strip_css(text: str) -> str:
    text = re.sub(r"/\*[\s\S]*?\*/", "", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text


def strip_html_comments(text: str) -> str:
    text = re.sub(r"<!--[\s\S]*?-->", "", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text


def process_vue(content: str) -> str:
    for tag, fn in (
        ("script", strip_js_ts),
        ("style", strip_css),
        ("template", strip_html_comments),
    ):
        pattern = re.compile(rf"<({tag})([^>]*)>([\s\S]*?)</\1>", re.IGNORECASE)

        def repl(m: re.Match[str], _fn=fn) -> str:
            return f"<{m.group(1)}{m.group(2)}>{_fn(m.group(3))}</{m.group(1)}>"

        content = pattern.sub(repl, content)
    return content


def main() -> None:
    for path in sorted(SRC.rglob("*")):
        if not path.is_file() or path.suffix not in {".ts", ".vue", ".css"}:
            continue
        raw = path.read_text(encoding="utf-8")
        if path.suffix == ".vue":
            new = process_vue(raw)
        elif path.suffix == ".css":
            new = strip_css(raw)
        else:
            new = strip_js_ts(raw)
        if new != raw:
            path.write_text(new, encoding="utf-8", newline="\n")
            print(f"updated: {path.relative_to(SRC.parent)}")


if __name__ == "__main__":
    main()
