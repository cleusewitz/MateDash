import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let vc = MainViewControllerKt.MainViewController()
        // 스플래시 LaunchBackground와 동일한 dark navy (#050508)
        vc.view.backgroundColor = UIColor(red: 5/255.0, green: 5/255.0, blue: 8/255.0, alpha: 1)
        return vc
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var composeStarted = false
    @State private var splashFaded = false

    var body: some View {
        ZStack {
            Color(red: 5/255.0, green: 5/255.0, blue: 8/255.0)
                .ignoresSafeArea()

            // ComposeView를 백그라운드에서 시작시키고, 그 위에 스플래시를 덮어 자연스러운
            // 페이드 아웃. Compose 초기화(5~10초)를 사용자가 체감 못 하게 함.
            if composeStarted {
                ComposeView()
                    .ignoresSafeArea()
                    .opacity(splashFaded ? 1.0 : 0.0)
            }

            if !splashFaded {
                VStack(spacing: 18) {
                    Image("SplashLogo")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 200, height: 200)
                        .cornerRadius(44)
                    Text("MateDash")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.white)
                    Text("Tesla 차량 대시보드")
                        .font(.system(size: 13))
                        .foregroundColor(Color(white: 0.6))
                    ProgressView()
                        .tint(.white)
                        .padding(.top, 12)
                }
                .transition(.opacity)
            }
        }
        .onAppear {
            // 1) Compose VC를 즉시 백그라운드 초기화 시작
            DispatchQueue.main.async {
                composeStarted = true
            }
            // 2) Compose 초기화 시간을 위해 1.8초 후 스플래시 페이드 아웃.
            //    실제 첫 프레임이 그 사이 준비되므로 사용자엔 자연스러운 전환으로 보임.
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
                withAnimation(.easeInOut(duration: 0.4)) {
                    splashFaded = true
                }
            }
        }
    }
}
