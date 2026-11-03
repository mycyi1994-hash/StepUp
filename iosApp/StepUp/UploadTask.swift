import BackgroundTasks
import StepUpKit

/// 쌓인 러닝을 올리는 백그라운드 작업 — 안드로이드 WorkManager 일꾼 대신. 규칙은 Kotlin(IosUploads → 공용 UploadPass).
/// iOS 가 네트워크가 될 때 깨운다(시각은 iOS 가 정한다). 앱이 앞에 올 때도 한 차례 돈다(StepUpApp).
enum UploadTask {
    static let identifier = "com.stepupcrew.stepup.upload"

    /// 앱이 켜질 때 한 번(시작 직후에만 등록할 수 있다)
    static func register() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: identifier, using: nil) { task in
            guard let task = task as? BGProcessingTask else { return }
            handle(task)
        }
    }

    /// 앱이 뒤로 갈 때 — 다음 차례를 맡겨 둔다
    static func schedule() {
        let request = BGProcessingTaskRequest(identifier: identifier)
        request.requiresNetworkConnectivity = true
        request.requiresExternalPower = false
        try? BGTaskScheduler.shared.submit(request)
    }

    private static func handle(_ task: BGProcessingTask) {
        schedule()
        task.expirationHandler = { IosUploads.shared.cancel() }
        IosUploads.shared.run { done in
            task.setTaskCompleted(success: done.boolValue)
        }
    }
}
