# hbz deliver prototype

Note: this is work in progress, so far this module only works for one tenant, called hbz in this sample.

## Setup and run Okapi

Best cd to some directory where you store all projects that will be used in this guide, like ~/code/ or ~/git/. 

```
git clone https://github.com/folio-org/okapi.git
cd okapi
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

## Get and build the mod-circulation project

Go back to the directory above the Okapi project and this project (e.g. some directory like ~/code/ or ~/git/) and clone the mod-circulation project next to the Okapi project. This directory layout is needed because Okapi's base path is where `mvn exec:exec` has been invoked and deployment files use relative paths.

```
git
├── okapi
├── hbz-deliver-module
└── mod-circulation
```
See https://github.com/folio-org/mod-circulation for documentation on how to get and build the module.

We will deploy this module later on in this guide.

## Get, build and deploy the deliver prototype on Okapi

Use another shell. Go back to the directory above Okapi.
```
git clone https://github.com/SBRitter/hbz-deliver-module
cd hbz-deliver-module
mvn package -DskipTests
./okapi_deploy_script_deliver
```

This will create a tenant in Okapi called hbz and deploy the deliver module.

## Deploy the mod-circulation on Okapi

Make sure you are in the directory of the hbz-deliver-module. Run the deploy script for the mod-circulation module. This will deploy the module on Okapi.

```
./okapi_deploy_script_circ
```

## Create demo data

Use http://localhost:9130/sampleData to create sample patrons and items to play around with.

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
* You can return a loan by clicking on "return"...
* ...or renew a loan for another two loan period by clicking on "renew"

## Testing

You can run the tests using the following steps:
* Run the circulation module with an embedded MongoDB on port 8081 (`java -jar circulation-fat.jar embed_mongo=true`), i.e. stand-alone without Okapi
* Build the deliver module: `mvn package -DskipTests`
* Run the deliver module: `java -jar target/hbz-deliver-module-fat.jar`
* Open a browser with `http://localhost:8080/deliver/loan`
* After testing stop the modules (Ctrl+C)

(Automatic start and stop of the circulation is planned)
