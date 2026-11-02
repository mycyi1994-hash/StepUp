import SwiftUI
import StepUpKit

// iOS 앱의 입구 — 화면은 모두 Kotlin(Compose Multiplatform, :sharedUi)이 그린다. 여기에는 iOS 에만 있는 일
// (위치 · 걸음 · 백그라운드 러닝 · Live Activity · 푸시 · 로그인)이 붙는다 — docs/ios/README.md.
@main
struct StepUpApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeRoot()
                .ignoresSafeArea()
        }
    }
}

/// Kotlin 쪽 맨 위 화면(`StepUpRoot`)을 SwiftUI 안에 띄운다.
struct ComposeRoot: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
