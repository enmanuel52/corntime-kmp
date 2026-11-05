//
//  Lottie.swift
//  CornTime
//
//  Created by Enmanuel on 6/5/26.
//

import Lottie
import SwiftUI

struct Lottie: View {
    var body: some View {
        LottieView {
            try await .dotLottieFile(.named("Entertainment"))
        }
        .playbackMode(.playing(.toProgress(0.5, loopMode: .playOnce)))
    }
}

#Preview {
    Lottie()
        .scaledToFit()
        .frame(maxHeight: 90)
}
