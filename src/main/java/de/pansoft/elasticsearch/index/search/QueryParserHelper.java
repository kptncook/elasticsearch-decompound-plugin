/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.pansoft.elasticsearch.index.search;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.mapper.TextSearchInfo;
import org.elasticsearch.index.query.SearchExecutionContext;
import org.elasticsearch.index.query.QueryShardException;
import org.elasticsearch.search.SearchModule;

import java.util.*;

/**
 * Helpers to extract and expand field names and boosts
 */
public final class QueryParserHelper {
    private QueryParserHelper() {}

    /**
     * Convert a list of field names encoded with optional boosts to a map that associates
     * the field name and its boost.
     * @param fields The list of fields encoded with optional boosts (e.g. ^0.35).
     * @return The converted map with field names and associated boosts.
     */
    public static Map<String, Float> parseFieldsAndWeights(List<String> fields) {
        final Map<String, Float> fieldsAndWeights = new HashMap<>();
        for (String field : fields) {
            int boostIndex = field.indexOf('^');
            String fieldName;
            float boost = 1.0f;
            if (boostIndex != -1) {
                fieldName = field.substring(0, boostIndex);
                boost = Float.parseFloat(field.substring(boostIndex+1));
            } else {
                fieldName = field;
            }
            // handle duplicates
            if (fieldsAndWeights.containsKey(field)) {
                boost *= fieldsAndWeights.get(field);
            }
            fieldsAndWeights.put(fieldName, boost);
        }
        return fieldsAndWeights;
    }

    public static Map<String, Float> resolveMappingFields(SearchExecutionContext context,
                                                          Map<String, Float> fieldsAndWeights) {
        return resolveMappingFields(context, fieldsAndWeights, null);
    }

    /**
     * Resolve all the field names and patterns present in the provided map with the
     * {@link SearchExecutionContext} and returns a new map containing all the expanded fields with their original boost.
     * @param context The context of the query.
     * @param fieldsAndWeights The map of fields and weights to expand.
     * @param fieldSuffix The suffix name to add to the expanded field names if a mapping exists for that name.
     *                    The original name of the field is kept if adding the suffix to the field name does not point to a valid field
     *                    in the mapping.
     */
    static Map<String, Float> resolveMappingFields(SearchExecutionContext context,
                                                          Map<String, Float> fieldsAndWeights,
                                                          String fieldSuffix) {
        Map<String, Float> resolvedFields = new HashMap<>();
        for (Map.Entry<String, Float> fieldEntry : fieldsAndWeights.entrySet()) {
            boolean allField = Regex.isMatchAllPattern(fieldEntry.getKey());
            boolean multiField = Regex.isSimpleMatchPattern(fieldEntry.getKey());
            float weight = fieldEntry.getValue() == null ? 1.0f : fieldEntry.getValue();
            Map<String, Float> fieldMap = resolveMappingField(context, fieldEntry.getKey(), weight, !multiField, !allField, fieldSuffix);

            for (Map.Entry<String, Float> field : fieldMap.entrySet()) {
                float boost = field.getValue();
                if (resolvedFields.containsKey(field.getKey())) {
                    boost *= resolvedFields.get(field.getKey());
                }
                resolvedFields.put(field.getKey(), boost);
            }
        }
        checkForTooManyFields(resolvedFields.size(), context, null);
        return resolvedFields;
    }

    /**
     * Resolves the provided pattern or field name from the {@link SearchExecutionContext} and return a map of
     * the expanded fields with their original boost.
     * @param context The context of the query
     * @param fieldOrPattern The field name or the pattern to resolve
     * @param weight The weight for the field
     * @param acceptAllTypes Whether all field type should be added when a pattern is expanded.
     *                       If false, only searchable field types are added.
     * @param acceptMetadataField Whether metadata fields should be added when a pattern is expanded.
     * @param fieldSuffix The suffix name to add to the expanded field names if a mapping exists for that name.
     *                    The original name of the field is kept if adding the suffix to the field name does not point to a valid field
     *                    in the mapping.
     */
    static Map<String, Float> resolveMappingField(SearchExecutionContext context, String fieldOrPattern, float weight,
                                                         boolean acceptAllTypes, boolean acceptMetadataField, String fieldSuffix) {
        Set<String> allFields = context.getMatchingFieldNames(fieldOrPattern);
        Map<String, Float> fields = new HashMap<>();

        for (String fieldName : allFields) {
            if (fieldSuffix != null && context.isFieldMapped(fieldName + fieldSuffix)) {
                fieldName = fieldName + fieldSuffix;
            }

            MappedFieldType fieldType = context.getFieldType(fieldName);
            if (!acceptMetadataField && fieldType.name().startsWith("_")) {
                // Ignore metadata fields
                continue;
            }

            if (!acceptMetadataField && fieldType.name().startsWith("_")) {
                // Ignore metadata fields
                continue;
            }

            if (!acceptAllTypes) {
                if (fieldType.getTextSearchInfo() == TextSearchInfo.NONE) {
                    continue;
                }
            }

            // Deduplicate aliases and their concrete fields.
            String resolvedFieldName = fieldType.name();
            if (allFields.contains(resolvedFieldName)) {
                fieldName = resolvedFieldName;
            }

            float w = fields.getOrDefault(fieldName, 1.0F);
            fields.put(fieldName, w * weight);
        }
        return fields;
    }

    static void checkForTooManyFields(int numberOfFields, SearchExecutionContext context, @Nullable String inputPattern) {
        Integer limit = SearchModule.INDICES_MAX_CLAUSE_COUNT_SETTING.get(context.getIndexSettings().getSettings());
        if (numberOfFields > limit) {
            StringBuilder errorMsg = new StringBuilder("field expansion ");
            if (inputPattern != null) {
                errorMsg.append("for [" + inputPattern + "] ");
            }
            errorMsg.append("matches too many fields, limit: " + limit + ", got: " + numberOfFields);
            throw new IllegalArgumentException(errorMsg.toString());
        }
    }

    /**
     * Returns true if any of the fields is the wildcard {@code *}, false otherwise.
     * @param fields A collection of field names
     */
    public static boolean hasAllFieldsWildcard(Collection<String> fields) {
        return fields.stream().anyMatch(Regex::isMatchAllPattern);
    }
}
