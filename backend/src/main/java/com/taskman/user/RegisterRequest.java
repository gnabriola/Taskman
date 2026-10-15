package com.taskman.user;

import java.nio.charset.StandardCharsets;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Passwords are 8 to 72 characters. 72 is BCrypt's limit in bytes: BCrypt ignores
 * everything after the 72nd byte, so longer passwords are rejected instead of silently
 * truncated.
 */
record RegisterRequest(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 100) String displayName,
		@NotBlank @Size(min = 8, max = 72) String password) {

	/** Surrounding spaces (e.g. from copy-paste) are trimmed before validation. */
	RegisterRequest {
		email = (email != null) ? email.strip() : null;
		displayName = (displayName != null) ? displayName.strip() : null;
	}

	@AssertTrue(message = "must be at most 72 bytes (BCrypt's limit)")
	boolean isPassword() {
		return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
	}

	@Override
	public String toString() {
		// Never log the password.
		return "RegisterRequest[email=" + email + ", displayName=" + displayName + "]";
	}

}
