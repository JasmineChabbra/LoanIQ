package io.loaniq.ai;

import io.loaniq.domain.EmploymentType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Default extractor used when no model provider is configured.
 *
 * <p>This exists so the service runs end to end — in tests, in CI, and on a
 * laptop with no API key — without pretending to be a model. It reads obvious
 * labelled amounts and leaves anything ambiguous unset, which is the same
 * contract the model-backed extractor honours.
 */
@Component
@ConditionalOnMissingBean(name = "chatDocumentExtractor")
public class HeuristicDocumentExtractor implements DocumentExtractor {

    private static final Logger log = LoggerFactory.getLogger(HeuristicDocumentExtractor.class);

    private static final Pattern INCOME =
            Pattern.compile("(?:net|monthly)\\s+(?:salary|income|pay)\\D{0,12}([0-9][0-9,]*(?:\\.[0-9]{1,2})?)");
    private static final Pattern OBLIGATIONS =
            Pattern.compile("(?:emi|obligations?|instal?ments?)\\D{0,12}([0-9][0-9,]*(?:\\.[0-9]{1,2})?)");
    private static final Pattern BUREAU = Pattern.compile("(?:bureau|cibil|credit)\\s+score\\D{0,8}([0-9]{3})");

    @Override
    public DocumentExtraction extract(List<SupportingDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return DocumentExtraction.empty("No documents supplied.");
        }
        String corpus = documents.stream()
                .map(SupportingDocument::content)
                .filter(c -> c != null && !c.isBlank())
                .reduce("", (a, b) -> a + "\n" + b)
                .toLowerCase(Locale.ROOT);

        BigDecimal income = firstAmount(INCOME, corpus);
        BigDecimal obligations = firstAmount(OBLIGATIONS, corpus);
        Integer bureau = firstInt(BUREAU, corpus);
        EmploymentType employment = detectEmployment(corpus);

        DocumentExtraction extraction = new DocumentExtraction(
                income,
                obligations,
                employment,
                bureau,
                0.5,
                "Extracted by the built-in heuristic reader; no model provider configured.");
        log.debug("Heuristic extraction produced {}", extraction);
        return extraction;
    }

    private static BigDecimal firstAmount(Pattern pattern, String corpus) {
        Matcher m = pattern.matcher(corpus);
        if (!m.find()) {
            return null;
        }
        try {
            return new BigDecimal(m.group(1).replace(",", ""));
        } catch (NumberFormatException e) {
            // A malformed amount is worse than no amount: leave it unset.
            return null;
        }
    }

    private static Integer firstInt(Pattern pattern, String corpus) {
        Matcher m = pattern.matcher(corpus);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    private static EmploymentType detectEmployment(String corpus) {
        if (corpus.contains("self-employed") || corpus.contains("self employed") || corpus.contains("proprietor")) {
            return EmploymentType.SELF_EMPLOYED;
        }
        if (corpus.contains("contractor") || corpus.contains("contract role")) {
            return EmploymentType.CONTRACT;
        }
        if (corpus.contains("payslip") || corpus.contains("salary slip") || corpus.contains("employer")) {
            return EmploymentType.SALARIED;
        }
        return null;
    }
}
