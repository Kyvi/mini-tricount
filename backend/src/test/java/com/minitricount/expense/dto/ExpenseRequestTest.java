package com.minitricount.expense.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseRequestTest {

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

    private static ExpenseRequest validRequest() {
        return new ExpenseRequest("Courses", new BigDecimal("10.00"), LocalDate.now(), 1L, List.of(1L, 2L));
    }

    @Test
    void noViolation_whenAllFieldsValid() {
        assertThat(validator.validate(validRequest())).isEmpty();
    }

    @Test
    void violation_whenDescriptionBlank() {
        Set<ConstraintViolation<ExpenseRequest>> violations = validator.validate(
                new ExpenseRequest(" ", new BigDecimal("10.00"), LocalDate.now(), 1L, List.of(1L)));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void violation_whenDescriptionExceeds255Characters() {
        Set<ConstraintViolation<ExpenseRequest>> violations = validator.validate(
                new ExpenseRequest("a".repeat(256), new BigDecimal("10.00"), LocalDate.now(), 1L, List.of(1L)));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void violation_whenAmountNull() {
        Set<ConstraintViolation<ExpenseRequest>> violations = validator.validate(
                new ExpenseRequest("Courses", null, LocalDate.now(), 1L, List.of(1L)));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void violation_whenAmountZero() {
        Set<ConstraintViolation<ExpenseRequest>> violations = validator.validate(
                new ExpenseRequest("Courses", new BigDecimal("0.00"), LocalDate.now(), 1L, List.of(1L)));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void violation_whenAmountNegative() {
        Set<ConstraintViolation<ExpenseRequest>> violations = validator.validate(
                new ExpenseRequest("Courses", new BigDecimal("-5.00"), LocalDate.now(), 1L, List.of(1L)));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void violation_whenAmountHasThreeDecimals() {
        Set<ConstraintViolation<ExpenseRequest>> violations = validator.validate(
                new ExpenseRequest("Courses", new BigDecimal("10.123"), LocalDate.now(), 1L, List.of(1L)));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void violation_whenAmountHasTooManyIntegerDigits() {
        Set<ConstraintViolation<ExpenseRequest>> violations = validator.validate(
                new ExpenseRequest("Courses", new BigDecimal("12345678901.00"), LocalDate.now(), 1L, List.of(1L)));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void noViolation_whenAmountHasExactlyEightIntegerDigits() {
        Set<ConstraintViolation<ExpenseRequest>> violations = validator.validate(
                new ExpenseRequest("Courses", new BigDecimal("12345678.00"), LocalDate.now(), 1L, List.of(1L)));
        assertThat(violations).isEmpty();
    }

    @Test
    void violation_whenAmountHasNineIntegerDigits() {
        Set<ConstraintViolation<ExpenseRequest>> violations = validator.validate(
                new ExpenseRequest("Courses", new BigDecimal("123456789.00"), LocalDate.now(), 1L, List.of(1L)));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void violation_whenExpenseDateNull() {
        Set<ConstraintViolation<ExpenseRequest>> violations = validator.validate(
                new ExpenseRequest("Courses", new BigDecimal("10.00"), null, 1L, List.of(1L)));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void violation_whenPaidByParticipantIdNull() {
        Set<ConstraintViolation<ExpenseRequest>> violations = validator.validate(
                new ExpenseRequest("Courses", new BigDecimal("10.00"), LocalDate.now(), null, List.of(1L)));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void violation_whenBeneficiaryListNull() {
        Set<ConstraintViolation<ExpenseRequest>> violations = validator.validate(
                new ExpenseRequest("Courses", new BigDecimal("10.00"), LocalDate.now(), 1L, null));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void violation_whenBeneficiaryListEmpty() {
        Set<ConstraintViolation<ExpenseRequest>> violations = validator.validate(
                new ExpenseRequest("Courses", new BigDecimal("10.00"), LocalDate.now(), 1L, List.of()));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void violation_whenBeneficiaryListContainsNull() {
        Set<ConstraintViolation<ExpenseRequest>> violations = validator.validate(
                new ExpenseRequest("Courses", new BigDecimal("10.00"), LocalDate.now(), 1L, Arrays.asList(1L, null)));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void violation_whenBeneficiaryListContainsDuplicates() {
        Set<ConstraintViolation<ExpenseRequest>> violations = validator.validate(
                new ExpenseRequest("Courses", new BigDecimal("10.00"), LocalDate.now(), 1L, List.of(1L, 2L, 1L)));
        assertThat(violations).isNotEmpty();
    }
}
