package com.taskman.shared.validation;

import java.time.DateTimeException;
import java.time.ZoneId;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

class IanaTimeZoneValidator implements ConstraintValidator<IanaTimeZone, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		try {
			ZoneId.of(value);
			return true;
		}
		catch (DateTimeException ex) {
			return false;
		}
	}

}
