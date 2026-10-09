package com.taskman.task.recurrence;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * How a task repeats. Stored as a subset of an RFC 5545 RRULE, e.g.
 * {@code FREQ=WEEKLY;BYDAY=MO,WE,FR}, plus a flag for Todoist's "every!" (count from
 * the completion date instead of the due date).
 * <p>
 * Supported parts: {@code FREQ} (DAILY, WEEKLY, MONTHLY, YEARLY), {@code INTERVAL},
 * {@code BYDAY} (weekly only), {@code BYMONTHDAY} (monthly and yearly) and
 * {@code BYMONTH} (yearly, together with BYMONTHDAY). One deliberate deviation from
 * RFC 5545: a month day that does not exist in a month (the 31st in November) falls
 * on the month's last day instead of skipping the month.
 * <p>
 * The behaviour is specified by {@code RecurrenceTest}.
 */
public record Recurrence(Frequency frequency, int interval, Set<DayOfWeek> days, Integer monthDay, Month month,
		boolean fromCompletion) {

	private static final Set<DayOfWeek> WEEKDAYS = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

	private static final Set<DayOfWeek> WEEKEND = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

	private static final Map<String, DayOfWeek> DAY_CODES = Map.of("MO", DayOfWeek.MONDAY, "TU", DayOfWeek.TUESDAY,
			"WE", DayOfWeek.WEDNESDAY, "TH", DayOfWeek.THURSDAY, "FR", DayOfWeek.FRIDAY, "SA", DayOfWeek.SATURDAY,
			"SU", DayOfWeek.SUNDAY);

	private static final int MAX_INTERVAL = 999;

	public Recurrence {
		if (frequency == null) {
			throw new InvalidRecurrenceException("FREQ is required");
		}
		if (interval < 1 || interval > MAX_INTERVAL) {
			throw new InvalidRecurrenceException("INTERVAL must be between 1 and " + MAX_INTERVAL);
		}
		days = (days == null || days.isEmpty()) ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(days));
		if (!days.isEmpty() && frequency != Frequency.WEEKLY) {
			throw new InvalidRecurrenceException("BYDAY is only supported with FREQ=WEEKLY");
		}
		if (monthDay != null && (monthDay < 1 || monthDay > 31)) {
			throw new InvalidRecurrenceException("BYMONTHDAY must be between 1 and 31");
		}
		if (monthDay != null && frequency != Frequency.MONTHLY && frequency != Frequency.YEARLY) {
			throw new InvalidRecurrenceException("BYMONTHDAY is only supported with FREQ=MONTHLY or FREQ=YEARLY");
		}
		if (month != null && frequency != Frequency.YEARLY) {
			throw new InvalidRecurrenceException("BYMONTH is only supported with FREQ=YEARLY");
		}
		if (frequency == Frequency.YEARLY && (month == null) != (monthDay == null)) {
			throw new InvalidRecurrenceException("FREQ=YEARLY needs both BYMONTH and BYMONTHDAY, or neither");
		}
	}

	// ---- Rule text ----

	/** Parses rule text such as {@code FREQ=WEEKLY;BYDAY=MO,FR}. Part order and case do not matter. */
	public static Recurrence parse(String rule, boolean fromCompletion) {
		if (rule == null || rule.isBlank()) {
			throw new InvalidRecurrenceException("Rule is empty");
		}
		Map<String, String> parts = new LinkedHashMap<>();
		for (String part : rule.strip().toUpperCase(Locale.ROOT).split(";")) {
			String[] keyValue = part.split("=", -1);
			if (keyValue.length != 2 || keyValue[1].isEmpty()) {
				throw new InvalidRecurrenceException("Malformed part '" + part + "'");
			}
			if (parts.put(keyValue[0], keyValue[1]) != null) {
				throw new InvalidRecurrenceException(keyValue[0] + " appears twice");
			}
		}
		for (String key : parts.keySet()) {
			if (!List.of("FREQ", "INTERVAL", "BYDAY", "BYMONTHDAY", "BYMONTH").contains(key)) {
				throw new InvalidRecurrenceException(key + " is not supported");
			}
		}
		Frequency frequency = parseFrequency(parts.get("FREQ"));
		int interval = parts.containsKey("INTERVAL") ? parseNumber("INTERVAL", parts.get("INTERVAL")) : 1;
		Set<DayOfWeek> days = parts.containsKey("BYDAY") ? parseDays(parts.get("BYDAY")) : Set.of();
		Integer monthDay = parts.containsKey("BYMONTHDAY") ? parseNumber("BYMONTHDAY", parts.get("BYMONTHDAY"))
				: null;
		Month month = parts.containsKey("BYMONTH") ? parseMonth(parts.get("BYMONTH")) : null;
		return new Recurrence(frequency, interval, days, monthDay, month, fromCompletion);
	}

	/** The rule in a canonical form: fixed part order, weekdays Monday first, INTERVAL=1 omitted. */
	public String rule() {
		List<String> parts = new ArrayList<>();
		parts.add("FREQ=" + frequency);
		if (interval != 1) {
			parts.add("INTERVAL=" + interval);
		}
		if (!days.isEmpty()) {
			parts.add("BYDAY=" + days.stream().sorted().map(Recurrence::dayCode).collect(Collectors.joining(",")));
		}
		if (month != null) {
			parts.add("BYMONTH=" + month.getValue());
		}
		if (monthDay != null) {
			parts.add("BYMONTHDAY=" + monthDay);
		}
		return String.join(";", parts);
	}

	// ---- Occurrences ----

	/** The first occurrence strictly after {@code date}. */
	public LocalDate nextAfter(LocalDate date) {
		return switch (frequency) {
			case DAILY -> date.plusDays(interval);
			case WEEKLY -> days.isEmpty() ? date.plusWeeks(interval) : nextWeekday(date);
			case MONTHLY -> (monthDay == null) ? date.plusMonths(interval) : nextMonthDay(date);
			case YEARLY -> (month == null) ? date.plusYears(interval) : nextYearDay(date);
		};
	}

	/**
	 * The first occurrence on or after {@code date}; used for a new recurring task. A
	 * rule without a fixed weekday or month day starts on {@code date} itself.
	 */
	public LocalDate firstOnOrAfter(LocalDate date) {
		boolean anchored = !days.isEmpty() || monthDay != null;
		return anchored ? nextAfter(date.minusDays(1)) : date;
	}

	/**
	 * The due date after the occurrence due on {@code dueDate} is completed on
	 * {@code today}. Normally the next occurrence after the due date, but never one
	 * that is already past; with "every!" it is counted from today.
	 */
	public LocalDate nextDue(LocalDate dueDate, LocalDate today) {
		if (fromCompletion) {
			return nextAfter(today);
		}
		return nextAfter(dueDate.isAfter(today) ? dueDate : today);
	}

	private LocalDate nextWeekday(LocalDate date) {
		// The rest of the current week (weeks start on Monday)...
		for (LocalDate day = date.plusDays(1); day.getDayOfWeek() != DayOfWeek.MONDAY; day = day.plusDays(1)) {
			if (days.contains(day.getDayOfWeek())) {
				return day;
			}
		}
		// ...otherwise the first matching day `interval` weeks later.
		LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(interval);
		for (LocalDate day = weekStart;; day = day.plusDays(1)) {
			if (days.contains(day.getDayOfWeek())) {
				return day;
			}
		}
	}

	private LocalDate nextMonthDay(LocalDate date) {
		LocalDate sameMonth = onDay(YearMonth.from(date), monthDay);
		if (sameMonth.isAfter(date)) {
			return sameMonth;
		}
		return onDay(YearMonth.from(date).plusMonths(interval), monthDay);
	}

	private LocalDate nextYearDay(LocalDate date) {
		LocalDate sameYear = onDay(YearMonth.of(date.getYear(), month), monthDay);
		if (sameYear.isAfter(date)) {
			return sameYear;
		}
		return onDay(YearMonth.of(date.getYear() + interval, month), monthDay);
	}

	/** The given day of the month, or the month's last day if it is shorter. */
	private static LocalDate onDay(YearMonth month, int day) {
		return month.atDay(Math.min(day, month.lengthOfMonth()));
	}

	// ---- Label ----

	/** A short English description, e.g. "every Mon, Wed, Fri" or "every! 3 days". */
	public String label() {
		String every = fromCompletion ? "every!" : "every";
		return switch (frequency) {
			case DAILY -> every + " " + unit(interval, "day");
			case WEEKLY -> every + " " + weeklyLabel();
			case MONTHLY -> every + " " + ((monthDay == null) ? unit(interval, "month")
					: (interval == 1) ? ordinal(monthDay) : unit(interval, "month") + " on the " + ordinal(monthDay));
			case YEARLY -> every + " " + ((month == null) ? unit(interval, "year")
					: (interval == 1) ? monthDayText() : unit(interval, "year") + " on " + monthDayText());
		};
	}

	private String weeklyLabel() {
		if (days.isEmpty()) {
			return unit(interval, "week");
		}
		if (interval == 1 && days.equals(WEEKDAYS)) {
			return "weekday";
		}
		if (interval == 1 && days.equals(WEEKEND)) {
			return "weekend";
		}
		String dayText = (days.size() == 1) ? dayName(days.iterator().next(), TextStyle.FULL) : dayList(days);
		if (interval == 1) {
			return dayText;
		}
		if (interval == 2 && days.size() == 1) {
			return "other " + dayText;
		}
		return unit(interval, "week") + " on " + dayText;
	}

	private String monthDayText() {
		return month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + monthDay;
	}

	/** "day" for 1, "3 days" otherwise: "every day", "every 3 days". */
	private static String unit(int amount, String name) {
		return (amount == 1) ? name : amount + " " + name + "s";
	}

	private static String dayList(Collection<DayOfWeek> days) {
		return days.stream().sorted().map(day -> dayName(day, TextStyle.SHORT)).collect(Collectors.joining(", "));
	}

	private static String dayName(DayOfWeek day, TextStyle style) {
		return day.getDisplayName(style, Locale.ENGLISH);
	}

	private static String ordinal(int n) {
		if (n % 100 >= 11 && n % 100 <= 13) {
			return n + "th";
		}
		return n + switch (n % 10) {
			case 1 -> "st";
			case 2 -> "nd";
			case 3 -> "rd";
			default -> "th";
		};
	}

	// ---- Parsing helpers ----

	private static Frequency parseFrequency(String value) {
		if (value == null) {
			throw new InvalidRecurrenceException("FREQ is required");
		}
		try {
			return Frequency.valueOf(value);
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidRecurrenceException("FREQ=" + value + " is not supported");
		}
	}

	private static int parseNumber(String key, String value) {
		try {
			return Integer.parseInt(value);
		}
		catch (NumberFormatException ex) {
			throw new InvalidRecurrenceException(key + " must be a number");
		}
	}

	private static Set<DayOfWeek> parseDays(String value) {
		Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
		for (String code : value.split(",", -1)) {
			DayOfWeek day = DAY_CODES.get(code);
			if (day == null) {
				throw new InvalidRecurrenceException("Unknown weekday '" + code + "' in BYDAY");
			}
			days.add(day);
		}
		return days;
	}

	private static Month parseMonth(String value) {
		int number = parseNumber("BYMONTH", value);
		if (number < 1 || number > 12) {
			throw new InvalidRecurrenceException("BYMONTH must be between 1 and 12");
		}
		return Month.of(number);
	}

	private static String dayCode(DayOfWeek day) {
		return day.name().substring(0, 2);
	}

}
