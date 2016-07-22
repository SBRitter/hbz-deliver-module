# hbz deliver prototype

## Install lsp-apis-impl and run it with embedded MongoDB

* Get the project: https://github.com/sling-incubator/lsp-apis-impl/
* Build it (run `mvn clean install` in the subdirectory `domain-models-poc` of that project) 
* Go to the subdirectory circulation: `cd lsp-apis-impl/circulation`
* Run it: `java -jar target/circulation-fat.jar`
* You're now using an embedded MongoDB

## Create demo data

Change into the project root directory of hbz-deliver-module (e.g. okapi/hbz-deliver-module/)

### Create an item

```
curl -X POST http://localhost:8081/apis/items \
-H "Content-Type: application/json" -H "Accept: application/json" \
-d @item_sample.json -H "Authorization: Bearer a2VybWl0Omtlcm1pdA=="
```

### Create a patron

```
curl -X POST http://localhost:8081/apis/patrons \
-H "Content-Type: application/json" -H "Accept: application/json" \
-d @patron_sample.json -H "Authorization: Bearer a2VybWl0Omtlcm1pdA=="
```
 
## Build and run the deliver prototype

* Make sure, you're still in the project root directory of the deliver module
* Build the project: `mvn package -DskipTests`
* Run it: `java -jar target/hbz-deliver-module-fat.jar`

### Loan item
* Open browser an go to `http://localhost:8080/deliver/loan`
* Enter the ids of the item and the patron you created before and click loan
* If you don't know them, run the following to get them: 

```
curl -XGET http://localhost:8081/apis/patrons \
-H "Accept: application/json" \
-H "Authorization: Bearer a2VybWl0Omtlcm1pdA=="
```

and

```
curl -XGET http://localhost:8081/apis/items \
-H "Accept: application/json" \
-H "Authorization: Bearer a2VybWl0Omtlcm1pdA=="
```

### List all loans of a patron and return item
* Open browser an go to `http://localhost:8080/deliver/listLoans`
* Enter the id of the patron you created the loan for
* Click on return on a loan

## Deploy in Okapi

You can use the okapi_deploy_script in the project root directory to deploy the module on Okapi. This script uses the two files deliver-module-deploy.json and deliver-module-proxy.json. Note that you probably need to alter the tenant to enable the module for within the script. 

If you call the UI via your browser after you deployed the module on Okapi through https://localhost:9130/loan and http://localhost:9130/listLoans, you have to somehow pass a tenant (this is required for anything done on Okapi). This can be done with a Firefox Addon called Http Header Mangler (https://github.com/disptr/httpheadermangler). You pass a file located somewhere in your filesystem. In this file, place the following lines:

```
localhost
X-Okapi-Tenant=your-tenant
```

Now everytime localhost is called, the additional header "X-Okapi-Tenant" will be sent.

## Testing

You can run the tests using the following steps:
* Run the lsp-apis-impl circulation module like desribed above with an embedded MongoDB (`java -jar circulation-fat.jar`)
* Run the tests of the hbz deliver module: `mvn clean verify`
* Stop the lsp-apis-impl circulation module (Ctrl+C)

(Automatic start and stop of the circulation is planned)
