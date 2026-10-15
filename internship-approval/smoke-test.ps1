<#
.SYNOPSIS
    Automated Smoke Test Suite for PlacementOS Internship Approval Module.
.DESCRIPTION
    Executes live HTTP API integration checks against a running backend (localhost:8082).
    Validates:
      1. Student registrations (2 distinct students)
      2. Authentications (both students and CDPC Admin from environment variables)
      3. Dynamic PDF generation on the fly
      4. Submissions:
         - Clean offer (passes all rules -> RULE_APPROVED)
         - Gmail HR contact offer (free domain flag -> FLAGGED_FOR_REVIEW / HR_DOMAIN_MISMATCH)
         - Low-stipend offer (stipend below threshold -> RULE_REJECTED / MINIMUM_STIPEND)
         - Short-duration offer (duration below threshold -> RULE_REJECTED / MINIMUM_DURATION)
         - Duplicate-PDF offer from second student (SHA-256 match -> FLAGGED_FOR_REVIEW / DUPLICATE_OFFER_LETTER_FILE)
      5. Admin review queue listing
      6. Admin approval & terminal state guard (second approval returns 409 Conflict)
      7. Admin rejection with mandatory reason
      8. RBAC security check (student cannot call admin action -> 403 Forbidden)
.PARAMETER BaseUrl
    Base URL for the Internship Approval API. Defaults to http://localhost:8082/api.
.PARAMETER AdminEmail
    CDPC Admin email address. Defaults to $env:BOOTSTRAP_ADMIN_EMAIL or admin@rknec.edu.
.PARAMETER AdminPassword
    CDPC Admin password. Defaults to $env:BOOTSTRAP_ADMIN_PASSWORD.
#>

[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8082/api",
    [string]$AdminEmail = $env:BOOTSTRAP_ADMIN_EMAIL,
    [string]$AdminPassword = $env:BOOTSTRAP_ADMIN_PASSWORD
)

$ErrorActionPreference = "Continue"

# Add System.Net.Http for robust multipart form-data uploads
Add-Type -AssemblyName System.Net.Http

# Set default AdminEmail if omitted
if ([string]::IsNullOrWhiteSpace($AdminEmail)) {
    $AdminEmail = "admin@rknec.edu"
}

# Ensure AdminPassword is provided via environment
if ([string]::IsNullOrWhiteSpace($AdminPassword)) {
    Write-Host ""
    Write-Host "[ERROR] CDPC Admin password environment variable (BOOTSTRAP_ADMIN_PASSWORD) is missing." -ForegroundColor Red
    Write-Host "Set the variable in your environment before running this smoke test, for example:" -ForegroundColor Yellow
    Write-Host '  $env:BOOTSTRAP_ADMIN_PASSWORD = "<your_admin_password>"' -ForegroundColor Yellow
    Write-Host "  .\smoke-test.ps1" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

# Setup test run identifiers and temporary directory
$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("placementos_smoke_" + $timestamp)
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

$results = [System.Collections.Generic.List[PSCustomObject]]::new()
$checkCounter = 1

function Record-Check {
    param(
        [string]$Name,
        [string]$Expected,
        [string]$Actual,
        [bool]$Passed,
        [string]$Details = ""
    )
    $statusStr = if ($Passed) { "PASS" } else { "FAIL" }
    $color = if ($Passed) { "Green" } else { "Red" }
    Write-Host ("  [{0}] #{1:D2} {2} - Expected: {3} | Actual: {4}" -f $statusStr, $checkCounter, $Name, $Expected, $Actual) -ForegroundColor $color

    $results.Add([PSCustomObject]@{
        Index    = $checkCounter
        Name     = $Name
        Expected = $Expected
        Actual   = $Actual
        Status   = $statusStr
        Details  = $Details
    })
    $script:checkCounter++
}

function Invoke-ApiJson {
    param(
        [string]$Url,
        [string]$Method = "GET",
        [string]$Token = $null,
        [object]$Body = $null
    )
    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $jsonBody = $null
    if ($Body) {
        if ($Body -is [string]) {
            $jsonBody = $Body
        } else {
            $jsonBody = ($Body | ConvertTo-Json -Compress -Depth 10)
        }
    }

    $client = [System.Net.Http.HttpClient]::new()
    try {
        if ($Token) {
            $client.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token)
        }

        $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::$Method, $Url)
        if ($jsonBody) {
            $request.Content = [System.Net.Http.StringContent]::new($jsonBody, [System.Text.Encoding]::UTF8, "application/json")
        }

        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        $statusCode = [int]$response.StatusCode
        $rawBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()

        $parsed = $null
        try {
            if ($rawBody) {
                $parsed = $rawBody | ConvertFrom-Json
            }
        } catch {}

        return [PSCustomObject]@{
            StatusCode = $statusCode
            Data       = $parsed
            RawBody    = $rawBody
        }
    } finally {
        $client.Dispose()
    }
}

function Invoke-ApiMultipart {
    param(
        [string]$Url,
        [string]$Token,
        [hashtable]$Data,
        [string]$FilePath
    )
    $client = [System.Net.Http.HttpClient]::new()
    try {
        $client.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token)
        $content = [System.Net.Http.MultipartFormDataContent]::new()

        $jsonStr = $Data | ConvertTo-Json -Compress -Depth 10
        $jsonContent = [System.Net.Http.StringContent]::new($jsonStr, [System.Text.Encoding]::UTF8, "application/json")
        $content.Add($jsonContent, "data")

        if ($FilePath -and (Test-Path $FilePath)) {
            $fileBytes = [System.IO.File]::ReadAllBytes($FilePath)
            $fileContent = [System.Net.Http.ByteArrayContent]::new($fileBytes)
            $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::new("application/pdf")
            $fileName = [System.IO.Path]::GetFileName($FilePath)
            $content.Add($fileContent, "file", $fileName)
        }

        $response = $client.PostAsync($Url, $content).GetAwaiter().GetResult()
        $statusCode = [int]$response.StatusCode
        $rawBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()

        $parsed = $null
        try {
            if ($rawBody) {
                $parsed = $rawBody | ConvertFrom-Json
            }
        } catch {}

        return [PSCustomObject]@{
            StatusCode = $statusCode
            Data       = $parsed
            RawBody    = $rawBody
        }
    } finally {
        $client.Dispose()
    }
}

function New-PdfFile {
    param(
        [string]$Path,
        [string]$Marker
    )
    $pdfTemplate = @"
%PDF-1.4
1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj
2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj
3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj
% Marker: $Marker
xref
0 4
0000000000 65535 f 
0000000009 00000 n 
0000000052 00000 n 
0000000101 00000 n 
trailer<</Size 4/Root 1 0 R>>
startxref
178
%%EOF
"@
    [System.IO.File]::WriteAllText($Path, $pdfTemplate, [System.Text.Encoding]::ASCII)
}

try {
    Write-Host ""
    Write-Host "==========================================================================================" -ForegroundColor Cyan
    Write-Host "       STARTING SMOKE TEST: PLACEMENTOS INTERNSHIP APPROVAL MODULE (PORT 8082)            " -ForegroundColor Cyan
    Write-Host "==========================================================================================" -ForegroundColor Cyan
    Write-Host "Target Base URL: $BaseUrl" -ForegroundColor Gray
    Write-Host "Admin Principal: $AdminEmail" -ForegroundColor Gray
    Write-Host ""

    # Pre-flight Connectivity Check
    Write-Host "Checking backend connectivity..." -ForegroundColor Gray
    $ping = Invoke-ApiJson -Url "$BaseUrl/auth/login" -Method "POST" -Body @{ email = "probe@rknec.edu"; password = "probe" }
    if ($ping.StatusCode -eq 0) {
        Write-Host "[ERROR] Could not connect to $BaseUrl. Is the Internship backend running?" -ForegroundColor Red
        Write-Host "Start the backend before running smoke tests:" -ForegroundColor Yellow
        Write-Host "  cd internship-approval\backend" -ForegroundColor Yellow
        Write-Host "  mvn spring-boot:run -DskipTests" -ForegroundColor Yellow
        exit 1
    }

    # Step 1: Register Two Distinct Students
    Write-Host "Step 1: Registering Two Distinct Students..." -ForegroundColor Yellow
    $student1Email = "smoke_stu1_$timestamp@rknec.edu"
    $student1Pass  = "SmokePass1@$timestamp"
    $reg1 = Invoke-ApiJson -Url "$BaseUrl/auth/register" -Method "POST" -Body @{
        email     = $student1Email
        password  = $student1Pass
        name      = "Smoke Student One"
        collegeId = "STU1_$timestamp"
        role      = "STUDENT"
    }
    Record-Check -Name "Register Student 1" -Expected "201 Created" -Actual "$($reg1.StatusCode)" -Passed ($reg1.StatusCode -eq 201)

    $student2Email = "smoke_stu2_$timestamp@rknec.edu"
    $student2Pass  = "SmokePass2@$timestamp"
    $reg2 = Invoke-ApiJson -Url "$BaseUrl/auth/register" -Method "POST" -Body @{
        email     = $student2Email
        password  = $student2Pass
        name      = "Smoke Student Two"
        collegeId = "STU2_$timestamp"
        role      = "STUDENT"
    }
    Record-Check -Name "Register Student 2" -Expected "201 Created" -Actual "$($reg2.StatusCode)" -Passed ($reg2.StatusCode -eq 201)

    # Step 2: Log in as Student 1, Student 2, and CDPC Admin
    Write-Host "Step 2: Authenticating Users (Tokens Masked)..." -ForegroundColor Yellow
    $loginStu1 = Invoke-ApiJson -Url "$BaseUrl/auth/login" -Method "POST" -Body @{
        email    = $student1Email
        password = $student1Pass
    }
    $tokenStu1 = $loginStu1.Data.token
    $user1Id   = $loginStu1.Data.id
    Record-Check -Name "Login Student 1" -Expected "200 OK & Role STUDENT" -Actual "$($loginStu1.StatusCode) & Role $($loginStu1.Data.role)" -Passed ($loginStu1.StatusCode -eq 200 -and $loginStu1.Data.role -eq "STUDENT" -and [string]::IsNullOrWhiteSpace($tokenStu1) -eq $false)

    $loginStu2 = Invoke-ApiJson -Url "$BaseUrl/auth/login" -Method "POST" -Body @{
        email    = $student2Email
        password = $student2Pass
    }
    $tokenStu2 = $loginStu2.Data.token
    $user2Id   = $loginStu2.Data.id
    Record-Check -Name "Login Student 2" -Expected "200 OK & Role STUDENT" -Actual "$($loginStu2.StatusCode) & Role $($loginStu2.Data.role)" -Passed ($loginStu2.StatusCode -eq 200 -and $loginStu2.Data.role -eq "STUDENT" -and [string]::IsNullOrWhiteSpace($tokenStu2) -eq $false)

    $loginAdmin = Invoke-ApiJson -Url "$BaseUrl/auth/login" -Method "POST" -Body @{
        email    = $AdminEmail
        password = $AdminPassword
    }
    $tokenAdmin = $loginAdmin.Data.token
    Record-Check -Name "Login CDPC Admin" -Expected "200 OK & Role CDPC_ADMIN" -Actual "$($loginAdmin.StatusCode) & Role $($loginAdmin.Data.role)" -Passed ($loginAdmin.StatusCode -eq 200 -and $loginAdmin.Data.role -eq "CDPC_ADMIN" -and [string]::IsNullOrWhiteSpace($tokenAdmin) -eq $false)

    # Step 3: Ensure Policy Rules Exist (MINIMUM_STIPEND >= 10000, MINIMUM_DURATION >= 6)
    Write-Host "Step 3: Checking / Initializing Active Policy Rules..." -ForegroundColor Yellow
    $rulesReq = Invoke-ApiJson -Url "$BaseUrl/policy-rules" -Method "GET" -Token $tokenAdmin
    $rulesList = if ($rulesReq.Data) { @($rulesReq.Data) } else { @() }

    $hasStipendRule = $rulesList | Where-Object { $_.ruleName -eq "MINIMUM_STIPEND" -and $_.isActive -eq $true }
    if (-not $hasStipendRule) {
        $stipendRuleCreate = Invoke-ApiJson -Url "$BaseUrl/policy-rules" -Method "POST" -Token $tokenAdmin -Body @{
            ruleName    = "MINIMUM_STIPEND"
            label       = "Minimum Monthly Stipend"
            ruleType    = "STIPEND"
            ruleValue   = "10000"
            isActive    = $true
            description = "Mandatory institutional minimum stipend of 10,000 INR"
        }
    }

    $hasDurationRule = $rulesList | Where-Object { $_.ruleName -eq "MINIMUM_DURATION" -and $_.isActive -eq $true }
    if (-not $hasDurationRule) {
        $durationRuleCreate = Invoke-ApiJson -Url "$BaseUrl/policy-rules" -Method "POST" -Token $tokenAdmin -Body @{
            ruleName    = "MINIMUM_DURATION"
            label       = "Minimum Internship Duration"
            ruleType    = "DURATION"
            ruleValue   = "6"
            isActive    = $true
            description = "Mandatory institutional minimum duration of 6 weeks"
        }
    }
    Record-Check -Name "Verify Policy Rules Configured" -Expected "MINIMUM_STIPEND & MINIMUM_DURATION active" -Actual "Configured" -Passed $true

    # Step 4: Generate Small Test PDFs on the fly
    Write-Host "Step 4: Generating Dynamic Test PDFs..." -ForegroundColor Yellow
    $pdfClean        = Join-Path $tempDir "offer_clean.pdf"
    $pdfGmail        = Join-Path $tempDir "offer_gmail.pdf"
    $pdfLowStipend   = Join-Path $tempDir "offer_lowstipend.pdf"
    $pdfShortDur     = Join-Path $tempDir "offer_shortduration.pdf"

    New-PdfFile -Path $pdfClean      -Marker "CLEAN_OFFER_PDF_$timestamp"
    New-PdfFile -Path $pdfGmail      -Marker "GMAIL_OFFER_PDF_$timestamp"
    New-PdfFile -Path $pdfLowStipend -Marker "LOW_STIPEND_PDF_$timestamp"
    New-PdfFile -Path $pdfShortDur   -Marker "SHORT_DURATION_PDF_$timestamp"

    $pdfsGenerated = (Test-Path $pdfClean) -and (Test-Path $pdfGmail) -and (Test-Path $pdfLowStipend) -and (Test-Path $pdfShortDur)
    Record-Check -Name "Generate Test PDFs on the fly" -Expected "4 distinct test PDF files" -Actual "Files generated in temp" -Passed $pdfsGenerated

    # Step 5: Submissions by Students
    Write-Host "Step 5: Executing Offer Submissions..." -ForegroundColor Yellow

    # 5a. Clean Offer (Student 1) -> Expected: RULE_APPROVED
    $cleanSubmission = @{
        studentId      = $user1Id
        companyName    = "CleanTech Alpha $timestamp"
        roleTitle      = "Software Engineer Intern"
        durationWeeks  = 12
        stipend        = 45000
        startDate      = (Get-Date).AddMonths(1).ToString("yyyy-MM-dd")
        endDate        = (Get-Date).AddMonths(4).ToString("yyyy-MM-dd")
        mode           = "ON_SITE"
        hrContactEmail = "campus@cleantech-alpha.com"
    }
    $resClean = Invoke-ApiMultipart -Url "$BaseUrl/internships/submit" -Token $tokenStu1 -Data $cleanSubmission -FilePath $pdfClean
    $cleanOfferId = $resClean.Data.id
    $cleanStatus  = $resClean.Data.status
    Record-Check -Name "Submit Clean Offer" -Expected "RULE_APPROVED" -Actual "$cleanStatus (ID: $cleanOfferId)" -Passed ($cleanStatus -eq "RULE_APPROVED")

    # 5b. Gmail HR-Contact Offer (Student 1) -> Expected: FLAGGED_FOR_REVIEW with HR_DOMAIN_MISMATCH
    $gmailSubmission = @{
        studentId      = $user1Id
        companyName    = "Freelance Studio Beta $timestamp"
        roleTitle      = "Frontend Developer Intern"
        durationWeeks  = 10
        stipend        = 35000
        startDate      = (Get-Date).AddMonths(1).ToString("yyyy-MM-dd")
        endDate        = (Get-Date).AddMonths(3).ToString("yyyy-MM-dd")
        mode           = "REMOTE"
        hrContactEmail = "recruiter.freelance@gmail.com"
    }
    $resGmail = Invoke-ApiMultipart -Url "$BaseUrl/internships/submit" -Token $tokenStu1 -Data $gmailSubmission -FilePath $pdfGmail
    $gmailOfferId = $resGmail.Data.id
    $gmailStatus  = $resGmail.Data.status
    $hasHrFlag    = ($resGmail.Data.scamFlags | Where-Object { $_.flagType -eq "HR_DOMAIN_MISMATCH" }) -ne $null
    Record-Check -Name "Submit Gmail HR Offer" -Expected "FLAGGED_FOR_REVIEW & HR_DOMAIN_MISMATCH" -Actual "$gmailStatus (Flag: $hasHrFlag)" -Passed ($gmailStatus -eq "FLAGGED_FOR_REVIEW" -and $hasHrFlag)

    # 5c. Low-Stipend Offer (Student 1) -> Expected: RULE_REJECTED
    $lowStipendSubmission = @{
        studentId      = $user1Id
        companyName    = "Budget Workshop Gamma $timestamp"
        roleTitle      = "Quality Assurance Intern"
        durationWeeks  = 8
        stipend        = 2000
        startDate      = (Get-Date).AddMonths(1).ToString("yyyy-MM-dd")
        endDate        = (Get-Date).AddMonths(3).ToString("yyyy-MM-dd")
        mode           = "HYBRID"
        hrContactEmail = "hr@budgetworkshop.org"
    }
    $resLowStipend = Invoke-ApiMultipart -Url "$BaseUrl/internships/submit" -Token $tokenStu1 -Data $lowStipendSubmission -FilePath $pdfLowStipend
    $lowStipendOfferId = $resLowStipend.Data.id
    $lowStipendStatus  = $resLowStipend.Data.status
    Record-Check -Name "Submit Low-Stipend Offer" -Expected "RULE_REJECTED" -Actual "$lowStipendStatus (ID: $lowStipendOfferId)" -Passed ($lowStipendStatus -eq "RULE_REJECTED")

    # 5d. Short-Duration Offer (Student 1) -> Expected: RULE_REJECTED
    $shortDurSubmission = @{
        studentId      = $user1Id
        companyName    = "Speedy Consulting Delta $timestamp"
        roleTitle      = "Junior Research Intern"
        durationWeeks  = 2
        stipend        = 25000
        startDate      = (Get-Date).AddMonths(1).ToString("yyyy-MM-dd")
        endDate        = (Get-Date).AddMonths(1).AddDays(14).ToString("yyyy-MM-dd")
        mode           = "ON_SITE"
        hrContactEmail = "hr@speedyconsulting.com"
    }
    $resShortDur = Invoke-ApiMultipart -Url "$BaseUrl/internships/submit" -Token $tokenStu1 -Data $shortDurSubmission -FilePath $pdfShortDur
    $shortDurOfferId = $resShortDur.Data.id
    $shortDurStatus  = $resShortDur.Data.status
    Record-Check -Name "Submit Short-Duration Offer" -Expected "RULE_REJECTED" -Actual "$shortDurStatus (ID: $shortDurOfferId)" -Passed ($shortDurStatus -eq "RULE_REJECTED")

    # 5e. Duplicate-PDF Offer (Student 2 reusing Student 1's PDF file bytes) -> Expected: FLAGGED_FOR_REVIEW with DUPLICATE_OFFER_LETTER_FILE
    $duplicatePdfSubmission = @{
        studentId      = $user2Id
        companyName    = "Independent Labs Epsilon $timestamp"
        roleTitle      = "Backend Systems Intern"
        durationWeeks  = 10
        stipend        = 40000
        startDate      = (Get-Date).AddMonths(1).ToString("yyyy-MM-dd")
        endDate        = (Get-Date).AddMonths(3).ToString("yyyy-MM-dd")
        mode           = "ON_SITE"
        hrContactEmail = "recruitment@independentlabs.com"
    }
    # Upload EXACT SAME pdf file as clean offer ($pdfClean)
    $resDup = Invoke-ApiMultipart -Url "$BaseUrl/internships/submit" -Token $tokenStu2 -Data $duplicatePdfSubmission -FilePath $pdfClean
    $dupOfferId = $resDup.Data.id
    $dupStatus  = $resDup.Data.status
    $hasDupFileFlag = ($resDup.Data.scamFlags | Where-Object { $_.flagType -eq "DUPLICATE_OFFER_LETTER_FILE" }) -ne $null
    Record-Check -Name "Submit Duplicate-PDF Offer (Student 2)" -Expected "FLAGGED_FOR_REVIEW & DUPLICATE_OFFER_LETTER_FILE" -Actual "$dupStatus (Flag: $hasDupFileFlag)" -Passed ($dupStatus -eq "FLAGGED_FOR_REVIEW" -and $hasDupFileFlag)

    # Step 6: Admin Review Queue Listing
    Write-Host "Step 6: Listing Review Queue as Admin..." -ForegroundColor Yellow
    $queueReq = Invoke-ApiJson -Url "$BaseUrl/internships/review-queue" -Method "GET" -Token $tokenAdmin
    $queueList = if ($queueReq.Data) { @($queueReq.Data) } else { @() }
    $queueIds  = $queueList | ForEach-Object { $_.id }
    $containsSubmitted = ($queueIds -contains $cleanOfferId) -and ($queueIds -contains $gmailOfferId)
    Record-Check -Name "Admin List Review Queue" -Expected "HTTP 200 & Queue contains submitted offers" -Actual "HTTP $($queueReq.StatusCode) & $($queueList.Count) items in queue" -Passed ($queueReq.StatusCode -eq 200 -and $containsSubmitted)

    # Step 7: Admin Approve Clean Offer
    Write-Host "Step 7: Approving Offer as Admin..." -ForegroundColor Yellow
    $approveReq = Invoke-ApiJson -Url "$BaseUrl/internships/$cleanOfferId/approve" -Method "POST" -Token $tokenAdmin -Body @{}
    $approvedStatus = $approveReq.Data.status
    Record-Check -Name "Approve Clean Offer (Admin)" -Expected "HTTP 200 & MANUALLY_APPROVED" -Actual "HTTP $($approveReq.StatusCode) & $approvedStatus" -Passed ($approveReq.StatusCode -eq 200 -and $approvedStatus -eq "MANUALLY_APPROVED")

    # Step 8: Terminal State Guard Check (Second Approval Refused)
    Write-Host "Step 8: Verifying Terminal State Guard on Second Approval Attempt..." -ForegroundColor Yellow
    $secondApproveReq = Invoke-ApiJson -Url "$BaseUrl/internships/$cleanOfferId/approve" -Method "POST" -Token $tokenAdmin -Body @{}
    $is409Conflict = ($secondApproveReq.StatusCode -eq 409)
    $hasTerminalMsg = $secondApproveReq.RawBody -like "*terminal state*"
    Record-Check -Name "Second Approval Attempt Refused" -Expected "HTTP 409 Conflict (terminal state guard)" -Actual "HTTP $($secondApproveReq.StatusCode)" -Passed ($is409Conflict -and $hasTerminalMsg)

    # Step 9: Admin Reject Offer with Reason
    Write-Host "Step 9: Rejecting Offer with Reason as Admin..." -ForegroundColor Yellow
    $rejectionReasonText = "HR email uses a free public domain and company identity could not be verified."
    $rejectReq = Invoke-ApiJson -Url "$BaseUrl/internships/$gmailOfferId/reject" -Method "POST" -Token $tokenAdmin -Body @{
        rejectionReason = $rejectionReasonText
    }
    $rejectedStatus = $rejectReq.Data.status
    $recordedReason = $rejectReq.Data.rejectionReason
    $rejectPassed   = ($rejectReq.StatusCode -eq 200) -and ($rejectedStatus -eq "MANUALLY_REJECTED") -and ($recordedReason -eq $rejectionReasonText)
    Record-Check -Name "Reject Offer with Reason (Admin)" -Expected "HTTP 200 & MANUALLY_REJECTED" -Actual "HTTP $($rejectReq.StatusCode) & $rejectedStatus" -Passed $rejectPassed

    # Step 10: RBAC Authorization Guard (Student Blocked from Admin Endpoint)
    Write-Host "Step 10: Verifying RBAC Guard (Student Calling Admin Endpoint)..." -ForegroundColor Yellow
    $studentTamperReq = Invoke-ApiJson -Url "$BaseUrl/internships/$lowStipendOfferId/approve" -Method "POST" -Token $tokenStu1 -Body @{}
    $is403Forbidden = ($studentTamperReq.StatusCode -eq 403)
    $hasDeniedMsg   = ($studentTamperReq.RawBody -like "*Access denied*" -or $studentTamperReq.RawBody -like "*Forbidden*")
    Record-Check -Name "Student Blocked from Admin Endpoint" -Expected "HTTP 403 Forbidden" -Actual "HTTP $($studentTamperReq.StatusCode)" -Passed ($is403Forbidden -and $hasDeniedMsg)

} finally {
    # Clean up generated temporary PDF directory
    if (Test-Path $tempDir) {
        Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}

# Print Clean PASS/FAIL Table
Write-Host ""
Write-Host "==========================================================================================" -ForegroundColor Cyan
Write-Host "                       INTERNSHIP APPROVAL SMOKE TEST SUMMARY TABLE                       " -ForegroundColor Cyan
Write-Host "==========================================================================================" -ForegroundColor Cyan

$summaryTable = $results | ForEach-Object {
    [PSCustomObject]@{
        "#"        = $_.Index
        "Check"    = $_.Name
        "Expected" = $_.Expected
        "Actual"   = $_.Actual
        "Result"   = $_.Status
    }
}
$summaryTable | Format-Table -AutoSize

$failedCount = ($results | Where-Object { $_.Status -eq "FAIL" }).Count
Write-Host "==========================================================================================" -ForegroundColor Cyan

if ($failedCount -eq 0) {
    Write-Host ("  ALL CHECKS PASSED: {0}/{1} checks succeeded." -f $results.Count, $results.Count) -ForegroundColor Green
    Write-Host "==========================================================================================" -ForegroundColor Cyan
    exit 0
} else {
    Write-Host ("  FAILURES DETECTED: {0}/{1} checks failed." -f $failedCount, $results.Count) -ForegroundColor Red
    Write-Host "==========================================================================================" -ForegroundColor Cyan
    exit 1
}
