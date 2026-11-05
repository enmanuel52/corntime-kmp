//
//  ScrollTransition.swift
//  CornTime
//
//  Created by Enmanuel on 6/7/26.
//

import SwiftUI

extension View {

    func intagramStory() -> some View {

        scrollTransition { content, phase in
            content
                .rotation3DEffect(
                    .degrees(phase.computeDegrees()),
                    axis: (x: 0, y: 1, z: 0),
                    anchor: UnitPoint(x: phase.computeXAnchor(), y: 0.5)
                )
        }
    }

    func hStackPlacing() -> some View {
        scrollTransition { content, phase in
            content
                .rotation3DEffect(
                    .degrees(phase.isBottomTrailing ? 30 : 0),
                    axis: (x: 0, y: 1, z: 0),
                    anchor: .leading
                )
                .scaleEffect(
                    x: phase.isBottomTrailing ? 0.8 : 1,
                    y: phase.isBottomTrailing ? 0.8 : 1
                )
        }
    }

    func vStackPlacing() -> some View {
        scrollTransition { content, phase in
            content
                .rotation3DEffect(
                    .degrees(phase.isBottomTrailing ? 30 : 0),
                    axis: (x: 1, y: 0, z: 0),
                    anchor: .leading
                )
                .scaleEffect(
                    x: phase.isBottomTrailing ? 0.8 : 1,
                    y: phase.isBottomTrailing ? 0.8 : 1
                )
        }
    }

    func vRollEffect() -> some View {
        scrollTransition { content, phase in
            content
                .rotation3DEffect(
                    .degrees(phase.rollDegrees),
                    axis: (x: 1, y: 0, z: 0),
                    anchor: .leading
                )
                .scaleEffect(
                    x: phase.isBottomTrailing ? 0.8 : 1,
                    y: phase.isBottomTrailing ? 0.8 : 1
                )
        }
    }
}

extension ScrollTransitionPhase {

    nonisolated var isBottomTrailing: Bool {
        switch self {
        case .bottomTrailing:
            true
        default:
            false
        }
    }

    fileprivate nonisolated func computeDegrees(ceiling: Double = 45) -> Double
    {
        return switch self {
        case .topLeading:
            -ceiling
        case .identity:
            0
        case .bottomTrailing:
            ceiling
        }
    }

    fileprivate nonisolated func computeXAnchor() -> Double {
        return switch self {
        case .topLeading:
            1
        case .identity:
            0.5
        case .bottomTrailing:
            0
        }
    }

    nonisolated var rollDegrees: Double {
        switch self {
        case .topLeading:
            -30
        case .identity:
            0
        case .bottomTrailing:
            30
        }
    }
}
