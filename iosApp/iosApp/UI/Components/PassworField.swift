//
//  PassworField.swift
//  CornTime
//
//  Created by Enmanuel on 6/7/26.
//
import SwiftUI

struct PassworField: View {
    let text: Binding<String>
    let label: String
    let leadingImage: String?
    var error: String?
    @State private var showError = true

    var body: some View {
        VStack(alignment: .leading) {
            HStack {
                if let image = leadingImage {
                    LeadingIcon(image: image)
                }
                SecureField(label, text: text)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            }
            .textFieldStyle(leadingImage: leadingImage != nil)
            .animation(nil, value: showError)

            if showError {
                Text(error ?? "").foregroundColor(.red)
                    .transition(.scale)
                    .animation(.default, value: error)
            }

        }
        .onChange(of: error) { _, newValue in
            withAnimation {
                showError = newValue != nil
            }
        }
    }
}
