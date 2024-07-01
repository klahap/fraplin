package default_code

import okhttp3.HttpUrl

class FrappeSiteNotExistsException(siteUrl: HttpUrl) : Exception("frappe site '$siteUrl' not exists")
