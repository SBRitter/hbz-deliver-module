# hbz deliver prototype

Note: this is work in progress, so far this module only works for one tenant, called hbz in this sample.

## Setup and run Okapi

clone this fork (best cd to some directory where you store projects that will be used in this guide, like ~/code/ or ~/git/)
git clone https://github.com/SBRitter/okapi.git. Then, checkout the feature branch, build and run okapi.

```
cd okapi
git checkout deliver-prototype
mvn clean install -DskipTests
mvn exec:exec
```

## Configure browser to send tenant id

If you want to use the module via your browser after you deployed the module on Okapi through https://localhost:9130/loan and http://localhost:9130/listLoans, you have to somehow pass a tenant (this is required for anything done on Okapi). This can be done with a Firefox Addon called Http Header Mangler (https://addons.mozilla.org/firefox/addon/http-header-mangler/). You pass a file located somewhere in your filesystem. In this file, place the following lines:

```
localhost
X-Okapi-Tenant=hbz
```

Now everytime localhost is called, the additional header "X-Okapi-Tenant" will be sent.

## Build and deploy the deliver prototype on Okapi

Use another shell. Go to the project root directory of the deliver module and build the project. Then, run the deploy script for deliver.
```
cd okapi/hbz-deliver-module
mvn package -DskipTests
./okapi_deploy_script_deliver
```

This will create a tenant in Okapi called hbz and deploy the deliver module.

## Deploy circulation module of lsp-apis-impl on Okapi

Go back to the directory above the Okapi project (e.g. some directory like ~/code/ or ~/git/). Get the project and build & run the two subprojects domain-models-poc and circulation.

```
cd ../..
git clone https://github.com/sling-incubator/lsp-apis-impl.git
cd lsp-apis-impl/domain-models-poc
mvn clean install -DskipTests
cd ../circulation
mvn clean install -DskipTests
```

Go back to the directory of the hbz-deliver-module and run the deploy script for the lsp-apis-impl circulation submodule. This will deploy the lsp-apis-impl circulation module on Okapi.

```
cd ../../okapi/hbz-deliver-module
./okapi_deploy_script_lsp
```

## Create demo data

Make sure you're still in the project root directory of hbz-deliver-module okapi/hbz-deliver-module/.

### Create an item

```
curl -X POST http://localhost:9130/apis/items \
-H "Content-Type: application/json" -H "Accept: application/json" \
-d @item_sample.json -H "Authorization: Bearer a2VybWl0Omtlcm1pdA==" \
-H "X-Okapi-Tenant: hbz"
```

### Create a patron

```
curl -X POST http://localhost:9130/apis/patrons \
-H "Content-Type: application/json" -H "Accept: application/json" \
-d @patron_sample.json -H "Authorization: Bearer a2VybWl0Omtlcm1pdA==" \
-H "X-Okapi-Tenant: hbz"
```

### Loan item
* Open browser an go to `http://localhost:9130/deliver/loan`
* Enter the ids of the item and the patron you created before and click loan
* If you don't know them, run the following to get them: 

```
curl -XGET http://localhost:9130/apis/patrons \
-H "Accept: application/json" \
-H "Authorization: Bearer a2VybWl0Omtlcm1pdA==" \
-H "X-Okapi-Tenant: hbz"
```

and

```
curl -XGET http://localhost:9130/apis/items \
-H "Accept: application/json" \
-H "Authorization: Bearer a2VybWl0Omtlcm1pdA==" \
-H "X-Okapi-Tenant: hbz"
```

### List all loans of a patron and return item
* Open browser an go to `http://localhost:9130/deliver/listLoans`
* Enter the id of the patron you created the loan for
* Click on return on a loan

## Testing

You can run the tests using the following steps:
* Run the lsp-apis-impl circulation module with an embedded MongoDB on port 8081 (`java -jar circulation-fat.jar`), i.e. stand-alone without Okapi
* Run the tests of the hbz deliver module: `mvn clean verify`
* Stop the lsp-apis-impl circulation module (Ctrl+C)

(Automatic start and stop of the circulation is planned)
