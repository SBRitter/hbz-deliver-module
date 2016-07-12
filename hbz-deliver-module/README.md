# hbz deliver prototype

## Install lsp-apis-impl and run it with embedded MongoDB

* Get the project: https://github.com/sling-incubator/lsp-apis-impl/
* Build it (run `mvn clean install` in the subdirectory `domain-models-poc` of that project) 
* Start it using: `java -jar circulation-fat.jar`(you're now using an embedded MongoDB)

## Create demo data

Change into the project root directory of hbz-deliver-module (e.g. okapi/hbz-deliver-module/)

### Create an item

```
curl -X POST http://localhost:8081/apis/items \
-H "Content-Type: text/plain" -H "Accept: text/plain" \
-d @item_sample.json -H "Authorization: Bearer a2VybWl0Omtlcm1pdA=="
```

### Create a patron

```
curl -X POST http://localhost:8081/apis/patrons \
 -H "Content-Type: text/plain" -H "Accept: text/plain" \
 -d @patron_sample.json -H "Authorization: Bearer a2VybWl0Omtlcm1pdA=="
```
 
## Build and run the deliver prototype

Make sure, you're still in the project root directory of the deliver module
mvn package
java -jar target/hbz-deliver-module-fat.jar

### Loan item
* Open browser an go to localhost:8080/deliver/loan
* Enter the ids of the item and the patron you created before and click loan
* If you don't know them, run the following to get them: 

```
curl -XGET http://localhost:8081/apis/patrons \
-H "Content-Type: text/plain" -H "Accept: application/json" \
-H "Authorization: Bearer a2VybWl0Omtlcm1pdA=="
```

and

```
curl -XGET http://localhost:8081/apis/items \
-H "Content-Type: text/plain" -H "Accept: application/json" \
-H "Authorization: Bearer a2VybWl0Omtlcm1pdA=="
```

### List all loans of a patron and return item
* Open browser an go to localhost:8080/deliver/listLoans
* Enter the id of the patron you created the loan for
* Click on return on a loan