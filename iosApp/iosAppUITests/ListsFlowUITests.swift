import XCTest

/// Lists, watchlist and favorites on iOS, driven against the shared ViewModels with in-memory fakes.
final class ListsFlowUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-uiTesting"]
        app.launch()
    }

    private func logIn() {
        let profile = app.buttons["movies.profile"]
        XCTAssertTrue(profile.waitForExistence(timeout: 10))
        profile.tap()
        app.buttons["settings.login"].tap()

        let user = app.textFields["login.username"]
        XCTAssertTrue(user.waitForExistence(timeout: 5))
        user.tap()
        user.typeText("tim")
        let pass = app.secureTextFields["login.password"]
        pass.tap()
        pass.typeText("12345678")
        app.buttons["login.submit"].tap()

        XCTAssertTrue(app.buttons["settings.logout"].waitForExistence(timeout: 10))
        app.navigationBars["Settings"].buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.navigationBars["Movies"].waitForExistence(timeout: 5))
    }

    private func openListsTab() {
        app.tabBars.buttons["Lists"].tap()
        XCTAssertTrue(app.navigationBars["Watchlist"].waitForExistence(timeout: 10))
    }

    private func firstMovieRow() -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'movie.row.'")).firstMatch
    }

    @MainActor
    func testListsTabAndFavoritesAreOnlyAvailableWhileLoggedIn() {
        XCTAssertTrue(app.navigationBars["Movies"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.tabBars.buttons["Lists"].exists)
        XCTAssertFalse(app.buttons["movies.favorites"].exists)

        logIn()

        XCTAssertTrue(app.tabBars.buttons["Lists"].exists)
        XCTAssertTrue(app.buttons["movies.favorites"].exists)
    }

    @MainActor
    func testWatchlistShowsTheAccountsMoviesAndOpensTheirDetails() {
        logIn()
        openListsTab()

        let row = firstMovieRow()
        XCTAssertTrue(row.waitForExistence(timeout: 10))
        row.tap()

        XCTAssertTrue(app.navigationBars["Details"].waitForExistence(timeout: 10))
    }

    @MainActor
    func testSwipingToRemoveShowsTheUndoBannerAndUndoBringsTheMovieBack() {
        logIn()
        openListsTab()
        let row = firstMovieRow()
        XCTAssertTrue(row.waitForExistence(timeout: 10))
        let identifier = row.identifier

        row.swipeLeft()
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'movie.remove.'")).firstMatch.tap()

        XCTAssertTrue(app.otherElements["undo.banner"].waitForExistence(timeout: 5)
            || app.staticTexts["Movie removed from watchlist"].waitForExistence(timeout: 5))
        app.buttons["undo.button"].tap()

        XCTAssertTrue(app.buttons[identifier].waitForExistence(timeout: 5))
    }

    @MainActor
    func testSwipingRightMovesAMovieToFavoritesWithItsOwnMessage() {
        logIn()
        openListsTab()
        let row = firstMovieRow()
        XCTAssertTrue(row.waitForExistence(timeout: 10))

        row.swipeRight()
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'movie.favorite.'")).firstMatch.tap()

        XCTAssertTrue(app.staticTexts["Movie added to favorites"].waitForExistence(timeout: 5))
    }

    @MainActor
    func testMyListsOpensFromTheWatchlistAndShowsTheCreatedLists() {
        logIn()
        openListsTab()

        app.buttons["watchlist.lists"].tap()

        XCTAssertTrue(app.navigationBars["My lists"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'list.row.'")).firstMatch
            .waitForExistence(timeout: 10))
    }

    @MainActor
    func testCreatingAListValidatesTheFormThenClosesIt() {
        logIn()
        openListsTab()
        app.buttons["watchlist.lists"].tap()
        XCTAssertTrue(app.navigationBars["My lists"].waitForExistence(timeout: 10))

        app.buttons["lists.add"].tap()
        let submit = app.buttons["list.form.submit"]
        XCTAssertTrue(submit.waitForExistence(timeout: 5))

        submit.tap()
        XCTAssertTrue(app.staticTexts["The list needs a name"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["The list needs a description"].exists)

        let name = app.textFields["list.form.name"]
        name.tap()
        name.typeText("Weekend")
        let description = app.textFields["list.form.description"]
        description.tap()
        description.typeText("Cozy movies")
        submit.tap()

        XCTAssertFalse(submit.waitForExistence(timeout: 5), "the sheet closes once the list is created")
    }

    @MainActor
    func testAListOpensItsMoviesAndBackReturnsToTheLists() {
        logIn()
        openListsTab()
        app.buttons["watchlist.lists"].tap()
        let list = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'list.row.'")).firstMatch
        XCTAssertTrue(list.waitForExistence(timeout: 10))

        list.tap()
        XCTAssertTrue(firstMovieRow().waitForExistence(timeout: 10))

        app.navigationBars.buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.navigationBars["My lists"].waitForExistence(timeout: 5))
    }

    @MainActor
    func testFavoritesOpenFromTheMoviesToolbar() {
        logIn()

        app.buttons["movies.favorites"].tap()

        XCTAssertTrue(app.navigationBars["Favorites"].waitForExistence(timeout: 10))
        XCTAssertTrue(firstMovieRow().waitForExistence(timeout: 10))
    }

    @MainActor
    func testLoggingOutRemovesTheListsTab() {
        logIn()
        XCTAssertTrue(app.tabBars.buttons["Lists"].exists)

        app.buttons["movies.profile"].tap()
        app.buttons["settings.logout"].tap()
        app.navigationBars["Settings"].buttons.element(boundBy: 0).tap()

        XCTAssertTrue(app.navigationBars["Movies"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.tabBars.buttons["Lists"].exists)
    }
}
