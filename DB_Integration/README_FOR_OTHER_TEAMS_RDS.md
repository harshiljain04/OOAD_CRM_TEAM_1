# ERP Integration Subsystem
# RDS Integration Guide For Other Teams

This document explains how other subsystem teams should use the shared live database on Amazon RDS through the Integration team JAR.

Important rule:

- Do not connect to MySQL directly from your subsystem code.
- Do not write your own JDBC queries for the shared ERP database.
- Use the shared JAR provided by the Integration subsystem.

The database is live on RDS, but all access must happen through our subsystem SDK so that permissions, logging, joins, and access rules are enforced centrally.

## Files We Will Give You

The Integration team will provide:

1. `erp-subsystem-sdk-1.0.0.jar`
2. `application-rds-template.properties`
3. this README

Main JAR file:

- [erp-subsystem-sdk-1.0.0.jar]

RDS config template:

- [application-rds-template.properties]
## What This JAR Does

The JAR gives each subsystem a Java facade class. Your team should use the facade that matches your subsystem.

Examples:

- `CRM`
- `Marketing`
- `SalesManagement`
- `OrderProcessing`
- `SupplyChain`
- `Manufacturing`
- `HR`
- `ProjectManagement`
- `Reporting`
- `DataAnalytics`
- `BusinessIntelligence`
- `Automation`
- `BusinessControl`
- `FinancialManagement`
- `UI`

These classes internally handle:

- connection pooling
- permission checks
- CRUD operations
- audit logging
- join handling
- database exceptions

## Architecture You Must Follow

```text
Your Subsystem Code
        |
        v
Integration Team JAR
        |
        v
Subsystem Facade Class
        |
        v
Shared RDS MySQL Database
```

You must not bypass the JAR and talk to RDS directly.

## Step 1: Copy The Config File

Copy `application-rds-template.properties` into your project.

Recommended location:

```text
src/main/resources/application-rds.properties
```

Important:

- Copy the template and fill in your own environment values.
- Do not commit real passwords to git.
- Prefer environment-specific config files that are ignored by git.

Example on how it looks:

```properties
db.host=my-rds-endpoint.ap-south-1.rds.amazonaws.com
db.port=3306
db.name=erp_subsystem
db.username=erp_app_user
db.password=erp_password_here
db.pool.maxSize=10
db.backup.dir=backups
db.dump.command=mysqldump
db.restore.command=mysql
```

## Step 2: Add The JAR To Your Project

If you are using a simple Java project:

1. Create a `lib` folder in your project
2. Copy `erp-subsystem-sdk-1.0.0.jar` into it
3. Add it to your classpath

If you are compiling from command line:

```powershell
javac -cp ".;lib\erp-subsystem-sdk-1.0.0.jar" src\your\files\*.java
```

Run with:

```powershell
java -cp ".;lib\erp-subsystem-sdk-1.0.0.jar" your.main.Class
```

If your project already has a build system, add the JAR as an external dependency.

## Step 3: Use The Correct Subsystem Facade

Create your subsystem object using `SubsystemFactory`.

### Example: Business Process Controls

```java
import com.erp.sdk.config.DatabaseConfig;
import com.erp.sdk.factory.SubsystemFactory;
import com.erp.sdk.subsystem.BusinessControl;
import com.erp.sdk.subsystem.SubsystemName;

import java.nio.file.Path;

public class Demo {
    public static void main(String[] args) throws Exception {
        DatabaseConfig config = DatabaseConfig.fromProperties(
                Path.of("src", "main", "resources", "application-rds.properties")
        );

        try (BusinessControl control = (BusinessControl) SubsystemFactory.create(
                SubsystemName.BUSINESS_CONTROL,
                config
        )) {
            System.out.println(control.readAll("workflows", java.util.Map.of(), "control_lead"));
        }
    }
}
```

### Example: CRM

```java
import com.erp.sdk.config.DatabaseConfig;
import com.erp.sdk.factory.SubsystemFactory;
import com.erp.sdk.subsystem.CRM;
import com.erp.sdk.subsystem.SubsystemName;

import java.nio.file.Path;

public class Demo {
    public static void main(String[] args) throws Exception {
        DatabaseConfig config = DatabaseConfig.fromProperties(
                Path.of("src", "main", "resources", "application-rds.properties")
        );

        try (CRM crm = (CRM) SubsystemFactory.create(
                SubsystemName.CRM,
                config
        )) {
            System.out.println(crm.readAll("customers", java.util.Map.of(), "integration_lead"));
        }
    }
}
```

## Step 4: Use Only The Allowed Methods

Use these methods from your subsystem facade:

- `create(tableName, payload, username)`
- `readAll(tableName, filters, username)`
- `readById(tableName, idColumn, idValue, username)`
- `update(tableName, idColumn, idValue, payload, username)`
- `delete(tableName, idColumn, idValue, username)`
- `join(joinRequest, username)`

Do not use:

- `DriverManager.getConnection(...)`
- direct SQL statements against RDS
- your own DAO layer for the shared ERP database

## Step 5: Use A Valid Shared Username

The `username` parameter you pass into facade methods is the ERP user identity used for permission and audit logging.

Examples already seeded in the shared schema:

- `admin_main`
- `integration_lead`
- `mfg_lead`
- `supply_lead`
- `finance_user`
- `control_lead`

If your subsystem has a dedicated user seeded by the Integration team, use that username.

## How Permissions Work

Each subsystem is allowed to access only specific tables and columns.

If your code tries to access a table not granted to your subsystem, the SDK will throw an exception.

That is expected behavior.

Example:

```java
crm.readAll("permission_matrix", Map.of(), "integration_lead");
```

This should fail for most non-Integration subsystems.

## How To Verify It Works

### Test 1: Connection test

Try a simple read on a table your subsystem is allowed to access.

Example:

```java
control.readAll("workflows", Map.of(), "control_lead");
```

Expected result:

- data is returned from the live RDS database

### Test 2: CRUD test

Create one test row, read it, update it, and delete it.

Example for workflows:

```java
long id = control.create(
    "workflows",
    java.util.Map.of(
        "workflow_name", "RDS Test Workflow",
        "status", "In Progress",
        "created_by", "control_lead"
    ),
    "control_lead"
);
```

Then read:

```java
control.readAll("workflows", java.util.Map.of("workflow_name", "RDS Test Workflow"), "control_lead");
```

### Test 3: Permission test

Try reading a table your subsystem should not access.

Expected result:

- an unauthorized access exception is raised

### Test 4: Audit test

After a successful or failed operation, ask the Integration team to check `audit_logs`.

Expected result:

- your access attempt is recorded

## If Your Existing Code Uses JDBC Directly

You must replace it.

Patterns that must be removed:

- `DriverManager.getConnection(...)`
- direct SQL in DAOs
- separate local MySQL database names
- hardcoded local usernames/passwords

Replace those with:

- `DatabaseConfig.fromProperties(...)`
- `SubsystemFactory.create(...)`
- your subsystem facade methods

## Common Problems

### 1. Access denied

Possible reasons:

- wrong RDS username/password in properties file
- RDS security group is not allowing your IP
- your subsystem is trying to access a table it is not allowed to use

### 2. Unknown table

Possible reasons:

- wrong table name
- your subsystem should use a compatibility view instead
- Integration team has not deployed the latest schema

### 3. Timeout or cannot connect

Possible reasons:

- wrong RDS endpoint
- wrong port
- security group blocks access
- database instance is stopped or unavailable

## What You Should Send Back To The Integration Team If Something Fails

Share:

1. the exact Java code call you made
2. the exact table name you tried to access
3. the full exception message
4. your subsystem name

That makes it much easier to fix permissions or mappings.

## Final Rule

Every subsystem must use:

- the shared RDS database
- the Integration team JAR
- the matching subsystem facade class

Every subsystem must not:

- create its own ERP database
- access RDS directly through its own JDBC code
- bypass permission and audit handling

That is the intended design of the Integration subsystem.
