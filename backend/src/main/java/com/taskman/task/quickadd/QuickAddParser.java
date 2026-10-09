package com.taskman.task.quickadd;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.taskman.task.recurrence.Frequency;
import com.taskman.task.recurrence.Recurrence;

/**
 * Parses Todoist-style quick-add text such as {@code "pay rent tomorrow 5pm #home p1"}.
 * <p>
 * The text is split into words. Walking left to right, small rules try to match a
 * date, a time, a recurrence, a {@code #project}, {@code @labels} or a priority
 * starting at the current word. Only
 * the first date and the first time are used; every word no rule consumed becomes
 * the title. A recurrence ("every monday", "every! 3 days") gives the first due date
 * unless the text also names a date. The behaviour is specified by
 * {@code QuickAddParserTest}.
 */
public class QuickAddParser {

	private static final Pattern PROJECT = Pattern.compile("#(\\S+)");

	/** Same characters as allowed in label names: no whitespace, @ or #. */
	private static final Pattern LABEL = Pattern.compile("@([^\\s@#]+)");

	private static final Pattern PRIORITY = Pattern.compile("p([1-4])", Pattern.CASE_INSENSITIVE);

	private static final Pattern TIME_12H = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?(am|pm)");

	private static final Pattern HOUR_MINUTE = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?");

	private static final Pattern TIME_24H = Pattern.compile("(\\d{1,2}):(\\d{2})");

	private static final Pattern NUMBER = Pattern.compile("\\d{1,3}");

	private static final Pattern ORDINAL = Pattern.compile("(\\d{1,2})(st|nd|rd|th)");

	private static final Map<String, DayOfWeek> WEEKDAYS = Map.ofEntries(Map.entry("monday", DayOfWeek.MONDAY),
			Map.entry("mon", DayOfWeek.MONDAY), Map.entry("tuesday", DayOfWeek.TUESDAY),
			Map.entry("tue", DayOfWeek.TUESDAY), Map.entry("tues", DayOfWeek.TUESDAY),
			Map.entry("wednesday", DayOfWeek.WEDNESDAY), Map.entry("wed", DayOfWeek.WEDNESDAY),
			Map.entry("thursday", DayOfWeek.THURSDAY), Map.entry("thu", DayOfWeek.THURSDAY),
			Map.entry("thurs", DayOfWeek.THURSDAY), Map.entry("friday", DayOfWeek.FRIDAY),
			Map.entry("fri", DayOfWeek.FRIDAY), Map.entry("saturday", DayOfWeek.SATURDAY),
			Map.entry("sat", DayOfWeek.SATURDAY), Map.entry("sunday", DayOfWeek.SUNDAY),
			Map.entry("sun", DayOfWeek.SUNDAY));

	private static final Map<String, Month> MONTHS = Map.ofEntries(Map.entry("jan", Month.JANUARY),
			Map.entry("january", Month.JANUARY), Map.entry("feb", Month.FEBRUARY), Map.entry("february", Month.FEBRUARY),
			Map.entry("mar", Month.MARCH), Map.entry("march", Month.MARCH), Map.entry("apr", Month.APRIL),
			Map.entry("april", Month.APRIL), Map.entry("may", Month.MAY), Map.entry("jun", Month.JUNE),
			Map.entry("june", Month.JUNE), Map.entry("jul", Month.JULY), Map.entry("july", Month.JULY),
			Map.entry("aug", Month.AUGUST), Map.entry("august", Month.AUGUST), Map.entry("sep", Month.SEPTEMBER),
			Map.entry("sept", Month.SEPTEMBER), Map.entry("september", Month.SEPTEMBER),
			Map.entry("oct", Month.OCTOBER), Map.entry("october", Month.OCTOBER), Map.entry("nov", Month.NOVEMBER),
			Map.entry("november", Month.NOVEMBER), Map.entry("dec", Month.DECEMBER),
			Map.entry("december", Month.DECEMBER));

	/** A rule matched {@code length} words starting at the current position. */
	private record Match<T>(T value, int length) {
	}

	@FunctionalInterface
	private interface Rule<T> {

		Optional<Match<T>> apply(List<String> words, int start, LocalDate today);

	}

	private final List<Rule<LocalDate>> dateRules = List.of(QuickAddParser::relativeDay, QuickAddParser::nextWeek,
			QuickAddParser::weekday, QuickAddParser::inPeriod, QuickAddParser::isoDate, QuickAddParser::monthDay,
			QuickAddParser::dayMonth);

	private final List<Rule<LocalTime>> timeRules = List.of(QuickAddParser::time12h, QuickAddParser::time12hSpaced,
			QuickAddParser::time24h);

	public ParsedTask parse(String text, LocalDateTime now) {
		String[] original = text.strip().split("\\s+");
		List<String> words = new ArrayList<>();
		for (String word : original) {
			words.add(word.toLowerCase(Locale.ROOT));
		}
		boolean[] consumed = new boolean[original.length];
		LocalDate today = now.toLocalDate();

		LocalDate dueDate = null;
		LocalTime dueTime = null;
		String projectName = null;
		Integer priority = null;
		Recurrence recurrence = null;
		// Keyed by lower case, so "@errands @Errands" yields one label, as first written.
		Map<String, String> labels = new LinkedHashMap<>();

		for (int i = 0; i < words.size(); i++) {
			if (consumed[i] || words.get(i).isEmpty()) {
				continue;
			}
			Matcher project = PROJECT.matcher(original[i]);
			if (projectName == null && project.matches()) {
				projectName = project.group(1);
				consumed[i] = true;
				continue;
			}
			Matcher label = LABEL.matcher(original[i]);
			if (label.matches()) {
				labels.putIfAbsent(label.group(1).toLowerCase(Locale.ROOT), label.group(1));
				consumed[i] = true;
				continue;
			}
			if (recurrence == null) {
				Optional<Match<Recurrence>> repeat = recurrence(words, i, today);
				if (repeat.isPresent()) {
					recurrence = repeat.get().value();
					consume(consumed, words, i, repeat.get().length(), "");
					continue;
				}
			}
			Matcher prio = PRIORITY.matcher(words.get(i));
			if (priority == null && prio.matches()) {
				priority = Integer.valueOf(prio.group(1));
				consumed[i] = true;
				continue;
			}
			if (dueDate == null) {
				Optional<Match<LocalDate>> date = firstMatch(dateRules, words, i, today);
				if (date.isPresent()) {
					dueDate = date.get().value();
					consume(consumed, words, i, date.get().length(), "on");
					continue;
				}
			}
			if (dueTime == null) {
				Optional<Match<LocalTime>> time = firstMatch(timeRules, words, i, today);
				if (time.isPresent()) {
					dueTime = time.get().value();
					consume(consumed, words, i, time.get().length(), "at");
				}
			}
		}

		if (recurrence != null && dueDate == null) {
			dueDate = recurrence.firstOnOrAfter(today);
			if (dueDate.equals(today) && dueTime != null && !dueTime.isAfter(now.toLocalTime())) {
				// Today's occurrence is already past: start with the next one.
				dueDate = recurrence.nextAfter(today);
			}
		}
		if (dueTime != null && dueDate == null) {
			// A time on its own means its next occurrence: today if still ahead, else tomorrow.
			dueDate = dueTime.isAfter(now.toLocalTime()) ? today : today.plusDays(1);
		}

		List<String> titleWords = new ArrayList<>();
		for (int i = 0; i < original.length; i++) {
			if (!consumed[i] && !original[i].isEmpty()) {
				titleWords.add(original[i]);
			}
		}
		return new ParsedTask(String.join(" ", titleWords), dueDate, dueTime, projectName, priority, recurrence,
				List.copyOf(labels.values()));
	}

	private static <T> Optional<Match<T>> firstMatch(List<Rule<T>> rules, List<String> words, int start,
			LocalDate today) {
		for (Rule<T> rule : rules) {
			Optional<Match<T>> match = rule.apply(words, start, today);
			if (match.isPresent()) {
				return match;
			}
		}
		return Optional.empty();
	}

	/** Marks the matched words, plus a directly preceding filler word like "on" or "at". */
	private static void consume(boolean[] consumed, List<String> words, int start, int length, String filler) {
		for (int i = start; i < start + length; i++) {
			consumed[i] = true;
		}
		if (start > 0 && !consumed[start - 1] && words.get(start - 1).equals(filler)) {
			consumed[start - 1] = true;
		}
	}

	// ---- Recurrence rule ----

	/**
	 * "every day", "every 3 weeks", "every other friday", "every mon, wed and fri",
	 * "every weekday", "every 15th", "every oct 31", and the same with "every!".
	 * Plain "every week/month/year" keeps today's weekday or date as its anchor;
	 * "every!" has no anchor because it counts from the completion date.
	 */
	private static Optional<Match<Recurrence>> recurrence(List<String> words, int start, LocalDate today) {
		String keyword = words.get(start);
		if (!keyword.equals("every") && !keyword.equals("every!")) {
			return Optional.empty();
		}
		boolean fromCompletion = keyword.equals("every!");
		// A date of the year first, so the 7 in "every 7 october" is not read as an interval.
		if (start + 2 < words.size()) {
			String first = words.get(start + 1);
			String second = words.get(start + 2);
			Optional<Recurrence> yearly = yearlyOn(first, second, fromCompletion)
				.or(() -> yearlyOn(second, first, fromCompletion));
			if (yearly.isPresent()) {
				return Optional.of(new Match<>(yearly.get(), 3));
			}
		}
		int next = start + 1;
		int interval = 1;
		if (next < words.size() && words.get(next).equals("other")) {
			interval = 2;
			next++;
		}
		else if (next < words.size() && NUMBER.matcher(words.get(next)).matches()) {
			interval = Integer.parseInt(words.get(next));
			if (interval < 1) {
				return Optional.empty();
			}
			next++;
		}
		if (next >= words.size()) {
			return Optional.empty();
		}
		String word = words.get(next);
		int length = next + 1 - start;

		Frequency unit = switch (word) {
			case "day", "days" -> Frequency.DAILY;
			case "week", "weeks" -> Frequency.WEEKLY;
			case "month", "months" -> Frequency.MONTHLY;
			case "year", "years" -> Frequency.YEARLY;
			default -> null;
		};
		if (unit != null) {
			return Optional.of(new Match<>(unitRecurrence(unit, interval, today, fromCompletion), length));
		}
		if (interval == 1 && (word.equals("weekday") || word.equals("weekend"))) {
			Set<DayOfWeek> days = word.equals("weekday") ? EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
					: EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
			return Optional.of(new Match<>(new Recurrence(Frequency.WEEKLY, 1, days, null, null, fromCompletion),
					length));
		}
		Optional<Match<Set<DayOfWeek>>> days = weekdayList(words, next);
		if (days.isPresent() && interval <= 2) {
			Recurrence weekly = new Recurrence(Frequency.WEEKLY, interval, days.get().value(), null, null,
					fromCompletion);
			return Optional.of(new Match<>(weekly, next - start + days.get().length()));
		}
		if (interval != 1) {
			return Optional.empty();
		}
		Matcher ordinal = ORDINAL.matcher(word);
		if (ordinal.matches()) {
			int day = Integer.parseInt(ordinal.group(1));
			if (day < 1 || day > 31) {
				return Optional.empty();
			}
			return Optional.of(new Match<>(new Recurrence(Frequency.MONTHLY, 1, null, day, null, fromCompletion),
					length));
		}
		return Optional.empty();
	}

	private static Recurrence unitRecurrence(Frequency unit, int interval, LocalDate today, boolean fromCompletion) {
		if (fromCompletion) {
			return new Recurrence(unit, interval, null, null, null, true);
		}
		return switch (unit) {
			case DAILY -> new Recurrence(unit, interval, null, null, null, false);
			case WEEKLY -> new Recurrence(unit, interval, Set.of(today.getDayOfWeek()), null, null, false);
			case MONTHLY -> new Recurrence(unit, interval, null, today.getDayOfMonth(), null, false);
			case YEARLY -> new Recurrence(unit, interval, null, today.getDayOfMonth(), today.getMonth(), false);
		};
	}

	/** "mon", "mon, wed, fri", "mon,fri", "tue and thu". */
	private static Optional<Match<Set<DayOfWeek>>> weekdayList(List<String> words, int start) {
		Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
		int consumed = 0;
		for (int i = start; i < words.size(); i++) {
			String word = words.get(i);
			if (word.equals("and") && !days.isEmpty() && i + 1 < words.size()
					&& WEEKDAYS.containsKey(stripComma(words.get(i + 1)))) {
				continue;
			}
			Set<DayOfWeek> found = EnumSet.noneOf(DayOfWeek.class);
			for (String part : stripComma(word).split(",")) {
				DayOfWeek day = WEEKDAYS.get(part);
				if (day == null) {
					found = null;
					break;
				}
				found.add(day);
			}
			if (found == null) {
				break;
			}
			days.addAll(found);
			consumed = i - start + 1;
		}
		return days.isEmpty() ? Optional.empty() : Optional.of(new Match<>(days, consumed));
	}

	private static String stripComma(String word) {
		return word.endsWith(",") ? word.substring(0, word.length() - 1) : word;
	}

	/** A yearly rule from a month and a day such as "oct" and "31"; Feb 29 is allowed. */
	private static Optional<Recurrence> yearlyOn(String monthWord, String dayWord, boolean fromCompletion) {
		Month month = MONTHS.get(monthWord);
		if (month == null || !NUMBER.matcher(dayWord).matches()) {
			return Optional.empty();
		}
		int day = Integer.parseInt(dayWord);
		if (day < 1 || day > month.maxLength()) {
			return Optional.empty();
		}
		return Optional.of(new Recurrence(Frequency.YEARLY, 1, null, day, month, fromCompletion));
	}

	// ---- Date rules ----

	private static Optional<Match<LocalDate>> relativeDay(List<String> words, int start, LocalDate today) {
		return switch (words.get(start)) {
			case "today", "tod" -> Optional.of(new Match<>(today, 1));
			case "tomorrow", "tmr", "tmrw" -> Optional.of(new Match<>(today.plusDays(1), 1));
			default -> Optional.empty();
		};
	}

	/** "next week" is next Monday; "next friday" is the same as "friday". */
	private static Optional<Match<LocalDate>> nextWeek(List<String> words, int start, LocalDate today) {
		if (!words.get(start).equals("next") || start + 1 >= words.size()) {
			return Optional.empty();
		}
		String next = words.get(start + 1);
		if (next.equals("week")) {
			return Optional.of(new Match<>(today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)), 2));
		}
		DayOfWeek day = WEEKDAYS.get(next);
		return (day != null) ? Optional.of(new Match<>(today.with(TemporalAdjusters.next(day)), 2))
				: Optional.empty();
	}

	/** A weekday always means the next one strictly after today. */
	private static Optional<Match<LocalDate>> weekday(List<String> words, int start, LocalDate today) {
		DayOfWeek day = WEEKDAYS.get(words.get(start));
		return (day != null) ? Optional.of(new Match<>(today.with(TemporalAdjusters.next(day)), 1))
				: Optional.empty();
	}

	/** "in 3 days", "in 1 day", "in 2 weeks". */
	private static Optional<Match<LocalDate>> inPeriod(List<String> words, int start, LocalDate today) {
		if (!words.get(start).equals("in") || start + 2 >= words.size()
				|| !NUMBER.matcher(words.get(start + 1)).matches()) {
			return Optional.empty();
		}
		int amount = Integer.parseInt(words.get(start + 1));
		return switch (words.get(start + 2)) {
			case "day", "days" -> Optional.of(new Match<>(today.plusDays(amount), 3));
			case "week", "weeks" -> Optional.of(new Match<>(today.plusWeeks(amount), 3));
			default -> Optional.empty();
		};
	}

	private static Optional<Match<LocalDate>> isoDate(List<String> words, int start, LocalDate today) {
		try {
			return Optional.of(new Match<>(LocalDate.parse(words.get(start)), 1));
		}
		catch (DateTimeParseException ex) {
			return Optional.empty();
		}
	}

	/** "oct 31", "october 31". */
	private static Optional<Match<LocalDate>> monthDay(List<String> words, int start, LocalDate today) {
		if (start + 1 >= words.size()) {
			return Optional.empty();
		}
		return calendarDate(words.get(start), words.get(start + 1), today);
	}

	/** "31 oct". */
	private static Optional<Match<LocalDate>> dayMonth(List<String> words, int start, LocalDate today) {
		if (start + 1 >= words.size()) {
			return Optional.empty();
		}
		return calendarDate(words.get(start + 1), words.get(start), today);
	}

	/** A day and month without a year: this year, or next year if already past. */
	private static Optional<Match<LocalDate>> calendarDate(String monthWord, String dayWord, LocalDate today) {
		Month month = MONTHS.get(monthWord);
		if (month == null || !NUMBER.matcher(dayWord).matches()) {
			return Optional.empty();
		}
		try {
			LocalDate date = LocalDate.of(today.getYear(), month, Integer.parseInt(dayWord));
			return Optional.of(new Match<>(date.isBefore(today) ? date.plusYears(1) : date, 2));
		}
		catch (DateTimeException ex) {
			// e.g. "feb 30": not a date, so the words stay in the title
			return Optional.empty();
		}
	}

	// ---- Time rules ----

	/** "5pm", "9:30am". */
	private static Optional<Match<LocalTime>> time12h(List<String> words, int start, LocalDate today) {
		Matcher m = TIME_12H.matcher(words.get(start));
		return m.matches() ? twelveHour(m.group(1), m.group(2), m.group(3), 1) : Optional.empty();
	}

	/** "5 pm", "9:30 am". */
	private static Optional<Match<LocalTime>> time12hSpaced(List<String> words, int start, LocalDate today) {
		if (start + 1 >= words.size()) {
			return Optional.empty();
		}
		Matcher m = HOUR_MINUTE.matcher(words.get(start));
		String suffix = words.get(start + 1);
		if (!m.matches() || !(suffix.equals("am") || suffix.equals("pm"))) {
			return Optional.empty();
		}
		return twelveHour(m.group(1), m.group(2), suffix, 2);
	}

	/** "17:00", "9:30". A colon is required, so "1984" is not a time. */
	private static Optional<Match<LocalTime>> time24h(List<String> words, int start, LocalDate today) {
		Matcher m = TIME_24H.matcher(words.get(start));
		if (!m.matches()) {
			return Optional.empty();
		}
		int hour = Integer.parseInt(m.group(1));
		int minute = Integer.parseInt(m.group(2));
		if (hour > 23 || minute > 59) {
			return Optional.empty();
		}
		return Optional.of(new Match<>(LocalTime.of(hour, minute), 1));
	}

	private static Optional<Match<LocalTime>> twelveHour(String hourText, String minuteText, String suffix,
			int length) {
		int hour = Integer.parseInt(hourText);
		int minute = (minuteText != null) ? Integer.parseInt(minuteText) : 0;
		if (hour < 1 || hour > 12 || minute > 59) {
			return Optional.empty();
		}
		// 12am is midnight, 12pm is noon
		int hour24 = (hour % 12) + (suffix.equals("pm") ? 12 : 0);
		return Optional.of(new Match<>(LocalTime.of(hour24, minute), length));
	}

}
