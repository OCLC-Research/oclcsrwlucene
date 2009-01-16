/*
   Copyright 2006 OCLC Online Computer Library Center, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
/*
 * SRWLuceneDatabase.java
 *
 * Created on August 4, 2003, 1:54 PM
 */

package ORG.oclc.os.SRW.Lucene;

import ORG.oclc.os.SRW.QueryResult;
import ORG.oclc.os.SRW.Record;
import ORG.oclc.os.SRW.SRWDatabase;
import ORG.oclc.os.SRW.SRWDiagnostic;
import ORG.oclc.os.SRW.SortTool;
import ORG.oclc.os.SRW.TermList;
import gov.loc.www.zing.srw.ExtraDataType;
import gov.loc.www.zing.srw.ScanRequestType;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;
import gov.loc.www.zing.srw.TermType;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
//import org.osuosl.srw.ResolvingQueryResult;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLTermNode;

/**
 *
 * @author  levan
 */
public class SRWLuceneDatabase extends SRWDatabase {
    static Log log=LogFactory.getLog(SRWLuceneDatabase.class);

    CqlQueryTranslator translator=null;
//    Hashtable          indexSynonyms=new Hashtable();
    Hashtable<String, RecordResolver> resolvers=new Hashtable<String, RecordResolver>();
    IndexSearcher      searcher=null;
    String             idFieldName=null, indexInfo=null, indexPath=null;

    @Override
    public void addRenderer(String schemaName, String schemaID, Properties props) throws InstantiationException {
        RecordResolver resolver=null;
        String resolverName=dbProperties.getProperty(schemaName+".resolver");
        if(resolverName==null) {
            log.debug("creating BasicLuceneRecordResolver");
            resolver=new BasicLuceneRecordResolver();
            resolver.init(props);
        }
        else {
            try {
                log.debug("creating resolver "+resolverName);
                Class  resolverClass=Class.forName(resolverName);
                log.debug("creating instance of class "+resolverClass);
                resolver=(RecordResolver)resolverClass.newInstance();
                resolver.init(dbProperties);
            }
            catch(Exception e) {
                log.error("Unable to create RecordResolver class "+resolverName+
                    " for database "+dbname);
                log.error(e, e);
                throw new InstantiationException(e.getMessage());
            }
        }
        resolvers.put(schemaName, resolver);
        resolvers.put(schemaID, resolver);
    }


    @Override
    public String getExtraResponseData(QueryResult result, SearchRetrieveRequestType request) {
        return null;
    }


    @Override
    public String getIndexInfo() {
        if(indexInfo==null) {
            indexInfo=makeIndexInfo(dbProperties, searcher, null);
        }
        return indexInfo;
    }


//    private void getIndexSynonyms(Properties properties) {
//        Enumeration keys=properties.keys();
//        String      index, key, value;
//        if(keys==null) {
//            if(log.isDebugEnabled())log.debug("no lucene index synonyms specified in properties file");
//            return;
//        }
//        while(keys.hasMoreElements()) {
//            key=(String)keys.nextElement();
//            if(key.startsWith("indexSynonym.")) {
//                value=properties.getProperty(key);
//                index=key.substring(13);
//                if(log.isDebugEnabled())log.debug("new indexSynonym: "+index+"="+value);
//                indexSynonyms.put(index, value);
//            }
//        }
//        indexSynonyms.put("srw.serverChoice", "");
//    }


    @Override
    public QueryResult getQueryResult(String queryStr, SearchRetrieveRequestType request) throws InstantiationException {
        log.debug("entering SRWLuceneDatabase.getQueryResult");
        LuceneQueryResult   result;
        Sort sort=null;
        String sortfield;

        try {
            if(log.isDebugEnabled())
                log.debug("query="+queryStr);
            CQLNode queryRoot = parser.parse(queryStr);
            //convert the CQL search to lucene search
            Query query=translator.makeQuery(queryRoot);
            log.info("lucene search="+query);

            String sortKey=request.getSortKeys();
            if(sortKey!=null && sortKey.length()>0) {
                log.info("sortKey="+sortKey);
                SortTool sortInfo=new SortTool(sortKey);
                sort=new Sort(new SortField(sortInfo.xpath,
                    sortInfo.dataType.equals("text")?SortField.STRING:SortField.INT,
                    !sortInfo.ascending));
            }

            // perform search
            Hits results;
            if(sort!=null)
                results = searcher.search(query, sort);
            else
                results = searcher.search(query);
            return new LuceneQueryResult(this, results);
        }
        catch(SRWDiagnostic e) {
            LuceneQueryResult lqr=new LuceneQueryResult();
            lqr.addDiagnostic(e.getCode(), e.getAddInfo());
            return lqr;
        }
        catch(Exception e) {
            log.error(e, e);
            LuceneQueryResult lqr=new LuceneQueryResult();
            lqr.addDiagnostic(SRWDiagnostic.GeneralSystemError, e.getMessage());
            return lqr;
        }
    }


    @Override
    public TermList getTermList(CQLTermNode cqlTermNode, int position,
      int maxTerms, ScanRequestType scanRequestType) {
        log.debug("in getTermList: cqlTermNode="+cqlTermNode+", position="+position+", maxTerms="+maxTerms);
        TermList list=new TermList();
        if(position>1) {
            log.debug("unsupported responsePosition="+position);
            list.addDiagnostic(SRWDiagnostic.ResponsePositionOutOfRange, Integer.toString(position));
        }
        else {
            try {
                int      i;
                Query q=translator.makeQuery(cqlTermNode);
                HashSet<Term> terms=new HashSet<Term>();
                q.extractTerms(terms);
                Term t=terms.iterator().next();
                log.debug("scan term="+t);
                TermEnum te=searcher.getIndexReader().terms(t);
                Vector<TermType> v=new Vector<TermType>();
                for(i=position; i<1; i++)
                    te.next();
                for(i=1; i<=maxTerms; i++) {
                    v.add(new TermType(te.term().text(), new NonNegativeInteger("0"), null, null, null));
                    if(!te.next())
                        break;
                }
                list.setTerms((TermType[])v.toArray(new TermType[0]));
            }
            catch(SRWDiagnostic e) {
                list.addDiagnostic(e.getCode(), e.getAddInfo());
            }
            catch(IOException e) {
                log.error(e, e);
                list.addDiagnostic(SRWDiagnostic.GeneralSystemError, e.getMessage());
            }
        }
        return list;
    }


    @Override
    public boolean hasaConfigurationFile() {
        return false;  // a configuration file is not required
    }


    @Override
    public void init(String dbname, String srwHome, String dbHome,
      String dbPropertiesFileName, Properties dbProperties) throws InstantiationException {
        if(log.isDebugEnabled())log.debug("entering SRWLuceneDatabase.init, dbname="+dbname);

        String xmlSchemaList=dbProperties.getProperty("xmlSchemas");
        if(xmlSchemaList==null) {
            log.info("No schemas specified in SRWDatabase.props ("+dbPropertiesFileName+")");
            log.info("The LuceneDocument schema will be automatically provided");
            dbProperties.put("xmlSchemas", "LuceneDocument");
            dbProperties.put("LuceneDocument.identifier", "info:srw/schema/1/LuceneDocument");
            dbProperties.put("LuceneDocument.location", "http://www.oclc.org/standards/Lucene/schema/LuceneDocument.xsd");
            dbProperties.put("LuceneDocument.namespace", "http://www.oclc.org/LuceneDocument");
            dbProperties.put("LuceneDocument.title", "Lucene records in their internal format");
        }
        super.initDB(dbname, srwHome, dbHome, dbPropertiesFileName, dbProperties);

        maxTerms=10;
        position=1;
        indexPath=dbProperties.getProperty("SRWLuceneDatabase.indexPath");
        log.debug("indexPath="+indexPath);
        if(indexPath==null) {
            // let's see if we can find the right directory
            // maybe dbHome?
            if(IndexReader.indexExists(dbHome))
                indexPath=dbHome;
            else {
                File file=new File(dbHome);
                File[] dir=file.listFiles();
                for(int i=0; i<dir.length; i++) {
                    if(dir[i].isDirectory()) {
                        if(IndexReader.indexExists(dir[i])) {
                            indexPath=dir[i].getAbsolutePath();
                            break;
                        }
                    }
                }
            }
            log.debug("indexPath="+indexPath);
        }
        if(indexPath==null) {
            log.error("Lucene indexPath not specified for database "+dbname);
            log.error("and index not found in dbHome "+dbHome+" or one of its subdirectories");
            throw new InstantiationException("Lucene indexPath not specified");
        }
        try {
            searcher=new IndexSearcher(indexPath);
        }
        catch(Exception e) {
            log.error("Unable to create IndexSearcher with path="+indexPath+
                " for database "+dbname);
            log.error(e, e);
            throw new InstantiationException(e.getMessage());
        }

        String translatorName=dbProperties.getProperty("SRWLuceneDatabase.CqlToLuceneQueryTranslator",
                "ORG.oclc.os.SRW.Lucene.BasicLuceneQueryTranslator");
        try {
            log.debug("creating translator "+translatorName);
            Class  translatorClass=Class.forName(translatorName);
            log.debug("creating instance of class "+translatorClass);
            translator=(CqlQueryTranslator)translatorClass.newInstance();
            translator.init(dbProperties, this);
        }
        catch(Exception e) {
            log.error("Unable to create CqlToLuceneQueryTranslator class "+translatorName+
                " for database "+dbname);
            log.error(e, e);
            throw new InstantiationException(e.getMessage());
        }
        if(log.isDebugEnabled())log.debug("leaving SRWLuceneDatabase.init");
    }

    public static String makeIndexInfo(Properties props, IndexSearcher searcher, Hashtable<String, String> indexMappings) {
        Collection c=searcher.getIndexReader().getFieldNames(IndexReader.FieldOption.INDEXED);
        Hashtable<String, String> sets=new Hashtable<String, String>();
        int             indexNum=0;
        String          index, indexSet, luceneIndex, prop;
        StringBuffer    sb=new StringBuffer("        <indexInfo>\n");
        StringTokenizer st;

        Iterator iter=c.iterator();
        while(iter.hasNext()) {
            index=(String)iter.next();
            props.put("qualifier.local."+index, index);
        }
        makeUnqualifiedIndexes(props);

        Enumeration enumer=props.propertyNames();
        while(enumer.hasMoreElements()) {
            prop=(String)enumer.nextElement();
            if(prop.startsWith("qualifier.")) {
                st=new StringTokenizer(prop.substring(10));
                index=st.nextToken();
                st=new StringTokenizer(index, ".");
                if(st.countTokens()==1) {
                    indexSet="local";
                    index=prop.substring(10);
                }
                else {
                    indexSet=st.nextToken();
                    index=prop.substring(10+indexSet.length()+1);
                }
                
                if(log.isDebugEnabled())log.debug("indexSet="+indexSet+", index="+index);
                if(sets.get(indexSet)==null) {  // new set
                    sb.append("          <set identifier=\"")
                      .append(props.getProperty("indexSet."+indexSet))
                      .append("\" name=\"").append(indexSet).append("\"/>\n");
                    sets.put(indexSet, indexSet);
                }
                sb.append("          <index>\n")
                  .append("            <title>").append(indexSet).append('.').append(index).append("</title>\n")
                  .append("            <map>\n")
                  .append("              <name set=\"").append(indexSet).append("\">").append(index).append("</name>\n")
                  .append("              </map>\n")
                  .append("            </index>\n");

                if(indexMappings!=null) {
                    // now for a bit of trickery for the CQL parser
                    // the line we just read isn't in the format the parser
                    // expect.  we just read:
                    // qualifier.<indexSet>.indexName=luceneIndexName
                    // the parser is expecting:
                    // qualifier.<indexSet>.indexName=1=<z39.50UseAttribute>
                    // it doesn't really care what Use attribute we provide,
                    // so we'll make up Use attribute numbers to correspond
                    // with the lucene indexes.
                    luceneIndex=props.getProperty(prop);
                    indexMappings.put(indexSet+"."+index, luceneIndex);
                    if(log.isDebugEnabled())
                        log.debug("mapping "+indexSet+"."+index+" to "+luceneIndex);
                    props.put(prop, "1="+(++indexNum));
                }
            }
            else if(prop.startsWith("hiddenQualifier.")) {
                st=new StringTokenizer(prop.substring(16));
                index=st.nextToken();
                if(indexMappings!=null) {
                    // now for a bit of trickery for the CQL parser
                    // the line we just read isn't in the format the parser
                    // expect.  we just read:
                    // qualifier.<indexSet>.indexName=luceneIndexName
                    // the parser is expecting:
                    // qualifier.<indexSet>.indexName=1=<z39.50UseAttribute>
                    // it doesn't really care what Use attribute we provide,
                    // so we'll make up Use attribute numbers to correspond
                    // with the lucene indexes.
                    luceneIndex=props.getProperty(prop);
                    indexMappings.put(index, luceneIndex);
                    if(log.isDebugEnabled())
                        log.debug("mapping "+index+" to "+luceneIndex);
                    props.put(prop, "1="+(++indexNum));
                }
            }
        }
        sb.append("          </indexInfo>\n");
        return sb.toString();
    }


    /**
     * Resolves a record from the identifier
     *
     * @param Id - identifier
     * @param extraDataType - nonstandard search parameters
     * @return record if found.
     */
    public Record resolve(String Id, ExtraDataType extraDataType) {
        // need to create a resolver in the init step from config info
        return null;
    }

    @Override
    public boolean supportsSort() {
        return true;
    }
}