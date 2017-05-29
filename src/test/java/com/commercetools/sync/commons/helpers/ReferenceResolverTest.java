package com.commercetools.sync.commons.helpers;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryDraft;
import static com.commercetools.sync.commons.MockUtils.getMockCategoryService;
import static com.commercetools.sync.commons.MockUtils.getMockTypeService;
import static com.commercetools.sync.commons.helpers.ReferenceResolver.resolveReferences;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReferenceResolverTest {

    private TypeService typeService;
    private CategoryService categoryService;
    private CategorySyncOptions syncOptions;

    /**
     * Sets up the services and the options needed for reference resolution.
     */
    @Before
    public void setup() {
        typeService = getMockTypeService();
        categoryService = getMockCategoryService();
        syncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class)).build();
    }

    @Test
    public void resolveReferences_WithNoUuidSet_ShouldResolveParentReference()
        throws ReferenceResolutionException {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            "parentExternalId", "customTypeId", new HashMap<>());
        final CategoryDraft draftWithResolvedReferences =
            resolveReferences(categoryDraft, typeService, categoryService, syncOptions);

        assertThat(draftWithResolvedReferences.getParent()).isNotNull();
        assertThat(draftWithResolvedReferences.getParent().getId()).isEqualTo("parentId");
    }

    @Test
    public void resolveReferences_WithUuidSetAndAllowed_ShouldNotResolveReferences()
        throws ReferenceResolutionException {
        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class))
                                                                                  .setAllowUuid(true)
                                                                                  .build();
        final String parentUuid = String.valueOf(UUID.randomUUID());
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            parentUuid, "customTypeId", new HashMap<>());

        final CategoryDraft draftWithResolvedReferences = resolveReferences(categoryDraft, typeService, categoryService,
            categorySyncOptions);

        assertThat(draftWithResolvedReferences.getParent()).isNotNull();
        assertThat(draftWithResolvedReferences.getParent().getId()).isEqualTo("parentId");

        assertThat(draftWithResolvedReferences.getCustom()).isNotNull();
        assertThat(draftWithResolvedReferences.getCustom().getType().getId()).isEqualTo("typeId");
    }

    @Test
    public void resolveReferences_WithParentUuidSetAndNotAllowed_ShouldNotResolveParentReference()
        throws ReferenceResolutionException {
        final String parentUuid = String.valueOf(UUID.randomUUID());
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            parentUuid, "customTypeId", new HashMap<>());

        assertThatThrownBy(() -> resolveReferences(categoryDraft, typeService, categoryService, syncOptions))
            .isInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve parent reference. Reason: Found a UUID in the id field. Expecting a key "
                + "without a UUID value. If you want to allow UUID values for reference keys, please use the "
                + "setAllowUuid(true) option in the sync options.");
    }

    @Test
    public void resolveReferences_WithCustomTypeUuidSetAndNotAllowed_ShouldNotResolveCustomTypeReference()
        throws ReferenceResolutionException {
        final String customTypeUuid = String.valueOf(UUID.randomUUID());
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            "parentExternalId", customTypeUuid, new HashMap<>());

        assertThatThrownBy(() -> resolveReferences(categoryDraft, typeService, categoryService, syncOptions))
            .isInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve custom type reference. Reason: Found a UUID in the id field. Expecting a key"
                + " without a UUID value. If you want to allow UUID values for reference keys, please use the"
                + " setAllowUuid(true) option in the sync options.");
    }

    @Test
    public void resolveReferences_WithNoUuidSet_ShouldResolveCustomTypeReference()
        throws ReferenceResolutionException {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            "parentExternalId", "customTypeId", new HashMap<>());
        final CategoryDraft draftWithResolvedReferences =
            resolveReferences(categoryDraft, typeService, categoryService, syncOptions);

        assertThat(draftWithResolvedReferences.getCustom()).isNotNull();
        assertThat(draftWithResolvedReferences.getCustom().getType().getId()).isEqualTo("typeId");
    }

    @Test
    public void resolveReferences_WithEmptyIdOnParentReference_ShouldNotResolveParentReference()
        throws ReferenceResolutionException {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            "parentExternalId", "customTypeId", new HashMap<>());
        when(categoryDraft.getParent()).thenReturn(Reference.of("type", ""));

        assertThatThrownBy(() -> resolveReferences(categoryDraft, typeService, categoryService, syncOptions))
            .isInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve parent reference. Reason: Reference 'id' field value is blank "
                + "(null/empty).");
    }

    @Test
    public void resolveReferences_WithNullIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference()
        throws ReferenceResolutionException {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            "parentExternalId", "customTypeId", new HashMap<>());

        final CustomFieldsDraft newCategoryCustomFieldsDraft = mock(CustomFieldsDraft.class);
        final ResourceIdentifier<Type> newCategoryCustomFieldsDraftTypeReference =
            ResourceIdentifier.ofId(null);
        when(newCategoryCustomFieldsDraft.getType()).thenReturn(newCategoryCustomFieldsDraftTypeReference);
        when(categoryDraft.getCustom()).thenReturn(newCategoryCustomFieldsDraft);

        assertThatThrownBy(() -> resolveReferences(categoryDraft, typeService, categoryService, syncOptions))
            .isInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve custom type reference. Reason: Reference 'id' field value is blank "
                + "(null/empty).");
    }

    @Test
    public void resolveReferences_WithEmptyIdOnCustomTypeReference_ShouldNotResolveCustomTypeReference()
        throws ReferenceResolutionException {
        final CategoryDraft categoryDraft = getMockCategoryDraft(Locale.ENGLISH, "myDraft", "externalId",
            "parentExternalId", "customTypeId", new HashMap<>());

        final CustomFieldsDraft newCategoryCustomFieldsDraft = mock(CustomFieldsDraft.class);
        final ResourceIdentifier<Type> newCategoryCustomFieldsDraftTypeReference =
            ResourceIdentifier.ofId("");
        when(newCategoryCustomFieldsDraft.getType()).thenReturn(newCategoryCustomFieldsDraftTypeReference);
        when(categoryDraft.getCustom()).thenReturn(newCategoryCustomFieldsDraft);

        assertThatThrownBy(() -> resolveReferences(categoryDraft, typeService, categoryService, syncOptions))
            .isInstanceOf(ReferenceResolutionException.class)
            .hasMessage("Failed to resolve custom type reference. Reason: Reference 'id' field value is blank "
                + "(null/empty).");
    }
}
