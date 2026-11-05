//
//  RateView.swift
//  CornTime
//
//  Created by Enmanuel on 6/1/26.
//

import Foundation
import SwiftUI

struct RateView: View {
    let stars: CGFloat
    var spacing: CGFloat = 4

    var body: some View {
        ZStack {
            HStack(spacing: spacing) {
                ForEach(1...5, id: \.self) { i in
                    Image(systemName: "star.fill")
                        .starModifier(.gray)
                }
            }
            HStack(spacing: spacing) {
                ForEach(1...5, id: \.self) { i in
                    if Double(i) < stars {
                        Image(systemName: "star.fill")
                            .starModifier(.yellow)
                    } else {
                        let itemPercent = 1.0 - (Double(i) - stars)
                        GeometryReader { geometry in
                            Image(systemName: "star.fill")
                                .unframedStarModifier(.yellow)
                                .clipShape(
                                    Rectangle().size(
                                        width: geometry.size.width
                                            * max(itemPercent, 0.0),  // coerceAtLeast
                                        height: geometry.size.height
                                    )
                                )
                        }.aspectRatio(1.0, contentMode: .fit)
                    }
                }
            }
        }
    }
}

extension Image {
    fileprivate func starModifier(_ color: Color = .yellow) -> some View {
        resizable()
            .foregroundColor(color)
            .frame(maxWidth: .infinity)
            .scaledToFit()
    }

    fileprivate func unframedStarModifier(_ color: Color = .yellow) -> some View
    {
        resizable()
            .foregroundColor(color)
            .scaledToFit()
    }
}

#Preview {
    RateView(stars: 3.6)
        .frame(width: 250)
}
