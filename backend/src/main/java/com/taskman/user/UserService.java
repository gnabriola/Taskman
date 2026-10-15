package com.taskman.user;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class UserService {

	private final AppUserRepository users;

	private final PasswordEncoder passwordEncoder;

	UserService(AppUserRepository users, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	void register(RegisterRequest request) {
		String email = request.email().strip().toLowerCase(Locale.ROOT);
		if (users.existsByEmail(email)) {
			throw new EmailTakenException();
		}
		users.save(new AppUser(email, request.displayName().strip(), passwordEncoder.encode(request.password())));
	}

}
