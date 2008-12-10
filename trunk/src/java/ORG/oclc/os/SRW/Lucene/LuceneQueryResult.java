/**
 * Copyright 2006 OCLC Online Computer Library Center, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * LuceneQueryResult.java
 *
 * Created on October 20, 2006, 2:58 PM
 */

package ORG.oclc.os.SRW.Lucene;

import ORG.oclc.os.SRW.QueryResult;
import ORG.oclc.os.SRW.RecordIterator;
import gov.loc.www.zing.srw.ExtraDataType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.Hits;
import ORG.oclc.os.SRW.Lucene.RecordResolver;

/**
 *
 * @author levan
 */
public class LuceneQueryResult extends QueryResult {
    static final Log log=LogFactory.getLog(LuceneQueryResult.class);

    Hits hits=null;
    SRWLuceneDatabase ldb=null;

    /** Creates a new instance of LuceneQueryResult */
    public LuceneQueryResult() {
    }

    /** Creates a new instance of LuceneQueryResult */
    public LuceneQueryResult(SRWLuceneDatabase ldb, Hits hits) {
        this.ldb=ldb;
        this.hits=hits;
    }

    public Hits getHits() {
        return hits;
    }

    public long getNumberOfRecords() {
        if(hits==null)
            return 0;
        return hits.length();
    }

    public QueryResult getSortedResult(String sortKeys) {
        return this;
    }

    public RecordIterator newRecordIterator(long whichRec, int numRecs,
      String schemaId, ExtraDataType edt) throws InstantiationException {
        log.debug("whichRec="+whichRec+", numRecs="+numRecs+", schemaId="+schemaId+", edt="+edt);
        return new LuceneRecordIterator(whichRec, schemaId, this, (RecordResolver)ldb.resolvers.get(schemaId), edt);
    }
}
