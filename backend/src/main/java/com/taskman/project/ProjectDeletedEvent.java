package com.taskman.project;

import java.util.UUID;

/**
 * Published inside the deleting transaction, before the project row is removed,
 * so listeners can clean up data that references the project.
 */
public record ProjectDeletedEvent(UUID projectId) {
}
