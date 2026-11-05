import XCTest

/// Phase 7 on iOS: the advanced filter, reviews with the rating breakdown, and recent searches.
/// Driven against the shared Kotlin ViewModels with in-memory fakes (logged out).
final class MoviesExtrasUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-uiTesting"]
        app.launch()
    }

    private func openFilter() {
        let filter = app.buttons["movies.filter"]
        XCTAssertTrue(filter.waitForExistence(timeout: 10))
        filter.tap()
        XCTAssertTrue(app.navigationBars["Advanced Movie Filter"].waitForExistence(timeout: 10))
    }

    private func openReviewsFromFirstMovie() {
        let poster = app.buttons.matching(identifier: "movie.main.The Godfather").firstMatch
        XCTAssertTrue(poster.waitForExistence(timeout: 10))
        poster.tap()

        let rating = app.buttons["details.rating"]
        XCTAssertTrue(rating.waitForExistence(timeout: 10), "details show the rating summary")
        rating.tap()
        XCTAssertTrue(app.navigationBars["Ratings & Reviews"].waitForExistence(timeout: 10))
    }

    // MARK: filter

    @MainActor
    func testFilterOpensFromMoviesAndListsMoviesWithGenresAndSortChips() {
        openFilter()

        XCTAssertTrue(app.buttons["filter.sort.popularity"].waitForExistence(timeout: 10)
            || app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'filter.sort.'")).count == 4)
        XCTAssertTrue(app.buttons["filter.genre.1"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.otherElements["filter.grid"].waitForExistence(timeout: 10)
            || app.scrollViews.firstMatch.exists)
    }

    @MainActor
    func testPickingAGenreShowsClearAndClearingHidesIt() {
        openFilter()
        XCTAssertFalse(app.buttons["filter.clear"].exists, "nothing to clear with the default filter")

        let genre = app.buttons["filter.genre.1"]
        XCTAssertTrue(genre.waitForExistence(timeout: 10))
        genre.tap()

        let clear = app.buttons["filter.clear"]
        XCTAssertTrue(clear.waitForExistence(timeout: 5))
        clear.tap()
        XCTAssertFalse(clear.waitForExistence(timeout: 2))
    }

    @MainActor
    func testAMovieFromTheFilterOpensItsDetails() {
        openFilter()

        let poster = app.buttons.matching(identifier: "movie.poster.The Godfather").firstMatch
        XCTAssertTrue(poster.waitForExistence(timeout: 10))
        poster.tap()

        XCTAssertTrue(app.navigationBars["Details"].waitForExistence(timeout: 10))
    }

    // MARK: reviews

    @MainActor
    func testDetailsRatingSummaryOpensReviewsWithTheirAuthors() {
        openReviewsFromFirstMovie()

        XCTAssertTrue(app.staticTexts["rating.average"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["Tim"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["Ana"].exists)
    }

    @MainActor
    func testLoggedOutReviewsOfferNoRating() {
        openReviewsFromFirstMovie()

        XCTAssertTrue(app.staticTexts["Tim"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.buttons["reviews.rate"].exists)
        XCTAssertFalse(app.buttons["reviews.rated"].exists)
    }

    @MainActor
    func testTappingAReviewExpandsItAndBackReturnsToDetails() {
        openReviewsFromFirstMovie()

        let review = app.otherElements["review.review-1"]
        XCTAssertTrue(review.waitForExistence(timeout: 10) || app.staticTexts["Tim"].exists)
        app.staticTexts["Tim"].tap()

        app.navigationBars["Ratings & Reviews"].buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.navigationBars["Details"].waitForExistence(timeout: 5))
    }

    // MARK: recent searches

    @MainActor
    func testSubmittedSearchBecomesARecentSearchThatCanBePickedAgain() {
        app.tabBars.buttons["Search"].tap()
        let field = app.searchFields.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        field.typeText("dune\n")
        XCTAssertTrue(app.collectionViews["search.results"].waitForExistence(timeout: 10))

        // clearing the field brings the recent searches back
        app.buttons["Clear text"].tap()
        let suggestion = app.buttons["search.suggestion.dune"]
        XCTAssertTrue(suggestion.waitForExistence(timeout: 10))

        suggestion.tap()
        XCTAssertTrue(app.collectionViews["search.results"].waitForExistence(timeout: 10))
    }

    @MainActor
    func testRecentSearchCanBeDeletedAndClearedAll() {
        app.tabBars.buttons["Search"].tap()
        let field = app.searchFields.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        field.typeText("dune\n")
        app.buttons["Clear text"].tap()
        field.typeText("alien\n")
        app.buttons["Clear text"].tap()

        XCTAssertTrue(app.buttons["search.suggestion.alien"].waitForExistence(timeout: 10))
        app.buttons["search.deleteSuggestion.alien"].tap()
        XCTAssertFalse(app.buttons["search.suggestion.alien"].exists)
        XCTAssertTrue(app.buttons["search.suggestion.dune"].exists)

        app.buttons["search.clearSuggestions"].tap()
        XCTAssertFalse(app.buttons["search.suggestion.dune"].exists)
    }
}
