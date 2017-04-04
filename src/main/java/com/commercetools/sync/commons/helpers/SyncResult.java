package com.commercetools.sync.commons.helpers;

import io.sphere.sdk.commands.UpdateAction;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.*;

import static com.commercetools.sync.commons.utils.StatisticsUtils.getReportTitle;
import static java.lang.String.format;

// TODO: UNIT TEST
// TODO: DOCUMENT
public class SyncResult<V> {

    private final List<UpdateAction<V>> updateActions;
    private final List<SyncError> errors;
    private final List<SyncWarning> warnings;
    private final Map<String, Integer> statistics;

    protected SyncResult(@Nonnull final List<UpdateAction<V>> updateActions,
                         @Nonnull final List<SyncError> errors,
                         @Nonnull final List<SyncWarning> warnings,
                         @Nonnull final Map<String, Integer> statistics) {
        this.updateActions = updateActions;
        this.errors = errors;
        this.warnings = warnings;
        this.statistics = statistics;
    }

    protected SyncResult() {
        this.updateActions = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.statistics = new HashMap<>();
    }

    public List<UpdateAction<V>> getUpdateActions() {
        return updateActions;
    }

    public List<SyncError> getErrors() {
        return errors;
    }

    public List<SyncWarning> getWarnings() {
        return warnings;
    }

    public Map<String, Integer> getStatistics() {
        return statistics;
    }

    /**
     * Merges a new SyncResult into this one.
     *
     * @param syncResult
     */
    public void merge(@Nonnull final SyncResult<V> syncResult) {
        this.updateActions.addAll(syncResult.getUpdateActions());
        this.errors.addAll(syncResult.getErrors());
        this.warnings.addAll(syncResult.getWarnings());
        mergeStatistics(syncResult);
    }

    public void mergeErrorsAndWarnings(@Nonnull final SyncResult<V> syncResult) {
        this.errors.addAll(syncResult.getErrors());
        this.warnings.addAll(syncResult.getWarnings());
    }

    public void mergeStatistics(@Nonnull final SyncResult<V> syncResult) {
        syncResult.statistics.forEach((key, value) -> this.statistics.merge(key, value, Integer::sum));
    }

    public static <V> SyncResult<V> of(@Nonnull final List<UpdateAction<V>> updateActions,
                                       @Nonnull final List<SyncError> errors,
                                       @Nonnull final List<SyncWarning> warnings,
                                       @Nonnull final Map<String, Integer> statistics) {
        return new SyncResult<>(updateActions, errors, warnings, statistics);
    }

    public static <V> SyncResult<V> emptyResult() {
        return new SyncResult<>();
    }

    public static <V> SyncResult<V> of(@Nonnull final UpdateAction<V> updateAction) {
        return of(new ArrayList<>(Arrays.asList(updateAction)), new ArrayList<>(), new ArrayList<>(), new HashMap<>());
    }

    public static <V> SyncResult<V> of(@Nonnull final List<UpdateAction<V>> updateActions) {
        return of(updateActions, new ArrayList<>(), new ArrayList<>(), new HashMap<>());
    }

    public static <V> SyncResult<V> ofUpdateAction(@Nonnull final UpdateAction<V> updateAction) {
        return of(new ArrayList<>(Arrays.asList(updateAction)), new ArrayList<>(), new ArrayList<>(), new HashMap<>());
    }

    public static <V> SyncResult<V> ofWarning(@Nonnull final SyncWarning warning) {
        return of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(Arrays.asList(warning)), new HashMap<>());
    }

    public static <V> SyncResult<V> ofError(@Nonnull final SyncError error) {
        return of(new ArrayList<>(), new ArrayList<>(Arrays.asList(error)), new ArrayList<>(), new HashMap<>());
    }

    public static <V> SyncResult<V> ofStatistic(@Nonnull final String statisticKey) {
        final HashMap<String, Integer> statistic = new HashMap<>();
        statistic.put(statisticKey, 1);
        return of(new ArrayList<>(), new ArrayList<>(Arrays.asList()), new ArrayList<>(), statistic);
    }

    public void addStatistic(@Nonnull final String statisticKey) {
        final Integer statValue = statistics.get(statisticKey);
        if (statValue != null) {
            statistics.replace(statisticKey, statValue + 1);
        } else {
            statistics.put(statisticKey, 1);
        }
    }

    public void addError(@Nonnull final SyncError error) {
        errors.add(error);
    }

    public void addWarning(@Nonnull final SyncWarning warning) {
        warnings.add(warning);
    }

    public String getResultStatistics() {
        return getStatisticsContent() + getWarningsReport() + getErrorsReport();
    }

    private String getWarningsReport() {
        final String title = getReportTitle("warnings", warnings.size());
        final String reportContent = getWarningsReportContent();
        return format("%s%s", title, reportContent);
    }

    private String getErrorsReport() {
        final String title = getReportTitle("errors", errors.size());
        final String reportContent = getErrorsReportContent();
        return format("%s%s", title, reportContent);
    }

    public String getErrorsReportContent() {
        final String messageFormat = "%d. %s\n\t| Resource ID: %s\n\t|Exception message:%s";
        String result = StringUtils.EMPTY;
        for (int i = 0; i < errors.size(); i++) {
            result += format(messageFormat, (i + 1), errors.get(i).getMessage(), errors.get(i).getResourceInternalId(),
                    errors.get(i).getException().getMessage());
        }
        return result;
    }

    private String getWarningsReportContent() {
        final String messageFormat = "%d. %s\n\t| Resource ID: %s\n";
        String result = StringUtils.EMPTY;
        for (int i = 0; i < warnings.size(); i++) {
            result += format(messageFormat, (i + 1), warnings.get(i).getMessage(), warnings.get(i).getResourceInternalId());
        }
        return result;
    }

    private String getStatisticsContent() {
        final String[] result = {StringUtils.EMPTY};
        statistics.keySet().forEach(message ->
                result[0] += message.replace("(x)", format("(%s)", statistics.get(message))) + "\n");
        return result[0] + "\n";
    }


}
