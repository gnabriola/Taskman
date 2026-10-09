package com.taskman.task.filter;

import com.taskman.shared.BadRequestException;

/** The query is not valid; the message names the 1-based position of the problem. */
public class FilterSyntaxException extends BadRequestException {

	FilterSyntaxException(String message) {
		super(message);
	}

}
