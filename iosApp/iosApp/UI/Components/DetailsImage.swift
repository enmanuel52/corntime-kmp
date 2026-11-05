//
//  DetailsImage.swift
//  CornTime
//
//  Created by Enmanuel on 6/7/26.
//
import SwiftUI

 struct DetailsImage: View {
    let url: String

    var body: some View {
        AsyncImage(url: URL(string: BASE_BACKDROP_IMAGE_URL + url)) { image in
            image.resizable()
                .frame(maxWidth: .infinity)
                .scaledToFit()
        } placeholder: {
            Image("movieBackdrop")
                .frame(maxWidth: .infinity)
                .scaledToFit()
        }
    }
}
