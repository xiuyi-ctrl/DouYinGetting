# Render the redesign mockups with headless Chrome.
# Usage: powershell -File render.ps1 [-Scale 3] [-Only home]
# Each screen: run the layout probe (--dump-dom, prints overflow), then screenshot.
param(
  [string]$Chrome = 'C:\Program Files\Google\Chrome\Application\chrome.exe',
  [int]$Scale = 2,
  [string]$Only = ''
)

$dir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$out  = Join-Path $dir 'png'
$tmp  = Join-Path $env:TEMP 'dsh-dyui'
New-Item -ItemType Directory -Force $out, $tmp | Out-Null
$base = 'file:///' + ($dir -replace '\\', '/')
$screens = @('home', 'reader-scan', 'reader-results', 'records', 'settings')
if ($Only) { $screens = @($Only) }

foreach ($f in $screens) {
  $url  = "$base/$f.html"
  $profile = Join-Path $tmp "profile-$f"
  $common = @('--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
              "--user-data-dir=$profile", '--window-size=390,844', "--force-device-scale-factor=$Scale",
              '--hide-scrollbars')

  $dom = Join-Path $tmp "$f.dom"
  Start-Process -FilePath $Chrome -Wait -RedirectStandardOutput $dom -RedirectStandardError (Join-Path $tmp "$f.err") -ArgumentList ($common + @('--virtual-time-budget=5000', '--dump-dom', $url)) | Out-Null
  $probe = [regex]::Match((Get-Content $dom -Raw), '<title>(.*?)</title>').Groups[1].Value

  $png = Join-Path $out "$f.png"
  Start-Process -FilePath $Chrome -Wait -RedirectStandardError (Join-Path $tmp "$f-shot.err") -ArgumentList ($common + @("--screenshot=$png", $url)) | Out-Null

  $size = if (Test-Path $png) { (Get-Item $png).Length } else { 0 }
  "{0,-16} {1,8} bytes  {2}" -f $f, $size, $probe
}

if (-not $Only) {
  $png = Join-Path $out 'preview.png'
  $all = @('--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
           "--user-data-dir=$tmp\profile-preview", '--window-size=2130,966', '--force-device-scale-factor=1.5',
           '--hide-scrollbars', "--screenshot=$png", "$base/preview.html")
  Start-Process -FilePath $Chrome -Wait -RedirectStandardError (Join-Path $tmp 'preview-shot.err') -ArgumentList $all | Out-Null
  $psize = 0
  if (Test-Path $png) { $psize = (Get-Item $png).Length }
  "preview          {0,8} bytes" -f $psize
}