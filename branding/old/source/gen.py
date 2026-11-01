import math, random

def star_pts(cx, cy, R, r, n=6, rot=-90):
    pts = []
    for i in range(n * 2):
        a = math.radians(rot + i * 180 / n)
        rad = R if i % 2 == 0 else r
        pts.append((cx + rad * math.cos(a), cy + rad * math.sin(a)))
    return pts

def P(pts):
    return " ".join(f"{x:.1f},{y:.1f}" for x, y in pts)

DE96 = """
<linearGradient id="brass" x1="0" y1="0" x2="0.3" y2="1">
  <stop offset="0" stop-color="#fbe49a"/><stop offset=".45" stop-color="#d9a441"/>
  <stop offset="1" stop-color="#8a5517"/></linearGradient>
<linearGradient id="copper" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="#e59a5c"/><stop offset="1" stop-color="#7a3b17"/></linearGradient>
<radialGradient id="ball" cx=".35" cy=".3" r=".8">
  <stop offset="0" stop-color="#fff4c4"/><stop offset=".5" stop-color="#d9a441"/>
  <stop offset="1" stop-color="#6e4210"/></radialGradient>
<radialGradient id="rivet" cx=".35" cy=".3" r=".8">
  <stop offset="0" stop-color="#ffe0b8"/><stop offset="1" stop-color="#5a2a0e"/></radialGradient>
<linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="#3a1a4a"/><stop offset=".45" stop-color="#9a3a55"/>
  <stop offset=".8" stop-color="#f29a45"/><stop offset="1" stop-color="#ffd27a"/></linearGradient>
<radialGradient id="sunglow"><stop offset="0" stop-color="#fff6c8"/>
  <stop offset=".25" stop-color="#ffe27a" stop-opacity=".95"/>
  <stop offset="1" stop-color="#ff9a3a" stop-opacity="0"/></radialGradient>
<linearGradient id="crust" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="#2a170e"/><stop offset=".7" stop-color="#6b3d1c"/>
  <stop offset="1" stop-color="#f0b84e"/></linearGradient>
<linearGradient id="beam" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="#b6fff0" stop-opacity=".85"/>
  <stop offset="1" stop-color="#52d8ea" stop-opacity=".15"/></linearGradient>
<linearGradient id="hull" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="#f2f5f8"/><stop offset="1" stop-color="#7a828c"/></linearGradient>
<linearGradient id="ribbon" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="#b23a24"/><stop offset="1" stop-color="#6e1c10"/></linearGradient>
<filter id="shadow" x="-20%" y="-20%" width="140%" height="140%">
  <feDropShadow dx="0" dy="6" stdDeviation="6" flood-color="#000" flood-opacity=".45"/></filter>
"""

def scene(cx, cy, s, uid):
    """Hollow Earth vignette inside a disc of radius 114*s centred on cx,cy."""
    def T(x, y):  # design coords are around (256,256)
        return cx + (x - 256) * s, cy + (y - 256) * s
    def p(pts):
        return P([T(x, y) for x, y in pts])
    R = 114 * s
    out = [f'<clipPath id="disc{uid}"><circle cx="{cx}" cy="{cy}" r="{R}"/></clipPath>',
           f'<g clip-path="url(#disc{uid})">',
           f'<rect x="{cx-R}" y="{cy-R}" width="{2*R}" height="{2*R}" fill="url(#sky)"/>']
    # inner sun
    sx, sy = T(240, 262)
    out.append(f'<circle cx="{sx}" cy="{sy}" r="{95*s}" fill="url(#sunglow)"/>')
    out.append(f'<circle cx="{sx}" cy="{sy}" r="{24*s}" fill="#fff8d8"/>')
    # crust arching overhead, with hanging spires
    x0, y0 = T(120, 214); qx, qy = T(256, 148); x1, y1 = T(392, 214)
    tx0, ty0 = T(120, 120); tx1, ty1 = T(392, 120)
    out.append(f'<path d="M{x0},{y0} Q{qx},{qy} {x1},{y1} L{tx1},{ty1} L{tx0},{ty0} Z" fill="url(#crust)"/>')
    def curve_y(x):
        # y on the quadratic for given design x (symmetric, x from 120..392)
        t = (x - 120) / 272
        return (1-t)**2*214 + 2*t*(1-t)*148 + t*t*214
    for x, L, w in [(160, 22, 9), (186, 36, 10), (214, 18, 7), (284, 26, 8), (322, 40, 11), (352, 20, 8)]:
        y = curve_y(x) - 4
        out.append(f'<polygon points="{p([(x-w, y), (x+w, y), (x+1, y+L)])}" fill="#4a2a14"/>')
        out.append(f'<polyline points="{p([(x-w+2, y+2), (x+1, y+L)])}" stroke="#f0b84e" stroke-width="{1.5*s}" fill="none" opacity=".7"/>')
    # rim light under the crust
    out.append(f'<path d="M{x0},{y0} Q{qx},{qy} {x1},{y1}" stroke="#ffd27a" stroke-width="{3*s}" fill="none"/>')
    # far mesas
    out.append(f'<polygon points="{p([(130,330),(130,300),(150,296),(158,284),(196,284),(204,298),(232,302),(262,302),(270,292),(300,290),(308,300),(352,304),(360,288),(384,288),(392,300),(392,330)])}" fill="#7a3550" opacity=".9"/>')
    # tractor beam + saucer
    out.append(f'<polygon points="{p([(300,236),(320,236),(342,332),(278,332)])}" fill="url(#beam)"/>')
    hx, hy = T(310, 230)
    out.append(f'<ellipse cx="{hx}" cy="{hy-5*s}" rx="{11*s}" ry="{10*s}" fill="#9cffd8" stroke="#1d2a30" stroke-width="{2*s}"/>')
    out.append(f'<ellipse cx="{hx}" cy="{hy}" rx="{28*s}" ry="{7.5*s}" fill="url(#hull)" stroke="#1d2a30" stroke-width="{2.5*s}"/>')
    for dx in (-17, -6, 6, 17):
        lx, ly = T(310 + dx, 232)
        out.append(f'<circle cx="{lx}" cy="{ly}" r="{2.2*s}" fill="#b6fff0"/>')
    # near ground: mesa + cactus
    out.append(f'<polygon points="{p([(120,372),(120,318),(150,316),(156,304),(184,304),(190,318),(240,322),(300,326),(392,322),(392,372)])}" fill="#2a1424"/>')
    # saguaro
    cxs = 206
    out.append(f'<g fill="#2a1424" stroke="#2a1424" stroke-linecap="round" stroke-width="{9*s}">'
               f'<line x1="{T(cxs,322)[0]}" y1="{T(cxs,322)[1]}" x2="{T(cxs,282)[0]}" y2="{T(cxs,282)[1]}"/>'
               f'<polyline fill="none" stroke-width="{6*s}" points="{p([(cxs,306),(cxs-11,306),(cxs-11,292)])}"/>'
               f'<polyline fill="none" stroke-width="{6*s}" points="{p([(cxs,300),(cxs+10,300),(cxs+10,288)])}"/></g>')
    out.append('</g>')
    out.append(f'<circle cx="{cx}" cy="{cy}" r="{R-2*s}" fill="none" stroke="#000" stroke-opacity=".35" stroke-width="{6*s}"/>')
    return "\n".join(out)

def badge(cx=256, cy=256, s=1.0, uid="a"):
    """Sheriff-star emblem; design space 512x512 scaled by s around cx,cy."""
    out = [f'<g filter="url(#shadow)">']
    pts = star_pts(cx, cy, 226*s, 132*s)
    out.append(f'<polygon points="{P(pts)}" fill="url(#brass)" stroke="#3a230d" stroke-width="{9*s}" stroke-linejoin="round"/>')
    # facet shading: darken one half of each point
    for i in range(0, 12, 2):
        tip = pts[i]; inner = pts[(i+1) % 12]
        out.append(f'<polygon points="{P([(cx,cy), tip, inner])}" fill="#3a1e05" opacity=".22"/>')
    inner_star = star_pts(cx, cy, 196*s, 118*s)
    out.append(f'<polygon points="{P(inner_star)}" fill="none" stroke="#fff1c0" stroke-opacity=".55" stroke-width="{2.5*s}" stroke-linejoin="round"/>')
    # ball tips
    for i in range(0, 12, 2):
        a = math.radians(-90 + i * 30)
        bx, by = cx + 232*s*math.cos(a), cy + 232*s*math.sin(a)
        out.append(f'<circle cx="{bx:.1f}" cy="{by:.1f}" r="{20*s}" fill="url(#ball)" stroke="#3a230d" stroke-width="{6*s}"/>')
    # copper ring with rivets
    out.append(f'<circle cx="{cx}" cy="{cy}" r="{134*s}" fill="url(#copper)" stroke="#3a230d" stroke-width="{7*s}"/>')
    for k in range(12):
        a = math.radians(k * 30 + 15)
        rx, ry = cx + 124*s*math.cos(a), cy + 124*s*math.sin(a)
        out.append(f'<circle cx="{rx:.1f}" cy="{ry:.1f}" r="{4.5*s}" fill="url(#rivet)" stroke="#3a1a08" stroke-width="{1.2*s}"/>')
    out.append(f'<circle cx="{cx}" cy="{cy}" r="{114*s}" fill="#000"/>')
    out.append('</g>')
    out.append(scene(cx, cy, s, uid))
    out.append(f'<circle cx="{cx}" cy="{cy}" r="{114*s}" fill="none" stroke="#3a230d" stroke-width="{5*s}"/>')
    return "\n".join(out)

def ribbon(cx, y, w, h, text, size):
    x0, x1 = cx - w/2, cx + w/2
    tail = h * 0.9
    out = []
    # tails (behind)
    for side in (-1, 1):
        xe = x0 if side < 0 else x1
        xo = xe + side * tail * 1.1
        out.append(f'<polygon points="{P([(xe - side*tail*0.4, y+h*0.25), (xo, y+h*0.25), (xo - side*tail*0.45, y+h*0.75), (xo, y+h*1.25), (xe - side*tail*0.4, y+h*1.25)])}" fill="#5a160c" stroke="#2a0c06" stroke-width="5" stroke-linejoin="round"/>')
    out.append(f'<path d="M{x0},{y} Q{cx},{y-h*0.35} {x1},{y} L{x1},{y+h} Q{cx},{y+h-h*0.35} {x0},{y+h} Z" fill="url(#ribbon)" stroke="#2a0c06" stroke-width="6" stroke-linejoin="round"/>')
    out.append(f'<path d="M{x0+8},{y+7} Q{cx},{y-h*0.35+7} {x1-8},{y+7}" stroke="#f0b84e" stroke-width="2" fill="none" opacity=".7"/>')
    out.append(f'<path d="M{x0+8},{y+h-7} Q{cx},{y+h-h*0.35-7} {x1-8},{y+h-7}" stroke="#f0b84e" stroke-width="2" fill="none" opacity=".7"/>')
    out.append(f'<path id="rpath" d="M{x0+10},{y+h*0.72} Q{cx},{y+h*0.72-h*0.35} {x1-10},{y+h*0.72}" fill="none"/>')
    out.append(f'<text font-family="Rye" font-size="{size}" fill="#fff1c8" stroke="#2a0c06" stroke-width="1" letter-spacing="1">'
               f'<textPath href="#rpath" startOffset="50%" text-anchor="middle">{text}</textPath></text>')
    return "\n".join(out)

def svg(w, h, body, bg=""):
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}"><defs>{DE96}</defs>{bg}{body}</svg>'

# ---------- 1. avatar: emblem only ----------
emblem = svg(512, 512, badge(256, 256, 1.0))
# ---------- 2. avatar: emblem + name ribbon ----------
emblem_named = svg(512, 512, badge(256, 236, 0.9) + ribbon(256, 372, 350, 68, "HOLLOW FRONTIER", 30))

# ---------- 3. banner 1920x480 ----------
random.seed(7)
W, H = 1920, 480
b = []
b.append(f'''<linearGradient id="bsky" x1="0" y1="0" x2="0" y2="1">
 <stop offset="0" stop-color="#140b26"/><stop offset=".45" stop-color="#4a1d4e"/>
 <stop offset=".75" stop-color="#b8434f"/><stop offset="1" stop-color="#f6a04a"/></linearGradient>
<linearGradient id="title" x1="0" y1="0" x2="0" y2="1">
 <stop offset="0" stop-color="#fff2b8"/><stop offset=".5" stop-color="#e8b14a"/><stop offset="1" stop-color="#a8641c"/></linearGradient>''')
bg = [f'<rect width="{W}" height="{H}" fill="url(#bsky)"/>']
for _ in range(140):
    x, y = random.uniform(0, W), random.uniform(0, H*0.5)
    bg.append(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="{random.choice([0.8,1,1.2,1.6])}" fill="#fff" opacity="{random.uniform(.3,.9):.2f}"/>')
# setting sun glow on horizon
bg.append(f'<ellipse cx="1500" cy="440" rx="520" ry="150" fill="#ffd27a" opacity=".35"/>')
# far mesas
bg.append(f'<polygon points="0,480 0,360 80,356 96,330 210,330 226,352 420,360 470,352 480,326 600,322 616,350 900,356 1180,350 1196,318 1330,316 1346,344 1600,350 1640,334 1760,332 1776,354 1920,356 1920,480" fill="#6a2a48" opacity=".9"/>')
# airship far left
ax, ay = 230, 120
bg.append(f'<g opacity=".85"><ellipse cx="{ax}" cy="{ay}" rx="90" ry="26" fill="#2a1630"/>'
          f'<polygon points="{ax+80},{ay} {ax+112},{ay-22} {ax+112},{ay+22}" fill="#2a1630"/>'
          f'<rect x="{ax-30}" y="{ay+28}" width="56" height="14" rx="3" fill="#2a1630"/>'
          f'<line x1="{ax-24}" y1="{ay+20}" x2="{ax-20}" y2="{ay+30}" stroke="#2a1630" stroke-width="3"/>'
          f'<line x1="{ax+20}" y1="{ay+20}" x2="{ax+18}" y2="{ay+30}" stroke="#2a1630" stroke-width="3"/>'
          f'<path d="M{ax-60},{ay-36} q-14,-14 4,-26 q10,-8 22,0" stroke="#5a3a5a" stroke-width="5" fill="none" opacity=".6"/></g>')
# saucer + beam over town (right)
ux, uy = 1600, 150
bg.append(f'<polygon points="{ux-22},{uy+10} {ux+22},{uy+10} {ux+90},{H-60} {ux-90},{H-60}" fill="url(#beam)" opacity=".75"/>')
bg.append(f'<ellipse cx="{ux}" cy="{uy-10}" rx="26" ry="22" fill="#9cffd8" stroke="#15202a" stroke-width="4"/>'
          f'<ellipse cx="{ux}" cy="{uy}" rx="70" ry="17" fill="url(#hull)" stroke="#15202a" stroke-width="5"/>')
for dx in (-46, -16, 16, 46):
    bg.append(f'<circle cx="{ux+dx}" cy="{uy+4}" r="5" fill="#b6fff0"/>')
# a cow being lifted
bg.append(f'<g transform="translate({ux-14},{uy+150}) rotate(-12)" fill="#1a0e1c"><rect x="0" y="0" width="34" height="16" rx="4"/><rect x="30" y="-6" width="12" height="12" rx="3"/><rect x="3" y="14" width="4" height="10"/><rect x="26" y="14" width="4" height="10"/></g>')
# frontier town silhouette
town = "#1a0e1c"
g = [f'<rect x="0" y="420" width="{W}" height="60" fill="{town}"/>']
x = 1180
specs = [(70, 70, 'ff'), (90, 96, 'ff'), (60, 60, 'gable'), (110, 110, 'ff'), (80, 80, 'steeple'), (70, 76, 'ff'), (95, 92, 'ff'), (60, 64, 'gable'), (90, 100, 'tower'), (80, 70, 'ff')]
for w, h, kind in specs:
    top = 420 - h
    if kind == 'ff':
        g.append(f'<rect x="{x}" y="{top}" width="{w}" height="{h}" fill="{town}"/>')
        g.append(f'<rect x="{x-4}" y="{top+h*0.45}" width="{w+8}" height="5" fill="{town}"/>')
        for wx in range(x+12, x+w-12, 22):
            if random.random() < .7:
                g.append(f'<rect x="{wx}" y="{top+14}" width="10" height="14" fill="#ffc768" opacity=".85"/>')
    elif kind == 'gable':
        g.append(f'<polygon points="{x},{420} {x},{top+20} {x+w/2},{top-8} {x+w},{top+20} {x+w},{420}" fill="{town}"/>')
        g.append(f'<rect x="{x+w/2-6}" y="{top+30}" width="12" height="16" fill="#ffc768" opacity=".8"/>')
    elif kind == 'steeple':
        g.append(f'<rect x="{x}" y="{top+30}" width="{w}" height="{h-30}" fill="{town}"/>')
        g.append(f'<polygon points="{x+w/2-16},{top+30} {x+w/2},{top-30} {x+w/2+16},{top+30}" fill="{town}"/>')
        g.append(f'<rect x="{x+w/2-2}" y="{top-48}" width="4" height="22" fill="{town}"/><rect x="{x+w/2-9}" y="{top-42}" width="18" height="4" fill="{town}"/>')
    elif kind == 'tower':  # water tower
        g.append(f'<rect x="{x+14}" y="{top+10}" width="{w-28}" height="44" rx="6" fill="{town}"/>')
        g.append(f'<polygon points="{x+10},{top+12} {x+w/2},{top-12} {x+w-10},{top+12}" fill="{town}"/>')
        for lx in (x+20, x+w-24):
            g.append(f'<rect x="{lx}" y="{top+50}" width="5" height="{h-50}" fill="{town}"/>')
        g.append(f'<line x1="{x+22}" y1="{top+60}" x2="{x+w-22}" y2="{420}" stroke="{town}" stroke-width="3"/>')
    x += w + 6
# cacti on left desert
for cx_, hgt in [(70, 70), (545, 46)]:
    g.append(f'<g stroke="{town}" stroke-linecap="round" fill="none"><line x1="{cx_}" y1="425" x2="{cx_}" y2="{425-hgt}" stroke-width="14"/>'
             f'<polyline points="{cx_},{425-hgt*0.45} {cx_-18},{425-hgt*0.45} {cx_-18},{425-hgt*0.75}" stroke-width="9"/>'
             f'<polyline points="{cx_},{425-hgt*0.6} {cx_+16},{425-hgt*0.6} {cx_+16},{425-hgt*0.85}" stroke-width="9"/></g>')
# rider on horse, mid-left
g.append(f'''<g transform="translate(215,360)" fill="{town}">
 <ellipse cx="0" cy="30" rx="34" ry="14"/><polygon points="26,24 50,0 60,4 44,32"/><polygon points="50,0 66,2 64,10 54,8"/>
 <rect x="-28" y="36" width="6" height="28"/><rect x="-14" y="38" width="6" height="26"/><rect x="12" y="38" width="6" height="26"/><rect x="24" y="36" width="6" height="28"/>
 <path d="M-32,26 q-16,4 -20,22" stroke="{town}" stroke-width="5" fill="none"/>
 <rect x="-6" y="0" width="12" height="22" rx="4"/><circle cx="0" cy="-6" r="7"/><ellipse cx="0" cy="-11" rx="14" ry="3"/><rect x="-6" y="-19" width="12" height="9" rx="2"/>
</g>''')
bg += g
# title block (left-centre) with emblem
title = []
title.append(badge(420, 215, 0.62, uid="b"))
title.append(f'<g filter="url(#shadow)"><text x="600" y="222" font-family="Rye" font-size="96" fill="url(#title)" stroke="#2a1408" stroke-width="5" paint-order="stroke" letter-spacing="3">HOLLOW</text>'
             f'<text x="600" y="330" font-family="Rye" font-size="96" fill="url(#title)" stroke="#2a1408" stroke-width="5" paint-order="stroke" letter-spacing="3">FRONTIER</text></g>')
title.append(f'<text x="606" y="385" font-family="Special Elite" font-size="25" fill="#ffe7c0" letter-spacing="2" opacity=".95">Wild West · Steam Age · Hollow Earth</text>')
banner = svg(W, H, "".join(b) + "".join(bg) + "".join(title))

# ---------- 4. in-game mod-list logo (transparent, 3:1) ----------
ig = (badge(110, 110, 0.4, uid="c") +
      f'<g filter="url(#shadow)"><text x="222" y="104" font-family="Rye" font-size="70" fill="url(#title2)" stroke="#2a1408" stroke-width="4" paint-order="stroke" letter-spacing="2">HOLLOW</text>'
      f'<text x="222" y="176" font-family="Rye" font-size="70" fill="url(#title2)" stroke="#2a1408" stroke-width="4" paint-order="stroke" letter-spacing="2">FRONTIER</text></g>')
ingame = svg(700, 220, '<linearGradient id="title2" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff2b8"/><stop offset=".5" stop-color="#e8b14a"/><stop offset="1" stop-color="#a8641c"/></linearGradient>' + ig)

import pathlib
d = pathlib.Path(__file__).parent
for name, s_ in [("emblem", emblem), ("emblem_named", emblem_named), ("banner", banner), ("ingame", ingame)]:
    (d / f"{name}.svg").write_text(s_)
print("ok")
