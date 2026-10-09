package com.taskman.task.filter;

/**
 * A parsed filter query: a tree of yes/no questions about a task, combined with
 * and/or/not. {@code toString()} prints the tree compactly, e.g.
 * {@code Or(Today, And(Overdue, Project(work)))}.
 * <p>
 * The interface is sealed, so a {@code switch} over a filter must handle every kind of
 * node; adding a node makes every such switch fail to compile until it is handled.
 */
public sealed interface Filter {

	record And(Filter left, Filter right) implements Filter {

		@Override
		public String toString() {
			return "And(" + left + ", " + right + ")";
		}

	}

	record Or(Filter left, Filter right) implements Filter {

		@Override
		public String toString() {
			return "Or(" + left + ", " + right + ")";
		}

	}

	record Not(Filter inner) implements Filter {

		@Override
		public String toString() {
			return "Not(" + inner + ")";
		}

	}

	/** Due today. */
	record Today() implements Filter {

		@Override
		public String toString() {
			return "Today";
		}

	}

	/** Due tomorrow. */
	record Tomorrow() implements Filter {

		@Override
		public String toString() {
			return "Tomorrow";
		}

	}

	/** Due before today, or earlier today if the task has a due time. */
	record Overdue() implements Filter {

		@Override
		public String toString() {
			return "Overdue";
		}

	}

	/** Has no due date. */
	record NoDate() implements Filter {

		@Override
		public String toString() {
			return "NoDate";
		}

	}

	/** Repeats. */
	record Recurring() implements Filter {

		@Override
		public String toString() {
			return "Recurring";
		}

	}

	/** Has priority {@code level}, 1 (highest) to 4. */
	record Priority(int level) implements Filter {

		@Override
		public String toString() {
			return "Priority(" + level + ")";
		}

	}

	/** In the project with this name; resolving the name is the caller's job. */
	record Project(String name) implements Filter {

		@Override
		public String toString() {
			return "Project(" + name + ")";
		}

	}

	/** Has the label with this name; resolving the name is the caller's job. */
	record Label(String name) implements Filter {

		@Override
		public String toString() {
			return "Label(" + name + ")";
		}

	}

}
