# Fraplin: A Gradle Plugin for Generating Kotlin DSL for [Frappe Framework Sites](https://frappeframework.com/docs/user/en/introduction)

Welcome to Fraplin! Fraplin is a Gradle plugin that generates Kotlin DSL (Domain Specific Language) for Frappe sites. This DSL facilitates CRUD (Create, Read, Update, Delete) operations by making REST API calls to a Frappe site, leveraging the strong typing capabilities of Kotlin.

## Features

- **Strongly Typed Kotlin DSL:** Provides a strongly typed interface for interacting with Frappe sites, reducing runtime errors and improving code clarity.
- **Automated CRUD Operations:** Simplifies CRUD operations via Kotlin DSL, making development faster and more efficient.
- **REST API Integration:** Utilizes Frappe's REST API for seamless communication between Kotlin and Frappe.

## Installation

To use Fraplin in your project, add the following to your `build.gradle` file:

```groovy
plugins {
    id "com.fraplin" version "1.0.1"
}
```

## Usage

### Configuration

Configure the Fraplin plugin in your `build.gradle.kts` file:

```kotlin

frappeDslGenerator {
    // use Frappe Cloud creds to connect to a Frappe Site
    site = FrappeDslGeneratorExtension.SiteConfig.Cloud(
        url = System.getenv("FRAPPE_SITE_URL").toHttpUrl(),
        cloudToken = System.getenv("FRAPPE_CLOUD_API_TOKEN"),
    )
    
    // or use directly an API of a Frappe Site
    site = FrappeDslGeneratorExtension.SiteConfig.Site(
        url = System.getenv("FRAPPE_SITE_URL").toHttpUrl(),
        userToken = System.getenv("FRAPPE_USER_API_TOKEN"),
    )

    packageName = "com.example.frappe.dsl"
    output = "$buildDir/generated/frappe/dsl"
    docTypes = setOf(
        DocTypeInfo("Company", strictTyped = true),
        DocTypeInfo("Account"),
        DocTypeInfo("Website Settings"),
        DocTypeInfo("Web Page"),
        DocTypeInfo("Email Domain"),
        DocTypeInfo("Email Account"),
    )
}
```

### Generating the DSL

Run the following Gradle task to generate the Kotlin DSL:

```bash
./gradlew generateFrappeDsl
```

This will generate Kotlin classes representing your Frappe site's data models and CRUD operations.

### Example Usage

Here's a basic example of how to use the generated Kotlin DSL to perform CRUD operations:

```kotlin
import com.example.frappe.dsl.FrappeClient

fun main() {
    val service = FrappeSiteService(
        siteUrl = "<site-url>".toHttpUrl(),
        userApiToken = "<token>",
        httpClient = OkHttpClient(),
    )
    
    // Create a new document
    val newDoc = service.createCompany(
        companyName = "MyCompany",
        country = Country.Link("Germany"),
        /* ... all other mandatory fields */
    ) {
        // set optional fields 
        website = "https://my-company.com"
    }
    
    // Read a document
    val doc = service.loadCompany("MyCompany")
    // Read multiple documents
    val docs = service.loadAll<Company> {
        filters {
            Company::name like "My%"
            Company::creation before LocalDate(year = 2024, month = Month.JUNE, dayOfMonth = 5).atTime(hour = 0, minute = 0)
            Company::country In setOf(Country.Link("Germany"), Country.Link("Switzerland"))
            // ... lot more options
        }
        orderByAsc(Company::name)
    }
    
    // Update a document
    service.updateCompany("MyCompany") {
        website = "https://my-company-2.com"
    }
    
    // Delete a document
    service.deleteCompany("MyCompany")
}
```

## Contributing

We welcome contributions to Fraplin! If you have any ideas, suggestions, or bug reports, please create an issue or submit a pull request on GitHub.

---

Thank you for using Fraplin! We hope this plugin makes your development with Frappe and Kotlin easier and more enjoyable.
