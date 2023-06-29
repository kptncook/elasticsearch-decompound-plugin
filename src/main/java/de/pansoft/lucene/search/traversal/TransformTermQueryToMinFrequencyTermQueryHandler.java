package de.pansoft.lucene.search.traversal;

import de.pansoft.lucene.index.query.frequency.MinFrequencyTermQuery;
import de.pansoft.lucene.index.query.term.MarkedTermQuery;
import de.pansoft.lucene.search.spans.SpanEmptyPayloadCheckQuery;
import de.pansoft.lucene.search.spans.SpanMinFrequencyFilterQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.query.SearchExecutionContext;

public class TransformTermQueryToMinFrequencyTermQueryHandler implements QueryHandler {

	private final int minFrequency;

	public TransformTermQueryToMinFrequencyTermQueryHandler(int minFrequency) {
		this.minFrequency = minFrequency;
	}

	@Override
	public Query handleQuery(final TraverserContext traverserContext, final SearchExecutionContext context,
							 final Query query, QueryTraverser queryTraverser) {
		final TermQuery termQuery = (TermQuery) query;

		MappedFieldType fieldType = context.getFieldType(termQuery.getTerm().field());
		if (fieldType != null && fieldType.getTextSearchInfo().isTokenized()) {
			return new SpanMinFrequencyFilterQuery(new SpanEmptyPayloadCheckQuery(new SpanTermQuery(termQuery.getTerm())), minFrequency);
		}
		return new MinFrequencyTermQuery(termQuery.getTerm(), minFrequency);
	}

	@Override
	public boolean acceptQuery(final TraverserContext traverserContext, final SearchExecutionContext context,
							   Query query) {
		return query != null && query instanceof TermQuery;
	}

}
