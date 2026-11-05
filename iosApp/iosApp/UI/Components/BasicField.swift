//
//  BasicField.swift
//  CornTime
//
//  Created by Enmanuel on 6/7/26.
//
import SwiftUI

struct BasicField: View {
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
                TextField(label, text: text)
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


internal struct LeadingIcon: View {
    let image: String
    var body: some View {
        Image(systemName: image).resizable()
            .aspectRatio(contentMode: .fit)
            .frame(height: 18)
            .foregroundColor(.secondary)
    }
}

private let textFieldCornerRadius = 12.0

extension View {
    /**
     @param [leadingImage] for the padding
     */
    func textFieldStyle(leadingImage: Bool) -> some View {
        padding(.vertical, AppDimensions.textField)
            .padding(
                .horizontal,
                leadingImage
                    ? AppDimensions.textField : AppDimensions.textFieldHPadding
            )
            .background(.textFieldBackground)
            .clipShape(RoundedRectangle(cornerRadius: textFieldCornerRadius))
    }
}

#Preview {
    @Previewable @State var text = "hi there"

    Form {
        BasicField(
            text: $text,
            label: "this is a label",
            leadingImage: nil,
            error: nil
        ).frame(maxWidth: .infinity)
            .padding(12)
        BasicField(
            text: $text,
            label: "this is a label",
            leadingImage: "envelope.fill",
            error: "hi there"
        ).frame(maxWidth: .infinity)
            .padding(12)
    }
}
