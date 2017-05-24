This project requires some setup in order to run, since we rely on the Stanford CoreNLP library.

First, import the project into eclipse.
Then, configure the build path to include the referenced libraries in the 'lib' folder. The libraries we include in our build of the project are:
- stanford-corenlp-3.7.0
- stanford-corenlp-3.7.0-models
- ejml-0.23
- javax.json
- joda-time
- jollyday
- protobuf
- slf4j-api
- slf4j-simple
- xom

In order to run this, we find the easiest way to be using the -CLI argument to search a parsed document. For quick testing, try using the 'test' dataset. Be prepared for very long parse times if you are using the actual HTML file.

How we used the test dataset:
Program arguments: test -CLI
VM args: none
Workspace directory: {root project folder}

How we used the actual wiki dataset:
Program arguments: data/small -CLI
VM args: -Xms512M -Xmx1024M
Workspace directory: {root project folder}

You can experiment with the THRESHOLD variable in the NLP file to determine how much the parser will care about the accuracy of the query to the sentence it is matching. A low threshold will match more, a high threshold will match less.

We are aware that certain searches cause outofmemory exceptions in the wiki text. Unfortunately, those are due to the CoreNLP library and we don't think there's anything we can do about that except for reduce the document text. So if you want, you can cut out about half of the wiki article and it seems to work fine.