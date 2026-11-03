import AuthenticationServices
import StepUpKit
import UIKit
#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

/// iOS 로그인 창 — Kotlin 의 `IosSignInUi` 약속을 채운다(nonce · Supabase 로그인 · 키체인 보관 · Apple 토큰 철회는 Kotlin `IosAuth`).
/// Apple: AuthenticationServices. 구글: GoogleSignIn SDK(Info.plist 의 GIDClientID 가 비면 "not_configured").
final class SignInUi: NSObject, IosSignInUi {
    private var appleDone: ((String?, String?, String?) -> Void)?

    func apple(hashedNonce: String, onDone: @escaping (String?, String?, String?) -> Void) {
        // 앞 요청이 끝나지 않았으면 그 요청은 취소로 닫는다(답이 두 번 가지 않게)
        appleDone?(nil, nil, "cancelled")
        appleDone = onDone
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.email]
        request.nonce = hashedNonce
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    func google(hashedNonce: String, onDone: @escaping (String?, String?) -> Void) {
        #if canImport(GoogleSignIn)
        guard let clientId = Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String, !clientId.isEmpty,
              let top = Self.topViewController() else {
            onDone(nil, "not_configured")
            return
        }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientId)
        // 구글 iOS SDK 의 토큰에는 우리 nonce 를 넣지 않는다(Supabase iOS 안내와 같다) — 서버에도 nonce 없이 보낸다(IosAuth)
        GIDSignIn.sharedInstance.signIn(withPresenting: top, hint: nil, additionalScopes: nil) { result, error in
            if let error = error as NSError? {
                onDone(nil, error.code == GIDSignInError.canceled.rawValue ? "cancelled" : error.localizedDescription)
                return
            }
            onDone(result?.user.idToken?.tokenString, nil)
        }
        #else
        onDone(nil, "not_configured")
        #endif
    }

    private func finishApple(_ token: String?, _ code: String?, _ error: String?) {
        let done = appleDone
        appleDone = nil
        done?(token, code, error)
    }

    static func topViewController() -> UIViewController? {
        let window = UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow }
            .first
        var top = window?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}

extension SignInUi: ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential else {
            finishApple(nil, nil, "Apple 로그인 응답을 읽지 못했어요")
            return
        }
        let token = credential.identityToken.flatMap { String(data: $0, encoding: .utf8) }
        let code = credential.authorizationCode.flatMap { String(data: $0, encoding: .utf8) }
        finishApple(token, code, nil)
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        let canceled = (error as? ASAuthorizationError)?.code == .canceled
        finishApple(nil, nil, canceled ? "cancelled" : error.localizedDescription)
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow }
            .first ?? ASPresentationAnchor()
    }
}
