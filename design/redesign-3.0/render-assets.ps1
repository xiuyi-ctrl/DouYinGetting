# Export the home artworks: SVG -> PNG at exact pixel sizes.
# Outputs: png/<name>.png (mockups) and android/<res-name>.png (drop-in drawables).
#
# Headless Chrome clamps its window to a minimum size, so each asset is laid out at
# a fixed pixel size at (0,0) of a larger viewport, then cropped to the exact size.
param([string]$Chrome = 'C:\Program Files\Google\Chrome\Application\chrome.exe')

$dir       = Split-Path -Parent $MyInvocation.MyCommand.Path
$pngDir    = Join-Path $dir 'png'
$android   = Join-Path $dir 'android'
$assetsDir = Join-Path $dir 'assets'
$tmp       = Join-Path $env:TEMP 'dsh-dyui'
New-Item -ItemType Directory -Force $pngDir, $android, $tmp | Out-Null
Add-Type -AssemblyName System.Drawing

$assets = @(
  @{ n = 'hero';           s = 'hero.svg';           w = 700; h = 840; a = 'hero_art.png' },
  @{ n = 'douyin-feature'; s = 'douyin-feature.svg'; w = 304; h = 304; a = 'douyin_feature.png' },
  @{ n = 'xhs-feature';    s = 'xhs-feature.svg';    w = 304; h = 304; a = 'xhs_feature.png' },
  @{ n = 'douyin-nav';     s = 'douyin-nav.svg';     w = 160; h = 160; a = 'douyin_nav.png' },
  @{ n = 'xhs-nav';        s = 'xhs-nav.svg';        w = 160; h = 160; a = 'xhs_nav.png' },
  @{ n = 'home-nav';       s = 'home-nav.svg';       w = 288; h = 288; a = 'home_nav.png' }
)

foreach ($a in $assets) {
  $wrapper = Join-Path $assetsDir ("render-" + $a.n + ".html")
  $html = "<!doctype html><meta charset=`"utf-8`"><style>html,body{margin:0;padding:0;background:transparent}img{display:block;width:$($a.w)px;height:$($a.h)px}</style><img src=`"$($a.s)`">"
  Set-Content -Path $wrapper -Value $html -Encoding ASCII

  $full = Join-Path $tmp ($a.n + '.full.png')
  $url  = 'file:///' + ($wrapper -replace '\\', '/')
  $args = @('--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
            "--user-data-dir=$tmp\asset-$($a.n)", '--window-size=900,1000',
            '--force-device-scale-factor=1', '--hide-scrollbars', '--default-background-color=00000000',
            '--allow-file-access-from-files', '--virtual-time-budget=3000', "--screenshot=$full", $url)
  Start-Process -FilePath $Chrome -Wait -RedirectStandardError (Join-Path $tmp "$($a.n)-asset.err") -ArgumentList $args | Out-Null

  $out = Join-Path $pngDir ($a.n + '.png')
  if (Test-Path $full) {
    $src = [System.Drawing.Image]::FromFile($full)
    $bmp = New-Object System.Drawing.Bitmap($a.w, $a.h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
    $g.DrawImage($src, (New-Object System.Drawing.Rectangle(0, 0, $a.w, $a.h)), (New-Object System.Drawing.Rectangle(0, 0, $a.w, $a.h)), [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose(); $src.Dispose()
    $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Copy-Item $out (Join-Path $android $a.a) -Force
    "{0,-16} -> {1}x{2}  {3} bytes" -f $a.n, $a.w, $a.h, (Get-Item $out).Length
  } else {
    "{0,-16} FAILED  {1}" -f $a.n, (Get-Content (Join-Path $tmp "$($a.n)-asset.err") -Raw)
  }
}
Remove-Item (Join-Path $assetsDir '_debug.html') -ErrorAction SilentlyContinue