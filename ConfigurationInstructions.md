# Implementing an SRWLuceneDatabase #

You need to let the SRW server know of the existence of your Lucene database.  You do this by adding three lines to the SRWServer.props file.  This file is typically found in your webapps/SRW/WEB-INF/classes directory.  If it is not there, or has a different name, then the webapps/SRW/web.xml file will have an explicit pointer to it.

Add these three lines to the SRWServer.props file:
```
  db.<databaseName>.class=ORG.oclc.SRW.Lucene.SRWLuceneDatabase.class
  db.<databaseName>.home=<full pathname to your Lucene database directory>
  db.<databaseName>.configuration=SRWDatabase.props
```

The `db.<databaseName>.home` property points to the directory that contains the database index directory.  This is also where SRW will expect to find any other files necessary for its operation.

The `db.<databaseName>.configuration` property is optional.  If the database home directory was specified and that directory or one of its subdirectories contains the Lucene index, then the SRWLuceneDatabase will be able to provide an acceptable minimal configuration automatically.

An example might be:
```
db.LuceneDemoDB.class=ORG.oclc.os.SRW.Lucene.SRWLuceneDatabase
db.LuceneDemoDB.home=f:/Lucene-2.0.0/
db.LuceneDemoDB.configuration=SRWDatabase.props
```

# Database Specific Configuration Information #
The SRWDatabase.props file serves two primary roles.  First, it contains much of the information needed to generate an Explain record for the database.  Second, it specifies classes and configuration information necessary to generate an SRW gateway to a local database system.

## General Database Information ##
|databaseInfo.title (recommended) | The name of the database.|
|:--------------------------------|:-------------------------|
|databaseInfo.description (optional) | A brief description of the database.|
|databaseInfo.author (optional) | The author/creator of the database.|
|databaseInfo.contact (recommended) | A person to contact with questions/problems.|
|databaseInfo.restrictions (optional) | Any usage restrictions.|


## Information about the Explain record ##
|metaInfo.dateModified (optional) | The date the record was last modified.|
|:--------------------------------|:--------------------------------------|
|metaInfo.aggregatedFrom (optional) | If the record was collected from another site, the URL of the original record.|
|metaInfo.dateAggregated (optional) | If the record was collected , the date the record was collected.|


## Default configuration values ##
|configInfo.maximumRecords (optional) | The maximum number of records that can be returned in a response.  Default is 20.|
|:------------------------------------|:---------------------------------------------------------------------------------|
|configInfo.numberOfRecords (optional) | The number of records to return in a response if not specified in the request.  Default is 10.|
|configInfo.resultSetTTL (optional) | The number of seconds that query results should be kept, if not specified in the request.  Default is 300.|


## Information about the record schemas supported and how to generate records in those schemas ##
|xmlschemas | A space separated list of schema names.|
|:----------|:---------------------------------------|
|defaultSchema (optional) | Which schema should be returned when the requester doesn’t specify a schema.  Defaults to the first schema on the xmlschemas list.|

For each schema in the list, we look for information about the schema and how to generate records in that schema.
|`<schemaName>.title` | The title of the schema|
|:--------------------|:-----------------------|
|`<schemaName>.identifier` | The URI that identifies the schema.|
|`<schemaName>.transformer` (optional) | The name of an XSLT stylesheet that can transform records from their native format to the desired schema.  If omitted, the record must be retrieved in the desired schema.|
|`<schemaName>.location` | The URL of the XSD or DTD description of the schema.|
|`<schemaName>.namespace` | The namespace associated with the schema.|

Traditionally, Lucene does not store the records that it indexes, but it does store some of the fields that it has indexed.  At the very least, it stores some sort of identifier that can be used to retrieve the original record.  By default, records are gotten from Lucene using the `BasicLuceneRecordResolver`, which retrieves all of the fields that Lucene was asked to keep after indexing.  If you want to provide a resolver to retrieve the actual documents from wherever they reside, you would specify them this way:
|`<schemaName>.resolver` (optional) | The name of a class that implements the `RecordResolver` interface.  Defaults to the `BasicLuceneRecordResolver`.|
|:----------------------------------|:-----------------------------------------------------------------------------------------------------------------|

If no schema information is provided, the SRWLuceneDatabase class will provide this information automatically:
```
xmlSchemas=LuceneDocument
LuceneDocument.identifier=info:srw/schema/1/LuceneDocument
LuceneDocument.location=http://www.oclc.org/standards/Lucene/schema/LuceneDocument.xsd
LuceneDocument.namespace=http://www.oclc.org/LuceneDocument
LuceneDocument.title=Lucene records in their internal format
```

## Lucene Specific Information ##
|SRWLuceneDatabase.indexPath (optional) | The full path to the directory that contains the Lucene index.  If omitted, it will default to the database home as specified in the SRWServer.props file.|
|:--------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------|
|`SRWLuceneDatabase.CqlToLuceneQueryTranslator` (optional) | The name of a class that will accept a CQL query and return a Lucene Query object.  If omitted, it will default to the `BasicLuceneQueryTranslator`.|

# Example SRWDatabase.props file #
Here is my SRWDatabase.props file for the Lucene Demo Database:
```
databaseInfo.title=Lucene Demo Database
databaseInfo.description=An index of the source code for Lucene
databaseInfo.contact=Ralph LeVan (levan@oclc.org)

qualifier.cql.serverChoice=contents

explainStyleSheet=/SRWLucene/explainResponse.xsl
scanStyleSheet=/SRWLucene/scanResponse.xsl
searchStyleSheet=/SRWLucene/searchRetrieveResponse.xsl
```