<#
  Hook PermissionRequest : notifie immediatement quand Claude Code a besoin
  d'une autorisation utilisateur.
#>
& (Join-Path $PSScriptRoot 'notify.ps1') `
  -Title 'Claude Code — intervention requise' `
  -Message 'Une autorisation est requise pour continuer. Revenez dans le terminal Claude Code.'
