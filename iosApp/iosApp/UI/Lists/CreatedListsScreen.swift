import Shared
import SwiftUI

struct CreatedListsScreen: View {
    @State private var model = ScreenModels.createdWatchLists()
    @State private var listToDelete: WatchList?
    let navToListDetails: (Int32, String) -> Void

    private func send(_ action: CreatedWatchListsAction) {
        model.viewModel.onAction(action: action)
    }

    private var isFormVisible: Binding<Bool> {
        Binding(
            get: { model.state.form.isVisible },
            set: { if $0 != model.state.form.isVisible { send(CreatedWatchListsActionOnToggleForm.shared) } }
        )
    }

    var body: some View {
        let state = model.state
        let paging = state.paging

        VStack {
            if paging.isLoadingFirstPage || state.isWorking {
                ProgressView().accessibilityIdentifier("lists.loading")
            } else if paging.isEmpty {
                Text("You have not created any list yet").accessibilityIdentifier("lists.empty")
            }

            if let error = paging.uiError, state.lists.isEmpty {
                VStack(spacing: AppDimensions.medium) {
                    Text(error.message)
                    Button("Retry") { send(CreatedWatchListsActionOnLoadNextPage.shared) }
                        .buttonStyle(.bordered)
                }
            }

            if !state.lists.isEmpty {
                List {
                    ForEach(state.lists, id: \.id) { list in
                        Button { send(CreatedWatchListsActionOnListClick(list: list)) } label: {
                            VStack(alignment: .leading, spacing: AppDimensions.small) {
                                Text(list.name).font(.headline)
                                Text(list.description_).font(.subheadline).foregroundColor(.secondary).lineLimit(2)
                            }
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("list.row.\(list.name)")
                        .swipeActions(edge: .trailing) {
                            Button(role: .destructive) { listToDelete = list } label: {
                                Label("Delete", systemImage: "trash")
                            }
                            .accessibilityIdentifier("list.delete.\(list.name)")
                        }
                        .onAppear {
                            if list.id == state.lists.last?.id { send(CreatedWatchListsActionOnLoadNextPage.shared) }
                        }
                    }
                }
                .listStyle(.plain)
                .scrollBounceBehavior(.basedOnSize)
                .accessibilityIdentifier("lists.list")
            }
        }
        .navigationTitle("My lists")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { send(CreatedWatchListsActionOnToggleForm.shared) } label: { Image(systemName: "plus") }
                    .accessibilityLabel("New list")
                    .accessibilityIdentifier("lists.add")
            }
        }
        .sheet(isPresented: isFormVisible) {
            CreateListForm(
                form: model.state.form,
                onName: { send(CreatedWatchListsActionOnNameChange(value: $0)) },
                onDescription: { send(CreatedWatchListsActionOnDescriptionChange(value: $0)) },
                onSubmit: { send(CreatedWatchListsActionOnSubmitForm.shared) },
                onCancel: { send(CreatedWatchListsActionOnToggleForm.shared) }
            )
        }
        .confirmationDialog(
            "Delete this list?",
            isPresented: Binding(get: { listToDelete != nil }, set: { if !$0 { listToDelete = nil } }),
            titleVisibility: .visible
        ) {
            Button("Delete", role: .destructive) {
                if let list = listToDelete { send(CreatedWatchListsActionOnDeleteList(listId: list.id)) }
                listToDelete = nil
            }
        }
        .errorAlert(state.error, onDismiss: { send(CreatedWatchListsActionOnDismissError.shared) })
        .onAppear { send(CreatedWatchListsActionOnScreenShown.shared) }
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .navigateToDetails(let details): navToListDetails(details.listId, details.name)
                case .listDeleted: break // Android removes the list's shortcut here
                }
            }
        }
    }
}

private struct CreateListForm: View {
    let form: Shared.CreateListForm
    let onName: (String) -> Void
    let onDescription: (String) -> Void
    let onSubmit: () -> Void
    let onCancel: () -> Void

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    BasicField(
                        text: Binding(get: { form.name }, set: onName),
                        label: String(localized: "List name"),
                        leadingImage: "textformat",
                        error: form.nameError?.message
                    )
                    .accessibilityIdentifier("list.form.name")

                    BasicField(
                        text: Binding(get: { form.description_ }, set: onDescription),
                        label: String(localized: "List description"),
                        leadingImage: "text.alignleft",
                        error: form.descriptionError?.message
                    )
                    .accessibilityIdentifier("list.form.description")
                }
            }
            .navigationTitle("New list")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel", action: onCancel) }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Create", action: onSubmit).accessibilityIdentifier("list.form.submit")
                }
            }
        }
        .presentationDetents([.medium])
    }
}
