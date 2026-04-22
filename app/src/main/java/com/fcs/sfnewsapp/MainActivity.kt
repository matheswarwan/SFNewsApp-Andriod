package com.fcs.sfnewsapp

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessaging
import com.salesforce.marketingcloud.MarketingCloudSdk
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk

class MainActivity : Activity() {
    private lateinit var scrollView: ScrollView
    private lateinit var content: LinearLayout
    private lateinit var identifiersText: TextView
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var addressInput: EditText
    private lateinit var contactKeyInput: EditText

    private var currentScreen: Screen = Screen.Home
    private var currentContactKey: String = ""
    private var currentFcmToken: String = ""
    private var currentSfmcSystemToken: String = ""
    private var currentSfmcDeviceId: String = ""
    private var currentSfmcContactKey: String = ""
    private var lastDeepLink: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createRootView())
        requestNotificationPermissionIfNeeded()
        routeIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (currentScreen == Screen.DeviceInfo) {
            refreshIdentifiers()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeIntent(intent)
    }

    private fun createRootView(): ScrollView {
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        scrollView = ScrollView(this).apply {
            isFillViewport = true
            addView(
                content,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
        return scrollView
    }

    private fun routeIntent(intent: Intent) {
        val uri = intent.data
        if (uri == null) {
            Log.d(TAG, "No deep link URI received")
            showHome()
            return
        }

        Log.d(TAG, "Received deep link URI: $uri")
        lastDeepLink = uri
        when (val route = routeFromUri(uri)) {
            is Route.DeviceInfo -> showDeviceInfo()
            is Route.UpdateDetails -> showUpdateDetails()
            is Route.Product -> showProduct(route.product)
            Route.Home -> showHome()
        }
    }

    private fun routeFromUri(uri: Uri): Route {
        val segments = uri.pathSegments
        return when {
            segments.isEmpty() -> Route.Home
            segments[0] == "device-info" -> Route.DeviceInfo
            segments[0] == "update-details" -> Route.UpdateDetails
            segments[0] == "product" -> {
                val product = products.firstOrNull { it.route == segments.getOrNull(1) }
                if (product == null) Route.Home else Route.Product(product)
            }
            else -> Route.Home
        }
    }

    private fun showHome() {
        currentScreen = Screen.Home
        resetContent()
        addTitle(getString(R.string.home_title))
        addBody(getString(R.string.home_subtitle))
        addDeepLinkStatus()

        addSection(getString(R.string.menu_title))
        addMenuButton(getString(R.string.device_info_title), getString(R.string.device_info_subtitle)) {
            showDeviceInfo()
        }
        addMenuButton(getString(R.string.update_details_title), getString(R.string.update_details_subtitle)) {
            showUpdateDetails()
        }

        addSection(getString(R.string.products_title))
        products.forEach { product ->
            addMenuButton(product.title, product.subtitle) {
                showProduct(product)
            }
        }
    }

    private fun showDeviceInfo() {
        currentScreen = Screen.DeviceInfo
        resetContent()
        addBackButton()
        addTitle(getString(R.string.device_info_title))
        addBody(getString(R.string.device_info_body))
        addDeepLinkStatus()

        identifiersText = TextView(this).apply {
            text = getString(R.string.identifiers_loading)
            textSize = 14f
            setTextIsSelectable(true)
        }
        content.addView(identifiersText, matchWrap(top = 16))

        addButton(getString(R.string.copy_all)) {
            copyToClipboard(getString(R.string.copy_all), identifiersText.text.toString())
        }
        addButton(getString(R.string.copy_contact_key)) {
            copyToClipboard(getString(R.string.copy_contact_key), currentContactKey)
        }
        addButton(getString(R.string.copy_fcm_token)) {
            copyToClipboard(getString(R.string.copy_fcm_token), currentFcmToken)
        }
        addButton(getString(R.string.copy_sfmc_system_token)) {
            copyToClipboard(getString(R.string.copy_sfmc_system_token), currentSfmcSystemToken)
        }
        addButton(getString(R.string.copy_sfmc_device_id)) {
            copyToClipboard(getString(R.string.copy_sfmc_device_id), currentSfmcDeviceId)
        }
        addButton(getString(R.string.copy_sfmc_contact_key)) {
            copyToClipboard(getString(R.string.copy_sfmc_contact_key), currentSfmcContactKey)
        }

        val optButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(
                makeButton(getString(R.string.opt_in)) { optIn() },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
            addView(
                makeButton(getString(R.string.opt_out)) { optOut() },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
        }
        content.addView(optButtons, matchWrap(top = 16))

        refreshIdentifiers()
    }

    private fun showUpdateDetails() {
        currentScreen = Screen.UpdateDetails
        resetContent()
        addBackButton()
        addTitle(getString(R.string.update_details_title))
        addBody(getString(R.string.update_details_body))
        addDeepLinkStatus()

        addSection(getString(R.string.user_details_title))
        nameInput = createInput(getString(R.string.name_hint))
        emailInput = createInput(getString(R.string.email_hint))
        phoneInput = createInput(getString(R.string.phone_hint))
        addressInput = createInput(getString(R.string.address_hint))
        content.addView(nameInput, matchWrap(top = 8))
        content.addView(emailInput, matchWrap(top = 8))
        content.addView(phoneInput, matchWrap(top = 8))
        content.addView(addressInput, matchWrap(top = 8))
        addButton(getString(R.string.update_user_details)) {
            updateUserDetails()
        }

        addSection(getString(R.string.contact_key_title))
        contactKeyInput = createInput(getString(R.string.contact_key_hint))
        content.addView(contactKeyInput, matchWrap(top = 8))
        addButton(getString(R.string.update_contact_key)) {
            updateContactKey()
        }
    }

    private fun showProduct(product: ProductTile) {
        currentScreen = Screen.Product
        resetContent()
        addBackButton()
        addTitle(product.title)
        addBody(product.body)
        addDeepLinkStatus()
        addSection(getString(R.string.sample_deep_link_title))
        addBody("sfnews://app/product/${product.route}")
    }

    private fun resetContent() {
        content.removeAllViews()
        scrollView.scrollTo(0, 0)
    }

    private fun addBackButton() {
        addButton(getString(R.string.back_home)) {
            lastDeepLink = null
            showHome()
        }
    }

    private fun addTitle(text: String) {
        content.addView(
            TextView(this).apply {
                this.text = text
                textSize = 24f
                gravity = Gravity.CENTER_HORIZONTAL
            },
            matchWrap(),
        )
    }

    private fun addSection(text: String) {
        content.addView(
            TextView(this).apply {
                this.text = text
                textSize = 18f
            },
            matchWrap(top = 24),
        )
    }

    private fun addBody(text: String) {
        content.addView(
            TextView(this).apply {
                this.text = text
                textSize = 15f
            },
            matchWrap(top = 8),
        )
    }

    private fun addDeepLinkStatus() {
        val text = lastDeepLink?.let { getString(R.string.deep_link_received, it.toString()) }
            ?: getString(R.string.no_deep_link)
        content.addView(
            TextView(this).apply {
                this.text = text
                textSize = 14f
                setTextIsSelectable(true)
            },
            matchWrap(top = 16),
        )
    }

    private fun addMenuButton(title: String, subtitle: String, onClick: () -> Unit) {
        content.addView(
            makeButton("$title\n$subtitle", onClick),
            matchWrap(top = 12),
        )
    }

    private fun addButton(text: String, onClick: () -> Unit) {
        content.addView(
            makeButton(text, onClick),
            matchWrap(top = 8),
        )
    }

    private fun makeButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            isAllCaps = false
            setOnClickListener { onClick() }
        }
    }

    private fun createInput(hint: String): EditText {
        return EditText(this).apply {
            this.hint = hint
            setSingleLine(false)
            minLines = 1
        }
    }

    private fun matchWrap(top: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = dp(top)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun refreshIdentifiers() {
        val contactKey = AppIdentity.getOrCreateContactKey(this)
        updateIdentifierText(
            contactKey = contactKey,
            fcmToken = getString(R.string.value_loading),
            sfmcSystemToken = getString(R.string.value_pending_sfmc),
            sfmcDeviceId = getString(R.string.value_pending_sfmc),
            sfmcContactKey = getString(R.string.value_pending_sfmc),
        )

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                updateIdentifierText(
                    contactKey = contactKey,
                    fcmToken = token,
                    sfmcSystemToken = currentSfmcSystemToken,
                    sfmcDeviceId = currentSfmcDeviceId,
                    sfmcContactKey = currentSfmcContactKey,
                )
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Unable to get FCM token", error)
                updateIdentifierText(
                    contactKey = contactKey,
                    fcmToken = "Unavailable: ${error.message ?: error.javaClass.simpleName}",
                    sfmcSystemToken = currentSfmcSystemToken,
                    sfmcDeviceId = currentSfmcDeviceId,
                    sfmcContactKey = currentSfmcContactKey,
                )
            }

        if (!MarketingCloudSdk.isReady()) {
            Log.d(TAG, "SFMC SDK is not ready yet")
            return
        }

        MarketingCloudSdk.requestSdk { sdk ->
            val registrationManager = sdk.registrationManager
            currentSfmcSystemToken = registrationManager.systemToken.orEmpty()
                .ifBlank { getString(R.string.value_not_available_yet) }
            currentSfmcDeviceId = registrationManager.deviceId.orEmpty()
                .ifBlank { getString(R.string.value_not_available_yet) }
            currentSfmcContactKey = registrationManager.contactKey.orEmpty()
                .ifBlank { contactKey }

            runOnUiThread {
                updateIdentifierText(
                    contactKey = contactKey,
                    fcmToken = currentFcmToken,
                    sfmcSystemToken = currentSfmcSystemToken,
                    sfmcDeviceId = currentSfmcDeviceId,
                    sfmcContactKey = currentSfmcContactKey,
                )
            }
        }
    }

    private fun updateIdentifierText(
        contactKey: String,
        fcmToken: String,
        sfmcSystemToken: String,
        sfmcDeviceId: String,
        sfmcContactKey: String,
    ) {
        currentContactKey = contactKey
        currentFcmToken = fcmToken
        currentSfmcSystemToken = sfmcSystemToken
        currentSfmcDeviceId = sfmcDeviceId
        currentSfmcContactKey = sfmcContactKey
        if (::identifiersText.isInitialized) {
            identifiersText.text = getString(
                R.string.identifiers_template,
                contactKey,
                fcmToken,
                sfmcSystemToken,
                sfmcDeviceId,
                sfmcContactKey,
            )
        }
    }

    private fun copyToClipboard(label: String, value: String) {
        if (value.isBlank()) {
            Toast.makeText(this, getString(R.string.copy_value_missing), Toast.LENGTH_SHORT).show()
            return
        }

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(this, getString(R.string.copy_success, label), Toast.LENGTH_SHORT).show()
    }

    private fun optIn() {
        if (requestNotificationPermissionIfNeeded()) {
            Toast.makeText(this, getString(R.string.allow_notifications_then_opt_in), Toast.LENGTH_LONG).show()
            return
        }

        val contactKey = AppIdentity.getOrCreateContactKey(this)
        val attributes = currentUserAttributes()
        runWithMarketingCloud(getString(R.string.sfmc_not_ready)) { sdk ->
            sdk.mp { push ->
                val registrationEditor = push.registrationManager.edit()
                    .setContactKey(contactKey)
                    .addTag("general")
                attributes.forEach { (key, value) ->
                    registrationEditor.setAttribute(key, value)
                }

                push.pushMessageManager.enablePush()
                registrationEditor.commit()
            }

            sdk.identity.setProfileId(contactKey)
            attributes.forEach { (key, value) ->
                sdk.identity.setProfileAttribute(key, value)
            }

            runOnUiThread {
                Toast.makeText(this, getString(R.string.opt_in_success), Toast.LENGTH_SHORT).show()
                refreshIdentifiers()
            }
        }
    }

    private fun optOut() {
        runWithMarketingCloud(getString(R.string.sfmc_not_ready)) { sdk ->
            sdk.mp { it.pushMessageManager.disablePush() }
            runOnUiThread {
                Toast.makeText(this, getString(R.string.opt_out_success), Toast.LENGTH_SHORT).show()
                refreshIdentifiers()
            }
        }
    }

    private fun updateUserDetails() {
        val attributes = currentUserAttributes()

        if (attributes.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_user_details), Toast.LENGTH_SHORT).show()
            return
        }

        runWithMarketingCloud(getString(R.string.sfmc_not_ready)) { sdk ->
            sdk.mp { push ->
                val registrationEditor = push.registrationManager.edit()
                attributes.forEach { (key, value) ->
                    registrationEditor.setAttribute(key, value)
                }
                val registrationCommitted = registrationEditor.commit()

                runOnUiThread {
                    val message = if (registrationCommitted) {
                        getString(R.string.user_details_updated)
                    } else {
                        getString(R.string.user_details_update_failed)
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    refreshIdentifiers()
                }
            }

            attributes.forEach { (key, value) ->
                sdk.identity.setProfileAttribute(key, value)
            }
        }
    }

    private fun currentUserAttributes(): Map<String, String> {
        if (!::nameInput.isInitialized) return emptyMap()
        return mapOf(
            "name" to nameInput.text.toString().trim(),
            "email" to emailInput.text.toString().trim(),
            "phone" to phoneInput.text.toString().trim(),
            "address" to addressInput.text.toString().trim(),
        ).filterValues { it.isNotBlank() }
    }

    private fun updateContactKey() {
        val newContactKey = contactKeyInput.text.toString().trim()
        if (newContactKey.isBlank()) {
            Toast.makeText(this, getString(R.string.contact_key_required), Toast.LENGTH_SHORT).show()
            return
        }

        AppIdentity.setContactKey(this, newContactKey)
        runWithMarketingCloud(getString(R.string.sfmc_not_ready)) { sdk ->
            sdk.mp { push ->
                val registrationCommitted = push.registrationManager.edit()
                    .setContactKey(newContactKey)
                    .addTag("general")
                    .commit()

                runOnUiThread {
                    currentContactKey = newContactKey
                    contactKeyInput.text.clear()
                    val message = if (registrationCommitted) {
                        getString(R.string.contact_key_updated)
                    } else {
                        getString(R.string.contact_key_update_failed)
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    refreshIdentifiers()
                }
            }
            sdk.identity.setProfileId(newContactKey)
        }
    }

    private fun runWithMarketingCloud(
        notReadyMessage: String,
        action: (SFMCSdk) -> Unit,
    ) {
        if (!MarketingCloudSdk.isReady()) {
            Toast.makeText(this, notReadyMessage, Toast.LENGTH_SHORT).show()
            return
        }

        SFMCSdk.requestSdk { sdk ->
            action(sdk)
        }
    }

    private fun requestNotificationPermissionIfNeeded(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return false
        }

        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_POST_NOTIFICATIONS,
        )
        return true
    }

    private enum class Screen {
        Home,
        DeviceInfo,
        UpdateDetails,
        Product,
    }

    private sealed class Route {
        data object Home : Route()
        data object DeviceInfo : Route()
        data object UpdateDetails : Route()
        data class Product(val product: ProductTile) : Route()
    }

    private data class ProductTile(
        val route: String,
        val title: String,
        val subtitle: String,
        val body: String,
    )

    private val products by lazy {
        listOf(
            ProductTile(
                route = "breaking",
                title = getString(R.string.product_breaking_title),
                subtitle = getString(R.string.product_breaking_subtitle),
                body = getString(R.string.product_breaking_body),
            ),
            ProductTile(
                route = "markets",
                title = getString(R.string.product_markets_title),
                subtitle = getString(R.string.product_markets_subtitle),
                body = getString(R.string.product_markets_body),
            ),
            ProductTile(
                route = "sports",
                title = getString(R.string.product_sports_title),
                subtitle = getString(R.string.product_sports_subtitle),
                body = getString(R.string.product_sports_body),
            ),
            ProductTile(
                route = "weather",
                title = getString(R.string.product_weather_title),
                subtitle = getString(R.string.product_weather_subtitle),
                body = getString(R.string.product_weather_body),
            ),
        )
    }

    private companion object {
        const val TAG = "MainActivity"
        const val REQUEST_POST_NOTIFICATIONS = 1001
    }
}
