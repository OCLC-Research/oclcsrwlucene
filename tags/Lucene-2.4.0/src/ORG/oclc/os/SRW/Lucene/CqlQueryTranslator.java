/*
 * CqlToLuceneQueryTranslator.java
 *
 * Created on October 31, 2006, 11:23 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package ORG.oclc.os.SRW.Lucene;

import ORG.oclc.os.SRW.SRWDiagnostic;
import java.util.Properties;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.z3950.zing.cql.CQLNode;

/**
 *
 * @author levan
 */
public interface CqlQueryTranslator {
    void init(Properties properties, SRWLuceneDatabase ldb) throws InstantiationException;

    public Query makeQuery(CQLNode node) throws SRWDiagnostic;
    public Term getTerm();
}
