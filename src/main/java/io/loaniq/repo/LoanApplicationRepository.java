package io.loaniq.repo;

import io.loaniq.domain.ApplicationStatus;
import io.loaniq.domain.LoanApplication;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

    Optional<LoanApplication> findByIdempotencyKey(String idempotencyKey);

    List<LoanApplication> findByApplicantRefOrderByCreatedAtDesc(String applicantRef);

    List<LoanApplication> findByStatus(ApplicationStatus status);
}
