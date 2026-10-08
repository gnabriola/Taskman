import { Component, output } from '@angular/core';

@Component({
  selector: 'app-add-task-form',
  template: `
    <form (submit)="submit($event, titleInput)" class="flex gap-2">
      <input
        #titleInput
        type="text"
        maxlength="500"
        placeholder="Add a task and press Enter"
        aria-label="New task title"
        class="flex-1 rounded border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none"
      />
      <button
        type="submit"
        class="rounded bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700"
      >
        Add
      </button>
    </form>
  `,
})
export class AddTaskForm {
  readonly add = output<string>();

  protected submit(event: Event, input: HTMLInputElement): void {
    event.preventDefault();
    const title = input.value.trim();
    if (title) {
      this.add.emit(title);
      input.value = '';
    }
  }
}
