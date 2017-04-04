package com.commercetools.sync.commons.utils;

import javax.annotation.Nonnull;
import java.util.Collections;

import static java.lang.String.format;

// TODO: UNIT TEST
// TODO: DOCUMENT
public class StatisticsUtils {

    public static String getReportTitle(final String itemsNameInPlural, final int numberOfItems) {
        final String title = format("There were (%d) %s that were found during the sync process.",
                numberOfItems, itemsNameInPlural);
        final String horizontalLine = getRepeatedString("-", 75);
        return format("%s\n%s", title, horizontalLine);
    }

    public static String getHorizontalLine(final int size) {
        return getRepeatedString("-", size);
    }

    public static String getRepeatedString(@Nonnull final String stringToRepeat, final int numberOfReps) {
        return format("%s\n", String.join("", Collections.nCopies(numberOfReps, stringToRepeat)));
    }
}
