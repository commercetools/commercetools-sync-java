package com.commercetools.sync.commons.helpers;

import com.commercetools.sync.categories.helpers.CategoryCustomActionBuilder;
import com.commercetools.sync.channels.helpers.ChannelCustomActionBuilder;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.inventory.helpers.InventoryCustomActionBuilder;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.types.Custom;

import javax.annotation.Nonnull;

import static com.commercetools.sync.commons.enums.Error.UPDATE_ACTION_NOT_IMPLEMENTED;
import static java.lang.String.format;

/**
 * Factory class that has the main objective of creating an instance of the of a concrete implementation of the
 * {@link GenericCustomActionBuilder}, which is responsible for building custom update actions, according to the type
 * of the {@code resource} instance provided to the {@link GenericCustomActionBuilderFactory#of(Custom)} function.
 */
@SuppressWarnings("unchecked")
public class GenericCustomActionBuilderFactory<T extends Custom & Resource<T>, S extends GenericCustomActionBuilder<T>> {

    private S getBuilder(@Nonnull final T resource) throws BuildUpdateActionException {
        if (resource instanceof Category) {
            return (S) new CategoryCustomActionBuilder();
        }
        if (resource instanceof Channel) {
            return (S) new ChannelCustomActionBuilder();
        }
        if (resource instanceof InventoryEntry) {
            return (S) new InventoryCustomActionBuilder();
        }
        throw new BuildUpdateActionException(format(UPDATE_ACTION_NOT_IMPLEMENTED.getDescription(),
                resource.toReference().getTypeId()));
    }

    /**
     * Given a {@code resource} of the type U which represents a CTP resource (currently either Category or
     * Channel) creates an instance of the of a concrete implementation of the
     * {@link GenericCustomActionBuilder}, which is responsible for building custom update actions, according to the type
     * of the {@code resource} instance provided. For example, if U is of type {@link Category} this method will return
     * an instance of {@link CategoryCustomActionBuilder}, if U is of type {@link Channel} this method will return an
     * instance of {@link ChannelCustomActionBuilder} instance.
     *
     * @param resource the resource from which the type of the CTP resource to choose the update action builder.
     * @param <U>      the type of the resource.
     * @return an instance which represents a concrete implementation of the {@link GenericCustomActionBuilder}
     * which is responsible for building custom update actions.
     * @throws BuildUpdateActionException exception thrown in case a concrete implementation of the the {@link GenericCustomActionBuilder}
     *                                    for the provided resource type is not implemented yet.
     */
    public static <U extends Custom & Resource<U>> GenericCustomActionBuilder of(@Nonnull final U resource)
            throws BuildUpdateActionException {
        return new GenericCustomActionBuilderFactory().getBuilder(resource);
    }


}
