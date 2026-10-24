package io.loaniq.ai;

import java.util.List;

/**
 * Pulls structured facts out of unstructured applicant documents.
 *
 * <p>Kept behind an interface so the underwriting flow does not depend on any
 * particular model vendor, and so tests can run without a network call.
 */
public interface DocumentExtractor {

    DocumentExtraction extract(List<SupportingDocument> documents);
}
