package com.commercetools.sync.commons;

import com.commercetools.sync.commons.helpers.CtpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.types.CustomFieldsDraft;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockUtils {

    private static final String CTP_KEY = "testKey";
    private static final String CTP_ID = "testId";
    private static final String CTP_SECRET = "testSecret";

    public static CustomFieldsDraft getMockCustomFieldsDraft() {
        final Map<String, JsonNode> customFieldsJsons = new HashMap<>();
        customFieldsJsons.put("invisibleInShop", JsonNodeFactory.instance.booleanNode(false));
        customFieldsJsons.put("backgroundColor", JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"));
        return CustomFieldsDraft.ofTypeKeyAndJson("StepCategoryTypeKey", customFieldsJsons);
    }

    public static CtpClient getMockCtpClient() {
        final CtpClient ctpClient = mock(CtpClient.class);
        final BlockingSphereClient client = mock(BlockingSphereClient.class);
        when(ctpClient.getClientConfig()).thenReturn(SphereClientConfig.of(CTP_KEY, CTP_ID, CTP_SECRET));
        when(ctpClient.getClient()).thenReturn(client);
        return ctpClient;
    }
}
