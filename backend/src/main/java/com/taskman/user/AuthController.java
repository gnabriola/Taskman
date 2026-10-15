package com.taskman.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Register, log in and see who is logged in. Logging out is handled by Spring
 * Security at {@code POST /api/auth/logout}.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

	private final UserService userService;

	private final AuthenticationManager authenticationManager;

	private final SecurityContextRepository securityContextRepository;

	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

	private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

	AuthController(UserService userService, AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository,
			SessionAuthenticationStrategy sessionAuthenticationStrategy) {
		this.userService = userService;
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	UserResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		userService.register(request);
		return logIn(request.email(), request.password(), httpRequest, httpResponse);
	}

	/** Wrong email and wrong password give the same 401, so emails cannot be probed. */
	@PostMapping("/login")
	UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		return logIn(request.email(), request.password(), httpRequest, httpResponse);
	}

	@GetMapping("/me")
	UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
		return UserResponse.from(principal);
	}

	/**
	 * Checks the password, then makes the session authenticated. Since Spring Security 6
	 * the context is not saved automatically outside its own login filters, so this
	 * saves it to the session explicitly.
	 */
	private UserResponse logIn(String email, String password, HttpServletRequest request,
			HttpServletResponse response) {
		Authentication authentication = authenticationManager
			.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password));
		sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
		SecurityContext context = contextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		contextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
		return UserResponse.from((UserPrincipal) authentication.getPrincipal());
	}

}
