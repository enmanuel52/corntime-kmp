//
//  SectionLabel.swift
//  CornTime
//
//  Created by Enmanuel on 6/7/26.
//
import SwiftUI

struct SectionLabel: View {
    let label: String
    let color: Color = .gray

    var body: some View {
        Text(label)
            .font(.headline)
            .fontWeight(.semibold)
            .foregroundStyle(color)
            .padding()
    }
}

#Preview("label") {
    SectionLabel(label: "This is a label")
}
