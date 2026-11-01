# Screenshots each page in html/ with headless Edge into png/, lays the options out on sheet.png, and renders the
# chosen set into branding/ at every published size.
# Usage: py branding/source/gen.py; powershell -File branding/source/render.ps1
$ErrorActionPreference = 'Continue' # Edge reports "bytes written" on stderr
$D = $PSScriptRoot
$edge = 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
New-Item -ItemType Directory -Force "$D\png" | Out-Null
foreach ($f in Get-ChildItem "$D\html" -Filter *.html) {
    $size = if ($f.BaseName -like '*_banner') { '1920,480' } elseif ($f.BaseName -like '*_ingame') { '700,220' } else { '512,512' }
    $out = "$D\png\$($f.BaseName).png"
    Remove-Item $out -ErrorAction SilentlyContinue
    & $edge --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 --allow-file-access-from-files `
        --default-background-color=00000000 `
        --virtual-time-budget=3000 "--window-size=$size" "--screenshot=$out" $f.FullName 2>$null | Out-Null
    while (-not (Test-Path $out)) { Start-Sleep -Milliseconds 200 }
}
# The sheet: per option, the banner, then the avatar at 256 and at 64 (how it shows in a mod list)
$opts = 'A', 'B', 'C', 'D'
$in = @(); $f = ''; $i = 0; $rows = @()
foreach ($o in $opts) {
    $in += '-i', "$D\png\${o}_banner.png", '-i', "$D\png\${o}_avatar.png"
    $f += "[$($i):v]scale=1440:360[b$o];[$($i+1):v]split[a1$o][a2$o];[a1$o]scale=256:256[x$o];[a2$o]scale=64:64[y$o];" +
          "color=c=0x1a1a1a:s=1840x400[c$o];[c$o][b$o]overlay=20:20[t$o];[t$o][x$o]overlay=1480:20[u$o];[u$o][y$o]overlay=1756:212," +
          "drawtext=fontfile='C\:/Windows/Fonts/bahnschrift.ttf':text='$o':fontsize=60:fontcolor=white:x=1760:y=40[r$o];"
    $rows += "[r$o]"; $i += 2
}
$f += ($rows -join '') + "vstack=inputs=$($opts.Count)"
ffmpeg -v error -y @in -filter_complex $f -frames:v 1 "$D\sheet.png"

# The chosen set (C2, Two Worlds), rendered again at each size it's published at into branding/
$final = Split-Path $D -Parent
$exports = @(
    @('C2_banner', '1920,480', 1, 'curseforge_banner_1920x480'),
    @('C2_ingame', '700,220', 1, 'ingame_logo_700x220'))
foreach ($s in 400, 512, 1024) {
    $exports += , @('C2_avatar', '512,512', ($s / 512), "avatar_${s}x$s")
    $exports += , @('C2_avatar_named', '512,512', ($s / 512), "avatar_named_${s}x$s")
}
foreach ($e in $exports) {
    $out = "$final\$($e[3]).png"
    Remove-Item $out -ErrorAction SilentlyContinue
    & $edge --headless=new --disable-gpu --hide-scrollbars "--force-device-scale-factor=$($e[2].ToString([Globalization.CultureInfo]::InvariantCulture))" `
        --allow-file-access-from-files --default-background-color=00000000 --virtual-time-budget=3000 `
        "--window-size=$($e[1])" "--screenshot=$out" "$D\html\$($e[0]).html" 2>$null | Out-Null
    while (-not (Test-Path $out)) { Start-Sleep -Milliseconds 200 }
}
