/*
 * Copyright (C) 2014 Jörg Prante
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses
 * or write to the Free Software Foundation, Inc., 51 Franklin Street,
 * Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * The interactive user interfaces in modified source and object code
 * versions of this program must display Appropriate Legal Notices,
 * as required under Section 5 of the GNU Affero General Public License.
 *
 */
package org.xbib.elasticsearch.index.analysis.decompound;

import java.io.IOException;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.indices.analysis.AnalysisModule.AnalysisProvider;

public class DecompoundTokenFilterAnalysisProvider implements AnalysisProvider<TokenFilterFactory>{

	private final long maxDecompoundEntries;
	
	public DecompoundTokenFilterAnalysisProvider(long maxDecompoundEntries) {
		this.maxDecompoundEntries = maxDecompoundEntries;
	}
	
	@Override
	public TokenFilterFactory get(IndexSettings indexSettings, Environment environment, String name, Settings settings)
			throws IOException {
		return new DecompoundTokenFilterFactory(indexSettings, name, settings, maxDecompoundEntries);
	}

}