import SwiftUI
import ComposeAppFramework

@main
struct iOSApp: App {
    
    init() {
        #if DEBUG
        CommonAppLauncher().launch(debug: true, koinConfig: nil)
        #else
        CommonAppLauncher().launch(debug: false, koinConfig: nil)
        #endif
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
