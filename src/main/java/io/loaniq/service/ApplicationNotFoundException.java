package io.loaniq.service;

import java.util.UUID;

public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(UUID id) {
        super("No application with id " + id);
    }
}
