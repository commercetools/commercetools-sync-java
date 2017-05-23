package com.commercetools.sync.inventories;

/**
 * Indicates whether sync meets problem which makes further sync unavailable. It may refer to different scope of data,
 * that are unable to be synced.
 * e.g. May occur during fetching supply channels what makes all drafts unable to be synced.
 * e.g.2. May occur during fetching batch of old inventory entries what makes batch of draft unable to be synced.
 */
class SyncProblemException extends RuntimeException {
}
