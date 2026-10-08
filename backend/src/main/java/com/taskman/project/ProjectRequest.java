package com.taskman.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record ProjectRequest(@NotBlank @Size(max = 120) String name) {
}
