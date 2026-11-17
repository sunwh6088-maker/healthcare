Add-Type -AssemblyName System.Drawing
$dir = Join-Path $PSScriptRoot 'slices'
$files = Get-ChildItem $dir -Filter '*.png' | Where-Object { $_.Name -notlike 'conv-*' } | Sort-Object Name
$i = 0
foreach ($f in $files) {
  $img = [System.Drawing.Image]::FromFile($f.FullName)
  if ($img.Height -ge 100) {
    $out = Join-Path $dir ("conv-{0:D2}.png" -f $i)
    $img.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Output ("{0} -> {1} ({2}x{3})" -f $f.Name, $out, $img.Width, $img.Height)
    $i++
  }
  $img.Dispose()
}
Write-Output "Done"
