import XCTest

/// Login, settings and onboarding on iOS, driven against the shared ViewModels with in-memory fakes.
final class AccountFlowUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
    }

    private func launch(_ extra: [String] = []) {
        app.launchArguments = ["-uiTesting"] + extra
        app.launch()
    }

    private func openSettings() {
        let profile = app.buttons["movies.profile"]
        XCTAssertTrue(profile.waitForExistence(timeout: 10))
        profile.tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 5))
    }

    private func openLogin() {
        openSettings()
        app.buttons["settings.login"].tap()
        XCTAssertTrue(app.buttons["login.submit"].waitForExistence(timeout: 5))
    }

    private func fillLogin(username: String, password: String) {
        let user = app.textFields["login.username"]
        user.tap()
        user.typeText(username)
        let pass = app.secureTextFields["login.password"]
        pass.tap()
        pass.typeText(password)
    }

    @MainActor
    func testProfileButtonOpensSettingsWithLoginOfferedWhileLoggedOut() {
        launch()

        openSettings()

        XCTAssertTrue(app.buttons["settings.login"].exists)
        XCTAssertFalse(app.buttons["settings.logout"].exists)
        XCTAssertFalse(app.otherElements["settings.user"].exists)
    }

    @MainActor
    func testSubmittingAnEmptyFormShowsBothFieldErrors() {
        launch()
        openLogin()

        app.buttons["login.submit"].tap()

        XCTAssertTrue(app.staticTexts["Username is required"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Password is required"].exists)
        XCTAssertFalse(app.buttons["login.submit"].isEnabled, "stays disabled until a field is edited")
    }

    @MainActor
    func testAShortPasswordIsRejectedBeforeAnyRequest() {
        launch()
        openLogin()
        fillLogin(username: "tim", password: "1234567")

        app.buttons["login.submit"].tap()

        XCTAssertTrue(app.staticTexts["Password must be at least 8 characters"].waitForExistence(timeout: 5))
    }

    @MainActor
    func testRejectedCredentialsShowAnAlertAndKeepTheForm() {
        launch()
        openLogin()
        fillLogin(username: "intruder", password: "12345678")

        app.buttons["login.submit"].tap()

        XCTAssertTrue(app.alerts["Error"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.alerts["Error"].staticTexts["Wrong username or password"].exists)
        app.alerts["Error"].buttons["Accept"].tap()
        XCTAssertTrue(app.buttons["login.submit"].exists, "the form is still there to retry")
    }

    @MainActor
    func testLoggingInShowsTheAccountInSettingsAndLoggingOutReturnsToLoggedOut() {
        launch()
        openLogin()
        fillLogin(username: "tim", password: "12345678")

        app.buttons["login.submit"].tap()

        // success pops the login screen back to Settings, now with the account
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["settings.logout"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["settings.login"].exists)

        app.buttons["settings.logout"].tap()

        XCTAssertTrue(app.buttons["settings.login"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["settings.logout"].exists)
    }

    @MainActor
    func testChoosingADarkThemeKeepsTheSelection() {
        launch()
        openSettings()

        app.buttons["Dark"].tap()

        XCTAssertTrue(app.buttons["Dark"].isSelected)
        XCTAssertFalse(app.buttons["Light"].isSelected)
    }

    @MainActor
    func testOnboardingIsNotShownByDefault() {
        launch()

        XCTAssertTrue(app.navigationBars["Movies"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.buttons["onboarding.next"].exists)
    }

    @MainActor
    func testOnboardingWalksThroughEveryStepThenDisappearsForGood() {
        launch(["-showOnboarding"])
        let next = app.buttons["onboarding.next"]
        XCTAssertTrue(next.waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["Explore Current and Upcoming Movies"].exists)

        next.tap()
        XCTAssertTrue(app.staticTexts["Search Movies by Title"].waitForExistence(timeout: 5))
        next.tap()
        next.tap()
        XCTAssertTrue(app.staticTexts["Learn About Actors and Their Films"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["Get started"].exists)

        next.tap()

        XCTAssertTrue(app.navigationBars["Movies"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.buttons["onboarding.next"].exists)
    }

    @MainActor
    func testSkippingOnboardingGoesStraightToMovies() {
        launch(["-showOnboarding"])

        let skip = app.buttons["onboarding.skip"]
        XCTAssertTrue(skip.waitForExistence(timeout: 10))
        skip.tap()

        XCTAssertTrue(app.navigationBars["Movies"].waitForExistence(timeout: 10))
    }
}
