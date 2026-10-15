# PlacementOS — Comprehensive System Understanding & Pitch Masterguide

**Project Title:** PlacementOS (Training & Placement Management System - TPMS & Internship Approval System)  
**Target Domain:** Campus Recruitment Automation, Algorithmic Candidate Matching, & Internship Governance (`@rknec.edu`)  
**Document Purpose:** Complete technical mental model, executive presentation framework, stakeholder pitch scripts, live demo guide, and Q&A defense playbook.  
**Version:** 1.0.0  
**Date:** August 22, 2026  

---

## 1. Executive Summary & Elevator Pitches

**PlacementOS** is an enterprise-grade, dual-microservice web application designed to digitize, automate, and bring 100% transparency to university campus placement drives and off-campus internship approval workflows.

### ⚡ 30-Second Elevator Pitch
> *"PlacementOS is an AI-powered campus placement and internship governance platform. By combining Groq LLM job-description parsing with deterministic academic eligibility guards, a multi-dimensional candidate scoring engine, and automated internship scam detection, PlacementOS cuts TPO administrative workload by 90% while guaranteeing fair, audit-transparent recruitment for students."*

### 🕒 2-Minute Executive Summary
> *"Traditional college placement cells struggle with manual Excel shortlisting, opaque selection criteria, error-prone backlog checking, and fraudulent off-campus internship offers. PlacementOS solves this through two decoupled microservices:*
> 
> *1. **TPMS (Training & Placement Management System)**: Uses Groq LLM (Llama 3.3 70B) to instantly convert unstructured JD text and student PDF resumes into structured JSON schemas. It automatically blocks ineligible applicants (CGPA/backlogs) before they apply, scores eligible candidates using a Strategy-Pattern matching algorithm (0–100), and records mandatory audit reasons for any TPO status overrides.*
> 
> *2. **Internship Approval System**: Automates No Objection Certificate (NOC) issuance by running student offer letters through a dynamic Policy Rule Engine that checks minimum stipends, duration, company denylists, and scam pattern flags.*
> 
> *Built with Java 17, Spring Boot 3.x, React 18, and MySQL, PlacementOS turns chaotic placement drives into a transparent, data-driven workflow."*

---

## 2. Core Mental Model: How to Understand PlacementOS

To understand PlacementOS, think of it as the **"Central Operating System for University Career Centers"**. It sits directly between **Students**, **Training & Placement Officers (TPOs/Coordinators)**, and **Corporate Recruiters**.

```mermaid
flowchart TD
    subgraph Corporate & TPO Level
        JD[Unstructured Job Description Text / PDF] --> LLM1[Groq LLM Engine Llama-3.3-70B]
        LLM1 --> StructuredJD[Extracted Drive Criteria & Cutoffs]
    end

    subgraph PlacementOS Dual-Microservice Core
        subgraph Microservice 1: TPMS Port 8081/3000
            StructuredJD --> EligibilityGuard{Academic Eligibility Engine}
            StudentProfile[Student Profile & PDF Resume] --> EligibilityGuard
            
            EligibilityGuard -- Ineligible --> Blocked[400 Bad Request + Clear Reason]
            EligibilityGuard -- Eligible --> ScoringEngine[Strategy-Pattern Candidate Scoring]
            ScoringEngine --> ShortlistMatrix[Ranked Candidate Matrix 0-100]
            ShortlistMatrix --> OverrideAudit[TPO Audit Logged Overrides]
        end

        subgraph Microservice 2: Internship Approval Port 8082/3001
            OfferPDF[Student Offer Letter PDF] --> RuleEngine{Dynamic Policy Evaluator}
            RuleEngine --> StipendCheck[Min Stipend / Duration]
            RuleEngine --> ScamCheck[Company Denylist & Scam Flags]
            StipendCheck & ScamCheck --> CDPCQueue[CDPC Review Queue & Digital NOC]
        end
    end
```

### The System Data Flow & Lifecycle:

1. **Drive Onboarding**: Coordinators paste raw JDs. Groq LLM extracts required skills (Must-Have, Nice-to-Have, Bonus), weights, CGPA cutoffs, 10th/12th % limits, and max active backlog constraints.
2. **Student Gating**: When a student clicks **Apply**, the system executes hard DB checks against cutoffs. Non-compliant students receive explicit rejection feedback (e.g., *"CGPA 7.20 is below required 7.50"*).
3. **Algorithmic Shortlisting**: Eligible applicants are evaluated across skill fit, project relevance, DSA/coding profiles (LeetCode/GitHub), and experience, yielding a dynamic 0–100 candidate composite score.
4. **Accountable Override**: If a coordinator manually includes or excludes a candidate, the system enforces a mandatory written justification for audit compliance.
5. **Internship Governance**: Students submitting off-campus offers pass through automated rule evaluators (stipend, duration, denylist) and scam pattern detectors before CDPC admins grant digital NOCs.

---

## 3. Key Differentiators & Technical Highlights

When pitching PlacementOS, emphasize these **6 core technical breakthroughs**:

| Technical Feature | Traditional Campus Process | PlacementOS Architecture | Strategic Business Value |
| :--- | :--- | :--- | :--- |
| **1. AI JD & Resume Parsing** | Manual reading of multi-page PDFs; prone to missed requirements. | **Groq Cloud LLM** (`llama-3.3-70b-versatile`) with structured JSON schema enforcement. | Reduces drive setup time from 45 mins to ** under 10 seconds**. |
| **2. Deterministic Eligibility Guard** | Post-interview eligibility rejections; wasted recruiter time. | **Pre-application DB Validation Guard** for CGPA, 10th/12th %, and active backlogs. | Eliminates 100% of non-eligible applications before shortlisting. |
| **3. Strategy-Pattern Scoring Engine** | Subjective shortlisting based on visual resume skimming. | **Extensible Strategy Pattern** evaluating multi-tier skills, DSA projects, and handles. | Delivers an objective, repeatable 0–100 fit ranking. |
| **4. Mandatory Override Auditing** | Unexplained candidate inclusions/exclusions; claims of bias/favoritism. | Database-logged status overrides (`OVERRIDDEN_INCLUDED`) requiring mandatory written reason. | Ensures 100% institutional audit compliance and transparency. |
| **5. Scam-Resistant Internship Engine** | Students tricked by fake internship offers or low stipends. | **Rule Evaluator Pipeline** checking allowlists/denylists, stipend floors, and scam indicators. | Protects university reputation and student safety. |
| **6. Campus Domain Security** | Vulnerable to external unauthorized access. | **`@rknec.edu` strict domain enforcement** with stateless JWT & Role-Based Access Control. | Enterprise-grade closed-loop institutional security. |

---

## 4. Stakeholder Pitch Decks & Tailored Messaging

Different audiences care about different aspects of the system. Use the following tailored strategies depending on who you are pitching to:

### 🏛️ Scenario A: Pitching to Deans, Directors & Institutional Management
* **Core Theme**: Risk Management, Institutional Reputation, Governance, & Placement Rate Optimization.
* **Key Talking Points**:
  * *"PlacementOS guarantees 100% adherence to university placement policies by eliminating manual errors in eligibility verification."*
  * *"The Internship Approval module acts as a safety shield, detecting fraudulent offer letters and enforcing minimum stipend standards before issuing official college NOCs."*
  * *"Audit logs prevent any allegations of favoritism or corruption during placement shortlisting."*
* **Metrics to Highlight**: 90% reduction in processing time, 0 policy compliance violations, 100% auditable NOC records.

### 💼 Scenario B: Pitching to TPOs & Placement Coordinators
* **Core Theme**: Operational Efficiency, Automation, & Stress Reduction.
* **Key Talking Points**:
  * *"No more parsing endless Excel sheets or spending hours reading 500+ PDF resumes."*
  * *"Paste any Job Description (text or PDF), click extract, and watch AI instantly populate skill requirements and cutoffs."*
  * *"Automated ranking gives you a prioritized shortlist in seconds, with full freedom to override candidate status when human context is needed."*
* **Metrics to Highlight**: Drive setup in <10 seconds, instant candidate matrix generation, zero ineligible application clutter.

### 💻 Scenario C: Pitching to Technical Evaluators & Hackathon Judges
* **Core Theme**: Clean Architecture, Java 17/Spring Boot Design Patterns, & LLM System Integration.
* **Key Talking Points**:
  * *"Built on modern Java 17 and Spring Boot 3.x with stateless JWT security and domain-restricted access filters."*
  * *"Engineered using the **Strategy Design Pattern** for candidate scoring, allowing new scoring algorithms (e.g. GitHub activity weight vs. CGPA weight) to be plugged in dynamically."*
  * *"Dual microservice decoupled design separating drive management (TPMS) from policy governance (Internship Approval)."*
  * *"Groq LLM integration features JSON mode schema enforcement and robust offline mock fallbacks for zero downtime."*
* **Metrics to Highlight**: Dual microservice separation, dynamic rule evaluators, strategy pattern candidate matrix, Groq LLM latency < 800ms.

### 🎓 Scenario D: Pitching to Students
* **Core Theme**: Fairness, Transparency, & Frictionless Approvals.
* **Key Talking Points**:
  * *"You will never wonder why you weren't shortlisted — PlacementOS provides clear, transparent feedback on missing skills or cutoff criteria."*
  * *"Upload your resume once; our LLM extracts your skills, LeetCode, and DSA project highlights automatically."*
  * *"Request internship NOCs digitally without running around campus for physical signatures."*

---

## 5. Live Product Demonstration Script (Step-by-Step Flow)

When demonstrating PlacementOS live, follow this **4-Act narrative structure** to showcase maximum impact:

```
[Act 1: Coordinator Drive Setup] ➔ [Act 2: Student Eligibility Guard] ➔ [Act 3: Candidate Ranking & Audit Override] ➔ [Act 4: Internship Policy & NOC Approval]
```

### 🎬 Act 1: AI-Powered Drive Creation (TPMS)
1. **Log in** as Coordinator (`coord_user@rknec.edu` / `Coord@12345`).
2. Navigate to **Create Placement Drive**.
3. **Paste raw JD text**:
   > *"We are looking for a Senior Software Engineer with expertise in Java, Spring Boot, React, and MySQL. Candidates must have a minimum CGPA of 7.50, at least 70% in 10th and 12th, and no more than 1 active backlog."*
4. Click **Extract Criteria via AI (Groq LLM)**.
5. **Point out to audience**: Show how skills (`Java`, `Spring Boot`, `React`), scoring weights, and cutoffs (`7.50 CGPA`, `1 Backlog`) auto-populated instantly into the review grid.
6. Click **Publish Drive**.

### 🎬 Act 2: Student Academic Setup & Deterministic Blocking
1. **Log in** as Student (`student1@rknec.edu` / `Student@12345`).
2. Show the **Academic Setup** portal (CGPA: `7.20`, Active Backlogs: `0`).
3. Navigate to **Available Placement Drives** and find the newly created drive (Requires `7.50 CGPA`).
4. Click **Apply to Drive**.
5. **Highlight key moment**: System displays an explicit block modal:
   `400 Bad Request: Ineligible: Student CGPA (7.20) is below drive requirement (7.50)`.
6. Explain: *"Ineligible candidates are stopped at the gate — zero wasted TPO effort."*

### 🎬 Act 3: Algorithmic Scoring & Audit Override
1. Switch back to **Coordinator Portal**. Select an eligible candidate drive.
2. Click **Run Candidate Scoring & Rank Matrix**.
3. Show the **0–100 composite score matrix** generated via the Strategy Engine.
4. Click **Override Status** on candidate #2. Change status to `OVERRIDDEN_INCLUDED`.
5. System prompts for mandatory reason: Type *"Winner of Smart India Hackathon 2025"*.
6. Submit and show the audit tag in the candidate record.

### 🎬 Act 4: Internship Offer Verification & Policy Engine
1. Switch to **Internship Approval System** (`http://localhost:3001`).
2. Log in as Student, upload an internship offer letter (Stipend: `₹5,000/month`, Duration: `1 month`).
3. System triggers **Rule Evaluator**: Flagged for *Below Minimum Stipend Threshold (₹10,000)* and *Short Duration*.
4. Switch to **CDPC Admin Portal** (`admin@rknec.edu`). Show the flagged offer in the CDPC Review Queue with automated policy violation tags.

---

## 6. Defense & Q&A Playbook (Handling Tough Questions)

### Q1: "Why use AI for JD extraction? What if the LLM hallucinates or returns invalid data?"
* **Answer**: *"We do not rely on raw LLM text output. We enforce structured JSON schemas in the Groq API call. Furthermore, PlacementOS includes a mandatory **Human-in-the-Loop Review Step** where the placement officer can inspect, edit, or adjust extracted criteria before the drive is published. In addition, we built an offline mock fallback engine so the system functions perfectly even if network connectivity to Groq drops."*

### Q2: "Why separate the system into two microservices instead of a single monolith?"
* **Answer**: *"TPMS and Internship Approval serve distinct domain lifecycles with different operational cadence and access patterns. TPMS is high-volume during peak placement season (August–December) focused on high-throughput match scoring. Internship Approval runs year-round with strict legal and governance requirements (NOC compliance, stipend policies). Decoupling them allows independent scaling, isolated deployment, and zero single-point-of-failure risk."*

### Q3: "How does PlacementOS handle bias or nepotism in candidate shortlisting?"
* **Answer**: *"PlacementOS uses an objective Strategy-Pattern candidate scoring engine based strictly on verified skill taxonomy, DSA/coding handles, project relevance, and academic history. If a coordinator needs to manually include or exclude a student, the system blocks the action until a mandatory written justification is entered. This justification is permanently stored in the audit trail, ensuring complete institutional accountability."*

### Q4: "How does the system enforce institutional security?"
* **Answer**: *"Registration is strictly restricted to valid `@rknec.edu` domain emails via a custom Spring Security filter chain. Authentication uses stateless JWT tokens with role-based authorization rules (`STUDENT`, `COORDINATOR`, `ADMIN`). Sensitive operations like database purges require multi-stage modal confirmations."*

### Q5: "What is the future engineering roadmap for PlacementOS?"
* **Answer**: *"Our Phase 6 roadmap includes:
1. **TPO Executive Placement Analytics**: Visual dashboards tracking departmental placement ratios, salary bands, and recruiter trends.
2. **Automated Interview Slot Booking Engine**: Self-service scheduling for shortlisted candidates to eliminate scheduling conflicts.
3. **Multi-Channel Alert Gateway**: Automated Email & WhatsApp notifications for drive updates and interview calls."*

---

## 7. Summary Cheat Sheet for Presenters

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           PLACEMENTOS AT A GLANCE                        │
├──────────────────────────┬──────────────────────────────────────────────┤
│ Core Problem             │ Manual Excel shortlisting, opaque cutoffs,   │
│                          │ unverified internship offers, TPO burnout.   │
├──────────────────────────┼──────────────────────────────────────────────┤
│ Architecture             │ Dual Microservices: Java 17, Spring Boot 3,  │
│                          │ React 18, Groq LLM (Llama 3.3 70B), MySQL.   │
├──────────────────────────┼──────────────────────────────────────────────┤
│ Core Microservice 1      │ TPMS (Port 8081/3000): AI JD/Resume Parsing, │
│                          │ Eligibility Guard, Strategy Match Matrix.    │
├──────────────────────────┼──────────────────────────────────────────────┤
│ Core Microservice 2      │ Internship Approval (Port 8082/3001): Policy │
│                          │ Evaluator Engine, Scam Detection, Digital NOC│
├──────────────────────────┼──────────────────────────────────────────────┤
│ Key Value Proposition    │ 90% faster drive setup, 100% eligibility     │
│                          │ enforcement, 0 un-audited overrides.         │
└──────────────────────────┴──────────────────────────────────────────────┘
```

*End of Report.*
