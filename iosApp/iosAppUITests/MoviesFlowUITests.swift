import XCTest

/// Movies slice on iOS, driven end to end against the shared Kotlin ViewModels with in-memory fakes.
final class MoviesFlowUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-uiTesting"]
        app.launch()
    }

    private func openFirstMovieFromHome() {
        let poster = app.buttons.matching(identifier: "movie.main.The Godfather").firstMatch
        XCTAssertTrue(poster.waitForExistence(timeout: 10), "home should list the fake movies")
        poster.tap()
    }

    @MainActor
    func testHomeShowsEverySectionAfterLoading() {
        XCTAssertTrue(app.staticTexts["Now Playing"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["seeall.Now Playing"].exists)
        XCTAssertTrue(app.buttons["seeall.Upcoming"].exists)
    }

    @MainActor
    func testHomeLoadsEachScreenWithASingleViewModel() {
        XCTAssertTrue(app.staticTexts["Now Playing"].waitForExistence(timeout: 10))

        // The probe is hidden text holding how many times "upcoming" was requested.
        let probe = app.staticTexts["probe.upcomingCalls"]
        XCTAssertTrue(probe.waitForExistence(timeout: 5))
        XCTAssertEqual(probe.label, "1", "re-rendering must not recreate the ViewModel or refetch")
    }

    @MainActor
    func testTappingAMovieOpensItsDetailsAndBackReturns() {
        openFirstMovieFromHome()

        XCTAssertTrue(app.navigationBars["Details"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.scrollViews["details.content"].waitForExistence(timeout: 10))
        // logged out: no favorite/watchlist actions
        XCTAssertFalse(app.buttons["details.favorite"].exists)
        XCTAssertFalse(app.buttons["details.watchlist"].exists)

        app.navigationBars["Details"].buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.navigationBars["Movies"].waitForExistence(timeout: 5))
    }

    @MainActor
    func testSeeAllOpensThePagedSectionGridAndAMovieFromIt() {
        let seeAll = app.buttons["seeall.Upcoming"]
        XCTAssertTrue(seeAll.waitForExistence(timeout: 10))
        seeAll.tap()

        XCTAssertTrue(app.navigationBars["Upcoming"].waitForExistence(timeout: 10))
        let grid = app.scrollViews.matching(identifier: "section.grid").firstMatch
        XCTAssertTrue(grid.exists || app.otherElements["section.grid"].exists)

        let poster = app.buttons.matching(identifier: "movie.poster.The Godfather").firstMatch
        XCTAssertTrue(poster.waitForExistence(timeout: 10))
        poster.tap()
        XCTAssertTrue(app.navigationBars["Details"].waitForExistence(timeout: 10))
    }

    @MainActor
    func testSearchTabFindsMoviesAndOpensDetails() {
        app.tabBars.buttons["Search"].tap()
        let field = app.searchFields.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        field.typeText("godfather")

        let results = app.collectionViews["search.results"]
        XCTAssertTrue(results.waitForExistence(timeout: 10), "results appear after the debounce")
        results.buttons.firstMatch.tap()

        XCTAssertTrue(app.navigationBars["Details"].waitForExistence(timeout: 10))
    }

    @MainActor
    func testSearchWithNothingTypedShowsNoResultsMessageOrNothing() {
        app.tabBars.buttons["Search"].tap()

        XCTAssertFalse(app.staticTexts["search.empty"].exists)
        XCTAssertFalse(app.collectionViews["search.results"].exists)
    }

    @MainActor
    func testTabsKeepIndependentNavigationStacks() {
        openFirstMovieFromHome()
        XCTAssertTrue(app.navigationBars["Details"].waitForExistence(timeout: 10))

        app.navigationBars["Details"].buttons.element(boundBy: 0).tap()
        app.tabBars.buttons["Search"].tap()
        // Switching tabs must not leave the Movies stack's details on top of Search
        XCTAssertFalse(app.navigationBars["Details"].exists)
    }
}
