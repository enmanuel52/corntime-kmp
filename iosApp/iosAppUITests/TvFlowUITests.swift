import XCTest

/// TV shows slice on iOS, driven end to end against the shared Kotlin ViewModels with the in-memory fake
/// TV data source. Written to follow the same robot style as the other flows; the page objects below hide
/// identifiers and waits so the tests read as user stories.
final class TvFlowUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-uiTesting"]
        app.launch()
    }

    // MARK: - Robots

    private var tvTab: XCUIElement { app.tabBars.buttons["TV Shows"] }

    private func openTvTab() {
        tvTab.tap()
        XCTAssertTrue(app.navigationBars["TV Shows"].waitForExistence(timeout: 10))
    }

    private func tvPoster(_ name: String = "Breaking Bad") -> XCUIElement {
        app.buttons.matching(identifier: "tv.poster.\(name)").firstMatch
    }

    private func openFirstTvShowDetails() {
        let poster = tvPoster()
        XCTAssertTrue(poster.waitForExistence(timeout: 10), "the TV home should list the fake TV shows")
        poster.tap()
        XCTAssertTrue(app.scrollViews["tvdetails.content"].waitForExistence(timeout: 10))
    }

    private func tvRow(prefix: String) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", prefix)).firstMatch
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

    private func openWatchlistTvSegment() {
        app.tabBars.buttons["Lists"].tap()
        XCTAssertTrue(app.navigationBars["Watchlist"].waitForExistence(timeout: 10))
        let picker = app.segmentedControls["watchlist.kind"]
        XCTAssertTrue(picker.waitForExistence(timeout: 5))
        picker.buttons["TV Shows"].tap()
    }

    // MARK: - Home and sections

    @MainActor
    func testTvTabShowsEverySectionAfterLoading() {
        openTvTab()

        XCTAssertTrue(app.staticTexts["Popular"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["seeall.Popular"].exists)
        XCTAssertTrue(app.buttons["seeall.Top Rated"].exists)
        XCTAssertTrue(app.buttons["seeall.On The Air"].exists)
        XCTAssertTrue(app.buttons["seeall.Airing Today"].exists)
    }

    @MainActor
    func testFavoritesButtonIsHiddenWhileLoggedOut() {
        openTvTab()

        XCTAssertTrue(tvPoster().waitForExistence(timeout: 10))
        XCTAssertFalse(app.buttons["tv.favorites"].exists)
    }

    @MainActor
    func testSeeAllOpensThePagedSectionGridAndATvShowFromIt() {
        openTvTab()
        let seeAll = app.buttons["seeall.Popular"]
        XCTAssertTrue(seeAll.waitForExistence(timeout: 10))
        seeAll.tap()

        XCTAssertTrue(app.navigationBars["Popular"].waitForExistence(timeout: 10))
        let poster = tvPoster()
        XCTAssertTrue(poster.waitForExistence(timeout: 10))
        poster.tap()

        XCTAssertTrue(app.navigationBars["Details"].waitForExistence(timeout: 10))
    }

    // MARK: - Details, seasons, episodes

    @MainActor
    func testTappingATvShowOpensItsDetailsWithoutAccountActionsWhileLoggedOut() {
        openTvTab()
        openFirstTvShowDetails()

        XCTAssertTrue(app.navigationBars["Details"].exists)
        XCTAssertTrue(app.buttons["season.card.1"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.buttons["tvdetails.favorite"].exists)
        XCTAssertFalse(app.buttons["tvdetails.watchlist"].exists)

        app.navigationBars["Details"].buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.navigationBars["TV Shows"].waitForExistence(timeout: 5))
    }

    @MainActor
    func testTvShowToSeasonsToEpisodesToEpisodeDetails() {
        openTvTab()
        openFirstTvShowDetails()

        app.buttons["seeall.Seasons"].tap()
        XCTAssertTrue(app.navigationBars["Seasons"].waitForExistence(timeout: 10))
        let season = app.buttons["season.row.2"]
        XCTAssertTrue(season.waitForExistence(timeout: 10))
        season.tap()

        XCTAssertTrue(app.collectionViews["episodes.list"].waitForExistence(timeout: 10))
        let episode = app.buttons["episode.row.3"]
        XCTAssertTrue(episode.waitForExistence(timeout: 10))
        episode.tap()

        XCTAssertTrue(app.navigationBars["Episode details"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["episodedetails.name"].waitForExistence(timeout: 10))
    }

    @MainActor
    func testASeasonCardInTheDetailsOpensItsEpisodes() {
        openTvTab()
        openFirstTvShowDetails()

        let card = app.buttons["season.card.1"]
        XCTAssertTrue(card.waitForExistence(timeout: 10))
        card.tap()

        XCTAssertTrue(app.collectionViews["episodes.list"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["episode.row.1"].exists)
    }

    @MainActor
    func testTheSeasonToggleOpensTheOverviewInsteadOfTheEpisodes() {
        openTvTab()
        openFirstTvShowDetails()
        app.buttons["seeall.Seasons"].tap()
        let toggle = app.buttons["season.toggle.1"]
        XCTAssertTrue(toggle.waitForExistence(timeout: 10))

        toggle.tap()

        XCTAssertTrue(app.navigationBars["Seasons"].exists, "toggling the overview must not navigate")
        XCTAssertFalse(app.collectionViews["episodes.list"].exists)
    }

    @MainActor
    func testAGuestStarInEpisodeDetailsOpensTheActor() {
        openTvTab()
        openFirstTvShowDetails()
        app.buttons["season.card.1"].tap()
        let episode = app.buttons["episode.row.1"]
        XCTAssertTrue(episode.waitForExistence(timeout: 10))
        episode.tap()
        XCTAssertTrue(app.scrollViews["episodedetails.content"].waitForExistence(timeout: 10))

        let guest = app.buttons.matching(identifier: "person.1").firstMatch
        XCTAssertTrue(guest.waitForExistence(timeout: 10))
        guest.tap()

        XCTAssertTrue(app.buttons["actor.movie.First Known"].waitForExistence(timeout: 10))
    }

    @MainActor
    func testACastMemberInTvShowDetailsOpensTheActor() {
        openTvTab()
        openFirstTvShowDetails()

        let cast = app.buttons.matching(identifier: "person.1").firstMatch
        XCTAssertTrue(cast.waitForExistence(timeout: 10))
        cast.tap()

        XCTAssertTrue(app.buttons["actor.movie.First Known"].waitForExistence(timeout: 10))
    }

    @MainActor
    func testTheTvTabKeepsItsOwnNavigationStack() {
        openTvTab()
        openFirstTvShowDetails()

        app.tabBars.buttons["Movies"].tap()
        XCTAssertTrue(app.navigationBars["Movies"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.scrollViews["tvdetails.content"].exists)
    }

    // MARK: - Search scope

    @MainActor
    func testSearchScopeSwitchesBetweenMoviesAndTvShows() {
        app.tabBars.buttons["Search"].tap()
        let field = app.searchFields.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        field.typeText("wire")

        let scope = app.segmentedControls.firstMatch
        XCTAssertTrue(scope.waitForExistence(timeout: 10), "the scope bar offers Movies | TV Shows")
        scope.buttons["TV Shows"].tap()

        let results = app.collectionViews["tvsearch.results"]
        XCTAssertTrue(results.waitForExistence(timeout: 10), "TV results appear after the debounce")
        XCTAssertFalse(app.collectionViews["search.results"].exists)

        scope.buttons["Movies"].tap()
        XCTAssertTrue(app.collectionViews["search.results"].waitForExistence(timeout: 10))
    }

    @MainActor
    func testSearchingTvShowsOpensTheirDetails() {
        app.tabBars.buttons["Search"].tap()
        let field = app.searchFields.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        app.segmentedControls.firstMatch.buttons["TV Shows"].tap()
        field.typeText("breaking")

        let results = app.collectionViews["tvsearch.results"]
        XCTAssertTrue(results.waitForExistence(timeout: 10))
        results.buttons.firstMatch.tap()

        XCTAssertTrue(app.scrollViews["tvdetails.content"].waitForExistence(timeout: 10))
    }

    // MARK: - Favorites and watchlist (logged in)

    @MainActor
    func testFavoriteTvShowsOpenFromTheTvToolbarAndListTheShows() {
        logIn()
        openTvTab()

        app.buttons["tv.favorites"].tap()

        XCTAssertTrue(app.navigationBars["Favorite TV shows"].waitForExistence(timeout: 10))
        XCTAssertTrue(tvRow(prefix: "tv.row.").waitForExistence(timeout: 10))
    }

    @MainActor
    func testRemovingAFavoriteTvShowShowsTheUndoBannerAndUndoBringsItBack() {
        logIn()
        openTvTab()
        app.buttons["tv.favorites"].tap()
        let row = tvRow(prefix: "tv.row.")
        XCTAssertTrue(row.waitForExistence(timeout: 10))
        let identifier = row.identifier

        row.swipeLeft()
        tvRow(prefix: "tv.remove.").tap()

        XCTAssertTrue(app.staticTexts["TV show removed from favorites"].waitForExistence(timeout: 5))
        app.buttons["undo.button"].tap()

        XCTAssertTrue(app.buttons[identifier].waitForExistence(timeout: 5))
    }

    @MainActor
    func testWatchlistHasATvSegmentWithTheAccountsTvShows() {
        logIn()
        openWatchlistTvSegment()

        XCTAssertTrue(tvRow(prefix: "tv.row.").waitForExistence(timeout: 10))
        XCTAssertFalse(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'movie.row.'")).firstMatch.exists)
    }

    @MainActor
    func testSwipingATvWatchlistRowRemovesItWithUndo() {
        logIn()
        openWatchlistTvSegment()
        let row = tvRow(prefix: "tv.row.")
        XCTAssertTrue(row.waitForExistence(timeout: 10))

        row.swipeLeft()
        tvRow(prefix: "tv.remove.").tap()

        XCTAssertTrue(app.staticTexts["TV show removed from watchlist"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["undo.button"].exists)
    }

    @MainActor
    func testSwipingRightMovesATvShowToFavorites() {
        logIn()
        openWatchlistTvSegment()
        let row = tvRow(prefix: "tv.row.")
        XCTAssertTrue(row.waitForExistence(timeout: 10))

        row.swipeRight()
        tvRow(prefix: "tv.favorite.").tap()

        XCTAssertTrue(app.staticTexts["TV show added to favorites"].waitForExistence(timeout: 5))
    }

    @MainActor
    func testTvDetailsOffersFavoriteAndWatchlistWhileLoggedIn() {
        logIn()
        openTvTab()
        openFirstTvShowDetails()

        XCTAssertTrue(app.buttons["tvdetails.favorite"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["tvdetails.watchlist"].exists)
    }
}
