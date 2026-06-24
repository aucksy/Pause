# Renders the Play Store assets from play-store/src/*.html to play-store/*.png using Edge headless.
# Run from anywhere: powershell -NoProfile -ExecutionPolicy Bypass -File render.ps1
$ErrorActionPreference = 'Stop'
$edge = "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
if (-not (Test-Path $edge)) { $edge = "C:\Program Files\Microsoft\Edge\Application\msedge.exe" }
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$src  = Join-Path $root 'src'
$udd  = Join-Path $env:TEMP ("edge-shot-" + [guid]::NewGuid().ToString('N'))

$jobs = @(
  @{ html='icon.html';            out='icon-512.png';                  w=512;  h=512  },
  @{ html='feature.html';         out='feature-graphic-1024x500.png';  w=1024; h=500  },
  @{ html='shot1-home.html';      out='screenshot-1-home.png';         w=1080; h=1920 },
  @{ html='shot2-overlay.html';   out='screenshot-2-overlay.png';      w=1080; h=1920 },
  @{ html='shot3-private.html';   out='screenshot-3-private.png';      w=1080; h=1920 },
  @{ html='shot4-personalize.html'; out='screenshot-4-personalize.png'; w=1080; h=1920 }
)

foreach ($j in $jobs) {
  $inUri = ([System.Uri](Join-Path $src $j.html)).AbsoluteUri
  $outPath = Join-Path $root $j.out
  if (Test-Path $outPath) { Remove-Item $outPath -Force }
  & $edge --headless=new --disable-gpu --no-first-run --no-default-browser-check `
    --hide-scrollbars --force-device-scale-factor=1 --user-data-dir="$udd" `
    --window-size=$($j.w),$($j.h) --screenshot="$outPath" $inUri | Out-Null
  if (Test-Path $outPath) {
    $kb = [int]((Get-Item $outPath).Length / 1KB)
    Write-Output ("OK  {0,-32} {1}x{2}  {3} KB" -f $j.out, $j.w, $j.h, $kb)
  } else {
    Write-Output ("FAIL {0}" -f $j.out)
  }
}
if (Test-Path $udd) { Remove-Item $udd -Recurse -Force -ErrorAction SilentlyContinue }
