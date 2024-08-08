# Fraplin: A Gradle Plugin for Generating Kotlin DSL for Frappe Sites

Welcome to Fraplin! Fraplin is a Gradle plugin that generates Kotlin DSL (Domain Specific Language) for Frappe sites. This DSL facilitates CRUD (Create, Read, Update, Delete) operations by making REST API calls to a Frappe site, leveraging the strong typing capabilities of Kotlin.

## Features

- **Strongly Typed Kotlin DSL:** Provides a strongly typed interface for interacting with Frappe sites, reducing runtime errors and improving code clarity.
- **Automated CRUD Operations:** Simplifies CRUD operations via Kotlin DSL, making development faster and more efficient.
- **REST API Integration:** Utilizes Frappe's REST API for seamless communication between Kotlin and Frappe.

## Installation

To use Fraplin in your project, add the following to your `build.gradle` file:

```kotlin
plugins {
    id("io.github.klahap.fraplin") version "$VERSION"
}
```

## Usage

### Configuration

Configure the Fraplin plugin in your `build.gradle.kts` file:

```kotlin

fraplin {
    // Specify the path to the file that stores all DocTypes.
    // This file can be generated with `gradle generateFraplinSpec` 
    // or used as input to generate the DSL with `gradle generateFraplinDsl`
    specFile = Path("$projectDir/src/main/resources/fraplin.json")

    output { // Define output settings
        packageName = "com.example.frappe.dsl"
        output = "$buildDir/generated/frappe/dsl"
    }

    input { // Define input sources for generating DocTypes
        sourceSite { // Use a direct API connection to a Frappe Site
            url = System.getenv("FRAPPE_SITE_URL").toHttpUrl()
            userToken = System.getenv("FRAPPE_USER_API_TOKEN")
        }
        sourceCloud { // Or, use Frappe Cloud credentials to connect to a Frappe Site
            url = System.getenv("FRAPPE_SITE_URL").toHttpUrl()
            cloudToken = System.getenv("FRAPPE_CLOUD_API_TOKEN")
            team = System.getenv("FRAPPE_CLOUD_TEAM")
        }
        sourceRepo { // Or, use GitHub repositories as a source
            gitHub { // public GitHub repository
                owner = "frappe"; repo = "frappe"; version = "v15.36.1"
            }
            gitHub { // private GitHub repository
                owner = "foobar"; repo = "hello_world"; version = "f92bcc0faff8f7aed11c90421f27264b779ba6b6"
                appName = "my_app"
                creds {
                    username = System.getenv("GITHUB_USERNAME")
                    token = System.getenv("GITHUB_TOKEN")
                }
            }
            local { // local git repository
                path = Path("/my/local/frappe/app/repo/my_app")
                appName = "my_app"
            }
        }

        // Add DocTypes to be generated
        addDocType("Company", strictTyped = true)
        addDocType("Account")
        addDocType("Website Settings")
        addDocType("Web Page")
        addDocType("Email Domain")
        addDocType("Email Account")
    }
}
```

### Generating the DSL

Run the following Gradle task to generate the Kotlin DSL:

```bash
./gradlew generateFraplin
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