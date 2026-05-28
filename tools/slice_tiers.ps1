Add-Type -AssemblyName System.Drawing

$tiersPath = "C:\Users\Shukhrat\Desktop\New folder\git\Android\teirs-removebg-preview.png"
$outDir = "C:\Users\Shukhrat\Desktop\New folder\git\Android\app\src\main\res\drawable"

$img = [System.Drawing.Image]::FromFile($tiersPath)
$w = $img.Width / 4
$h = $img.Height / 2

$names = @("tier_bronze", "tier_silver", "tier_gold", "tier_grandmaster", "tier_epic", "tier_legend", "tier_mythic")
$idx = 0

for ($y = 0; $y -lt 2; $y++) {
    for ($x = 0; $x -lt 4; $x++) {
        if ($idx -ge $names.Length) { continue }
        
        $rect = New-Object System.Drawing.Rectangle([int]($x * $w), [int]($y * $h), [int]$w, [int]$h)
        $bmp = New-Object System.Drawing.Bitmap([int]$w, [int]$h)
        $gfx = [System.Drawing.Graphics]::FromImage($bmp)
        $gfx.DrawImage($img, (New-Object System.Drawing.Rectangle(0, 0, [int]$w, [int]$h)), $rect, [System.Drawing.GraphicsUnit]::Pixel)
        $gfx.Dispose()
        
        $outFile = Join-Path $outDir "$($names[$idx]).png"
        $bmp.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
        $bmp.Dispose()
        Write-Host "Saved $($names[$idx])"
        $idx++
    }
}
$img.Dispose()
Write-Host "Done"
