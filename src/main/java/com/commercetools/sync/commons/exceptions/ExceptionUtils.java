package com.commercetools.sync.commons.exceptions;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

final class ExceptionUtils {
    @Nonnull
    static String buildMessage(@Nonnull final String message, @Nonnull final Set<Throwable> causes,
                               final int causesTabbingDepth) {
        final String causesString = asMessage(causes, causesTabbingDepth);
        return isNotBlank(causesString) ? format("%s %s", message, causesString) :  message;
    }

    @Nonnull
    private static String asMessage(@Nonnull final Set<Throwable> causes, final int tabbingDepth) {
        final String tabs = buildTabs(tabbingDepth);
        final List<String> causesMessages = causes
            .stream()
            .map(Throwable::getMessage)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
        return !causesMessages.isEmpty() ? buildCausesMessage(tabs, causesMessages) : "";
    }

    @Nonnull
    private static String buildCausesMessage(@Nonnull final String tabs, @Nonnull final List<String> causesMessages) {
        return format("Causes:%n%s%s", tabs, joinCauses(tabs, causesMessages));
    }

    @Nonnull
    private static String joinCauses(@Nonnull final String tabs, @Nonnull final List<String> causesMessages) {
        return String.join(format("%n%s", tabs), causesMessages);
    }

    @Nonnull
    private static String buildTabs(final int numberOfTabs) {
        return IntStream.range(0, numberOfTabs)
                        .mapToObj(index -> "\t")
                        .collect(Collectors.joining());
    }

    private ExceptionUtils() {
    }
}
