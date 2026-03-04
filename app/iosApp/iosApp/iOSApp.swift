import SwiftUI
import ComposeAppFramework

@main
struct iOSApp: App {
    
    init() {
        let commonAppLauncher = SharedCommonAppLauncher()
        #if DEBUG
        commonAppLauncher.launch(debug: true, koinConfig: nil)
        #else
        commonAppLauncher.launch(debug: false, koinConfig: nil)
        #endif
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
