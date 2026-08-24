<#
  Affiche une notification Windows (toast via l'API balloon tip de la zone de
  notification, redirigée automatiquement vers le Centre de notifications sur
  Windows 10/11). Aucune dependance externe : uniquement System.Windows.Forms,
  present nativement dans Windows PowerShell 5.1.
#>
param(
  [Parameter(Mandatory = $true)][string]$Title,
  [Parameter(Mandatory = $true)][string]$Message
)

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$icon = New-Object System.Windows.Forms.NotifyIcon
try {
  $icon.Icon = [System.Drawing.SystemIcons]::Information
  $icon.Visible = $true
  $icon.BalloonTipTitle = $Title
  $icon.BalloonTipText = $Message
  $icon.BalloonTipIcon = [System.Windows.Forms.ToolTipIcon]::Info
  $icon.ShowBalloonTip(5000)
  Start-Sleep -Seconds 5
}
finally {
  $icon.Dispose()
}
