package com.taskman.shared;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** A single source of "now", so code that depends on the current time can be tested. */
@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
