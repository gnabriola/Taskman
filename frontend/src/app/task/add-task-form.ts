import { Component, ElementRef, output, viewChild } from '@angular/core';

@Component({
  selector: 'app-add-task-form',
  template: `
    <form (submit)="submit($event)" class="flex gap-2">
      <input
        #textInput
        type="text"
        maxlength="500"
        placeholder="e.g. pay rent tomorrow 5pm #home p1"
        aria-label="New task"
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

  private readonly input = viewChild.required<ElementRef<HTMLInputElement>>('textInput');

  /** Called by the parent once the task was created, so rejected text is not lost. */
  clear(): void {
    this.input().nativeElement.value = '';
  }

  protected submit(event: Event): void {
    event.preventDefault();
    const text = this.input().nativeElement.value.trim();
    if (text) {
      this.add.emit(text);
    }
  }
}
