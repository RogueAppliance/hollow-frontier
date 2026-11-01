# Cuts the vertical short (9:16, for Shorts and TikTok) from the finished trailer: a few stretches of it, joined on its bar
# lines (the cut is on a 120 bpm grid, a bar every 2 s) so the music jumps on the beat, each shot cropped to a 9:16 window
# around its subject and blown up to 1080x1920. Run cut.ps1 first.
#
# Usage: powershell -File trailer/short.ps1 [-Source hollow_frontier_trailer_shaders] [-OutName hollow_frontier_short]
param([string]$Source = 'hollow_frontier_trailer_shaders', [string]$OutName = 'hollow_frontier_short')
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$src = Join-Path $root "out\$Source.mp4"
$target = Join-Path $root "out\$OutName.mp4"
$inv = [Globalization.CultureInfo]::InvariantCulture
function Num([double]$x) { $x.ToString('0.###', $inv) }

# Stretches of the trailer (seconds), each a run of shots: from, to, and the crop's centre (a fraction across) per shot.
# The music is cut only where stretches meet.
$stretches = @(
    @{ shots = @(, @(0, 4, 0.50)) },                                        # the town from the air (the comma keeps it a list of one)
    @{ shots = @(@(16, 17.5, 0.55), @(17.5, 20, 0.50), @(20, 22, 0.58)) },  # loading, the shootout, the dynamite
    @{ shots = @(@(32, 39, 0.50), @(39, 40, 0.50), @(40, 42, 0.50)) },      # the abduction, black, the Mothership
    @{ shots = @(@(62, 64, 0.50), @(64, 68, 0.36), @(68, 74, 0.50, 'fit'), @(74, 78, 0.52)) } # the Archon, the saucer, the title, Mr. Gray
)
# 'fit' shots (the title card: the poster and its tagline are wider than a 9:16 window) keep a wider window, 1100 across (the tagline's),
# fitted to the width between black bars

$chains = @()
$labels = ''
$audio = @()
$alabels = ''
$v = 0
$a = 0
foreach ($stretch in $stretches) {
    foreach ($shot in $stretch.shots) {
        $from, $to, $cx, $mode = $shot
        # A 608 x 1080 window (9:16 of the frame's height), its centre at cx, kept inside the frame
        $w = if ($mode -eq 'fit') { 1100 } else { 608 }
        $x = [Math]::Min([Math]::Max(0, [int](1920 * $cx - $w / 2)), 1920 - $w)
        $fit = if ($mode -eq 'fit') { "scale=1080:-2:flags=lanczos,pad=1080:1920:(ow-iw)/2:(oh-ih)/2:black" } else { "scale=1080:1920:flags=lanczos" }
        $chains += "[0:v]trim=start=$(Num $from):end=$(Num $to),setpts=PTS-STARTPTS,crop=$($w):1080:$($x):0,$fit,setsar=1[v$v]"
        $labels += "[v$v]"
        $v++
    }
    $first = $stretch.shots[0][0]
    $last = $stretch.shots[-1][1]
    # A few hundredths' fade at each join, so the music's jump doesn't click
    $audio += "[0:a]atrim=start=$(Num $first):end=$(Num $last),asetpts=PTS-STARTPTS,afade=t=in:d=0.04,afade=t=out:st=$(Num ($last - $first - 0.06)):d=0.06[a$a]"
    $alabels += "[a$a]"
    $a++
}
$filter = ($chains + $audio) -join ';'
$filter += ";$($labels)concat=n=$($v):v=1:a=0,fade=t=in:st=0:d=0.3[v];$($alabels)concat=n=$($a):v=0:a=1[a]"
& ffmpeg -hide_banner -loglevel warning -y -i $src -filter_complex $filter -map '[v]' -map '[a]' `
    -c:v libx264 -preset slow -crf 20 -pix_fmt yuv420p -r 60 -c:a aac -b:a 192k -movflags +faststart $target
if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed ($LASTEXITCODE)" }
Write-Host "wrote $target"
