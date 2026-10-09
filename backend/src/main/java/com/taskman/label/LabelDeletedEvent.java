package com.taskman.label;

import java.util.UUID;

/**
 * Published inside the deleting transaction, before the label row is removed, so
 * listeners can drop references to the label.
 */
public record LabelDeletedEvent(UUID labelId) {
}
