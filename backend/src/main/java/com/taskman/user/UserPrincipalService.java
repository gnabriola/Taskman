package com.taskman.user;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads users by email for Spring Security's password check. */
@Service
@Transactional(readOnly = true)
class UserPrincipalService implements UserDetailsService {

	private final AppUserRepository users;

	UserPrincipalService(AppUserRepository users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String email) {
		return users.findByEmail(email.strip().toLowerCase(Locale.ROOT))
			.map(UserPrincipal::from)
			.orElseThrow(() -> new UsernameNotFoundException("No user with this email"));
	}

}
