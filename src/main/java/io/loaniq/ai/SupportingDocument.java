package io.loaniq.ai;

/**
 * A document supplied with an application.
 *
 * @param kind free-text label such as "payslip" or "bank_statement"
 * @param content plain-text content already extracted from the original file
 */
public record SupportingDocument(String kind, String content) {}
