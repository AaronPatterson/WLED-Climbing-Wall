<#
.SYNOPSIS
    Cuts a release: bumps the version, builds a signed APK, verifies the
    signature, and publishes it as a GitHub release.

.DESCRIPTION
    The steps are individually easy and collectively easy to get wrong three
    months from now. In particular, forgetting to bump versionCode produces an
    update that silently does not apply on any device, with nothing to show for
    it but a confused six-year-old.

    Every check here fails loudly rather than carrying on. A release that stops
    halfway costs a minute; one that ships a bad artifact costs a round trip to
    every device in the house.

.PARAMETER VersionName
    The human-facing version, e.g. "0.4.0". versionCode is bumped automatically.

.PARAMETER DryRun
    Do everything except commit, tag, push and publish. Use this to check the
    build and signature without creating anything permanent.

.EXAMPLE
    ./scripts/release.ps1 -VersionName 0.4.0 -DryRun
    ./scripts/release.ps1 -VersionName 0.4.0
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidatePattern('^\d+\.\d+\.\d+$')][string]$VersionName,
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'

function Step($message) { Write-Host "`n==> $message" -ForegroundColor Cyan }
function Fail($message) { Write-Host "`nFAILED: $message" -ForegroundColor Red; exit 1 }

$repoRoot = Split-Path $PSScriptRoot -Parent
Set-Location $repoRoot

$gradleFile = Join-Path $repoRoot 'app/build.gradle.kts'
$apkPath    = Join-Path $repoRoot 'app/build/outputs/apk/release/app-release.apk'
$tag        = "v$VersionName"

# --- Preconditions -----------------------------------------------------------
# Checked before anything is modified, so a failure here leaves no mess.

Step 'Checking the working tree'

if (git status --porcelain) {
    Fail "Working tree is dirty. Commit or stash first - this script makes a version-bump commit and won't sweep unrelated changes into it."
}

$branch = git rev-parse --abbrev-ref HEAD
if ($branch -ne 'main') {
    Fail "On branch '$branch'. Releases are cut from main, otherwise the tag points at history that isn't published."
}

if (git tag --list $tag) {
    Fail "Tag $tag already exists. Pick a new version - a tag that moves is worse than one that's missing."
}

git fetch --quiet origin
if ((git rev-parse HEAD) -ne (git rev-parse origin/main)) {
    Fail "Local main differs from origin/main. Pull or push first, so the tag matches what people can actually download."
}

if (-not $env:JAVA_HOME) {
    Fail "JAVA_HOME is not set. See docs/releasing.md."
}

# Resolved up front rather than at the publish step. Discovering it is missing
# after the tag has been pushed leaves a tag with no release behind it.
$gh = (Get-Command gh -ErrorAction SilentlyContinue).Source
if (-not $gh) { $gh = 'C:\Program Files\GitHub CLI\gh.exe' }
if (-not (Test-Path $gh)) {
    Fail "GitHub CLI not found on PATH or at $gh. Install it, or the release can be built but not published."
}

# --- Version bump ------------------------------------------------------------

Step 'Bumping the version'

$content = Get-Content $gradleFile -Raw
if ($content -notmatch 'versionCode\s*=\s*(\d+)') { Fail "Couldn't find versionCode in $gradleFile" }
$currentCode = [int]$Matches[1]
$newCode = $currentCode + 1

if ($content -notmatch 'versionName\s*=\s*"([^"]+)"') { Fail "Couldn't find versionName in $gradleFile" }
$currentName = $Matches[1]

Write-Host "  versionCode $currentCode -> $newCode"
Write-Host "  versionName $currentName -> $VersionName"

$content = $content -replace 'versionCode\s*=\s*\d+', "versionCode = $newCode"
$content = $content -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$VersionName`""
Set-Content -Path $gradleFile -Value $content -NoNewline

# --- Build -------------------------------------------------------------------

Step 'Building the signed release'

# Deliberately not passing -PallowUnsigned: if credentials are missing this
# should stop here, not produce something no device can install.
& "$repoRoot/gradlew.bat" assembleRelease
if ($LASTEXITCODE -ne 0) {
    git checkout -- $gradleFile
    Fail 'Build failed. The version bump has been reverted.'
}

if (-not (Test-Path $apkPath)) {
    git checkout -- $gradleFile
    Fail "Expected $apkPath. An app-release-unsigned.apk here means signing was skipped."
}

# --- Verify the signature ----------------------------------------------------
# The build already refuses to produce an unsigned APK, so this is belt and
# braces. It costs a second, and the failure it guards against is only
# discoverable on the devices themselves.

Step 'Verifying the signature'

$apksigner = Get-ChildItem "$env:LOCALAPPDATA/Android/Sdk/build-tools/*/apksigner.bat" |
    Sort-Object { [version]($_.Directory.Name -replace '-.*$') } |
    Select-Object -Last 1
if (-not $apksigner) { Fail 'apksigner not found in the Android SDK build-tools.' }

$verifyOutput = & $apksigner.FullName verify --print-certs -v $apkPath 2>&1 | Out-String
if ($LASTEXITCODE -ne 0) { Fail "apksigner rejected the APK:`n$verifyOutput" }

foreach ($scheme in 'v2', 'v3') {
    if ($verifyOutput -notmatch "Scheme $scheme\): true") {
        Fail "APK is not signed with the $scheme scheme:`n$verifyOutput"
    }
}
Write-Host '  v2 and v3 present'

if ($verifyOutput -match 'certificate DN: ([^\r\n]+)') { Write-Host "  $($Matches[1])" }

# --- Publish -----------------------------------------------------------------

$asset = Join-Path $repoRoot "app/build/outputs/apk/release/wled-climb-$VersionName.apk"
Copy-Item $apkPath $asset -Force

if ($DryRun) {
    Step 'Dry run - stopping before anything permanent'
    Write-Host "  Would commit:  Release $VersionName"
    Write-Host "  Would tag:     $tag"
    Write-Host "  Would publish: $(Split-Path $asset -Leaf)"
    Write-Host "`n  Reverting the version bump."
    git checkout -- $gradleFile
    Remove-Item $asset -Force
    exit 0
}

Step 'Committing, tagging and publishing'

git add $gradleFile
git commit --quiet -m "Release $VersionName

versionCode $currentCode -> $newCode, so devices see this as an update.
Built and signature-verified by scripts/release.ps1."

git tag -a $tag -m "Release $VersionName"
git push --quiet origin main
git push --quiet origin $tag

& $gh release create $tag $asset `
    --title "$VersionName" `
    --notes "Signed release build. Install via Obtainium, or download the APK directly.

First install on a device that currently has a debug build needs an uninstall first - the signing certificate differs. See docs/releasing.md."

Step "Released $VersionName"
Write-Host "  https://github.com/AaronPatterson/WLED-Climbing-Wall/releases/tag/$tag"
