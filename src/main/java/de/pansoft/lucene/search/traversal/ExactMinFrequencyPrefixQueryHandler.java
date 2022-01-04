package de.pansoft.lucene.search.traversal;

import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.query.SearchExecutionContext;

import de.pansoft.lucene.index.query.frequency.MinFrequencyPrefixQuery;
import de.pansoft.lucene.search.spans.SpanEmptyPayloadCheckQuery;
import de.pansoft.lucene.search.spans.SpanMinFrequencyFilterQuery;

public class ExactMinFrequencyPrefixQueryHandler implements QueryHandler {

	@Override
	public Query handleQuery(final TraverserContext traverserContext, final SearchExecutionContext context,
							 final Query query, QueryTraverser queryTraverser) {
		final MinFrequencyPrefixQuery multiTermQuery = (MinFrequencyPrefixQuery) query;
		MappedFieldType fieldType = context.getFieldType(multiTermQuery.getField());
		if (fieldType != null && fieldType.getTextSearchInfo().isTokenized()) {
			return new SpanMinFrequencyFilterQuery(
					new SpanEmptyPayloadCheckQuery(new SpanMultiTermQueryWrapper<MultiTermQuery>(multiTermQuery)),
					multiTermQuery.getMinFrequency());
		}
		return multiTermQuery;
	}

	@Override
	public boolean acceptQuery(final TraverserContext traverserContext, final SearchExecutionContext context, Query query) {
		return query != null && query instanceof MinFrequencyPrefixQuery;
	}

}
