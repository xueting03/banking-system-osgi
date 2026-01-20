# Banking System OSGi

Prereqs: Java 17+ and Maven.

## Download runtime bundles (not committed)
```
mkdir -p bundles
# Equinox framework + console
curl -L -o org.eclipse.osgi_3.24.0.v20251126-0427.jar https://download.eclipse.org/equinox/drops/R-4.38-202512010920/org.eclipse.osgi_3.24.0.v20251126-0427.jar
curl -L -o org.eclipse.equinox.console_1.4.1100.v20250722-0745.jar https://download.eclipse.org/equinox/drops/R-4.38-202512010920/org.eclipse.equinox.console_1.4.1100.v20250722-0745.jar
# Felix Gogo console dependencies
curl -L -o bundles/org.apache.felix.gogo.runtime-1.1.6.jar https://repo1.maven.org/maven2/org/apache/felix/org.apache.felix.gogo.runtime/1.1.6/org.apache.felix.gogo.runtime-1.1.6.jar
curl -L -o bundles/org.apache.felix.gogo.command-1.1.2.jar https://repo1.maven.org/maven2/org/apache/felix/org.apache.felix.gogo.command/1.1.2/org.apache.felix.gogo.command-1.1.2.jar
curl -L -o bundles/org.apache.felix.gogo.shell-1.1.4.jar https://repo1.maven.org/maven2/org/apache/felix/org.apache.felix.gogo.shell/1.1.4/org.apache.felix.gogo.shell-1.1.4.jar
```

## Build
```
mvn clean package
```

## Run Equinox with console
```
java -Dosgi.bundles=reference:file:bundles/org.apache.felix.gogo.runtime-1.1.6.jar@start,reference:file:bundles/org.apache.felix.gogo.command-1.1.2.jar@start,reference:file:bundles/org.apache.felix.gogo.shell-1.1.4.jar@start,reference:file:org.eclipse.equinox.console_1.4.1100.v20250722-0745.jar@start -jar org.eclipse.osgi_3.24.0.v20251126-0427.jar -clean -console -configuration configuration
```
You’ll see the Gogo prompt `g!` (modern replacement for the old `osgi>`).

## Install and start bundles inside the console
```
install file:bundles/org.osgi.util.function-1.2.0.jar
install file:bundles/org.osgi.util.promise-1.3.0.jar
install file:bundles/org.osgi.service.component-1.5.0.jar
install file:bundles/org.apache.felix.scr-2.2.4.jar
install file:bundles/h2-2.2.224.jar

install file:banking-api/target/banking-api-1.0.0.jar
install file:banking-persistence/target/banking-persistence-1.0.0.jar
install file:banking-account/target/banking-account-1.0.0.jar
install file:banking-customer/target/banking-customer-1.0.0.jar
install file:banking-deposit/target/banking-deposit-1.0.0.jar
install file:banking-transaction/target/banking-transaction-1.0.0.jar
install file:customer-support/target/customer-support-1.0.0.jar
install file:banking-card/target/banking-card-1.0.0.jar  
install file:banking-cli/target/banking-cli-1.0.0.jar

ss   # note the assigned bundle IDs
start <api-id>
start <account-id>
start <customer-id>
start <deposit-id>
start <transaction-id>
start <cli-id>
start <persistence-id>
start <support-id>
```
Start account before transaction so the account service is available; the transaction bundle will then run its demo. The CLI bundle provides interactive commands for the banking system. Use `stop 0` to shut down the framework.

The persistence bundle exposes a shared H2 DataSource at `jdbc:h2:./bankdb;AUTO_SERVER=TRUE`; the support bundle uses it to create the `SUPPORT_TICKET` table and persist tickets between runs.
```
java -cp ~/.m2/repository/com/h2database/h2/2.2.224/h2-2.2.224.jar org.h2.tools.Server -web -webPort 8082 -ifNotExists -baseDir /Users/teojiesern/Documents/school/WIF3006_CBSE/aa/banking-system-osgi (Replace this with the path to your project)
```