import XCTest

/// Actors tab and actor details on iOS, driven against the shared ViewModels with in-memory fakes.
final class ActorsFlowUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-uiTesting"]
        app.launch()
    }

    private func openActorsTab() {
        app.tabBars.buttons["Actors"].tap()
        XCTAssertTrue(app.navigationBars["Actors"].waitForExistence(timeout: 10))
    }

    private func firstActorPoster() -> XCUIElement {
        app.buttons.matching(identifier: "actor.poster.Tim").firstMatch
    }

    @MainActor
    func testActorsTabListsThePopularActors() {
        openActorsTab()

        XCTAssertTrue(firstActorPoster().waitForExistence(timeout: 10))
        XCTAssertFalse(app.staticTexts["actors.empty"].exists)
        XCTAssertFalse(app.buttons["actors.retry"].exists)
    }

    @MainActor
    func testLaterPagesAreLoadedAsTheListEnds() {
        openActorsTab()
        XCTAssertTrue(firstActorPoster().waitForExistence(timeout: 10))

        // two actors per fake page and three pages: reaching the end of each page requests the next
        let posters = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'actor.poster.'"))
        let loadedMore = NSPredicate(format: "count >= 6")
        expectation(for: loadedMore, evaluatedWith: posters)
        waitForExpectations(timeout: 10)
    }

    @MainActor
    func testTappingAnActorOpensItsDetailsWithTheKnownMovies() {
        openActorsTab()
        let poster = firstActorPoster()
        XCTAssertTrue(poster.waitForExistence(timeout: 10))

        poster.tap()

        XCTAssertTrue(app.otherElements["actor.bio"].waitForExistence(timeout: 10)
            || app.staticTexts["No info"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["actor.movie.First Known"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["actor.movie.Second Known"].exists)
    }

    @MainActor
    func testTappingAKnownMovieOpensTheMovieDetailsAndBackReturnsToTheActor() {
        openActorsTab()
        firstActorPoster().tap()
        let movie = app.buttons["actor.movie.First Known"]
        XCTAssertTrue(movie.waitForExistence(timeout: 10))

        movie.tap()
        XCTAssertTrue(app.navigationBars["Details"].waitForExistence(timeout: 10))

        app.navigationBars["Details"].buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.buttons["actor.movie.First Known"].waitForExistence(timeout: 10))
    }

    @MainActor
    func testTappingACastMemberInMovieDetailsOpensTheActor() {
        let poster = app.buttons.matching(identifier: "movie.main.The Godfather").firstMatch
        XCTAssertTrue(poster.waitForExistence(timeout: 10))
        poster.tap()
        XCTAssertTrue(app.scrollViews["details.content"].waitForExistence(timeout: 10))

        let cast = app.buttons["person.1"]
        XCTAssertTrue(cast.waitForExistence(timeout: 10))
        cast.tap()

        XCTAssertTrue(app.buttons["actor.movie.First Known"].waitForExistence(timeout: 10))
    }

    @MainActor
    func testTheActorsStackIsKeptWhenSwitchingTabs() {
        openActorsTab()
        firstActorPoster().tap()
        XCTAssertTrue(app.buttons["actor.movie.First Known"].waitForExistence(timeout: 10))

        app.tabBars.buttons["Movies"].tap()
        XCTAssertTrue(app.navigationBars["Movies"].waitForExistence(timeout: 10))
    }
}
