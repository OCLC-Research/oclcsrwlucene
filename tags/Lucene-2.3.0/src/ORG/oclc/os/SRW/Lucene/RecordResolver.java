/*
 * OCKHAM P2PREGISTRY Copyright 2006 Oregon State University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ORG.oclc.os.SRW.Lucene;

import gov.loc.www.zing.srw.ExtraDataType;
import java.util.Properties;
import ORG.oclc.os.SRW.Record;
import org.apache.lucene.document.Document;

/**
 * Interface for resolving records and schemas.
 *
 * @author peter
 *         Date: Oct 25, 2005
 *         Time: 9:49:08 AM
 */
public interface RecordResolver {

    /**
     * Resolves a record from the identifier
     *
     * @param Document - the document that Lucene has stored
     * @param IdFieldName - the name of the field that contains the identifier
     * @param extraDataType - nonstandard SRW request parameters
     * @return record if found.
     */
    public Record resolve(Document doc, String IdFieldName, ExtraDataType extraDataType);

    /**
     * Initialize the resolver.
     * 
     * @param properties
     */
    public void init(Properties properties);
}
