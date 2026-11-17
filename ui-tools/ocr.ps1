Add-Type -AssemblyName System.Runtime.WindowsRuntime

$null = [Windows.Media.Ocr.OcrEngine, Windows.Foundation, ContentType = WindowsRuntime]
$null = [Windows.Graphics.Imaging.BitmapDecoder, Windows.Foundation, ContentType = WindowsRuntime]
$null = [Windows.Storage.StorageFile, Windows.Storage, ContentType = WindowsRuntime]
$null = [Windows.Globalization.Language, Windows.Foundation, ContentType = WindowsRuntime]

$getAwaiterBaseMethod = [WindowsRuntimeSystemExtensions].GetMember('GetAwaiter') |
    Where-Object { $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' } |
    Select-Object -First 1

function Await($WinRtTask, $ResultType) {
    $getAwaiterBaseMethod.MakeGenericMethod($ResultType).Invoke($null, @($WinRtTask)).GetResult()
}

$lang = New-Object Windows.Globalization.Language 'en-US'
$engine = [Windows.Media.Ocr.OcrEngine]::TryCreateFromLanguage($lang)
if ($null -eq $engine) {
    $engine = [Windows.Media.Ocr.OcrEngine]::TryCreateFromUserProfileLanguages()
}
Write-Output ("OCR engine language: " + $engine.RecognizerLanguage.DisplayName)

$dir = Join-Path $PSScriptRoot 'slices'
$files = Get-ChildItem $dir -Filter 'big-*rgb-*.png' | Sort-Object Name
$outLines = New-Object System.Collections.Generic.List[string]

foreach ($f in $files) {
    $outLines.Add("===== " + $f.Name + " =====")
    $file = Await ([Windows.Storage.StorageFile]::GetFileFromPathAsync($f.FullName)) ([Windows.Storage.StorageFile])
    $stream = Await ($file.OpenAsync([Windows.Storage.FileAccessMode]::Read)) ([Windows.Storage.Streams.IRandomAccessStream])
    $decoder = Await ([Windows.Graphics.Imaging.BitmapDecoder]::CreateAsync($stream)) ([Windows.Graphics.Imaging.BitmapDecoder])
    $bitmap = Await ($decoder.GetSoftwareBitmapAsync()) ([Windows.Graphics.Imaging.SoftwareBitmap])
    $result = Await ($engine.RecognizeAsync($bitmap)) ([Windows.Media.Ocr.OcrResult])

    foreach ($line in $result.Lines) {
        $words = @()
        foreach ($w in $line.Words) { $words += $w.Text }
        $outLines.Add(($words -join ' '))
    }
    $stream.Dispose()
}

$outPath = Join-Path $dir 'ocr-result-en.txt'
[System.IO.File]::WriteAllLines($outPath, $outLines, (New-Object System.Text.UTF8Encoding($false)))
Write-Output ("Written: " + $outPath)
