package com.minitricount.participant.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ParticipantRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void noViolation_whenNameIsPresentAndWithinLimit() {
        Set<ConstraintViolation<ParticipantRequest>> violations =
                validator.validate(new ParticipantRequest("Alice"));

        assertThat(violations).isEmpty();
    }

    @Test
    void noViolation_whenNameIsExactly255Characters() {
        Set<ConstraintViolation<ParticipantRequest>> violations =
                validator.validate(new ParticipantRequest("a".repeat(255)));

        assertThat(violations).isEmpty();
    }

    @Test
    void violation_whenNameIsBlank() {
        Set<ConstraintViolation<ParticipantRequest>> violations =
                validator.validate(new ParticipantRequest(" "));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void violation_whenNameExceeds255Characters() {
        Set<ConstraintViolation<ParticipantRequest>> violations =
                validator.validate(new ParticipantRequest("a".repeat(256)));

        assertThat(violations).isNotEmpty();
    }
}
