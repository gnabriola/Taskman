package com.taskman.task;

import com.taskman.label.LabelDeletedEvent;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Removes a label from all tasks when the label is deleted. Runs synchronously inside
 * the label's delete transaction, before the label row is removed, so the
 * task_label.label_id foreign key is never violated.
 */
@Component
class LabelDeletionListener {

	private final TaskRepository tasks;

	LabelDeletionListener(TaskRepository tasks) {
		this.tasks = tasks;
	}

	@EventListener
	void on(LabelDeletedEvent event) {
		tasks.removeLabelFromAllTasks(event.labelId());
	}

}
