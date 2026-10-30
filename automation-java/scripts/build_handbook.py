"""Build the offline handbook from repository Markdown and inline SVGs. Standard library only."""
from pathlib import Path
import html
import re

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
CHAPTERS = [
    ("architecture", "ARCHITECTURE.md", "Architecture and flowcharts"),
    ("hands-on", "HANDBOOK.md", "Hands-on guide"),
    ("edge-cases", "EDGE_CASE_CATALOG.md", "Edge-case catalog"),
    ("demo", "DEMO_SCRIPT.md", "Demo script"),
    ("validation", "VALIDATION.md", "Validation record"),
]
DESTINATIONS = {name: "#" + key for key, name, _ in CHAPTERS}

def inline(text):
    # Escape first. Only repository-authored formatting becomes HTML.
    text = html.escape(text)
    text = re.sub(r"\x60([^\x60]+)\x60", lambda m: "<code>" + m.group(1) + "</code>", text)
    text = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", text)
    def link(m):
        target = html.unescape(m.group(2))
        target = DESTINATIONS.get(target, target)
        return '<a href="' + html.escape(target, quote=True) + '">' + m.group(1) + "</a>"
    return re.sub(r"\[([^\]]+)\]\(([^)]+)\)", link, text)

def render(markdown):
    lines = markdown.splitlines()
    result = []
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if not line:
            i += 1
            continue
        if line.startswith("\x60\x60\x60"):
            language = line[3:]
            i += 1
            content = []
            while i < len(lines) and not lines[i].strip().startswith("\x60\x60\x60"):
                content.append(lines[i]); i += 1
            result.append('<div class="code-label">' + html.escape(language or "text") + '</div><pre><code>' + html.escape("\n".join(content)) + "</code></pre>")
            i += 1
            continue
        image = re.fullmatch(r"!\[([^\]]*)\]\(([^)]+)\)", line)
        if image:
            path = DOCS / image.group(2)
            if path.suffix == ".svg":
                result.append('<figure>' + path.read_text(encoding="utf-8") + '<figcaption>' + html.escape(image.group(1)) + "</figcaption></figure>")
            i += 1
            continue
        heading = re.match(r"^(#{1,6}) (.+)$", line)
        if heading:
            level = min(len(heading.group(1)) + 1, 6)
            result.append(f"<h{level}>" + inline(heading.group(2)) + f"</h{level}>")
            i += 1
            continue
        if line.startswith("|"):
            rows = []
            while i < len(lines) and lines[i].strip().startswith("|"):
                cells = [c.strip() for c in lines[i].strip().strip("|").split("|")]
                if not all(re.fullmatch(r":?-+:?", c) for c in cells):
                    rows.append(cells)
                i += 1
            result.append('<div class="table-wrap"><table><thead><tr>' + "".join("<th>" + inline(c) + "</th>" for c in rows[0]) + "</tr></thead><tbody>")
            for row in rows[1:]:
                result.append("<tr>" + "".join("<td>" + inline(c) + "</td>" for c in row) + "</tr>")
            result.append("</tbody></table></div>")
            continue
        bullet = re.match(r"^(- |\d+\. )(.+)$", line)
        if bullet:
            ordered = bullet.group(1) != "- "
            tag = "ol" if ordered else "ul"
            result.append("<" + tag + ">")
            while i < len(lines):
                item = re.match(r"^(- |\d+\. )(.+)$", lines[i].strip())
                if not item or (item.group(1) != "- ") != ordered: break
                result.append("<li>" + inline(item.group(2)) + "</li>"); i += 1
            result.append("</" + tag + ">")
            continue
        paragraph = [line]
        i += 1
        while i < len(lines) and lines[i].strip() and not re.match(r"^(#|\||- |\d+\. |\x60\x60\x60|!\[)", lines[i].strip()):
            paragraph.append(lines[i].strip()); i += 1
        result.append("<p>" + inline(" ".join(paragraph)) + "</p>")
    return "\n".join(result)

css = """
:root{--navy:#123c59;--teal:#137f70;--ink:#203748;--muted:#536b7a}
*{box-sizing:border-box}html{scroll-behavior:smooth}
body{overflow-wrap:anywhere;margin:0;background:#f0f4f7;color:var(--ink);font:16px/1.7 'Segoe UI',Arial,sans-serif}
a{color:#126e9b;text-decoration-thickness:1px;text-underline-offset:3px}
.cover{background:var(--navy);color:white;padding:56px max(5vw,24px);border-bottom:7px solid #36b59d}
.cover .eyebrow{font-size:13px;letter-spacing:2px;color:#a9dfd6;font-weight:700}
h1{font-size:clamp(30px,4vw,52px);line-height:1.15;margin:14px 0}
.cover p{max-width:850px;color:#dceaf0}.badges{display:flex;gap:12px;flex-wrap:wrap;margin-top:24px}
.badges span{border:1px solid #7a9dad;border-radius:5px;padding:7px 14px}
button{background:#137f70;color:white;border:0;border-radius:5px;padding:11px 17px;font:inherit;cursor:pointer}
.layout{display:grid;grid-template-columns:230px minmax(0,1fr);max-width:1480px;margin:auto}
nav{position:sticky;top:0;align-self:start;padding:28px 20px;font-size:14px}
nav strong{display:block;color:var(--navy);margin:4px 0 14px}nav a{display:block;text-decoration:none;padding:9px 0}
main{min-width:0;padding:30px 36px 80px}
article{background:white;border:1px solid #dce5eb;border-radius:8px;padding:36px 42px;margin:0 0 30px;scroll-margin-top:18px}
h2{font-size:29px;line-height:1.25;color:var(--navy);margin:0 0 25px}
h3{font-size:22px;line-height:1.35;color:var(--navy);margin:36px 0 14px;border-bottom:1px solid #d9e4e9;padding-bottom:8px}
h4{color:var(--navy)}p{margin:12px 0 18px}li{margin:7px 0}
figure{margin:22px 0;background:white}figure svg{width:100%;height:auto;display:block}
figcaption{font-size:13px;color:var(--muted);text-align:center;margin-top:8px}
.table-wrap{overflow:auto;margin:20px 0}table{width:100%;border-collapse:collapse;font-size:13.5px;line-height:1.55;table-layout:fixed}
th,td{text-align:left;vertical-align:top;border:1px solid #d8e2e8;padding:11px;overflow-wrap:anywhere}
th{background:var(--navy);color:white}tr:nth-child(even){background:#f4f8fa}
code{font-family:Consolas,'Courier New',monospace;font-size:.9em;overflow-wrap:anywhere}
p code,li code,td code{background:#edf3f6;padding:2px 4px;border-radius:3px}
.code-label{background:#e3edf2;color:#294c60;border:1px solid #cfdee7;border-bottom:0;padding:5px 14px;font-size:12px;font-weight:700;margin-top:18px}
pre{margin:0 0 22px;padding:16px;background:#f5f8fa;border:1px solid #cfdee7;color:#18334d;white-space:pre-wrap;overflow-wrap:anywhere;font-size:13px;line-height:1.6}
footer{color:var(--muted);padding:0 36px 32px;text-align:center;font-size:13px}
@media(max-width:850px){.layout{display:block}nav{position:static;padding:18px 22px}nav a{display:inline-block;margin-right:18px}main{padding:10px}article{padding:24px 20px}table{font-size:12px}th,td{padding:7px}}
@media print{@page{size:A4;margin:17mm}body{background:white;font-size:10.5pt;line-height:1.5}.cover{background:white;color:#123c59;padding:0 0 20px}.cover p,.cover .eyebrow{color:#36596b}.badges span{border-color:#9baeb8}nav,button{display:none}.layout{display:block}main{padding:0}article{border:0;border-radius:0;padding:0;margin:0;break-before:page}h2,h3,h4{break-after:avoid}h2{font-size:21pt}h3{font-size:15pt}table{font-size:8pt}tr{break-inside:avoid}thead{display:table-header-group}figure{break-inside:avoid}figure svg{max-height:245mm}pre{font-size:8.5pt;white-space:pre-wrap}a{color:#203748}footer{padding-top:16px}}
"""
nav = "".join(f'<a href="#{key}">{title}</a>' for key, _, title in CHAPTERS)
articles = "\n".join(f'<article id="{key}">{render((DOCS / filename).read_text(encoding="utf-8"))}</article>' for key, filename, _ in CHAPTERS)
page = f"""<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>BankVerse Playwright Java handbook</title><style>{css}</style></head>
<body><header class="cover"><div class="eyebrow">BANKVERSE ENGINEERING HANDBOOK</div><h1>Playwright Java<br>UI and API testing</h1><p>A practical guide to the framework architecture, real banking scenarios, Playwright edge cases, and a repeatable demo. All diagrams and content work offline.</p><div class="badges"><span>Java 17</span><span>Maven and TestNG</span><span>3 browser engines</span><span>UI and HTTP API</span></div></header>
<div class="layout"><nav aria-label="Handbook contents"><strong>IN THIS HANDBOOK</strong>{nav}<button onclick="window.print()">Print or save as PDF</button></nav><main>{articles}</main></div><footer>BankVerse Java automation | Repository documentation | 19 October 2026</footer></body></html>"""
(DOCS / "handbook.html").write_text(page, encoding="utf-8")
print("Built docs/handbook.html")
