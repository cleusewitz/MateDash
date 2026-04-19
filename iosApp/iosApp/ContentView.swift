import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let vc = MainViewControllerKt.MainViewController()
        vc.view.backgroundColor = UIColor(red: 0.043, green: 0.043, blue: 0.043, alpha: 1) // 0xFF0B0B0B
        return vc
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var isReady = false

    var body: some View {
        ZStack {
            Color(red: 0.043, green: 0.043, blue: 0.043)
                .ignoresSafeArea()

            if isReady {
                ComposeView()
                    .ignoresSafeArea()
            } else {
                VStack(spacing: 12) {
                    Text("MateDash")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.white)
                    ProgressView()
                        .tint(.white)
                }
            }
        }
        .onAppear {
            DispatchQueue.main.async {
                isReady = true
            }
        }
    }
}
