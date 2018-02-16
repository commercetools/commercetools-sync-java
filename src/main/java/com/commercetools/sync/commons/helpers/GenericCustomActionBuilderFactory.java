package com.commercetools.sync.commons.helpers;

import com.commercetools.sync.categories.helpers.CategoryCustomActionBuilder;
import com.commercetools.sync.channels.helpers.ChannelCustomActionBuilder;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.inventories.helpers.InventoryCustomActionBuilder;
import com.commercetools.sync.products.helpers.AssetCustomActionBuilder;
import com.commercetools.sync.products.helpers.PriceCustomActionBuilder;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.types.Custom;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

/**
 * Factory class that has the main objective of creating an instance of a concrete implementation of the
 * {@link GenericCustomActionBuilder}, which is responsible for building custom update actions, according to the type
 * of the {@code currentResource} instance and the {@code currentContainerResourceClass} provided to the
 * {@link GenericCustomActionBuilderFactory#of(Custom, Class)}} function.
 *
 * @param <T> Represents a resource that has custom fields. It could be a primary resource like Category,
 *            Channel, InventoryEntry, etc.. Or a secondary resource like a Product Price, Category Price, Asset, etc..
 * @param <S> Represents a concrete implementation of the {@link GenericCustomActionBuilder}.
 *            (e.g. {@link AssetCustomActionBuilder})
 * @param <U> Represents a primary resource Category, Product, Channel, InventoryEntry, etc.. This resource represents
 *            the endpoint that the update action will be issued on.
 */
@SuppressWarnings("unchecked")
public class GenericCustomActionBuilderFactory<T extends Custom, S extends GenericCustomActionBuilder,
    U extends Resource<U>> {
    private static final String UPDATE_ACTION_NOT_IMPLEMENTED =
        "Update actions for resource: '%s' and container resource: '%s' is not implemented.";

    /**
     * Container of enums that provides a mapping between the CTP resource that contains the custom fields, the CTP
     * resource that the update action will be performed on and the corresponding concrete custom action
     * builder class. For example, the {@link Category} resources point to the {@link CategoryCustomActionBuilder}
     * which contains implementations of update methods responsible for updating custom fields for categories. For
     * example, product assets will have a mapping that contains the {@link Asset} resource which contains the custom
     * fields, the {@link Product} resource which is the container resource of the asset where the update action will be
     * issued on and {@link AssetCustomActionBuilder} which contains the implementations of update method methods
     * responsible for updating custom fields for the asset on the product.
     *
     * <p>Note: To add new concrete custom builders for new CTP resources, a new mapping has to be adding in this enum.
     */
    enum ConcreteBuilder {
        PRODUCT_ASSET(AssetCustomActionBuilder.class, Asset.class, Product.class),
        CATEGORY_ASSET(
            com.commercetools.sync.categories.helpers.AssetCustomActionBuilder.class, Asset.class, Category.class),
        PRICE(PriceCustomActionBuilder.class, Price.class, Product.class),

        CATEGORY(CategoryCustomActionBuilder.class, Category.class, Category.class),
        CHANNEL(ChannelCustomActionBuilder.class, Channel.class, Channel.class),
        INVENTORY(InventoryCustomActionBuilder.class, InventoryEntry.class, InventoryEntry.class);

        private final Class<? extends GenericCustomActionBuilder> builderClass;
        private final Class<? extends Custom> resourceClass;
        private final Class<? extends Resource> containerResourceClass;

        ConcreteBuilder(@Nonnull final Class<? extends GenericCustomActionBuilder> builderClass,
                        @Nonnull final Class<? extends Custom> resourceClass,
                        @Nonnull final Class<? extends Resource> containerResourceClass) {
            this.builderClass = builderClass;
            this.resourceClass = resourceClass;
            this.containerResourceClass = containerResourceClass;
        }

        /**
         * Returns the concrete custom action builder of {@code this} instance of the enum.
         *
         * @return the concrete custom action builder of {@code this} instance of the enum.
         */
        public Class<? extends GenericCustomActionBuilder> getBuilderClass() {
            return builderClass;
        }

        /**
         * Returns the class of the resource that contains the custom fields of {@code this} instance of the enum.
         *
         * @return the class of the resource that contains the custom fields of {@code this} instance of the enum.
         */
        public Class<? extends Custom> getResourceClass() {
            return resourceClass;
        }

        /**
         * Returns the class of the container resource that the update action will be performed on of {@code this}
         * instance of the enum.
         *
         * @return the class of the container resource that the update action will be performed on of {@code this}
         *         instance of the enum.
         */
        public Class<? extends Resource> getContainerResourceClass() {
            return containerResourceClass;
        }
    }

    /**
     * Creates an instance of the concrete implementation of the {@link GenericCustomActionBuilder}, which is
     * responsible for building custom update actions, according to the type of the {@code currentResource} instance
     * and the {@code currentContainerResourceClass} provided. The method uses the {@link ConcreteBuilder} enum to find
     * the mapping of the concrete builder. The available mappings for resolution are:
     *
     * <ul>
     * <li>if {@code <T>} is of type {@link Asset} and the {@code currentContainerResourceClass} is of
     * type {@link Product}, this method will return an instance of {@link AssetCustomActionBuilder}</li>
     *
     * <li>if {@code <T>} is of type {@link Asset} and the {@code currentContainerResourceClass} is of
     * type {@link Category}, this method will return an instance of
     * {@link com.commercetools.sync.categories.helpers.AssetCustomActionBuilder}</li>
     *
     * <li>if {@code <T>} is of type {@link Price} and the {@code currentContainerResourceClass} is of type
     * {@link Product} this method will return an instance of {@link PriceCustomActionBuilder}</li>
     *
     * <li>if {@code <T>} is of type {@link Category} and the {@code currentContainerResourceClass} is null or of
     * type {@link Category} this method will return an instance of {@link CategoryCustomActionBuilder}</li>
     *
     * <li>if {@code <T>} is of type {@link Channel} and the {@code currentContainerResourceClass} is null or of
     * type {@link Channel} this method will return an instance of {@link ChannelCustomActionBuilder}</li>
     *
     * <li>if {@code <T>} is of type {@link InventoryEntry} and the {@code currentContainerResourceClass} is null
     * or of type {@link InventoryEntry} this method will return an instance of
     * {@link InventoryCustomActionBuilder}</li>
     *
     * <li>In any other case the method will throw a {@link BuildUpdateActionException}</li>
     * </ul>
     *
     * @param currentResource               the currentResource from which a corresponding concrete custom builder
     *                                      should be created according to it's type.
     * @param currentContainerResourceClass optional field representing the class of the container resource. For
     *                                      example, if the update action builder should be resolved for a product
     *                                      asset, this field would have the value {@code Product.class}
     * @return an instance of the concrete implementation of the {@link GenericCustomActionBuilder} responsible for
     *         building custom update actions according to the type of the {@code currentResource} instance and the
     *         {@code currentContainerResourceClass} provided.
     * @throws BuildUpdateActionException thrown if no concrete implementation of {@link GenericCustomActionBuilder}
     *                                    exists yet for the mapping combination.
     * @throws IllegalAccessException     if the {@code currentResource} class or its nullary constructor is not
     *                                    accessible.
     * @throws InstantiationException     if {@code currentResource} {@code Class} represents an abstract class,
     *                                    an interface, an array class, a primitive type, or void;
     *                                    or if the {@code currentResource} class has no nullary constructor;
     *                                    or if the instantiation fails for some other reason.
     */
    private S getBuilder(@Nonnull final T currentResource,
                         @Nullable final Class<U> currentContainerResourceClass) throws BuildUpdateActionException,
        IllegalAccessException, InstantiationException {
        for (ConcreteBuilder concreteBuilder : ConcreteBuilder.values()) {

            final Class<? extends GenericCustomActionBuilder> builderClass = concreteBuilder.getBuilderClass();
            final Class<? extends Custom> resourceClass = concreteBuilder.getResourceClass();
            final Class<? extends Resource> containerResourceClass = concreteBuilder.getContainerResourceClass();


            if (currentContainerResourceClass != null) {
                // If container class is defined, both container and resource must match current enum.
                if (resourceClass.isInstance(currentResource)
                    && containerResourceClass.equals(currentContainerResourceClass)) {
                    return (S) builderClass.newInstance();
                }
            } else {
                // If container class is null, only resource must match current enum.
                if (resourceClass.isInstance(currentResource)) {
                    return (S) builderClass.newInstance();
                }
            }
        }
        throw new BuildUpdateActionException(format(UPDATE_ACTION_NOT_IMPLEMENTED,
            currentResource.getClass().getSimpleName(),
            ofNullable(currentContainerResourceClass).map(Class::getSimpleName).orElse(null)));
    }

    /**
     * Creates an instance of the concrete implementation of the {@link GenericCustomActionBuilder}, which is
     * responsible for building custom update actions, according to the type of the {@code currentResource} instance
     * and the {@code currentContainerResourceClass} provided. The method uses the {@link ConcreteBuilder} enum to find
     * the mapping of the concrete builder. The available mappings for resolution are:
     *
     * <ul>
     * <li>if {@code <T>} is of type {@link Asset} and the {@code currentContainerResourceClass} is of
     * type {@link Product}, this method will return an instance of {@link AssetCustomActionBuilder}</li>
     *
     * <li>if {@code <T>} is of type {@link Asset} and the {@code currentContainerResourceClass} is of
     * type {@link Category}, this method will return an instance of
     * {@link com.commercetools.sync.categories.helpers.AssetCustomActionBuilder}</li>
     *
     * <li>if {@code <T>} is of type {@link Price} and the {@code currentContainerResourceClass} is of type
     * {@link Product} this method will return an instance of {@link PriceCustomActionBuilder}</li>
     *
     * <li>if {@code <T>} is of type {@link Category} and the {@code currentContainerResourceClass} is null or of
     * type {@link Category} this method will return an instance of {@link CategoryCustomActionBuilder}</li>
     *
     * <li>if {@code <T>} is of type {@link Channel} and the {@code currentContainerResourceClass} is null or of
     * type {@link Channel} this method will return an instance of {@link ChannelCustomActionBuilder}</li>
     *
     * <li>if {@code <T>} is of type {@link InventoryEntry} and the {@code currentContainerResourceClass} is null
     * or of type {@link InventoryEntry} this method will return an instance of
     * {@link InventoryCustomActionBuilder}</li>
     *
     * <li>In any other case the method will throw a {@link BuildUpdateActionException}</li>
     * </ul>
     *
     * @param <U>                    the type of the resource which has the custom fields.
     * @param <V>                    the type of the resource to do the update action on.
     * @param resource               the currentResource from which a corresponding concrete custom builder
     *                               should be created according to it's type.
     * @param containerResourceClass optional field representing the class of the container resource. For
     *                               example, if the update action builder should be resolved for a product
     *                               asset, this field would have the value {@code Product.class}.
     * @return an instance of the concrete implementation of the {@link GenericCustomActionBuilder} responsible for
     *         building custom update actions according to the type of the {@code currentResource} instance and the
     *         {@code currentContainerResourceClass} provided.
     * @throws BuildUpdateActionException exception thrown in case a concrete implementation of the the
     *                                    {@link GenericCustomActionBuilder} for the provided resource type is not
     *                                    implemented yet.
     * @throws IllegalAccessException     thrown by {@link #getBuilder(Custom, Class)} )} if the {@code resource}
     *                                    class or its nullary constructor is not accessible.
     * @throws InstantiationException     thrown by {@link #getBuilder(Custom, Class)} )} if {@code resource}
     *                                    {@code Class} represents an abstract class, an interface, an array class,
     *                                    a primitive type, or void; or if the {@code resource} class has no nullary
     *                                    constructor; or if the instantiation fails for some other reason.
     */
    public static <U extends Custom, V extends Resource<V>> GenericCustomActionBuilder of(
        @Nonnull final U resource,
        @Nullable final Class<V> containerResourceClass)
        throws BuildUpdateActionException, InstantiationException, IllegalAccessException {

        return new GenericCustomActionBuilderFactory().getBuilder(resource, containerResourceClass);
    }


}
