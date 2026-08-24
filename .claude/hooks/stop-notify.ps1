<#
  Hook Stop : notifie quand Claude a termine sa reponse et attend la
  prochaine instruction. Ignore la notification si ce Stop correspond a une
  continuation automatique declenchee par un autre hook (champ
  "stop_hook_active" du JSON recu sur stdin), pour eviter les faux positifs.
#>
$rawInput = [Console]::In.ReadToEnd()
$stopHookActive = $false

if ($rawInput) {
  try {
    $data = $rawInput | ConvertFrom-Json
    if ($data.stop_hook_active -eq $true) {
      $stopHookActive = $true
    }
  }
  catch {
    # stdin vide ou non-JSON : on considere qu'il ne s'agit pas d'une
    # continuation automatique et on notifie normalement.
  }
}

if (-not $stopHookActive) {
  & (Join-Path $PSScriptRoot 'notify.ps1') `
    -Title 'Claude Code — tâche terminée' `
    -Message 'Claude a termine sa reponse et attend votre prochaine instruction.'
}
