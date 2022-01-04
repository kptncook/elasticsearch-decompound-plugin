package de.pansoft.lucene.search.traversal;

import de.pansoft.lucene.index.query.term.MarkedTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.query.SearchExecutionContext;

import de.pansoft.lucene.search.spans.SpanEmptyPayloadCheckQuery;

public class ExactTermQueryHandler implements QueryHandler {

	@Override
	public Query handleQuery(final TraverserContext traverserContext, final SearchExecutionContext context,
							 final Query query, QueryTraverser queryTraverser) {
		final TermQuery termQuery = (TermQuery) query;
		MappedFieldType fieldType = context.getFieldType(termQuery.getTerm().field());
		if (fieldType != null && fieldType.getTextSearchInfo().isTokenized()) {
			if ((termQuery instanceof MarkedTermQuery
						&& ((MarkedTermQuery)termQuery).getContext() == MarkedTermQuery.Context.PHRASE)
					|| traverserContext.getBoostExactTokens() == null) {
				return new SpanEmptyPayloadCheckQuery(new SpanTermQuery((termQuery).getTerm()));
			} else if (traverserContext.getBoostExactTokens() != null) {
				return ExactQueryPartBooster.query(
						new SpanEmptyPayloadCheckQuery(new SpanTermQuery((termQuery).getTerm())),
						termQuery,
						traverserContext.getBoostExactTokens());
			}
		}
		return termQuery;
	}

	@Override
	public boolean acceptQuery(final TraverserContext traverserContext, final SearchExecutionContext context,
							   Query query) {
		return query != null && query instanceof TermQuery;
	}

}
