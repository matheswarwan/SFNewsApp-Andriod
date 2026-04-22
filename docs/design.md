# Design

## Current UX

The app starts on a simple native Android screen. It displays:

- Home menu with tiles
- Device Info page for tokens, IDs, opt-in, opt-out, and copy controls
- Update Details page for user details and contact key registration
- Product tile pages for Breaking News, Markets, Sports, and Weather
- Notification permission prompt on Android 13+

## Deep Links

Supported scheme and host:

```text
sfnews://app
```

Example:

```text
sfnews://app/product/weather
```

## Notifications

Notification small icon:

```text
app/src/main/res/drawable/ic_notification.xml
```

Default marketing notification channel:

```text
marketing_default
```

Channel display name:

```text
Marketing Notifications
```

## Design Change Log

- Initial app screen and deep link display were added.
- Package/application ID set to `com.fcs.sfnewsapp`.
- Launch screen now displays Firebase and SFMC identifiers for testing.
- Copy buttons added for testing identifiers.
- Android 13+ notification permission request added.
- SFMC opt-in/opt-out buttons and registration forms added.
- Home/menu routing added.
- Product tile pages added.
- Deep links route to product and utility pages.
