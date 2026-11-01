# Cuts the trailer together from trailer/clips (filmed by `gradlew runTrailer`) following trailer/edit.csv, scores it
# with tools/music/TrailerScore.java (original, synthesized, cued to the cut) and, up to the saucer, a western recording
# from trailer/music (Kevin MacLeod's "Still Pickin'", CC BY 4.0: credit it wherever the trailer is posted), lays the
# game's own sounds over it from trailer/sfx.csv, and writes trailer/out/hollow_frontier_trailer.mp4.
#
#   sfx.csv    one row per sound: cue (a name from edit.csv's events), offset (seconds from it), sound ("mc:mob/cow/say2"
#              from the game's assets, "mod:block/saloon_piano/the_entertainer" from the mod's), gain, pitch, pan (-1 to
#              1), and optionally from and len (seconds of a longer sound to take)
#
#   edit.csv   one row per shot, in cut order: number, name, in (seconds into its clip), duration, caption, and optionally
#              zoom, zx, zy (crop in by zoom, centred at zx, zy as fractions of the frame);
#              events   "name@s;name@s": a score cue at that many seconds into the clip (gun@2.7 is the cold open's shot)
#              cap_at, cap_len   when the caption shows, from the start of the shot (default: all of it)
#              flash    1 to come in from a white flash
#              The name "black" is a beat of black; "title" is the title card: with logo (a transparent PNG under
#              trailer/) and plate (a clip name, its in-point the row's in), the logo eases in over that plate.
#
# Usage: powershell -File trailer/cut.ps1
#        powershell -File trailer/cut.ps1 -ClipDir clips_shaders -OutName hollow_frontier_trailer_shaders   (the -Pshaders shoot)
param([string]$ClipDir = 'clips', [string]$OutName = 'hollow_frontier_trailer')
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$project = Split-Path $root -Parent
$clips = Join-Path $root $ClipDir
$out = Join-Path $root 'out'
New-Item -ItemType Directory -Force $out | Out-Null
$rows = Import-Csv (Join-Path $root 'edit.csv')
$inv = [Globalization.CultureInfo]::InvariantCulture

# The title and the western captions in Algerian, the Grays' and the Hollow's in Bahnschrift
$western = "C\:/Windows/Fonts/ALGER.TTF"
$modern = "C\:/Windows/Fonts/bahnschrift.ttf"

function Num([double]$x) { $x.ToString('0.###', $inv) }

$inputs = @()
$chains = @()
$labels = ''
$captions = @()
$cues = [ordered]@{}
$t = 0.0
$i = 0
$nIn = 0 # inputs so far (a title card takes two: its plate and its logo)
foreach ($row in $rows) {
    $dur = [double]$row.duration
    $label = "v$i"
    $src = $nIn
    $file = Join-Path $clips ('{0:00}_{1}.mp4' -f [int]$row.number, $row.name)
    $flash = if ($row.flash -eq '1') { ",fade=t=in:st=0:d=0.3:color=white" } else { '' }
    if ($row.name -eq 'title' -and $row.logo) {
        # The title card: a background plate (a clip, darkened and vignetted), and the logo (a transparent PNG) easing in
        # over it, settling from a touch larger as it fades up
        $plate = Join-Path $clips "$($row.plate).mp4"
        $inputs += @('-i', $plate, '-loop', '1', '-t', (Num $dur), '-i', (Join-Path $root $row.logo))
        $nIn += 2
        $chains += "[$($src):v]trim=start=$($row.in):duration=$dur,setpts=PTS-STARTPTS,fps=60,scale=1920:1080:flags=lanczos," +
            "eq=brightness=-0.08:saturation=0.85,vignette=PI/4$flash,setsar=1[plate$i];" +
            "[$($src + 1):v]format=rgba,fps=60,scale=w='1920*(1+0.06*max(0\,1-t/0.8))':h=-1:eval=frame,fade=t=in:st=0.15:d=0.6:alpha=1[logo$i];" +
            "[plate$i][logo$i]overlay=x=(W-w)/2:y=(H-h)/2:eval=frame:shortest=1,setsar=1[$label]"
        foreach ($event in "$($row.events)".Split(';', [StringSplitOptions]::RemoveEmptyEntries)) {
            $name, $at = $event.Split('@')
            $cues[$name] = $t + [double]$at
        }
        $labels += "[$label]"
        $t += $dur
        $i++
        continue
    }
    $nIn++
    if ($row.name -eq 'title') {
        # The title card without a logo: the name slammed in (big, settling), the line under it fading up
        $inputs += @('-f', 'lavfi', '-t', (Num $dur), '-i', "color=c=black:s=1920x1080:r=60")
        $chains += "[$($src):v]drawtext=fontfile='$western':text='HOLLOW FRONTIER':fontsize='160+70*max(0,1-t/0.14)':fontcolor=0xF2D9A6:borderw=4:bordercolor=0x2A1A0A:x=(w-tw)/2:y=(h-th)/2-60," +
            "drawtext=fontfile='$modern':text='A NEOFORGE MOD FOR MINECRAFT 26.3':fontsize=40:fontcolor=white:x=(w-tw)/2:y=(h/2)+90:alpha='min(1,max(0,(t-0.6)/0.5))'$flash,setsar=1[$label]"
    } elseif ($row.name -eq 'black') {
        $inputs += @('-f', 'lavfi', '-t', (Num $dur), '-i', "color=c=black:s=1920x1080:r=60")
        $chains += "[$($src):v]setsar=1[$label]"
    } elseif (Test-Path $file) {
        $inputs += @('-i', $file)
        # zoom (optional): crop to the middle 1/zoom of the frame, centred on (zx, zy) as fractions of it, and blow it back up
        $zoom = if ($row.zoom) { [double]$row.zoom } else { 1.0 }
        $crop = ''
        if ($zoom -gt 1.0) {
            $zx = if ($row.zx) { [double]$row.zx } else { 0.5 }
            $zy = if ($row.zy) { [double]$row.zy } else { 0.5 }
            $crop = "crop=iw/$($zoom):ih/$($zoom):(iw-iw/$($zoom))*$($zx):(ih-ih/$($zoom))*$($zy),"
        }
        $chains += "[$($src):v]trim=start=$($row.in):duration=$dur,setpts=PTS-STARTPTS,fps=60,$($crop)scale=1920:1080:flags=lanczos$flash,setsar=1[$label]"
    } else {
        Write-Warning "missing $file; a slate stands in for it"
        $inputs += @('-f', 'lavfi', '-t', (Num $dur), '-i', "color=c=0x202020:s=1920x1080:r=60")
        $chains += "[$($src):v]drawtext=fontfile='$modern':text='$($row.number) $($row.name)':fontsize=64:fontcolor=gray:x=(w-tw)/2:y=(h-th)/2,setsar=1[$label]"
    }
    # Score cues: seconds into the clip, as seconds into the cut
    if ($row.events) {
        foreach ($event in $row.events.Split(';')) {
            $name, $at = $event.Split('@')
            $cues[$name] = $t + [double]$at - [double]$row.in
        }
    }
    if ($row.caption) {
        $grays = [int]$row.number -ge 17
        $font = if ($grays) { $modern } else { $western }
        $size = if ($grays) { 92 } else { 112 }
        $a = $t + $(if ($row.cap_at) { [double]$row.cap_at } else { 0.15 })
        $b = if ($row.cap_len) { $a + [double]$row.cap_len } else { $t + $dur - 0.1 }
        $captions += "drawtext=fontfile='$font':text='$($row.caption)':fontsize=$($size):fontcolor=white:borderw=3:bordercolor=0x000000AA:" +
            "x=(w-tw)/2:y=h*0.72:enable='between(t,$(Num $a),$(Num $b))':alpha='min(1,min((t-$(Num $a))/0.15,($(Num $b)-t)/0.15))'"
    }
    $labels += "[$label]"
    $t += $dur
    $i++
}
$total = $t
$cues['end'] = $total
Write-Host ("{0} shots, {1:0.0} s" -f $rows.Count, $total)

$video = ($chains -join ';') + ";$($labels)concat=n=$($rows.Count):v=1:a=0[cut];[cut]fade=t=in:st=0:d=0.4"
if ($captions.Count -gt 0) { $video += ',' + ($captions -join ',') }
$video += '[v]'

# The score, rendered to the cut's cues
$score = Join-Path $root 'music\score.wav'
$cueArgs = $cues.GetEnumerator() | ForEach-Object { "$($_.Key)=$(Num $_.Value)" }
Write-Host "cues: $($cueArgs -join ' ')"
& java (Join-Path $project 'tools\music\TrailerScore.java') $score @cueArgs
if ($LASTEXITCODE -ne 0) { throw "TrailerScore failed ($LASTEXITCODE)" }
$n = $nIn
$inputs += @('-i', $score)
$audio = ";[$($n):a]atrim=0:$(Num $total),asetpts=PTS-STARTPTS[score]"

# The frontier's music: a western recording (trailer/music is local only), from the top until the saucer passes over,
# where it stops dead. It's stretched (pitch kept) from its tempo to the cut's 120 bpm and trimmed to its first beat, so
# its beats land on the cut's half seconds (its first is at 0.0 s; its pulse is every 0.3 s, in half time 100 bpm).
$westernFile = Join-Path $root 'music\Still%20Pickin.mp3' # "Still Pickin'", Kevin MacLeod (incompetech.com), CC BY 4.0
$westernBpm = 100
$westernFirstBeat = 0.0
$westernGain = 1.15
if (-not (Test-Path $westernFile)) { throw "missing $westernFile" }
$inputs += @('-i', $westernFile)
$pass = [double]$cues['pass']
$audio += ";[$($n + 1):a]aresample=48000,atrim=start=$(Num $westernFirstBeat),asetpts=PTS-STARTPTS," +
    "rubberband=tempo=$(Num (120 / $westernBpm)):transients=crisp:channels=together,atrim=0:$(Num $pass),asetpts=PTS-STARTPTS," +
    "afade=t=in:d=0.3,afade=t=out:st=$(Num ($pass - 0.03)):d=0.03,volume=$(Num $westernGain)[western]"
$n++

# The game's own sounds over the score (trailer/sfx.csv): "mc:" from the game's assets (as the dev run downloaded them),
# "mod:" from the mod's, each pitched like the game pitches a sound (speed and pitch together), panned and placed at
# its cue plus its offset; "from"/"len" take a stretch of a longer one, faded out
$assets = Join-Path $env:USERPROFILE '.gradle\caches\neoformruntime\assets'
$index = Get-ChildItem (Join-Path $assets 'indexes') -Filter *.json -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$objects = if ($index) { (Get-Content $index.FullName -Raw | ConvertFrom-Json).objects } else { $null }
function SoundFile([string]$sound) {
    if ($sound.StartsWith('mc:')) {
        if (-not $objects) { return $null }
        $entry = $objects."minecraft/sounds/$($sound.Substring(3)).ogg"
        if (-not $entry) { return $null }
        return Join-Path $assets "objects\$($entry.hash.Substring(0, 2))\$($entry.hash)"
    }
    if ($sound.StartsWith('mod:')) {
        return Join-Path $project "src\main\resources\assets\hollowfrontier\sounds\$($sound.Substring(4)).ogg"
    }
    return Join-Path $root $sound
}
$mix = '[score][western]'
$k = 0
foreach ($fx in (Import-Csv (Join-Path $root 'sfx.csv'))) {
    if (-not $cues.Contains($fx.cue)) {
        continue
    }
    $file = SoundFile $fx.sound
    if (-not $file -or -not (Test-Path $file)) {
        Write-Warning "no sound $($fx.sound)"
        continue
    }
    $at = [double]$cues[$fx.cue] + [double]$fx.offset
    if ($at -lt 0 -or $at -ge $total) {
        continue
    }
    $inputs += @('-i', $file)
    $pitch = if ($fx.pitch) { [double]$fx.pitch } else { 1.0 }
    $pan = if ($fx.pan) { [double]$fx.pan } else { 0.0 }
    $gl = [Math]::Cos(($pan + 1) * [Math]::PI / 4)
    $gr = [Math]::Sin(($pan + 1) * [Math]::PI / 4)
    $trim = ''
    if ($fx.len) {
        $from = if ($fx.from) { [double]$fx.from } else { 0.0 }
        $trim = "atrim=start=$(Num $from):duration=$(Num ([double]$fx.len)),asetpts=PTS-STARTPTS,afade=t=in:d=0.03,afade=t=out:st=$(Num ([double]$fx.len - 0.35)):d=0.35,"
    }
    $ms = [int]($at * 1000)
    $audio += ";[$($n + 1 + $k):a]aformat=sample_fmts=fltp:channel_layouts=mono,aresample=48000,$($trim)asetrate=$(Num (48000 * $pitch)),aresample=48000," +
        "volume=$(Num ([double]$fx.gain)),pan=stereo|c0=$(Num $gl)*c0|c1=$(Num $gr)*c0,adelay=$($ms):all=1[fx$k]"
    $mix += "[fx$k]"
    $k++
}
$audio += ";$($mix)amix=inputs=$($k + 2):normalize=0:duration=first,alimiter=limit=0.89:level=disabled[a]"
Write-Host "$k sound effects"

$target = Join-Path $out "$OutName.mp4"
$filter = $video + $audio
$ffargs = @('-hide_banner', '-loglevel', 'warning', '-y') + $inputs + @('-filter_complex', $filter, '-map', '[v]', '-map', '[a]',
    '-c:v', 'libx264', '-preset', 'slow', '-crf', '17', '-pix_fmt', 'yuv420p', '-c:a', 'aac', '-b:a', '256k', '-movflags', '+faststart', $target)
& ffmpeg @ffargs
if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed ($LASTEXITCODE)" }
Write-Host "wrote $target"
