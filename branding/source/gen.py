# The mod page branding in the trailer's style (the wanted-poster title card, Algerian + Bahnschrift, shader stills
# from trailer/clips_shaders in bg/). Writes one HTML page per piece into html/: options A-D, C2 (Two Worlds, the one
# in use). render.ps1 screenshots them with headless Edge and exports C2 into branding/.
# Usage: py branding/source/gen.py; powershell -File branding/source/render.ps1
import os
from string import Template

D = os.path.dirname(os.path.abspath(__file__))
os.makedirs(os.path.join(D, 'html'), exist_ok=True)

DEFS = """
<radialGradient id="paper" cx=".5" cy=".45" r=".75">
  <stop offset="0" stop-color="#f3e2b8"/><stop offset=".7" stop-color="#e2c48c"/><stop offset="1" stop-color="#a97a3e"/>
</radialGradient>
<filter id="rough" x="-5%" y="-5%" width="110%" height="110%">
  <feTurbulence type="fractalNoise" baseFrequency=".035" numOctaves="3" seed="7" result="n"/>
  <feDisplacementMap in="SourceGraphic" in2="n" scale="16"/>
</filter>
<filter id="grain" x="0" y="0" width="100%" height="100%">
  <feTurbulence type="fractalNoise" baseFrequency=".9" numOctaves="2" seed="3" result="g"/>
  <feColorMatrix in="g" type="matrix" values="0 0 0 0 .35  0 0 0 0 .22  0 0 0 0 .08  0 0 0 .22 0" result="t"/>
  <feComposite in="t" in2="SourceGraphic" operator="in" result="tex"/>
  <feMerge><feMergeNode in="SourceGraphic"/><feMergeNode in="tex"/></feMerge>
</filter>
<filter id="ink"><feTurbulence type="fractalNoise" baseFrequency=".6" numOctaves="2" seed="11" result="n"/>
  <feDisplacementMap in="SourceGraphic" in2="n" scale="2.5"/></filter>
<filter id="shadow" x="-20%" y="-20%" width="140%" height="140%"><feDropShadow dx="6" dy="16" stdDeviation="16" flood-color="#000" flood-opacity=".75"/></filter>
<filter id="shadow_s" x="-10%" y="-20%" width="120%" height="140%"><feDropShadow dx="2" dy="5" stdDeviation="5" flood-color="#000" flood-opacity=".6"/></filter>
<filter id="glow"x="-10%" y="-30%" width="120%" height="160%"><feDropShadow dx="0" dy="6" stdDeviation="10" flood-color="#000" flood-opacity=".85"/></filter>
"""

INK = '#2a1406'
TEAL = '#17606e'

# The saucer-over-mesa sketch from the poster, centred on the saucer
SKETCH = f"""<g fill="none" stroke="{INK}" stroke-width="4" stroke-linecap="round" stroke-linejoin="round">
  <ellipse rx="70" ry="16"/><path d="M -28 -5 A 28 22 0 0 1 28 -5"/>
  <path d="M -48 12 L -90 82 M 48 12 L 90 82" stroke-dasharray="6 8"/>
  <path d="M -220 86 L -170 86 L -158 62 L -110 62 L -100 86 L 120 86 L 132 70 L 190 70 L 200 86 L 230 86"/>
</g>"""

# The same sketch with the West and the Grays spelled out: a saguaro on the mesa and a cow going up the beam
SKETCH_C = SKETCH[:-4] + f"""  <path d="M -196 86 V 30 M -196 66 H -212 V 48 M -196 56 H -180 V 38" stroke-width="7"/>
</g>
<path transform="translate(2,50) rotate(-12) scale(.8)" fill="{INK}"
  d="M -30 -12 H 22 V -17 H 37 V 2 H 22 V 12 H 16 V 24 H 9 V 12 H -16 V 24 H -23 V 12 H -30 Z"/>"""

def stamp(x, y, rot, s=1.0, word='REWARD'):
    return f"""<g fill="#7a1a12" opacity=".85" filter="url(#ink)" text-anchor="middle" transform="translate({x},{y}) rotate({rot}) scale({s})">
  <rect x="-118" y="-36" width="236" height="64" fill="none" stroke="#7a1a12" stroke-width="5"/>
  <text y="12" font-family="Algerian" font-size="40" letter-spacing="6">{word}</text></g>"""

def nails(hx, hy, r=9):
    return f"""<g fill="#5a5a5a" stroke="#222" stroke-width="2">
  <circle cx="{-hx}" cy="{-hy}" r="{r}"/><circle cx="{hx}" cy="{-hy}" r="{r}"/><circle cx="{-hx}" cy="{hy}" r="{r}"/><circle cx="{hx}" cy="{hy}" r="{r}"/></g>"""

def paper(hw, hh, inset=32):
    return f"""<g filter="url(#rough)"><rect x="{-hw}" y="{-hh}" width="{2*hw}" height="{2*hh}" fill="url(#paper)" filter="url(#grain)"/></g>
<rect x="{-hw+inset}" y="{-hh+inset}" width="{2*(hw-inset)}" height="{2*(hh-inset)}" fill="none" stroke="#3a1e0a" stroke-width="3" opacity=".7"/>
<rect x="{-hw+inset+10}" y="{-hh+inset+10}" width="{2*(hw-inset-10)}" height="{2*(hh-inset-10)}" fill="none" stroke="#3a1e0a" stroke-width="1.5" opacity=".6"/>"""

def poster(hollow=INK, rule=False, sketch=SKETCH):
    """The trailer's title-card poster, 940x660 centred on 0,0."""
    r = f'<rect x="-300" y="150" width="600" height="3" fill="{INK}" opacity=".75"/>' if rule else ''
    return f"""{paper(470, 330)}
<g filter="url(#ink)" fill="{INK}" text-anchor="middle">
  <text y="-200" font-family="Algerian" font-size="62" letter-spacing="18">WANTED</text>
  <text y="-150" font-family="Bahnschrift" font-size="18" letter-spacing="5" font-weight="600">FOR CRIMES AGAINST CATTLE AND THE NATURAL ORDER</text>
  <g transform="translate(0,-60)">{sketch}</g>
  <text y="128" font-family="Algerian" font-size="132" letter-spacing="4" fill="{hollow}">HOLLOW</text>{r}
  <text y="248" font-family="Algerian" font-size="132" letter-spacing="4">FRONTIER</text>
</g>{stamp(300, -200, 14)}{nails(440, 302)}"""

def sq_poster(hollow=INK, rule=False, sketch=SKETCH):
    """A square cut of the poster for avatars, 460x460 centred on 0,0."""
    r = f'<rect x="-150" y="96" width="300" height="2.5" fill="{INK}" opacity=".75"/>' if rule else ''
    return f"""{paper(230, 230, 22)}
<g filter="url(#ink)" fill="{INK}" text-anchor="middle">
  <text x="-52" y="-136" font-family="Algerian" font-size="48" letter-spacing="10">WANTED</text>
  <text y="-104" font-family="Bahnschrift" font-size="14" letter-spacing="4" font-weight="600">FOR CRIMES AGAINST CATTLE</text>
  <g transform="translate(0,-48) scale(.72)">{sketch}</g>
  <text y="74" font-family="Algerian" font-size="78" letter-spacing="2" fill="{hollow}">HOLLOW</text>{r}
  <text y="160" font-family="Algerian" font-size="78" letter-spacing="2">FRONTIER</text>
</g>{stamp(138, -146, 14, .52)}{nails(212, 212, 7)}"""

PAGE = Template("""<!doctype html><html><head><meta charset="utf-8"><style>
html,body{margin:0;width:${w}px;height:${h}px;overflow:hidden;background:$bg}
.bg{position:absolute;display:block}
svg{position:absolute;left:0;top:0;display:block}
</style></head><body>
$below
<svg width="$w" height="$h" viewBox="0 0 $w $h" xmlns="http://www.w3.org/2000/svg"><defs>$defs</defs>$svg</svg>
</body></html>""")

def img(src, left, top, width, extra='', clip=''):
    i = f'<img class="bg" src="../bg/{src}" style="left:{left}px;top:{top}px;width:{width}px;{extra}">'
    return f'<div class="bg" style="left:0;top:0;right:0;bottom:0;overflow:hidden;{clip}">{i}</div>' if clip else i

def shade(css):
    return f'<div class="bg" style="left:0;top:0;right:0;bottom:0;background:{css}"></div>'

def write(name, w, h, below, svg, bg='#000'):
    with open(os.path.join(D, 'html', name + '.html'), 'w', encoding='utf-8') as f:
        f.write(PAGE.substitute(w=w, h=h, below=below, defs=DEFS, svg=svg, bg=bg))

def tagline(x, y, size=30, sub=21, fill='#f3e3c0', anchor='middle', spacing=14):
    return f"""<g font-family="Bahnschrift" text-anchor="{anchor}" fill="{fill}" filter="url(#glow)">
  <text x="{x}" y="{y}" font-size="{size}" letter-spacing="{spacing}" font-weight="600">WILD WEST · STEAM AGE · HOLLOW EARTH</text>
  <text x="{x}" y="{y + sub * 2.1:.0f}" font-size="{sub}" letter-spacing="{spacing * .57:.0f}" fill-opacity=".8">A NEOFORGE MOD FOR MINECRAFT 26.3</text></g>"""

VIGNETTE = 'radial-gradient(ellipse at center, rgba(0,0,0,0) 40%, rgba(0,0,0,.7) 100%)'

# ---- A. The Poster: the trailer's title card as is, nailed up over the town at dusk -------------------------------
write('A_banner', 1920, 480,
      img('town.jpg', 0, -330, 1920, 'filter:brightness(.95) saturate(1.15)') + shade(VIGNETTE),
      f"""<g transform="translate(560,240) rotate(-2.2) scale(.62)" filter="url(#shadow)">{poster()}</g>
<g font-family="Bahnschrift" text-anchor="middle" fill="#f3e3c0" filter="url(#glow)">
  <text x="1400" y="178" font-size="46" letter-spacing="18" font-weight="600">WILD WEST</text>
  <text x="1400" y="246" font-size="46" letter-spacing="18" font-weight="600">STEAM AGE</text>
  <text x="1400" y="314" font-size="46" letter-spacing="18" font-weight="600">HOLLOW EARTH</text>
  <rect x="1180" y="346" width="440" height="2" fill="#f3e3c0" opacity=".6"/>
  <text x="1400" y="390" font-size="20" letter-spacing="8" fill-opacity=".8">A NEOFORGE MOD FOR MINECRAFT 26.3</text>
</g>""")
write('A_avatar', 512, 512,
      img('town.jpg', -330, -60, 1100, 'filter:brightness(.45) blur(3px)'),
      f'<g transform="translate(256,258) rotate(-3) scale(1.02)" filter="url(#shadow)">{sq_poster()}</g>')

# ---- B. Cinematic: a shader still, full bleed, the name in the trailer's own title type ----------------------------
TITLE = 'font-family="Algerian" fill="#f2d9a6" stroke="#2a1a0a" stroke-width="7" paint-order="stroke" stroke-linejoin="round"'
write('B_banner', 1920, 480,
      img('airship.jpg', 0, -250, 1920, 'filter:saturate(1.1)') +
      shade('linear-gradient(to right, rgba(0,0,0,0) 30%, rgba(10,5,0,.75) 70%)') + shade(VIGNETTE),
      f"""<g text-anchor="middle" filter="url(#glow)">
  <text x="1440" y="196" font-size="138" letter-spacing="6" {TITLE}>HOLLOW</text>
  <text x="1440" y="330" font-size="138" letter-spacing="6" {TITLE}>FRONTIER</text>
</g>{tagline(1440, 392, 24, 17, spacing=10)}""")
write('B_avatar', 512, 512,
      img('abduct_a.jpg', -230, 0, 910, 'filter:brightness(1.05)') +
      shade('linear-gradient(to bottom, rgba(0,0,0,0) 45%, rgba(0,0,0,.85) 100%)'),
      f"""<g text-anchor="middle" filter="url(#glow)">
  <text x="256" y="404" font-size="76" letter-spacing="3" {TITLE.replace('7', '5')}>HOLLOW</text>
  <text x="256" y="478" font-size="76" letter-spacing="3" {TITLE.replace('7', '5')}>FRONTIER</text></g>""")

# ---- C. Two Worlds: the frontier at sunset and the Hollow torn apart behind the two-tone poster -------------------
def torn(x0, x1, h, n=24, amp=10):
    import random
    rnd = random.Random(5)
    pts = [f'{x0}px 0']
    for i in range(1, n):
        t = i / n
        pts.append(f'{x0 + (x1 - x0) * t + rnd.uniform(-amp, amp):.0f}px {h * t:.0f}px')
    pts += [f'{x1}px {h}px', '100% 100%', '100% 0']
    return 'clip-path:polygon(' + ','.join(pts) + ')'
write('C_banner', 1920, 480,
      img('airship.jpg', -430, -250, 1920, 'filter:brightness(.85)') +
      img('saucer.jpg', 700, -330, 1920, 'filter:brightness(.85)', torn(1060, 860, 480)) +
      shade(VIGNETTE),
      f"""<g transform="translate(960,240) rotate(-2.2) scale(.6)" filter="url(#shadow)">{poster(TEAL, True)}</g>
<g font-family="Bahnschrift" fill="#f3e3c0" font-weight="600" font-size="24" letter-spacing="10" filter="url(#glow)">
  <text x="60" y="440">WILD WEST · STEAM AGE</text>
  <text x="1860" y="440" text-anchor="end">HOLLOW EARTH · THE GRAYS</text></g>""")
write('C_avatar', 512, 512,
      img('airship.jpg', -420, -140, 1000, 'filter:brightness(.7)') +
      img('saucer.jpg', -150, -170, 1000, 'filter:brightness(.7)', torn(300, 212, 512, 14, 6)),
      f'<g transform="translate(256,258) rotate(-3) scale(1.02)" filter="url(#shadow)">{sq_poster(TEAL, True)}</g>')

# ---- C2. Two Worlds, plainer: cowboys riding at sundown, the player's saucer over the Hollow's herd ---------------
RIDERS = 'filter:brightness(1.5) contrast(1.05) saturate(1.1);transform:scaleX(-1)' # flipped: they ride at the poster
BEAM = 'filter:brightness(1.15)'
C2_SEAM = torn(1040, 960, 480)
write('C2_banner', 1920, 480,
      img('riders.jpg', -60, -70, 1100, RIDERS) + img('flight.jpg', 820, -200, 1300, 'filter:brightness(1.05)', C2_SEAM) + shade(VIGNETTE),
      f"""<g transform="translate(960,240) rotate(-2.2) scale(.6)" filter="url(#shadow)">{poster(TEAL, True, SKETCH_C)}</g>
<g font-family="Bahnschrift" fill="#f3e3c0" font-weight="600" font-size="24" letter-spacing="10" filter="url(#glow)">
  <text x="60" y="440">WILD WEST · STEAM AGE</text>
  <text x="1860" y="440" text-anchor="end">HOLLOW EARTH · THE GRAYS</text></g>""")
# The sketch cut down for the small avatar: a shorter mesa, bolder lines and a bigger cow, so it holds up at 64px
AV_SKETCH = f"""<g fill="none" stroke="{INK}" stroke-width="5" stroke-linecap="round" stroke-linejoin="round">
  <ellipse rx="70" ry="16"/><path d="M -28 -5 A 28 22 0 0 1 28 -5"/>
  <path d="M -48 12 L -90 82 M 48 12 L 90 82" stroke-dasharray="7 8"/>
  <path d="M -145 86 L -120 86 L -112 66 L -96 66 L -90 86 L 100 86 L 108 72 L 128 72 L 134 86 L 145 86"/>
  <path d="M -128 86 V 34 M -128 68 H -142 V 50 M -128 58 H -114 V 42" stroke-width="7"/>
</g>
<path transform="translate(2,50) rotate(-12)" fill="{INK}"
  d="M -30 -12 H 22 V -17 H 37 V 2 H 22 V 12 H 16 V 24 H 9 V 12 H -16 V 24 H -23 V 12 H -30 Z"/>"""
AV_BG = (img('riders.jpg', -560, -150, 1100, RIDERS) + img('beam.jpg', 40, -10, 700, BEAM, torn(300, 212, 512, 14, 6)))
write('C2_avatar', 512, 512, AV_BG,
      f"""<g transform="translate(256,258) rotate(-3) scale(1.02)" filter="url(#shadow)">{paper(230, 230, 22)}
<g filter="url(#ink)" fill="{INK}" text-anchor="middle">
  <text y="-128" font-family="Algerian" font-size="66" letter-spacing="14">WANTED</text>
  <g transform="translate(0,-34) scale(1.4)">{AV_SKETCH}</g>
</g>{stamp(0, 162, -5, .78)}{nails(212, 212, 7)}</g>""")
write('C2_avatar_named', 512, 512, AV_BG,
      f'<g transform="translate(256,258) rotate(-3) scale(1.02)" filter="url(#shadow)">{sq_poster(TEAL, True, SKETCH_C)}</g>')
write('C2_ingame', 700, 220, '',
      f"""<g transform="translate(350,108) rotate(-1.5)" filter="url(#shadow_s)">{paper(330, 92, 16)}
<g filter="url(#ink)" fill="{INK}" text-anchor="middle">
  <g transform="translate(-178,-24) scale(.82)">{AV_SKETCH}</g>
  <text x="112" y="-6" font-family="Algerian" font-size="62" letter-spacing="2" fill="{TEAL}">HOLLOW</text>
  <rect x="-20" y="10" width="264" height="2" opacity=".75"/>
  <text x="112" y="64" font-family="Algerian" font-size="62" letter-spacing="2">FRONTIER</text>
</g>{nails(316, 78, 5)}</g>""", bg='transparent')

# ---- D. Handbill: no footage, the whole banner one long poster on barn boards ------------------------------------
BOARDS = ('repeating-linear-gradient(90deg, #2b1a0f 0 3px, #4a2f1c 3px 150px, #3d2616 150px 153px, #523422 153px 300px),'
          '#3a2414')
write('D_banner', 1920, 480,
      shade(BOARDS) + shade('radial-gradient(ellipse at center, rgba(0,0,0,0) 50%, rgba(0,0,0,.6) 100%)'),
      f"""<g transform="translate(960,240) rotate(-.8)" filter="url(#shadow)">{paper(900, 205, 26)}
<g filter="url(#ink)" fill="{INK}" text-anchor="middle">
  <g transform="translate(-600,-30) scale(1.05)">{SKETCH}</g>
  <text x="-600" y="112" font-family="Bahnschrift" font-size="18" letter-spacing="5" font-weight="600">FOR CRIMES AGAINST CATTLE</text>
  <text x="-600" y="140" font-family="Bahnschrift" font-size="18" letter-spacing="5" font-weight="600">AND THE NATURAL ORDER</text>
  <rect x="-302" y="-140" width="2.5" height="280" opacity=".6"/>
  <text x="270" y="-92" font-family="Algerian" font-size="56" letter-spacing="18">WANTED</text>
  <text x="290" y="48" font-family="Algerian" font-size="112" letter-spacing="4">HOLLOW FRONTIER</text>
  <rect x="-200" y="76" width="980" height="2.5" opacity=".7"/>
  <text x="290" y="124" font-family="Bahnschrift" font-size="26" letter-spacing="10" font-weight="600">WILD WEST · STEAM AGE · HOLLOW EARTH</text>
  <text x="290" y="158" font-family="Bahnschrift" font-size="17" letter-spacing="7">A NEOFORGE MOD FOR MINECRAFT 26.3</text>
</g>{stamp(660, -110, 12)}{nails(872, 178)}</g>""")
write('D_avatar', 512, 512,
      shade(BOARDS.replace('150px', '128px').replace('153px', '131px').replace('300px', '256px')),
      f"""<g transform="translate(256,258) rotate(-3)" filter="url(#shadow)">{paper(230, 230, 22)}
<g filter="url(#ink)" fill="{INK}" text-anchor="middle">
  <g transform="translate(0,-90) scale(.8)">{SKETCH}</g>
  <text y="150" font-family="Algerian" font-size="200" letter-spacing="6">HF</text>
</g>{stamp(100, -170, 12, .5)}{nails(212, 212, 7)}</g>""")
print('ok')
