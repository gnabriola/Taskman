package com.taskman.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.taskman.shared.security.CurrentUser;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The logged-in user as Spring Security sees it, stored in the HTTP session.
 * Serializable so sessions can later move to Redis. The password hash is only needed
 * while logging in and is erased afterwards ({@link CredentialsContainer}), so it never
 * sits in the session.
 */
public final class UserPrincipal implements UserDetails, CredentialsContainer, CurrentUser, Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private final UUID id;

	private final String email;

	private final String displayName;

	private String passwordHash;

	UserPrincipal(UUID id, String email, String displayName, String passwordHash) {
		this.id = id;
		this.email = email;
		this.displayName = displayName;
		this.passwordHash = passwordHash;
	}

	static UserPrincipal from(AppUser user) {
		return new UserPrincipal(user.getId(), user.getEmail(), user.getDisplayName(), user.getPasswordHash());
	}

	@Override
	public UUID id() {
		return id;
	}

	public String email() {
		return email;
	}

	public String displayName() {
		return displayName;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of();
	}

	@Override
	public void eraseCredentials() {
		passwordHash = null;
	}

}
