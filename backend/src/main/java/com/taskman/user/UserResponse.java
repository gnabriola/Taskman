package com.taskman.user;

import java.util.UUID;

record UserResponse(UUID id, String email, String displayName) {

	static UserResponse from(UserPrincipal principal) {
		return new UserResponse(principal.id(), principal.email(), principal.displayName());
	}

}
