param([string]$Chrome='C:\Program Files\Google\Chrome\Application\chrome.exe')
$ErrorActionPreference='Stop'
$dir=Split-Path -Parent $MyInvocation.MyCommand.Path
$tmp=Join-Path $env:TEMP 'xiuyi-dialogs-v1'
New-Item -ItemType Directory -Force $tmp | Out-Null
$shots=@(
 @{name='dialogs-light';board='common';theme='light';size='1400,1120'},
 @{name='dialogs-dark';board='common';theme='dark';size='1400,1120'},
 @{name='details-preview';board='detail';theme='light';size='1400,910'},
 @{name='waiting-about';board='extra';theme='light';size='1200,750'}
)
foreach($shot in $shots){
 $url='file:///'+($dir -replace '\\','/')+'/index.html?capture=1&board='+$shot.board+'&theme='+$shot.theme
 $args=@('--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--hide-scrollbars',"--user-data-dir=$tmp\$($shot.name)","--window-size=$($shot.size)",'--force-device-scale-factor=1','--virtual-time-budget=5000')
 Start-Process -FilePath $Chrome -WindowStyle Hidden -Wait -ArgumentList ($args+@('--dump-dom',$url)) -RedirectStandardOutput "$tmp\$($shot.name).dom" -RedirectStandardError "$tmp\$($shot.name).log" | Out-Null
 $probe=[regex]::Match((Get-Content "$tmp\$($shot.name).dom" -Raw),'<title>(.*?)</title>').Groups[1].Value
 Start-Process -FilePath $Chrome -WindowStyle Hidden -Wait -ArgumentList ($args+@("--screenshot=$dir\$($shot.name).png",$url)) -RedirectStandardError "$tmp\$($shot.name)-shot.log" | Out-Null
 "$($shot.name): $probe"
}
