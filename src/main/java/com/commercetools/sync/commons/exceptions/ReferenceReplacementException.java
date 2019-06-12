package com.commercetools.sync.commons.exceptions;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ReferenceReplacementException extends RuntimeException {
    private Set<Throwable> causes;

    public ReferenceReplacementException(@Nonnull final String message, @Nonnull final Set<Throwable> causes) {
        super(buildMessage(message, causes));
        this.causes = causes;
    }

    @Nonnull
    private static String buildMessage(@Nonnull final String message, @Nonnull final Set<Throwable> causes) {
        final String causesString = asMessage(causes);
        return isNotBlank(causesString) ? format("%s Causes:%n\t%s", message, causesString) :  message;
    }

    @Nonnull
    private static String asMessage(@Nonnull final Set<Throwable> causes) {
        final List<String> causesMessages = causes
            .stream()
            .map(Throwable::getMessage)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
        return String.join("\n\t", causesMessages);
    }

    public Set<Throwable> getCauses() {
        return causes;
    }
}
