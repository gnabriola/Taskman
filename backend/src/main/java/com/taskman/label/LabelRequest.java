package com.taskman.label;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Label names are single words because they are written as {@code @name} in quick-add
 * and filters.
 */
record LabelRequest(@NotBlank @Size(max = 60) @Pattern(regexp = "[^\\s@#]+",
		message = "must be a single word without spaces, @ or #") String name) {
}
