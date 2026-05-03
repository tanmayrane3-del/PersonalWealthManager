$root = 'D:\AndroidAppDevFiles\PersonalWealthManager\app\src\main\res'
$fixed = 0

Get-ChildItem -Path $root -Recurse -Filter "*.xml" | ForEach-Object {
    $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        $noBom = $bytes[3..($bytes.Length - 1)]
        [System.IO.File]::WriteAllBytes($_.FullName, $noBom)
        Write-Host "Fixed: $($_.Name)"
        $fixed++
    }
}

Write-Host ""
Write-Host "Total files fixed: $fixed"
