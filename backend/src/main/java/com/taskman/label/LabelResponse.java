package com.taskman.label;

import java.time.Instant;
import java.util.UUID;

record LabelResponse(UUID id, String name, Instant createdAt, Instant updatedAt) {

	static LabelResponse from(Label label) {
		return new LabelResponse(label.getId(), label.getName(), label.getCreatedAt(), label.getUpdatedAt());
	}

}
