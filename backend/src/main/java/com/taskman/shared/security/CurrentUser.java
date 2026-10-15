package com.taskman.shared.security;

import java.util.UUID;

/**
 * The logged-in user, as other modules see it. Controllers receive it with
 * {@code @AuthenticationPrincipal CurrentUser user} and pass {@code user.id()} on to
 * services, so no module depends on the user module's classes.
 */
public interface CurrentUser {

	UUID id();

}
