package com.resumematch.matcher;

/**
 * Decisao do matcher para uma parte de um requisito em conjunto.
 */
public record ComponentMatch(String name, MatchStatus status, String evidence) {
}
