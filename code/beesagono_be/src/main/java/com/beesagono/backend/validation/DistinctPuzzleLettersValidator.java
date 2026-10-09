package com.beesagono.backend.validation;


import com.beesagono.backend.dto.puzzle.UpdatePuzzleLettersRequest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DistinctPuzzleLettersValidator
        implements ConstraintValidator<DistinctPuzzleLetters, UpdatePuzzleLettersRequest> {

    @Override
    public boolean isValid(UpdatePuzzleLettersRequest  request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        String center = request.getCenterLetter();
        var outer = request.getOuterLetters();

        // If either of the two fields is null, let @NotBlank/@NotEmpty handle the failure
        if (center == null || outer == null) {
            return true;
        }

        // Verify that none of the outer letters (case-insensitive) match the center letter
        boolean containsCenter = outer.stream()
                .anyMatch(letter -> letter != null && letter.equalsIgnoreCase(center.trim()));

        if (containsCenter) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "La lettera centrale non può comparire tra le lettere esterne.")
                    .addPropertyNode("outerLetters")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}