//
//  FilledButton.swift
//  CornTime
//
//  Created by Enmanuel on 6/7/26.
//
import SwiftUI

struct FilledButton: View {
    let label: String
    var leadingImage: String? = nil
    let onClick: () -> Void

    var body: some View {
        Button(
            action: onClick,
            label: {
                HStack(spacing: AppDimensions.medium) {
                    if let image = leadingImage {
                        LeadingIcon(image: image)
                    }
                    Text(label)
                        .font(.body)
                }
                .padding(AppDimensions.buttonPadding)
                .frame(maxWidth: .infinity)
            }
        )
        .buttonStyle(.borderedProminent)
        .buttonBorderShape(.capsule)
    }
}
