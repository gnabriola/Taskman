package com.taskman.task.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses filter queries such as {@code (today | overdue) & #work} into a
 * {@link Filter} tree.
 * <p>
 * Grammar (EBNF; quoted text is typed literally, {@code { }} means "repeat"):
 *
 * <pre>
 * query   = or
 * or      = and { "|" and }
 * and     = not { "&amp;" not }
 * not     = "!" not | primary
 * primary = "(" or ")" | term
 * term    = today | tomorrow | overdue | "no date" | recurring | p1..p4 | #project | &#64;label
 * </pre>
 *
 * Each rule is one method below (recursive descent). Because {@code or} is built from
 * {@code and} parts, {@code &} binds tighter than {@code |}, as AND and OR do in SQL.
 * The behaviour is specified by {@code FilterParserTest}.
 */
public class FilterParser {

	private static final Pattern PRIORITY = Pattern.compile("p([1-4])", Pattern.CASE_INSENSITIVE);

	private static final String OPERATORS = "()&|!";

	private enum Kind {

		LPAREN, RPAREN, AND, OR, NOT, WORD, END

	}

	/** A piece of the query; {@code position} is 1-based, for error messages. */
	private record Token(Kind kind, String text, int position) {
	}

	public Filter parse(String query) {
		if (query == null || query.isBlank()) {
			throw new FilterSyntaxException("Filter is empty");
		}
		return new Parse(tokenize(query)).run();
	}

	// ---- Tokenizer: text -> tokens ----

	private static List<Token> tokenize(String query) {
		List<Token> tokens = new ArrayList<>();
		int i = 0;
		while (i < query.length()) {
			char c = query.charAt(i);
			if (Character.isWhitespace(c)) {
				i++;
				continue;
			}
			Kind operator = switch (c) {
				case '(' -> Kind.LPAREN;
				case ')' -> Kind.RPAREN;
				case '&' -> Kind.AND;
				case '|' -> Kind.OR;
				case '!' -> Kind.NOT;
				default -> null;
			};
			if (operator != null) {
				tokens.add(new Token(operator, String.valueOf(c), i + 1));
				i++;
				continue;
			}
			int start = i;
			while (i < query.length() && !Character.isWhitespace(query.charAt(i))
					&& OPERATORS.indexOf(query.charAt(i)) < 0) {
				i++;
			}
			tokens.add(new Token(Kind.WORD, query.substring(start, i), start + 1));
		}
		tokens.add(new Token(Kind.END, "", query.length() + 1));
		return tokens;
	}

	// ---- Parser: tokens -> tree, one method per grammar rule ----

	private static final class Parse {

		private final List<Token> tokens;

		private int index;

		Parse(List<Token> tokens) {
			this.tokens = tokens;
		}

		Filter run() {
			Filter filter = or();
			Token rest = peek();
			if (rest.kind() == Kind.RPAREN) {
				throw error("Unexpected ')' at position " + rest.position());
			}
			if (rest.kind() != Kind.END) {
				throw error("Expected & or | at position " + rest.position() + " but found '" + rest.text() + "'");
			}
			return filter;
		}

		/** or = and { "|" and } */
		private Filter or() {
			Filter left = and();
			while (accept(Kind.OR)) {
				left = new Filter.Or(left, and());
			}
			return left;
		}

		/** and = not { "&" not } */
		private Filter and() {
			Filter left = not();
			while (accept(Kind.AND)) {
				left = new Filter.And(left, not());
			}
			return left;
		}

		/** not = "!" not | primary */
		private Filter not() {
			if (accept(Kind.NOT)) {
				return new Filter.Not(not());
			}
			return primary();
		}

		/** primary = "(" or ")" | term */
		private Filter primary() {
			Token token = peek();
			if (accept(Kind.LPAREN)) {
				Filter inner = or();
				Token close = peek();
				if (!accept(Kind.RPAREN)) {
					throw error("Expected ')' at position " + close.position() + found(close));
				}
				return inner;
			}
			if (token.kind() == Kind.WORD) {
				index++;
				return term(token);
			}
			throw error("Expected a filter at position " + token.position() + found(token));
		}

		private Filter term(Token token) {
			String word = token.text().toLowerCase(Locale.ROOT);
			switch (word) {
				case "today":
					return new Filter.Today();
				case "tomorrow":
					return new Filter.Tomorrow();
				case "overdue":
					return new Filter.Overdue();
				case "recurring":
					return new Filter.Recurring();
				case "no":
					// "no date" is the only term made of two words.
					Token next = peek();
					if (next.kind() == Kind.WORD && next.text().equalsIgnoreCase("date")) {
						index++;
						return new Filter.NoDate();
					}
					throw unknown(token);
				default:
					break;
			}
			Matcher priority = PRIORITY.matcher(word);
			if (priority.matches()) {
				return new Filter.Priority(Integer.parseInt(priority.group(1)));
			}
			if (token.text().startsWith("#")) {
				return new Filter.Project(name(token, "a project name after '#'"));
			}
			if (token.text().startsWith("@")) {
				return new Filter.Label(name(token, "a label name after '@'"));
			}
			throw unknown(token);
		}

		private String name(Token token, String expected) {
			String name = token.text().substring(1);
			if (name.isEmpty()) {
				throw error("Expected " + expected + " at position " + token.position());
			}
			return name;
		}

		private Token peek() {
			return tokens.get(index);
		}

		private boolean accept(Kind kind) {
			if (peek().kind() == kind) {
				index++;
				return true;
			}
			return false;
		}

		private static String found(Token token) {
			return (token.kind() == Kind.END) ? " but the query ended" : " but found '" + token.text() + "'";
		}

		private static FilterSyntaxException unknown(Token token) {
			return error("Unknown filter '" + token.text() + "' at position " + token.position());
		}

		private static FilterSyntaxException error(String message) {
			return new FilterSyntaxException(message);
		}

	}

}
