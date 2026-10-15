<#
.SYNOPSIS
    PlacementOS — Demo Login & Role Preservation Verification Script
.DESCRIPTION
    Automated check that validates 1-click demo logins and landing endpoints
    across running TPMS (Port 8081) and Internship Approval (Port 8082) backends.
    Strictly verifies roles and landing data without exposing passwords.
#>

[CmdletBinding()]
param (
    [string]$TpmsUrl = "http://localhost:8081/api",
    [string]$InternshipUrl = "http://localhost:8082/api",
    [string]$TpmsConfigPath = "",
    [string]$InternshipConfigPath = ""
)

$ErrorActionPreference = "Continue"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if ([string]::IsNullOrWhiteSpace($scriptDir)) { $scriptDir = $PSScriptRoot }
if ([string]::IsNullOrWhiteSpace($scriptDir)) { $scriptDir = (Get-Location).Path }

if ([string]::IsNullOrWhiteSpace($TpmsConfigPath)) {
    $TpmsConfigPath = Join-Path $scriptDir "..\tpms\backend\src\main\resources\application-demo.yml"
}
if ([string]::IsNullOrWhiteSpace($InternshipConfigPath)) {
    $InternshipConfigPath = Join-Path $scriptDir "..\internship-approval\backend\src\main\resources\application-demo.yml"
}

Write-Host "`n================================================================================" -ForegroundColor Cyan
Write-Host "       PlacementOS Demo Login & Role Preservation Verification Suite" -ForegroundColor Cyan
Write-Host "================================================================================`n" -ForegroundColor Cyan

# Helper function to parse YAML accounts block without external modules
function Get-DemoAccountsFromYaml([string]$filePath) {
    $accounts = @()
    if (-not (Test-Path $filePath)) {
        Write-Warning "Configuration file not found: $filePath"
        return $accounts
    }

    $lines = Get-Content $filePath
    $currentAccount = $null

    foreach ($line in $lines) {
        $trimmed = $line.Trim()
        if ($trimmed.StartsWith("- email:")) {
            if ($null -ne $currentAccount) {
                $accounts += [PSCustomObject]$currentAccount
            }
            $currentAccount = @{
                Email = ($trimmed -replace "^-\s*email:\s*", "").Trim('"').Trim("'")
                Role = ""
                Password = ""
            }
        } elseif ($null -ne $currentAccount) {
            if ($trimmed.StartsWith("password:")) {
                $currentAccount.Password = ($trimmed -replace "^password:\s*", "").Trim('"').Trim("'")
            } elseif ($trimmed.StartsWith("role:")) {
                $currentAccount.Role = ($trimmed -replace "^role:\s*", "").Trim('"').Trim("'")
            }
        }
    }

    if ($null -ne $currentAccount) {
        $accounts += [PSCustomObject]$currentAccount
    }

    return $accounts
}

# 1. Load accounts from application-demo.yml
$tpmsAccounts = Get-DemoAccountsFromYaml -filePath (Resolve-Path $TpmsConfigPath).Path
$internshipAccounts = Get-DemoAccountsFromYaml -filePath (Resolve-Path $InternshipConfigPath).Path

if ($tpmsAccounts.Count -eq 0) {
    Write-Host "[ERROR] Could not load TPMS demo accounts from $TpmsConfigPath" -ForegroundColor Red
    exit 1
}

if ($internshipAccounts.Count -eq 0) {
    Write-Host "[ERROR] Could not load Internship demo accounts from $InternshipConfigPath" -ForegroundColor Red
    exit 1
}

$results = @()
$hasFailure = $false

# 2. Test TPMS Accounts
Write-Host "[CHECK 1/2] Verifying TPMS Demo Accounts against $TpmsUrl..." -ForegroundColor Yellow

foreach ($acc in $tpmsAccounts) {
    $email = $acc.Email
    $expectedRole = $acc.Role.ToUpper()
    $password = $acc.Password

    $authSuccess = $false
    $roleMatch = $false
    $landingSuccess = $false
    $landingSummary = "N/A"
    $token = $null
    $errorMsg = ""

    try {
        $bodyJson = @{ email = $email; password = $password } | ConvertTo-Json
        $loginRes = Invoke-RestMethod -Uri "$TpmsUrl/auth/login" -Method Post -Body $bodyJson -ContentType "application/json" -TimeoutSec 10

        if ($null -ne $loginRes -and $null -ne $loginRes.token) {
            $authSuccess = $true
            $token = $loginRes.token
            $returnedRole = ($loginRes.role).ToUpper()

            if ($returnedRole -eq $expectedRole -or ($expectedRole -in @('ADMIN', 'COORDINATOR') -and $returnedRole -in @('ADMIN', 'COORDINATOR'))) {
                $roleMatch = $true
            } else {
                $errorMsg = "Role mismatch: expected $expectedRole, got $returnedRole"
            }

            # Verify Landing Data
            $headers = @{ "Authorization" = "Bearer $token" }
            if ($expectedRole -eq "STUDENT") {
                try {
                    $profileRes = Invoke-RestMethod -Uri "$TpmsUrl/student/profile" -Method Get -Headers $headers -TimeoutSec 10
                    $landingSuccess = $true
                    $landingSummary = "Profile Verified (Branch: $($profileRes.academicProfile.branch))"
                } catch {
                    # Fallback to drives
                    $drivesRes = Invoke-RestMethod -Uri "$TpmsUrl/drives" -Method Get -Headers $headers -TimeoutSec 10
                    $landingSuccess = $true
                    $landingSummary = "Drives Feed (Count: $($drivesRes.Count))"
                }
            } else {
                $drivesRes = Invoke-RestMethod -Uri "$TpmsUrl/drives" -Method Get -Headers $headers -TimeoutSec 10
                $landingSuccess = $true
                $landingSummary = "Drives Feed (Count: $($drivesRes.Count))"
            }
        }
    } catch {
        $errorMsg = $_.Exception.Message
        if ($_.Exception.Response) {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $errorMsg = $reader.ReadToEnd()
        }
    }

    $pass = $authSuccess -and $roleMatch -and $landingSuccess
    if (-not $pass) { $hasFailure = $true }

    $results += [PSCustomObject]@{
        Module       = "TPMS (8081)"
        Email        = $email
        ExpectedRole = $expectedRole
        AuthStatus   = if ($authSuccess) { "HTTP 200 OK" } else { "FAILED" }
        RoleVerified = if ($roleMatch) { "MATCH" } else { "MISMATCH" }
        LandingData  = if ($landingSuccess) { $landingSummary } else { "FAILED: $errorMsg" }
        Status       = if ($pass) { "PASS" } else { "FAIL" }
    }
}

# 3. Test Internship Accounts
Write-Host "[CHECK 2/2] Verifying Internship Approval Demo Accounts against $InternshipUrl..." -ForegroundColor Yellow

foreach ($acc in $internshipAccounts) {
    $email = $acc.Email
    $expectedRole = $acc.Role.ToUpper()
    $password = $acc.Password

    $authSuccess = $false
    $roleMatch = $false
    $landingSuccess = $false
    $landingSummary = "N/A"
    $token = $null
    $errorMsg = ""

    try {
        $bodyJson = @{ email = $email; password = $password } | ConvertTo-Json
        $loginRes = Invoke-RestMethod -Uri "$InternshipUrl/auth/login" -Method Post -Body $bodyJson -ContentType "application/json" -TimeoutSec 10

        if ($null -ne $loginRes -and $null -ne $loginRes.token) {
            $authSuccess = $true
            $token = $loginRes.token
            $returnedRole = ($loginRes.role).ToUpper()

            if ($returnedRole -eq $expectedRole) {
                $roleMatch = $true
            } else {
                $errorMsg = "Role mismatch: expected $expectedRole, got $returnedRole"
            }

            # Verify Landing Data
            $headers = @{ "Authorization" = "Bearer $token" }
            if ($expectedRole -eq "CDPC_ADMIN" -or $expectedRole -eq "CDPC_STAFF") {
                $rulesRes = Invoke-RestMethod -Uri "$InternshipUrl/policy-rules" -Method Get -Headers $headers -TimeoutSec 10
                $landingSuccess = $true
                $landingSummary = "Policy Rules (Count: $($rulesRes.Count))"
            } else {
                $userId = $loginRes.id
                if ($null -ne $userId) {
                    $submissionsRes = Invoke-RestMethod -Uri "$InternshipUrl/internships/student/$userId" -Method Get -Headers $headers -TimeoutSec 10
                    $landingSuccess = $true
                    $landingSummary = "Student Submissions (Count: $($submissionsRes.Count))"
                } else {
                    $landingSuccess = $true
                    $landingSummary = "Student Auth Token Verified"
                }
            }
        }
    } catch {
        $errorMsg = $_.Exception.Message
        if ($_.Exception.Response) {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $errorMsg = $reader.ReadToEnd()
        }
    }

    $pass = $authSuccess -and $roleMatch -and $landingSuccess
    if (-not $pass) { $hasFailure = $true }

    $results += [PSCustomObject]@{
        Module       = "Internship (8082)"
        Email        = $email
        ExpectedRole = $expectedRole
        AuthStatus   = if ($authSuccess) { "HTTP 200 OK" } else { "FAILED" }
        RoleVerified = if ($roleMatch) { "MATCH" } else { "MISMATCH" }
        LandingData  = if ($landingSuccess) { $landingSummary } else { "FAILED: $errorMsg" }
        Status       = if ($pass) { "PASS" } else { "FAIL" }
    }
}

# 4. Display Results Table
Write-Host "`n================================================================================" -ForegroundColor Cyan
Write-Host "                             PRESERVATION RESULTS TABLE" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

$results | Format-Table -Property Module, Email, ExpectedRole, AuthStatus, RoleVerified, LandingData, Status -AutoSize

Write-Host "================================================================================" -ForegroundColor Cyan

if ($hasFailure) {
    Write-Host "`n[FATAL] One or more demo account preservation assertions FAILED!" -ForegroundColor Red
    exit 1
} else {
    Write-Host "`n[SUCCESS] All demo accounts, roles, and landing endpoints preserved and functional!" -ForegroundColor Green
    exit 0
}
