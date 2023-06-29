package de.pansoft.lucene.search.traversal;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.query.SearchExecutionContext;

import de.pansoft.lucene.index.query.frequency.MinFrequencyTermQuery;
import de.pansoft.lucene.search.spans.SpanEmptyPayloadCheckQuery;
import de.pansoft.lucene.search.spans.SpanMinFrequencyFilterQuery;

public class ExactMinFrequencyTermQuery implements QueryHandler {

	@Override
	public Query handleQuery(final TraverserContext traverserContext, SearchExecutionContext context,
							 Query query, QueryTraverser queryTraverser) {
		final MinFrequencyTermQuery minFrequencyTermQuery = (MinFrequencyTermQuery) query;
		MappedFieldType fieldType = context.getFieldType(minFrequencyTermQuery.getTerm().field());
		if (fieldType != null && fieldType.getTextSearchInfo().isTokenized()) {
			return new SpanMinFrequencyFilterQuery(new SpanEmptyPayloadCheckQuery(new SpanTermQuery((minFrequencyTermQuery).getTerm())), minFrequencyTermQuery.getMinFrequency());
		}
		return minFrequencyTermQuery;
	}

	@Override
	public boolean acceptQuery(final TraverserContext traverserContext, SearchExecutionContext context,
							   Query query) {
		return query != null && query instanceof MinFrequencyTermQuery;
	}

}
