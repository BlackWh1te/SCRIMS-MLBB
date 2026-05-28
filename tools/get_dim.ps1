Add-Type -AssemblyName System.Drawing

$logoPath = "C:\Users\Shukhrat\Desktop\New folder\git\Android\LOGO-jukebox-bg-removed.png"
if (Test-Path $logoPath) {
    $img = [System.Drawing.Image]::FromFile($logoPath)
    Write-Host "Logo: $($img.Width) x $($img.Height)"
    $img.Dispose()
} else {
    Write-Host "Logo file not found"
}

$tiersPath = "C:\Users\Shukhrat\Desktop\New folder\git\Android\teirs-removebg-preview.png"
if (Test-Path $tiersPath) {
    $img2 = [System.Drawing.Image]::FromFile($tiersPath)
    Write-Host "Tiers: $($img2.Width) x $($img2.Height)"
    $img2.Dispose()
} else {
    Write-Host "Tiers file not found"
}
