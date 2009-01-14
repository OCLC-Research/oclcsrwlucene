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
 * BasicLuceneRecordResolver.java
 *
 * Created on November 1, 2006, 1:40 PM
 */

package ORG.oclc.os.SRW.Lucene;

import ORG.oclc.os.SRW.Record;
import ORG.oclc.os.SRW.Utilities;
import gov.loc.www.zing.srw.ExtraDataType;
import java.util.Iterator;
import java.util.Properties;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;


/**
 *
 * @author levan
 */
public class BasicLuceneRecordResolver implements RecordResolver {
    static final String LUCENE_SCHEMA_ID="info:srw/schema/1/LuceneDocument";

    /** Creates a new instance of BasicLuceneRecordResolver */
    public BasicLuceneRecordResolver() {
    }

// Not legal until JDK 6    @Override
    public void init(Properties properties) {
    }

// Not legal until JDK 6    @Override
    public Record resolve(Document doc, String IdFieldName, ExtraDataType extraDataType) {
        // Enumeration fields=doc.fields(); // lucene 1.4
        Iterator fields=doc.getFields().iterator();
        Field field;
        StringBuffer sb=new StringBuffer("<LuceneDocument>");
        // while(fields.hasMoreElements()) {
        while(fields.hasNext()) {
            // field=(Field)fields.nextElement();
            field=(Field)fields.next();
            sb.append("<field name=\"").append(field.name()).append("\">");
            sb.append(Utilities.xmlEncode(field.stringValue()));
            sb.append("</field>");
        }
        sb.append("</LuceneDocument>");
        return new Record(sb.toString(), LUCENE_SCHEMA_ID);
    }
}
