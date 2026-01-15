package com.coding.interview.ekyc.service;

import com.coding.interview.ekyc.model.KYCDecision;
import com.coding.interview.ekyc.model.VerificationResult;
import com.coding.interview.ekyc.model.VerificationStatus;
import com.coding.interview.ekyc.model.VerificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
public class DecisionEngineService {

    private static final int MIN_DOCUMENT_CONFIDENCE = 85;
    private static final int MIN_BIOMETRIC_CONFIDENCE = 85;
    private static final double MIN_SIMILARITY_SCORE = 85.0;
    private static final int MIN_ADDRESS_CONFIDENCE = 80;


    public KYCDecision makeDecision(List<VerificationResult> results, String correlationId) {
        log.info("[{}] Making KYC decision based on {} verification results",
            correlationId, results.size());

        // Check sanctions first (CRITICAL - immediate rejection)
        VerificationResult sanctionsResult = findResult(results, VerificationType.SANCTIONS);
        if (sanctionsResult != null && sanctionsResult.getStatus() == VerificationStatus.HIT) {
            log.warn("[{}] Decision: REJECTED - Sanctions hit detected", correlationId);
            return KYCDecision.REJECTED;
        }

        // Check if sanctions check failed (must succeed)
        if (sanctionsResult != null && sanctionsResult.getStatus() == VerificationStatus.FAIL) {
            log.warn("[{}] Decision: MANUAL_REVIEW - Sanctions check failed (critical service)", correlationId);
            return KYCDecision.MANUAL_REVIEW;
        }

        boolean allPassed = true;
        boolean hasManualReview = false;

        // Evaluate each verification result
        for (VerificationResult result : results) {
            switch (result.getVerificationType()) {
                case ID_DOCUMENT:
                    KYCDecision docDecision = evaluateDocumentVerification(result, correlationId);
                    if (docDecision == KYCDecision.REJECTED) {
                        return KYCDecision.REJECTED;
                    } else if (docDecision == KYCDecision.MANUAL_REVIEW) {
                        hasManualReview = true;
                        allPassed = false;
                    } else if (result.getStatus() != VerificationStatus.PASS) {
                        allPassed = false;
                    }
                    break;

                case FACE_MATCH:
                    KYCDecision biometricDecision = evaluateBiometricVerification(result, correlationId);
                    if (biometricDecision == KYCDecision.REJECTED) {
                        return KYCDecision.REJECTED;
                    } else if (biometricDecision == KYCDecision.MANUAL_REVIEW) {
                        hasManualReview = true;
                        allPassed = false;
                    } else if (result.getStatus() != VerificationStatus.PASS) {
                        allPassed = false;
                    }
                    break;

                case ADDRESS:
                    KYCDecision addressDecision = evaluateAddressVerification(result, correlationId);
                    if (addressDecision == KYCDecision.REJECTED) {
                        return KYCDecision.REJECTED;
                    } else if (addressDecision == KYCDecision.MANUAL_REVIEW) {
                        hasManualReview = true;
                        allPassed = false;
                    } else if (result.getStatus() != VerificationStatus.PASS) {
                        allPassed = false;
                    }
                    break;

                case SANCTIONS:
                    // Already handled above
                    break;
            }
        }

        // Make final decision
        if (allPassed && sanctionsResult != null && sanctionsResult.getStatus() == VerificationStatus.CLEAR) {
            log.info("[{}] Decision: APPROVED - All verifications passed", correlationId);
            return KYCDecision.APPROVED;
        } else if (hasManualReview) {
            log.info("[{}] Decision: MANUAL_REVIEW - Some verifications require manual review", correlationId);
            return KYCDecision.MANUAL_REVIEW;
        } else {
            log.info("[{}] Decision: MANUAL_REVIEW - Not all verifications passed", correlationId);
            return KYCDecision.MANUAL_REVIEW;
        }
    }

    private KYCDecision evaluateDocumentVerification(VerificationResult result, String correlationId) {
        if (result.getStatus() == VerificationStatus.FAIL) {
            if (result.getReasons().stream().anyMatch(r -> r.toLowerCase().contains("expired"))) {
                log.warn("[{}] Document is expired - recommending REJECTION", correlationId);
                return KYCDecision.REJECTED;
            }
            log.info("[{}] Document verification failed - recommending MANUAL_REVIEW", correlationId);
            return KYCDecision.MANUAL_REVIEW;
        }

        if (result.getConfidence() != null && result.getConfidence() < MIN_DOCUMENT_CONFIDENCE) {
            log.info("[{}] Document confidence {}% is below threshold {}% - recommending MANUAL_REVIEW",
                correlationId, result.getConfidence(), MIN_DOCUMENT_CONFIDENCE);
            return KYCDecision.MANUAL_REVIEW;
        }

        return result.getStatus() == VerificationStatus.PASS ? KYCDecision.APPROVED : KYCDecision.MANUAL_REVIEW;
    }

    private KYCDecision evaluateBiometricVerification(VerificationResult result, String correlationId) {
        if (result.getStatus() == VerificationStatus.FAIL) {
            log.info("[{}] Biometric verification failed - recommending MANUAL_REVIEW", correlationId);
            return KYCDecision.MANUAL_REVIEW;
        }

        if (result.getConfidence() != null && result.getConfidence() < MIN_BIOMETRIC_CONFIDENCE) {
            log.info("[{}] Biometric confidence {}% is below threshold {}% - recommending MANUAL_REVIEW",
                correlationId, result.getConfidence(), MIN_BIOMETRIC_CONFIDENCE);
            return KYCDecision.MANUAL_REVIEW;
        }

        if (result.getSimilarityScore() != null && result.getSimilarityScore() < MIN_SIMILARITY_SCORE) {
            log.info("[{}] Similarity score {}% is below threshold {}% - recommending MANUAL_REVIEW",
                correlationId, result.getSimilarityScore(), MIN_SIMILARITY_SCORE);
            return KYCDecision.MANUAL_REVIEW;
        }

        return result.getStatus() == VerificationStatus.PASS ? KYCDecision.APPROVED : KYCDecision.MANUAL_REVIEW;
    }

    private KYCDecision evaluateAddressVerification(VerificationResult result, String correlationId) {
        if (result.getStatus() == VerificationStatus.FAIL) {
            log.info("[{}] Address verification failed - recommending MANUAL_REVIEW", correlationId);
            return KYCDecision.MANUAL_REVIEW;
        }

        if (result.getConfidence() != null && result.getConfidence() < MIN_ADDRESS_CONFIDENCE) {
            log.info("[{}] Address confidence {}% is below threshold {}% - recommending MANUAL_REVIEW",
                correlationId, result.getConfidence(), MIN_ADDRESS_CONFIDENCE);
            return KYCDecision.MANUAL_REVIEW;
        }

        return result.getStatus() == VerificationStatus.PASS ? KYCDecision.APPROVED : KYCDecision.MANUAL_REVIEW;
    }

    private VerificationResult findResult(List<VerificationResult> results, VerificationType type) {
        return results.stream()
            .filter(r -> r.getVerificationType() == type)
            .findFirst()
            .orElse(null);
    }
}

