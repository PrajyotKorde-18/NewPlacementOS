package com.placementos.internship.service;

import com.placementos.internship.entity.InternshipRequest;
import com.placementos.internship.entity.ScamPatternFlag;
import com.placementos.internship.repository.InternshipRequestRepository;
import com.placementos.internship.repository.ScamPatternFlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ScamPatternDetectorService {

    private final InternshipRequestRepository requestRepository;
    private final ScamPatternFlagRepository scamPatternFlagRepository;

    private static final Set<String> FREE_EMAIL_DOMAINS = Set.of(
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "rediffmail.com", "icloud.com"
    );

    private static final int TIME_WINDOW_DAYS = 60;

    @Transactional
    public List<ScamPatternFlag> detectAndLinkPatternFlags(InternshipRequest currentRequest) {
        List<ScamPatternFlag> detectedFlags = new ArrayList<>();
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(TIME_WINDOW_DAYS);

        // Signal 1: HR Email Domain Mismatch check
        String hrEmail = currentRequest.getHrContactEmail();
        if (hrEmail != null && hrEmail.contains("@")) {
            String domain = hrEmail.substring(hrEmail.indexOf("@") + 1).toLowerCase().trim();
            if (FREE_EMAIL_DOMAINS.contains(domain)) {
                String matchKey = domain;
                String detail = String.format("HR contact email '%s' uses a public/free email domain '%s' rather than an official corporate domain.", hrEmail, domain);
                ScamPatternFlag flag = createOrFetchFlag("HR_DOMAIN_MISMATCH", matchKey, detail, cutoffDate);
                linkRequestToFlag(currentRequest, flag);
                detectedFlags.add(flag);
            }
        }

        // Signal 2: Independent Company Name velocity check (within 60-day window across distinct students)
        if (currentRequest.getCompanyName() != null && !currentRequest.getCompanyName().isBlank()) {
            String companyName = currentRequest.getCompanyName().trim();
            List<InternshipRequest> companyMatches = requestRepository
                    .findByCompanyNameIgnoreCaseAndCreatedAtAfter(companyName, cutoffDate);

            List<InternshipRequest> distinctStudentMatches = companyMatches.stream()
                    .filter(r -> r.getStudent() != null && !r.getStudent().getId().equals(currentRequest.getStudent().getId()))
                    .toList();

            if (!distinctStudentMatches.isEmpty()) {
                List<InternshipRequest> allAffected = new ArrayList<>(distinctStudentMatches);
                allAffected.add(currentRequest);

                String matchKey = companyName.toLowerCase();
                String detail = String.format("Multiple offer submissions (%d students) detected for company '%s' within the past %d days.",
                        allAffected.size(), companyName, TIME_WINDOW_DAYS);

                ScamPatternFlag flag = createOrFetchFlag("MULTIPLE_REQUESTS_SAME_COMPANY", matchKey, detail, cutoffDate);
                for (InternshipRequest req : allAffected) {
                    linkRequestToFlag(req, flag);
                }
                detectedFlags.add(flag);
            }
        }

        // Signal 3: Independent Duplicate HR Contact Email check (within 60-day window across distinct students)
        if (hrEmail != null && !hrEmail.isBlank()) {
            String emailKey = hrEmail.trim();
            List<InternshipRequest> hrEmailMatches = requestRepository
                    .findByHrContactEmailIgnoreCaseAndCreatedAtAfter(emailKey, cutoffDate);

            List<InternshipRequest> distinctStudentMatches = hrEmailMatches.stream()
                    .filter(r -> r.getStudent() != null && !r.getStudent().getId().equals(currentRequest.getStudent().getId()))
                    .toList();

            if (!distinctStudentMatches.isEmpty()) {
                List<InternshipRequest> allAffected = new ArrayList<>(distinctStudentMatches);
                allAffected.add(currentRequest);

                String matchKey = emailKey.toLowerCase();
                String detail = String.format("Duplicate HR contact email '%s' submitted across %d distinct students within the past %d days.",
                        emailKey, allAffected.size(), TIME_WINDOW_DAYS);

                ScamPatternFlag flag = createOrFetchFlag("DUPLICATE_HR_CONTACT_EMAIL", matchKey, detail, cutoffDate);
                for (InternshipRequest req : allAffected) {
                    linkRequestToFlag(req, flag);
                }
                detectedFlags.add(flag);
            }
        }

        // Signal 4: Independent Offer Letter SHA-256 Hash Duplicate File check (within 60-day window across distinct students)
        String offerHash = currentRequest.getOfferLetterHash();
        if (offerHash != null && !offerHash.isBlank()) {
            List<InternshipRequest> hashMatches = requestRepository
                    .findByOfferLetterHashAndCreatedAtAfter(offerHash, cutoffDate);

            List<InternshipRequest> distinctStudentMatches = hashMatches.stream()
                    .filter(r -> r.getStudent() != null && !r.getStudent().getId().equals(currentRequest.getStudent().getId()))
                    .toList();

            if (!distinctStudentMatches.isEmpty()) {
                List<InternshipRequest> allAffected = new ArrayList<>(distinctStudentMatches);
                allAffected.add(currentRequest);

                String matchKey = offerHash;
                String detail = String.format("Duplicate offer letter file detected (SHA-256 match: %s) across %d distinct student submissions within the past %d days.",
                        offerHash, allAffected.size(), TIME_WINDOW_DAYS);

                ScamPatternFlag flag = createOrFetchFlag("DUPLICATE_OFFER_LETTER_FILE", matchKey, detail, cutoffDate);
                for (InternshipRequest req : allAffected) {
                    linkRequestToFlag(req, flag);
                }
                detectedFlags.add(flag);
            }
        }

        return detectedFlags;
    }

    private ScamPatternFlag createOrFetchFlag(String flagType, String matchKey, String detail, LocalDateTime cutoffDate) {
        Optional<ScamPatternFlag> existingFlag = scamPatternFlagRepository
                .findFirstByFlagTypeAndMatchKeyAndDetectedAtAfter(flagType, matchKey, cutoffDate);

        if (existingFlag.isPresent()) {
            ScamPatternFlag flag = existingFlag.get();
            flag.setDetail(detail);
            return scamPatternFlagRepository.save(flag);
        }

        ScamPatternFlag newFlag = ScamPatternFlag.builder()
                .flagType(flagType)
                .matchKey(matchKey)
                .detail(detail)
                .build();
        return scamPatternFlagRepository.save(newFlag);
    }

    private void linkRequestToFlag(InternshipRequest request, ScamPatternFlag flag) {
        if (request.getScamFlags() == null) {
            request.setScamFlags(new HashSet<>());
        }
        request.getScamFlags().add(flag);
        requestRepository.save(request);
    }
}
