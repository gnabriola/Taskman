package com.taskman.shared.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** The value must be an IANA time zone id such as "Europe/Berlin". {@code null} is valid. */
@Documented
@Constraint(validatedBy = IanaTimeZoneValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface IanaTimeZone {

	String message() default "must be a valid IANA time zone, e.g. Europe/Berlin";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
