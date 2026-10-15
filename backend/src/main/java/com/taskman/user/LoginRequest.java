package com.taskman.user;

import jakarta.validation.constraints.NotBlank;

record LoginRequest(@NotBlank String email, @NotBlank String password) {

	LoginRequest {
		email = (email != null) ? email.strip() : null;
	}

	@Override
	public String toString() {
		// Never log the password.
		return "LoginRequest[email=" + email + "]";
	}

}
