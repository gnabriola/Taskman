package com.taskman.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateTaskRequest(@NotBlank @Size(max = 500) String title, @Size(max = 10_000) String description) {
}
